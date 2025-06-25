package com.starise.ipn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starise.ipn.Util.AXCaller;
import com.starise.ipn.Util.AXWorker;
import com.starise.ipn.dto.AccountsDto;
import com.starise.ipn.entity.IpnLog;
import com.starise.ipn.entity.MpesaRequest;
import com.starise.ipn.entity.TenantEntity;
import com.starise.ipn.entity.TenantIdEntity;
import com.starise.ipn.model.*;
import com.starise.ipn.repository.TenantRepository;
import com.starise.ipn.sms.ProcessAlert;
import okhttp3.*;
import okhttp3.MediaType;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;

import static com.starise.ipn.Util.Logger.logEvent;

@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class IpnService
{
    private static final Logger logger = LoggerFactory.getLogger(IpnService.class);
    private static final Map<String, Lock> transactionLocks = new ConcurrentHashMap<>();

    private final AccountService accountService;
    private final TenantRepository tenantRepository;
    private final ProcessAlert processAlert;
    private final AXWorker axWorker;
    private final IpnLogService ipnLogService;
    private AXCaller caller;

    AccountsDto accountEntity = new AccountsDto();

    @Autowired
    public IpnService(AccountService accountService, TenantRepository tenantRepository,
                      ProcessAlert processAlert,
                      AXWorker axWorker,
                      IpnLogService ipnLogService,
                      AXCaller caller)
    {
        this.accountService = accountService;
        this.tenantRepository = tenantRepository;
        this.processAlert = processAlert;
        this.axWorker = axWorker;
        this.ipnLogService = ipnLogService;
        this.caller = caller;
    }

    public static boolean isBlank(Object object)
    {
        return object == null || "{}".equals(String.valueOf(object).trim()) || "[]".equals(String.valueOf(object).trim()) || String.valueOf(object).trim().isEmpty() || "null".equals(String.valueOf(object).trim()) || String.valueOf(object).trim().toLowerCase().contains("---select");
    }

    private static String getNodeValue(JsonNode node, String fieldName)
    {
        JsonNode valueNode = node.path(fieldName);
        return valueNode.isMissingNode() ? "Field not found" : valueNode.asText();
    }

    public TenantIdEntity validateIdNo(MpesaRequest mpesaRequest)
    {

        TenantIdEntity tenantDto = new TenantIdEntity();
        OkHttpClient client = new OkHttpClient();
        try
        {
            logger.info("request ::=>{}", mpesaRequest);

            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(axWorker.credentials.getBytes());

            String clientNo = mpesaRequest.getBillRefNumber();
            String bankCode = "";
            long clientCode = 0L;
            String accountType = "";

            if (clientNo.contains("#"))
            {
                String[] parts = clientNo.split("#");
                if (parts.length == 2)
                {
                    String bankCodeAndClientCode = parts[0];
                    accountType = parts[1];
                    // Assuming the bank code is always the first two characters
                    bankCode = mpesaRequest.getBusinessShortCode();//bankCodeAndClientCode.substring(0, 2);
                    clientNo = bankCodeAndClientCode;//bankCodeAndClientCode.substring(2);
                    clientCode = Long.parseLong(clientNo);
                    tenantDto.setId(Long.valueOf(clientNo));
                }
                else
                {
                    logger.info("Invalid clientNo format.");
                    tenantDto.setId(0L);
                }
            }
            else
            {
                bankCode = clientNo.substring(0, 2);
                clientNo = clientNo.substring(2);
            }

            logger.info("Bank Code: {}", bankCode);
            logger.info("Client Code: {}", clientCode);
            logger.info("Account Type: {}", accountType);

            logger.info("search client id ::=>{}", clientNo);

            if (!isBlank(tenantDto.getId()))
            {
                String urlEndpoint = "clients/" + tenantDto.getId();
                logger.info("url::=>{}{}", axWorker.externalUrl, urlEndpoint);

                // Build the request
                Request request = new Request.Builder()
                        .url(axWorker.externalUrl.concat(urlEndpoint))
                        .addHeader("Authorization", basicAuth)
                        .addHeader("Fineract-Platform-TenantId", axWorker.tenantId)
                        .build();

                // Send the request and get the response
                try (Response response = client.newCall(request).execute())
                {
                    String responseBody = response.body().string();
                    logger.info("API Response {}", responseBody);
                    boolean isResponseSuccessful = response.isSuccessful();
                    if (!response.isSuccessful())
                    {
                        tenantDto.setId(0L);
                        logger.info("url::=>Error on api response");
                        throw new IOException("Unexpected code " + response);
                        //  return tenantDto;
                    }

                    if (responseBody.isEmpty())
                    {
                        throw new IOException("Response body is empty");
                    }

                    // Parse the JSON response
                    try
                    {
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode rootNode = objectMapper.readTree(responseBody);
                        // Convert JsonNode to JSONObject
                        JSONObject jsonObject = new JSONObject(rootNode.toString());

                        // Extract specific fields from JSONObject
                        Long id = Long.valueOf(jsonObject.optString("id"));
                        String accountNo = jsonObject.optString("accountNo");
                        String statusValue = jsonObject.getJSONObject("status").optString("value");
                        boolean active = jsonObject.optBoolean("active");
                        String displayName = jsonObject.optString("displayName");
                        String mobileNo = jsonObject.optString("mobileNo");
                        String externalId = jsonObject.optString("externalId");

                        tenantDto.setId(id);
                        // Print the extracted fields
                        logger.info("Account No: {}", accountNo);
                        logger.info("Status Value: {}", statusValue);
                        logger.info("Active: {}", active);
                        logger.info("Display Name: {}", displayName);
                        logger.info("Mobile No: {}", mobileNo);
                        logger.info("External Id: {}", externalId);

                        if (!active)
                        {
                            tenantDto.setId(0L);
                        }
                    } catch (IOException | NumberFormatException | JSONException e)
                    {
                        throw new RuntimeException(e);
                    }
                } catch (Exception ex)
                {
                    //logger.error("No tenant Found " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
            else
            {
                logger.error("No tenant Found");
                tenantDto.setId(0L);
            }
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return tenantDto;
    }

    private void populateAccountEntity(AccountInfo accountInfo)
    {
        getAccountEntity().setAccount_no(accountInfo.getAccountNo());
        getAccountEntity().setId((long) accountInfo.getId());
        getAccountEntity().setProductName(accountInfo.getProductName());
        getAccountEntity().setProductPrefix(accountInfo.getShortProductName());
        getAccountEntity().setAccount_balance(accountInfo.getAccountBalance());
    }


    public AccountsDto getAccounts(Long clientID, String acctPrefix)
    {
        setAccountEntity(new AccountsDto());
        OkHttpClient client = new OkHttpClient();

        if (clientID == null || clientID == 0L)
        {
            throw new RuntimeException("No record Found");
        }

        String getLoanEndpoint = "clients/" + clientID + "/accounts";
        logger.info("Fetching accounts from endpoint::=> {}{}", axWorker.externalUrl, getLoanEndpoint);
        String basicAuth = Credentials.basic(axWorker.username, axWorker.password);

        Request request = new Request.Builder()
                .url(axWorker.externalUrl.concat(getLoanEndpoint))
                .get()
                .addHeader("Authorization", basicAuth)
                .addHeader("Fineract-Platform-TenantId", axWorker.tenantId)
                .build();

        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful())
            {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            logger.info("Accounts API Response::=> {}", responseBody);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            // Step 1: Process loan accounts if acctPrefix is not SV
            if (!acctPrefix.equalsIgnoreCase("SV") && isLoan(acctPrefix))
            {
                List<AccountInfo> activeLoanSavingsAccounts = accountService.extractLoanAccounts(jsonResponse.toString());
                AccountInfo prioritizedAccount = null;

                for (AccountInfo accountInfo : activeLoanSavingsAccounts)
                {
                    getAccountEntity().setLoan(true);

                    // Step 1.1: Exact match for `acctPrefix`
                    if (accountInfo.getShortProductName().equalsIgnoreCase(acctPrefix)
                            && accountInfo.getStatus().equalsIgnoreCase("Active"))
                    {
                        logger.error(" PROCESSING Loan For Passed Prefix: client ID {}, account prefix {}", clientID, acctPrefix);
                        populateAccountEntity(accountInfo);
                        return getAccountEntity(); // Found exact match, exit early
                    }

                    // Step 1.2: Prioritize `EM` accounts
                    if (prioritizedAccount == null
                            && accountInfo.getShortProductName().equalsIgnoreCase("EM")
                            && accountInfo.getStatus().equalsIgnoreCase("Active"))
                    {
                        logger.error(" PROCESSING Loan For EM: client ID {}, account prefix {}", clientID, acctPrefix);
                        prioritizedAccount = accountInfo; // Store `EM` account
                    }

                    // Step 1.3: Fallback to the first active account
                    if (prioritizedAccount == null && accountInfo.getStatus().equalsIgnoreCase("Active"))
                    {
                        logger.error("This is the fall back: client ID {}, account prefix {}", clientID, acctPrefix);
                        prioritizedAccount = accountInfo; // Store the first active account as fallback
                    }
                }

                // Step 1.4: Use prioritized account if it exists
                if (prioritizedAccount != null)
                {
                    populateAccountEntity(prioritizedAccount);
                    logger.error("This is the prioritized account Client ID {}, PriorityEntity{}", clientID, getAccountEntity().toString());
                    return getAccountEntity();
                }
            }

            // Step 2: Process savings accounts if no loans found or acctPrefix is `SV`
            if (acctPrefix.equalsIgnoreCase("SV") || getAccountEntity().getAccount_no() == null)
            {
                logger.error(" PROCESSING DEPOSIT: client ID {}, account prefix {}", clientID, acctPrefix);

                List<AccountInfo> activeSavingsAccounts = accountService.extractSavingsAccounts(jsonResponse.toString());
                for (AccountInfo accountInfo : activeSavingsAccounts)
                {
                    if (accountInfo.getStatus().equalsIgnoreCase("Active")
                            && !isLoan(accountInfo.getShortProductName()))
                    {
                        populateAccountEntity(accountInfo);
                        getAccountEntity().setLoan(false);
                        return getAccountEntity(); // Exit after finding the first active savings account
                    }
                }
            }

            logger.info("Fetched account::=> {}", getAccountEntity());

        } catch (Exception e)
        {
            logger.error("Error fetching accounts: {}", e.getMessage(), e);
        }

        // Step 3: Validate and return account entity (if no loans or savings found)
        return getAccountEntity(); //validateTxn(clientID, acctPrefix, getAccountEntity());
    }


    private AccountsDto validateTxn(Long clientID, String acctPrefix, AccountsDto accountEntity)
    {

        if (isLoan(acctPrefix) && isBlank(accountEntity.getId()))
        {
            logger.error("NO LOANS FOUND, PROCESSING DEPOSIT: client ID {}", clientID);
            return getAccounts(clientID, "SV");
        }
        else
        {
            return accountEntity;
        }

    }

    private boolean isLoan(String productPrefix)
    {
        return productPrefix.equalsIgnoreCase("BL")
                || productPrefix.equalsIgnoreCase("EM")
                || productPrefix.equalsIgnoreCase("EL")
                || productPrefix.equalsIgnoreCase("ML")
                || productPrefix.equalsIgnoreCase("SB");
    }

    private List<Integer> getOffice()
    {
        List<Integer> ids = new ArrayList<>();
        OkHttpClient client = getUnsafeOkHttpClient();
        String customerEndpoint = "offices";
        Request request = new Request.Builder()
                .url(axWorker.externalUrl.concat(customerEndpoint))
                .addHeader("Authorization", basicAuthToken())
                .addHeader("Fineract-Platform-TenantId", axWorker.tenantId)
                .build();

        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful())
            {
                throw new IOException("Unexpected code " + response);
            }

            String jsonString = response.body().string();
            try
            {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(jsonString);
                for (JsonNode node : rootNode)
                {
                    ids.add(node.get("id").asInt());
                }

            } catch (IOException e)
            {
                e.printStackTrace();
            }

        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return ids;
    }

    private String basicAuthToken()
    {
        return "Basic " + Base64.getEncoder().encodeToString(axWorker.credentials.getBytes());
    }

    private synchronized Long searchCustomerById(String idNo)
    {
        TenantEntity tenant = tenantRepository.fetchByExternalId(idNo);
        return tenant.getId();
    }

    private boolean hasSuffix(String clientId)
    {
        return clientId.toLowerCase().endsWith("bl")
                || clientId.toLowerCase().endsWith("el")
                || clientId.toLowerCase().endsWith("em")
                || clientId.toLowerCase().endsWith("sv")
                || clientId.toLowerCase().endsWith("sb");
    }

    private String[] splitClientId(String clientId)
    {
        String regex = "(\\d+)([a-zA-Z]+)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(clientId);

        // Check if the pattern matches and extract parts
        if (matcher.matches())
        {
            String numberPart = matcher.group(1); // Numeric part
            String letterPart = matcher.group(2).toUpperCase(); // Alphabetic part
            return new String[]{numberPart, letterPart};
        }

        return null;
    }

    public void writeToLog(AXCaller caller)
    {
        logEvent("<txn>" + "\r\n" + caller + "\r\n" + "</txn>");

    }

    // @Async
    @Async("customExecutor")
    public CompletableFuture<TenantIdEntity> processMpesaTxnAsync(MpesaRequest mpesaRequest)
    {
        long startTime = System.currentTimeMillis();
        caller = new AXCaller();
        return CompletableFuture.supplyAsync(() -> {
            try
            {
                caller.setCall("mpesaRequest", mpesaRequest);
                return postWithIdNo(mpesaRequest, caller);
            } catch (Exception e)
            {
                logger.error("Error processing transaction", e);
                caller.addError(e);
                handleIpnRequest(mpesaRequest, "F");
                throw new RuntimeException(e); // Propagate the exception
            } finally
            {
                caller.setDuration( System.currentTimeMillis()-startTime +" Ms");
                writeToLog(caller);
            }
        });
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TenantIdEntity postWithIdNo(MpesaRequest mpesaRequest, AXCaller caller) throws Exception
    {

        TenantIdEntity tenantDto = new TenantIdEntity();

        logger.info("============================== Start Processing ============================== \n {}", mpesaRequest.toString());
        String lockKey = mpesaRequest.getBillRefNumber() + "_" + mpesaRequest.getTransID();
        Lock lock = transactionLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        caller.setCall("start Processing", lockKey);

        if (mpesaRequest.getTransID() == null || mpesaRequest.getBillRefNumber() == null)
        {
            throw new IllegalArgumentException("Missing transaction identifiers");
        }

        try
        {
            lock.lock();
            caller.setCall("lock", lock);
            caller.setCall("activeLocks", transactionLocks.size());
            logger.info("Active locks: {}", transactionLocks.size());
            logger.info("Processing transaction {} for client {} on lock {}",
                    mpesaRequest.getTransID(),
                    mpesaRequest.getBillRefNumber(),
                    lockKey);

            Long customerId;
            AlertRequest alertRequest = new AlertRequest();
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(axWorker.credentials.getBytes());

            handleIpnRequest(mpesaRequest, "P");

            logger.info("postWithIdNo::=> {}", mpesaRequest.toString());


            String clientNo = mpesaRequest.getBillRefNumber().trim();
            String bankCode = mpesaRequest.getBusinessShortCode();
            String mpesaReceiptNo = mpesaRequest.getTransID();
            String mpesaSender = mpesaRequest.getFirstName();
            String mpesaTransTime = formatMpesaDate(mpesaRequest.getTransTime());
            BigDecimal mpesaAmount = new BigDecimal(mpesaRequest.getTransAmount());
            caller.setCall("clientNo", clientNo);


            long clientCode = 0L;
            String accountType = "";
            logger.info("clientNo: {}", clientNo);
            if (hasSuffix(clientNo))
            {
                String[] parts = splitClientId(clientNo);

                if (parts != null && parts.length == 2)
                {
                    String clientCodePart = parts[0].trim();
                    accountType = parts[1].trim();
                    if (accountType.equalsIgnoreCase("EL"))
                    {
                        accountType = "EM";
                    }
                    logger.info("clientNo: {}, account type: {}", clientNo, accountType);

                    clientNo = clientCodePart; //bankCodeAndClientCode.substring(2);
                    clientCode = searchCustomerById(clientNo); //getIdbyExternalId(clientNo);//Long.parseLong(clientNo);
                    tenantDto.setId(clientCode);
                }
                else
                {
                    logger.info("clientNo: {}", clientNo);
                    logger.info("Invalid clientNo format.");
                    tenantDto.setId(0L);

                }
            }
            else
            {
                logger.info("clientNo: {}", clientNo);
                clientCode = searchCustomerById(clientNo); //getIdbyExternalId(clientNo);
                tenantDto.setId(clientCode);
                accountType = "EM";
            }

            logger.info("biller ref: {}", mpesaRequest.getBillRefNumber());
            logger.info("Bank Code: {}", bankCode);
            logger.info("Client Code: {}", clientCode);
            logger.info("Account Type: {}", accountType);

            caller.setReference(lockKey);
            caller.setNarration("mpesa processing for " + clientCode);
            caller.setCall("tenantId", tenantDto.getId());

            //check if Id exists
            OkHttpClient client = getUnsafeOkHttpClient();

            if (!isBlank(tenantDto.getId()) && tenantDto.getId() > 0)
            {
                String urlEndpoint = "clients/" + tenantDto.getId();
                logger.info("client url::=>{}{}", axWorker.externalUrl, urlEndpoint);
                caller.setCall("getTenantreq", urlEndpoint);

                // Build the request
                Request request = new Request.Builder()
                        .url(axWorker.externalUrl.concat(urlEndpoint))
                        .addHeader("Authorization", basicAuth)
                        .addHeader("Fineract-Platform-TenantId", axWorker.tenantId)
                        .build();

                try (Response response = client.newCall(request).execute())
                {
                    String responseBody = response.body().string();
                    caller.setCall("getTenantres", responseBody);
                    logger.info("get tenant Response {}", responseBody);

                    if (!response.isSuccessful())
                    {
                        tenantDto.setId(0L);
                        logger.info("Error on API response");
                        throw new IOException("Unexpected code " + response);
                    }

                    if (responseBody.isEmpty())
                    {
                        throw new IOException("Response body is empty");
                    }

                    try
                    {
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode rootNode = objectMapper.readTree(responseBody);
                        JSONObject jsonObject = new JSONObject(rootNode.toString());

                        Long id = Long.valueOf(jsonObject.optString("id"));
                        String accountNo = jsonObject.optString("accountNo");
                        String statusValue = jsonObject.getJSONObject("status").optString("value");
                        boolean active = jsonObject.optBoolean("active");
                        String displayName = jsonObject.optString("displayName");
                        String mobileNo = formatMobileNumber(jsonObject.optString("mobileNo"));
                        String externalId = jsonObject.optString("externalId");

                        //set alert details
                        alertRequest.setAccountNo(accountNo);
                        alertRequest.setMobileNo(mobileNo);
                        alertRequest.setReceipt(mpesaReceiptNo);
                        alertRequest.setClientName(displayName);
                        alertRequest.setSenderName(mpesaSender);

                        alertRequest.setTxnDate(mpesaTransTime);
                        tenantDto.setId(id);

                        if (!active)
                        {
                            tenantDto.setId(0L);
                        }
                        else
                        {
                            Long tenatId = tenantDto.getId();
                            customerId = tenatId;
                            AccountsDto tenant = getAccounts(tenatId, accountType); // get the acounts based on suffix
                            caller.setCall("tenantAccounts", tenant.toString());

                            if (isBlank(tenant.getId()))
                            {
                                logger.info("Found no Loans for EM::=> resolve to process Deposit {}", tenatId);
                                caller.setCall("NoLoanAccounts", "Found no Loans for Loan::=> resolve to process Deposit " + tenant);
                                tenant = getAccounts(tenatId, "SV");
                            }

                            alertRequest.setBalance(isBlank(tenant.getAccount_balance()) ? BigDecimal.ZERO : BigDecimal.valueOf(tenant.getAccount_balance()).subtract(mpesaAmount));
                            logger.info("ACCOUNT Balance {}", alertRequest.getBalance());
                            caller.setCall("accountBalance", alertRequest.getBalance());


                            String txnDate = new SimpleDateFormat("dd MMM yyyy").format(new Date());
                            if (isLoan(tenant.getProductPrefix()) && tenant.isLoan())
                            {
                                logger.info("================ START PROCESSING LOANS {} - {} ================", tenant.getProductPrefix(), tenant.getAccount_no());
                                caller.setCall("processLoan", tenant.getProductPrefix() + " " + tenant.getAccount_no());
                                caller.setAccount(tenant.getAccount_no());

                                BigDecimal loanAmount = fetchLoanAmount(tenant.getId());
                                BigDecimal repaymentAmount = new BigDecimal(mpesaRequest.getTransAmount());
                                BigDecimal excessPayment = BigDecimal.ZERO;
                                alertRequest.setAmount(repaymentAmount); // ensure sms for the loans reads proper amount

                                caller.setCall("loanAmount", loanAmount);
                                caller.setCall("repaymentAmount", repaymentAmount);


                                if (repaymentAmount.compareTo(loanAmount) > 0)
                                {
                                    excessPayment = repaymentAmount.subtract(loanAmount);
                                    repaymentAmount = loanAmount;


                                    logger.info("=============================Excess amount evaluation =================================");
                                    logger.info("Repayment amount {}", repaymentAmount);
                                    logger.info("Excess amount for SV {}", excessPayment);
                                    logger.info("customer id {}", customerId);
                                    logger.info("=============================Excess amount evaluation end =================================");
                                }
                                caller.setCall("excessPayment", excessPayment);
                                caller.setCall("repaymentAmount", repaymentAmount);

                                alertRequest.setMessageType("LN");
                                String loanUrlEndpoint = "loans/" + tenant.getId() + "/transactions?command=repayment";
                                logger.info("urlEndpoint::=> {}", axWorker.externalUrl + loanUrlEndpoint);
                                String mpesaDate = axWorker.convertMifosDate(mpesaRequest.getTransTime());
                                caller.setCall("loanRequest", loanUrlEndpoint);
                                caller.setCall("mpesaDate", mpesaDate);

                                // Construct JSON request body
                                Map<String, Object> requestBody = new HashMap<>();
                                requestBody.put("dateFormat", "dd MMMM yyyy");
                                requestBody.put("locale", "en");
                                requestBody.put("transactionDate", txnDate); //txnDate//mpesaDate
                                requestBody.put("transactionAmount", repaymentAmount.toPlainString());
                                requestBody.put("paymentTypeId", "2");
                                requestBody.put("note", "mpesa loan payment");
                                requestBody.put("accountNumber", tenant.getAccount_no());
                                requestBody.put("checkNumber", mpesaRequest.getBillRefNumber());
                                requestBody.put("routingCode", mpesaRequest.getFirstName());
                                requestBody.put("receiptNumber", mpesaRequest.getTransID());
                                requestBody.put("bankNumber", "mpesa");

                                // Convert requestBody map to JSON string
                                ObjectMapper loanObjectMapper = new ObjectMapper();
                                String jsonBody = loanObjectMapper.writeValueAsString(requestBody);

                                logger.info("loan Request to CBS::=> {}", jsonBody);
                                caller.setCall("loanRequestToCbsreq", requestBody.toString());

                                RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
                                Request loanRequest = new Request.Builder()
                                        .url(axWorker.externalUrl.concat(loanUrlEndpoint))
                                        .post(body)
                                        .addHeader("Authorization", Credentials.basic(axWorker.username, axWorker.password))
                                        .addHeader("Fineract-Platform-TenantId", axWorker.tenantId)
                                        .addHeader("Content-Type", "application/json")
                                        .addHeader("ACCEPT", "application/json")
                                        .build();

                                try (Response loanResponse = client.newCall(loanRequest).execute())
                                {
                                    caller.setCall("loanRequestToCbsRes", loanResponse.body().string());
                                    if (!loanResponse.isSuccessful())
                                    {
                                        handleIpnRequest(mpesaRequest, "F");
                                        throw new IOException("Unexpected code " + loanResponse);
                                    }

                                    handleIpnRequest(mpesaRequest, "S");
                                    caller.setCall("handleIpnRequest", "Success");
                                    caller.setCall("alertRequest", alertRequest.toString());
                                    if (processAlert.sendAlert(alertRequest, caller))
                                    {
                                        caller.setCall("smsreceivingNo", alertRequest.getMobileNo());
                                        logger.info("Loan SMS sent to::=> {}", alertRequest.getMobileNo());
                                    }
                                }

                                logger.info("=============== PREPARE TO PROCESS SAVINGS FOR EXCESS PAYMENT OF {} FOR {} ", excessPayment, customerId);
                                logger.info("Account has excess payment process deposit::=> {}, amount greater than zero {}", excessPayment, excessPayment.compareTo(BigDecimal.ZERO));
                                caller.setCall("smsSent", alertRequest.getMobileNo());
                                if (excessPayment.compareTo(BigDecimal.ZERO) > 0)
                                {
                                    caller.setCall("ExcessAmountFound", "Found Excess payment for::=> " + tenantDto.getId() + " " + txnDate + " " + excessPayment + " " + tenant.getAccount_no() + " " + mpesaRequest.getBillRefNumber() + " " + mpesaRequest.getFirstName() + " " + mpesaRequest.getTransID() + " " + alertRequest.toString() + " " + mpesaRequest.toString());
                                    logger.info("Found Excess payment for::=> {}, {}, {}, {}, {}, {}, {}, {}, {}", tenantDto.getId(), txnDate, excessPayment, tenant.getAccount_no(), mpesaRequest.getBillRefNumber(), mpesaRequest.getFirstName(), mpesaRequest.getTransID(), alertRequest.toString(), mpesaRequest.toString());
                                    if (processToSavings(customerId, txnDate, excessPayment.toPlainString(), tenant.getAccount_no(), mpesaRequest.getBillRefNumber(), mpesaRequest.getFirstName(), mpesaRequest.getTransID(), alertRequest, mpesaRequest, caller))
                                    {
                                        caller.setCall("processedSavingForExcessAmount", "True");
                                        logger.info("Processing to savings account for account {} was successful for  amount::=> {}", tenant.getId(), excessPayment.toPlainString());
                                    }
                                }
                            }
                            else
                            {
                                logger.info("================ START PROCESSING DEPOSITS {} - {} ================", tenant.getProductPrefix(), tenant.getAccount_no());
                                alertRequest.setMessageType("SV");
                                alertRequest.setAmount(new BigDecimal(mpesaRequest.getTransAmount()));

                                String savingsUrlEndpoint = "savingsaccounts/" + tenant.getId() + "/transactions?command=deposit";
                                logger.info("SV urlEndpoint::=> {}", axWorker.externalUrl + savingsUrlEndpoint);
                                caller.setCall("processingSavings", savingsUrlEndpoint);

                                Map<String, Object> requestBody = new HashMap<>();
                                requestBody.put("dateFormat", "dd MMMM yyyy");
                                requestBody.put("locale", "en");
                                requestBody.put("transactionDate", txnDate);
                                requestBody.put("transactionAmount", mpesaRequest.getTransAmount());
                                requestBody.put("paymentTypeId", "2"); //2 =Paybill Collection Account
                                // requestBody.put("note", "mpesa Deposit");
                                requestBody.put("accountNumber", tenant.getAccount_no());
                                requestBody.put("checkNumber", mpesaRequest.getBillRefNumber());
                                requestBody.put("routingCode", mpesaRequest.getFirstName());
                                requestBody.put("receiptNumber", mpesaRequest.getTransID());
                                requestBody.put("bankNumber", "mpesa");

                                // Convert requestBody map to JSON string
                                ObjectMapper loanObjectMapper = new ObjectMapper();
                                String jsonBody = loanObjectMapper.writeValueAsString(requestBody);

                                logger.info("Deposit Request to CBS::=> {}", jsonBody);
                                caller.setCall("SavingsToCBSreq", jsonBody);

                                RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
                                Request loanRequest = new Request.Builder()
                                        .url(axWorker.externalUrl.concat(savingsUrlEndpoint))
                                        .post(body)
                                        .addHeader("Authorization", Credentials.basic(axWorker.username, axWorker.password))
                                        .addHeader("Fineract-Platform-TenantId", axWorker.tenantId)
                                        .addHeader("Content-Type", "application/json")
                                        .addHeader("ACCEPT", "application/json")
                                        .build();

                                try (Response savingsResponse = client.newCall(loanRequest).execute())
                                {
                                    caller.setCall("SavingsToCBSreqs", savingsResponse.body().string());
                                    boolean isTxnSuccessful = savingsResponse.isSuccessful();
                                    logger.info("is txn successful::=> {}", isTxnSuccessful);
                                    if (!isTxnSuccessful)
                                    {
                                        handleIpnRequest(mpesaRequest, "F");
                                        throw new IOException("Unexpected code " + savingsResponse);
                                    }

                                    handleIpnRequest(mpesaRequest, "S");
                                    if (processAlert.sendAlert(alertRequest, caller))
                                    {
                                        logger.info("saving SMS sent to::=> {}", alertRequest.getMobileNo());
                                    }
                                }
                            }
                            caller.setCall("completed", "====TXN COMPLETED====");

                            logger.info("tenantDto to CBS::=> {}", tenantDto.toString());
                        }
                    } catch (JSONException | NumberFormatException | IOException e)
                    {
                        caller.addError(e);
                        tenantDto.setId(0L);
                        handleIpnRequest(mpesaRequest, "F");
                        logger.error("No tenant Found {}", e.getMessage());

                    }

                } catch (Exception ex)
                {
                    caller.addError(ex);
                    tenantDto.setId(0L);
                    handleIpnRequest(mpesaRequest, "F");
                    logger.error("Main error {}", ex.getMessage());
                }

            }
            else
            {
                caller.setCall("Error", "No tenants Found");
                logger.error("No tenants Found");
                tenantDto.setId(0L);
                handleIpnRequest(mpesaRequest, "F");
            }

            logger.info("Active locks: {}", transactionLocks.size());
            return tenantDto;
        } finally
        {
            lock.unlock();
            transactionLocks.remove(lockKey); // Clean up
            logger.info("released Lock: {}", lockKey);
            logger.info("Active locks : {}", transactionLocks.size());
            caller.setCall("lockReleased", lockKey);
            caller.setCall("activeLocks", transactionLocks.size());
            caller.setCall("completed", "====OUT====");

        }
    }

    private boolean processToSavings(Long clientID, String txnDate, String txnAmt, String tenantAcctNo,
                                     String billerRefNo, String firstName, String transId,
                                     AlertRequest alertRequest, MpesaRequest mpesaRequest, AXCaller caller)
    {
        caller.setCall("processingExcess",clientID+": "+txnAmt+": "+tenantAcctNo);
        try
        {
            AccountsDto tenant = getAccounts(clientID, "SV");
            logger.error("+++++++++ CALLING DEPOSIT TRANSACTION FOR EXCESS AMOUNT ++++++++++++++ ");
            logger.error("passed Client if {} ", clientID);
            logger.error("tenantSvId {} ", tenant.getId());
            logger.error("txnDate {} ", txnDate);
            logger.error("txnAmt {} ", txnAmt);
            logger.error("billerRefNo {} ", billerRefNo);
            logger.error("firstName {} ", firstName);
            logger.error("alertRequest {} ", alertRequest);
            logger.error("mpesaRequest {} ", mpesaRequest);
            logger.error("++++++++++++++++++++END LOG DP ++++++++++++++ ");

            OkHttpClient client = getUnsafeOkHttpClient();

            alertRequest.setMessageType("SV");

            String savingsUrlEndpoint = "savingsaccounts/" + tenant.getId() + "/transactions?command=deposit";
            logger.info("process dp urlEndpoint::=> {}", axWorker.externalUrl + savingsUrlEndpoint);
            caller.setCall("processingSavingsExcess", savingsUrlEndpoint);
            // Construct JSON request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("dateFormat", "dd MMMM yyyy");
            requestBody.put("locale", "en");
            requestBody.put("transactionDate", txnDate);
            requestBody.put("transactionAmount", txnAmt);
            requestBody.put("paymentTypeId", "2"); //2 =Paybill Collection Account
            requestBody.put("accountNumber", tenantAcctNo);
            requestBody.put("checkNumber", billerRefNo);
            requestBody.put("routingCode", firstName);
            requestBody.put("receiptNumber", transId);
            requestBody.put("bankNumber", "mpesa");

            // Convert requestBody map to JSON string
            ObjectMapper loanObjectMapper = new ObjectMapper();
            String jsonBody = loanObjectMapper.writeValueAsString(requestBody);


            logger.info("process Dp Request to CBS::=> {}", jsonBody);
            caller.setCall("ExcessSavingsReq", jsonBody);

            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
            Request loanRequest = new Request.Builder()
                    .url(axWorker.externalUrl.concat(savingsUrlEndpoint))
                    .post(body)
                    .addHeader("Authorization", Credentials.basic(axWorker.username, axWorker.password))
                    .addHeader("Fineract-Platform-TenantId", axWorker.tenantId)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("ACCEPT", "application/json")
                    .build();

            try (Response savingsResponse = client.newCall(loanRequest).execute())
            {
                caller.setCall("savingsExcessRes", savingsResponse.body().string());
                if (!savingsResponse.isSuccessful())
                {
                    throw new IOException("Unexpected code " + savingsResponse);
                }
                handleIpnRequest(mpesaRequest, "S");
                alertRequest.setAmount(new BigDecimal(txnAmt));
                caller.setCall("processingAlertFor Excess",alertRequest);
                if (processAlert.sendAlert(alertRequest, caller))
                {
                    caller.setCall("processedAlert",jsonBody);
                    logger.info("SV SMS sent to::=> {}", alertRequest.getMobileNo());
                }
                return true;
            }
        } catch (Exception ex)
        {
            caller.addError(ex);
            logger.error("process DP error {}", ex.getMessage());
        }
        return false;
    }

    private BigDecimal fetchLoanAmount(Long loanId)
    {
        try
        {
            OkHttpClient client = new OkHttpClient();

            String getLoanEndpoint = "loans/" + loanId;

            logger.info("loans balance ::=> {}{}", axWorker.externalUrl, getLoanEndpoint);
            String basicAuth = Credentials.basic(axWorker.username, axWorker.password);

            Request request = new Request.Builder()
                    .url(axWorker.externalUrl.concat(getLoanEndpoint))
                    .get()
                    .addHeader("Authorization", basicAuth)
                    .addHeader("Fineract-Platform-TenantId", axWorker.tenantId)
                    .build();

            try (Response response = client.newCall(request).execute())
            {
                if (!response.isSuccessful())
                {
                    throw new IOException("Unexpected code " + response);
                }

                String responseBody = response.body().string();
                 logger.info("loans balance API Response::=> {} ", responseBody);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                JsonNode totalOutstandingNode = jsonResponse.path("summary").path("totalOutstanding");

                double totalOutstanding = 0.0;
                if (totalOutstandingNode.isDouble())
                {
                    totalOutstanding = totalOutstandingNode.asDouble();
                }
                else
                {
                    logEvent("Total Outstanding not found or not a double.");
                }

                return BigDecimal.valueOf(totalOutstanding);

            } catch (Exception e)
            {
                caller.addError(e);
                logger.error("Error in fetching loan {}", e.getMessage());
            }
        } catch (Exception e)
        {
            caller.addError(e);
            logger.error("main loans Error in fetching loan {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }


    private JsonNode convertResponseToJson(ResponseEntity<String> response)
    {
        try
        {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(response.getBody());
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public TenantEntity checkClient(MpesaRequest mpesaRequest, String tenantId)
    {

        TenantEntity tenantDto = new TenantEntity();
        String clientNo = mpesaRequest.getBillRefNumber().replace(tenantId, "").substring(1);
        String[] parts = clientNo.split("#");

        if (parts.length == 2)
        {
            String bankCodeAndClientCode = parts[0];
            String accountType = parts[1];

            // Assuming the bank code is always the first two characters
            String bankCode = bankCodeAndClientCode.substring(0, 2);
            String clientCode = bankCodeAndClientCode.substring(2);

            tenantDto.setId(Long.valueOf(clientCode));

            logEvent("Bank Code: " + bankCode);
            logEvent("Client Code: " + clientCode);
            logEvent("Account Type: " + accountType);
        }
        else
        {
            logEvent("Invalid clientNo format.");
        }
        if (!isBlank(tenantDto.getId()))
        {
            // Call external JSON web service
            RestTemplate restTemplate = new RestTemplate();
            String urlEndpoint = "clients/" + tenantDto.getId();
            ResponseEntity<String> response = restTemplate.postForEntity(axWorker.externalUrl + urlEndpoint, tenantDto, String.class);
            return tenantDto;
        }
        else
        {
            throw new RuntimeException("No record Found");
        }

    }

    private String formatMpesaDate(String timestamp)
    {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(timestamp, inputFormatter);
        return dateTime.format(outputFormatter);
    }

    public String formatMobileNumber(String mobileNumber)
    {
        if (mobileNumber.startsWith("+254"))
        {
            return mobileNumber.substring(1); // Remove the "+" from the beginning
        }
        else if (mobileNumber.startsWith("0"))
        {
            return "254" + mobileNumber.substring(1); // Replace the leading "0" with "254"
        }
        else
        {
            return mobileNumber; // Return the number as is if no special case applies
        }
    }

    public void handleIpnRequest(MpesaRequest mpesaRequest, String status)
    {
        // Create or update an IpnLog entity based on the IPN request
        IpnRequest request = new IpnRequest();
        request.setBillRefNumber(mpesaRequest.getBillRefNumber());
        request.setFirstName(mpesaRequest.getFirstName());
        request.setOrgAccountBalance(new BigDecimal(mpesaRequest.getOrgAccountBalance()));
        request.setMsisdn(mpesaRequest.getMsisdn());
        request.setInvoiceNumber(mpesaRequest.getInvoiceNumber());
        request.setTransId(mpesaRequest.getTransID());
        request.setTransAmount(new BigDecimal(mpesaRequest.getTransAmount()));
        request.setTransactionType(mpesaRequest.getTransactionType());
        request.setBusinessShortCode(mpesaRequest.getBusinessShortCode());
        request.setThirdPartyTransId(mpesaRequest.getThirdPartyTransID());
        request.setTransTime(DateConverter(mpesaRequest.getTransTime()));
        request.setStatus(status);


        IpnLog log = new IpnLog();
        log.setTransactionType(request.getTransactionType());
        log.setBillRefNumber(request.getBillRefNumber());
        log.setOrgAccountBalance(request.getOrgAccountBalance());
        log.setMsisdn(request.getMsisdn());
        log.setFirstName(request.getFirstName());
        log.setTransAmount(request.getTransAmount());
        log.setThirdPartyTransId(request.getThirdPartyTransId());
        log.setInvoiceNumber(request.getInvoiceNumber());
        log.setTransId(request.getTransId());
        log.setTransTime(request.getTransTime());
        log.setBusinessShortCode(request.getBusinessShortCode());
        log.setStatus(request.getStatus());

        // Call the insertOrUpdate method
        IpnLog ipnLog = ipnLogService.insertOrUpdate(log);
    }

    public LocalDateTime DateConverter(String dateString)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
        return dateTime;
    }

    private OkHttpClient getUnsafeOkHttpClient()
    {
        try
        {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager()
                    {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType)
                        {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType)
                        {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers()
                        {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            return builder.build();
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public AccountsDto getAccountEntity()
    {
        return accountEntity;
    }

    public void setAccountEntity(AccountsDto accountEntity)
    {
        this.accountEntity = accountEntity;
    }
}
