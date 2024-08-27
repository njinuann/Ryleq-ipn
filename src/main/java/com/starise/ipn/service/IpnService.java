package com.starise.ipn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starise.ipn.dto.AccountsDto;
import com.starise.ipn.dto.TenantDto;
import com.starise.ipn.entity.IpnLog;
import com.starise.ipn.entity.MpesaRequest;
import com.starise.ipn.entity.TenantEntity;
import com.starise.ipn.entity.TenantIdEntity;
import com.starise.ipn.model.*;
//import com.starise.ipn.repository.TenantIdRepository;
//import com.starise.ipn.repository.TenantRepository;
import com.starise.ipn.repository.TenantIdRepository;
import com.starise.ipn.repository.TenantRepository;
import com.starise.ipn.sms.ProcessAlert;
import okhttp3.*;
import okhttp3.MediaType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class IpnService {

    private final AccountService accountService;
    private final TenantIdRepository tenantIdRepository;
    private final TenantRepository tenantRepository;

    AccountsDto accountEntity = new AccountsDto();
    private RestTemplate restTemplate;

    @Autowired
    public IpnService(AccountService accountService, TenantIdRepository tenantIdRepository, TenantRepository tenantRepository) {
        this.accountService = accountService;
        this.tenantIdRepository = tenantIdRepository;
        this.tenantRepository = tenantRepository;
    }

    @Autowired
    private IpnLogService ipnLogService;


    private static final Logger logger = LoggerFactory.getLogger(IpnService.class);

    private String exernalUrl = "https://evolve.bgs.co.ke/fineract-provider/api/v1/";
    private String tenantId = "client_104";
    private String username = "mifos";
    private String password = "mifos123";
    private String credentials = username + ":" + password;


    public static boolean isBlank(Object object) {
        return object == null || "{}".equals(String.valueOf(object).trim()) || "[]".equals(String.valueOf(object).trim()) || "".equals(String.valueOf(object).trim()) || "null".equals(String.valueOf(object).trim()) || String.valueOf(object).trim().toLowerCase().contains("---select");
    }

    private static String getNodeValue(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        return valueNode.isMissingNode() ? "Field not found" : valueNode.asText();
    }

    public TenantIdEntity validateIdNo(MpesaRequest mpesaRequest) {

        TenantIdEntity tenantDto = new TenantIdEntity();
        OkHttpClient client = new OkHttpClient();
        try {
            System.out.println("request ::=> " + mpesaRequest.toString());
            logger.info("request ::=>{}", mpesaRequest.toString());

            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

            String clientNo = mpesaRequest.getBillRefNumber();
            String bankCode = "";
            long clientCode = 0L;
            String accountType = "";

            if (clientNo.contains("#")) {
                String[] parts = clientNo.split("#");
                if (parts.length == 2) {
                    String bankCodeAndClientCode = parts[0];
                    accountType = parts[1];
                    // Assuming the bank code is always the first two characters
                    bankCode = mpesaRequest.getBusinessShortCode();//bankCodeAndClientCode.substring(0, 2);
                    clientNo = bankCodeAndClientCode;//bankCodeAndClientCode.substring(2);
                    clientCode = Long.parseLong(clientNo);
                    tenantDto.setId(Long.valueOf(clientNo));
                } else {
                    logger.info("Invalid clientNo format.");
                    tenantDto.setId(0L);
                }
            } else {
                bankCode = clientNo.substring(0, 2);
                clientNo = clientNo.substring(2);
            }

            logger.info("Bank Code: {}", bankCode);
            logger.info("Client Code: {}", clientCode);
            logger.info("Account Type: {}", accountType);

            logger.info("search client id ::=>{}", clientNo);

            if (!isBlank(tenantDto.getId())) {
                String urlEndpoint = "clients/" + tenantDto.getId();
                System.out.println("url::=>" + exernalUrl + urlEndpoint);
                logger.info("url::=>{}{}", exernalUrl, urlEndpoint);

                // Build the request
                Request request = new Request.Builder()
                        .url(exernalUrl.concat(urlEndpoint))
                        .addHeader("Authorization", basicAuth)
                        .addHeader("Fineract-Platform-TenantId", tenantId)
                        .build();

                // Send the request and get the response
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    logger.info("API Response {}", responseBody);
                    boolean isResponseSuccessful = response.isSuccessful();
                    if (!response.isSuccessful()) {
                        tenantDto.setId(0L);
                        logger.info("url::=>Error on api response");
                        throw new IOException("Unexpected code " + response);
                        //  return tenantDto;
                    }

                    if (responseBody.isEmpty()) {
                        throw new IOException("Response body is empty");
                    }

                    // Parse the JSON response
                    try {
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
                        System.out.println("Account No: " + accountNo);
                        System.out.println("Status Value: " + statusValue);
                        System.out.println("Active: " + active);
                        System.out.println("Display Name: " + displayName);
                        System.out.println("Mobile No: " + mobileNo);
                        System.out.println("External Id: " + externalId);

                        if (!active) {
                            tenantDto.setId(0L);
                        }
                    } catch (IOException | NumberFormatException | JSONException e) {
                        throw new RuntimeException(e);
                    }
                } catch (Exception ex) {
                    //logger.error("No tenant Found " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                logger.error("No tenant Found");
                tenantDto.setId(0L);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return tenantDto;
    }


    public AccountsDto getAcounts(Long clientID, String acctPrefix) {
        System.out.println("clientID ::=> " + clientID);
        setAccountEntity(new AccountsDto());
        OkHttpClient client = new OkHttpClient();

        if (clientID != null && clientID != 0L) {
            String getLoanEndpoint = "clients/" + clientID + "/accounts";
            System.out.println("url::=>" + exernalUrl + getLoanEndpoint);
            logger.info("loans::=> {}{}", exernalUrl, getLoanEndpoint);
            String basicAuth = Credentials.basic(username, password);

            Request request = new Request.Builder()
                    .url(exernalUrl.concat(getLoanEndpoint))
                    .get()
                    .addHeader("Authorization", basicAuth)
                    .addHeader("Fineract-Platform-TenantId", tenantId)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String responseBody = response.body().string();
                System.out.println("API Response: " + responseBody);
                logger.info("Accounts API Response::=> {} ", responseBody);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                //   LoanAccountService service = new LoanAccountService();
//                List<LoanAccount> activeLoanAccounts = service.getActiveLoanAccounts(jsonResponse.toString());
//                List<LoanAccount> activeLSavingsAccounts = service.getActiveLoanAccounts(jsonResponse.toString());


                List<AccountInfo> activeAccounts = accountService.extractAccounts(jsonResponse.toString());
                for (AccountInfo accountInfo : activeAccounts) {
                    if (isLoan(acctPrefix)) {
                        if (accountInfo.getShortProductName().equalsIgnoreCase(acctPrefix) && accountInfo.getStatus().equalsIgnoreCase("Active")) {
                            getAccountEntity().setAccount_no(accountInfo.getAccountNo());
                            getAccountEntity().setId((long) accountInfo.getId());
                            getAccountEntity().setProductName(accountInfo.getProductName());
                            getAccountEntity().setProductPrefix(accountInfo.getShortProductName());
                        } else if (!accountInfo.getShortProductName().equalsIgnoreCase(acctPrefix) && accountInfo.getStatus().equalsIgnoreCase("Active")) {
                            getAccountEntity().setAccount_no(accountInfo.getAccountNo());
                            getAccountEntity().setId((long) accountInfo.getId());
                            getAccountEntity().setProductName(accountInfo.getProductName());
                            getAccountEntity().setProductPrefix(accountInfo.getShortProductName());
                        }
                    } else if (!isLoan(accountInfo.getShortProductName()) && accountInfo.getStatus().equalsIgnoreCase("Active")) {
                        getAccountEntity().setAccount_no(accountInfo.getAccountNo());
                        getAccountEntity().setId((long) accountInfo.getId());
                        getAccountEntity().setProductName(accountInfo.getProductName());
                        getAccountEntity().setProductPrefix(accountInfo.getShortProductName());
                    } else if (acctPrefix.equalsIgnoreCase("SV")) {
                        if (accountInfo.getStatus().equalsIgnoreCase("Active") && !isLoan(accountInfo.getShortProductName())) {
                            getAccountEntity().setAccount_no(accountInfo.getAccountNo());
                            getAccountEntity().setId((long) accountInfo.getId());
                            getAccountEntity().setProductName(accountInfo.getProductName());
                            getAccountEntity().setProductPrefix(accountInfo.getShortProductName());
                        }
                    }

                }

                logger.info("fetched account::=> {}", getAccountEntity());

            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error fetching loans: " + e.getMessage(), e);
//                throw new RuntimeException("Error fetching loans", e);
            }
        } else {
            throw new RuntimeException("No record Found");
        }
        return getAccountEntity();
    }

    private boolean isLoan(String productPrefix) {
        return productPrefix.equalsIgnoreCase("BL")
                || productPrefix.equalsIgnoreCase("EM")
                || productPrefix.equalsIgnoreCase("EL")
                || productPrefix.equalsIgnoreCase("ML");
    }

    private List<Integer> getOffice() {
        List<Integer> ids = new ArrayList<>();
        OkHttpClient client = getUnsafeOkHttpClient();
        String customerEndpoint = "offices";
        Request request = new Request.Builder()
                .url(exernalUrl.concat(customerEndpoint))
                .addHeader("Authorization", basicAuthToken())
                .addHeader("Fineract-Platform-TenantId", tenantId)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String jsonString = response.body().string();
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(jsonString);
                for (JsonNode node : rootNode) {
                    ids.add(node.get("id").asInt());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ids;
    }

    private Long getIdbyExternalId(String idNo) {
        List<Integer> officeId = getOffice();
        Long custId = null;

        for (Integer id : officeId) {
            custId = fetchClientByIdandOfice(idNo, id);
            if (custId != null && custId != 0 && !isBlank(custId)) {
                return custId;
            }
        }

        return custId;  // Return null or the last checked custId if none were valid
    }

    private Long fetchClientByIdandOfice(String idNo, int officeId) {
        long customerId = 0L;
        OkHttpClient client = getUnsafeOkHttpClient();
        String customerEndpoint = "clients?officeId=" + officeId;
        Request request = new Request.Builder()
                .url(exernalUrl.concat(customerEndpoint))
                .addHeader("Authorization", basicAuthToken())
                .addHeader("Fineract-Platform-TenantId", tenantId)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String jsonString = response.body().string();
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(jsonString);
                JsonNode pageItems = rootNode.path("pageItems");

                for (JsonNode item : pageItems) {
                    String externalId = item.path("externalId").asText();
                    if (externalId.equalsIgnoreCase(idNo)) {
                        customerId = Long.parseLong(item.path("id").asText());
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.info("fetched account::=> {}", getAccountEntity());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return customerId;
    }

    private String basicAuthToken() {
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    private Long searchCustomerById(String idNo) {
        TenantEntity tenant = tenantRepository.fetchByExternalId(idNo);
        return tenant.getId();
    }

    public TenantIdEntity postWithIdNo(MpesaRequest mpesaRequest) throws Exception {
        ProcessAlert processAlert = new ProcessAlert();
        AlertRequest alertRequest = new AlertRequest();
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        handleIpnRequest(mpesaRequest, "P");

        System.out.println("request ::=> " + mpesaRequest.toString());
        logger.info("postWithIdNo::=> {}", mpesaRequest.toString());
        TenantIdEntity tenantDto = new TenantIdEntity();

        String clientNo = mpesaRequest.getBillRefNumber();
        String bankCode = mpesaRequest.getBusinessShortCode();

        long clientCode = 0L;
        String accountType = "";
        logger.info("clientNo: {}", clientNo);
        if (clientNo.contains("#")) {
            String[] parts = clientNo.split("#");
            if (parts.length == 2) {
                String clientCodePart = parts[0];
                accountType = parts[1];
                if (accountType.equalsIgnoreCase("EL")) {
                    accountType = "EM";
                }

                clientNo = clientCodePart; //bankCodeAndClientCode.substring(2);
                clientCode = searchCustomerById(clientNo);//getIdbyExternalId(clientNo);//Long.parseLong(clientNo);
                tenantDto.setId(clientCode);
            } else {
                logger.info("clientNo: {}", clientNo);
                logger.info("Invalid clientNo format.");
                tenantDto.setId(0L);

            }
        } else {
            logger.info("clientNo: {}", clientNo);
            clientCode = searchCustomerById(clientNo);//getIdbyExternalId(clientNo);
            tenantDto.setId(clientCode);
            accountType = "EM";
        }

        logger.info("biller ref: {}", mpesaRequest.getBillRefNumber());
        logger.info("Bank Code: {}", bankCode);
        logger.info("Client Code: {}", clientCode);
        logger.info("Account Type: {}", accountType);


        //check if Id exists
        OkHttpClient client = getUnsafeOkHttpClient();

        //String customerEndpoint = "fineract-provider/api/v1/clients/" + clientNo;
        if (!isBlank(tenantDto.getId()) && tenantDto.getId() > 0) {
            String urlEndpoint = "clients/" + tenantDto.getId();
            System.out.println("url::=>" + exernalUrl + urlEndpoint);
            logger.info("client url::=>{}{}", exernalUrl, urlEndpoint);

            // Build the request
            Request request = new Request.Builder()
                    .url(exernalUrl.concat(urlEndpoint))
                    .addHeader("Authorization", basicAuth)
                    .addHeader("Fineract-Platform-TenantId", tenantId)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                logger.info("API Response {}", responseBody);

                if (!response.isSuccessful()) {
                    tenantDto.setId(0L);
                    logger.info("Error on API response");
                    throw new IOException("Unexpected code " + response);
                }

                if (responseBody.isEmpty()) {
                    throw new IOException("Response body is empty");
                }

                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(responseBody);
                    JSONObject jsonObject = new JSONObject(rootNode.toString());

                    Long id = Long.valueOf(jsonObject.optString("id"));
                    String accountNo = jsonObject.optString("accountNo");
                    String statusValue = jsonObject.getJSONObject("status").optString("value");
                    boolean active = jsonObject.optBoolean("active");
                    String displayName = jsonObject.optString("displayName");
                    String mobileNo = jsonObject.optString("mobileNo");
                    String externalId = jsonObject.optString("externalId");

                    //set alert details
                    alertRequest.setAccountNo(accountNo);
                    alertRequest.setMobileNo(mobileNo);
                    alertRequest.setReceipt(mpesaRequest.getTransID());
                    alertRequest.setClientName(displayName);
                    alertRequest.setSenderName(mpesaRequest.getFirstName());
                    alertRequest.setAmount(new BigDecimal(mpesaRequest.getTransAmount()));
                    alertRequest.setTxnDate(formatMpesaDate(mpesaRequest.getTransTime()));


                    tenantDto.setId(id);

                    if (!active) {
                        tenantDto.setId(0L);
                    } else {
                        Long tenatId = tenantDto.getId();
                        AccountsDto tenant = getAcounts(tenatId, accountType);

                        String txnDate = new SimpleDateFormat("dd MMM yyyy").format(new Date());
                        if (isLoan(accountType)) {
                            alertRequest.setMessageType("LN");
                            String loanUrlEndpoint = "loans/" + tenant.getId() + "/transactions?command=repayment";
                            logger.info("urlEndpoint::=> {}", exernalUrl + loanUrlEndpoint);

                            // Construct JSON request body
                            Map<String, Object> requestBody = new HashMap<>();
                            requestBody.put("dateFormat", "dd MMMM yyyy");
                            requestBody.put("locale", "en");
                            requestBody.put("transactionDate", txnDate);
                            requestBody.put("transactionAmount", mpesaRequest.getTransAmount());
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

                            logger.info("Mpesa Request to CBS::=> {}", jsonBody);

                            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
                            Request loanRequest = new Request.Builder()
                                    .url(exernalUrl.concat(loanUrlEndpoint))
                                    .post(body)
                                    .addHeader("Authorization", Credentials.basic(username, password))
                                    .addHeader("Fineract-Platform-TenantId", tenantId)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("ACCEPT", "application/json")
                                    .build();

                            try (Response loanResponse = client.newCall(loanRequest).execute()) {
                                if (!loanResponse.isSuccessful()) {
                                    throw new IOException("Unexpected code " + loanResponse);
                                }
                                handleIpnRequest(mpesaRequest, loanResponse.isSuccessful() ? "S" : "F");
                                if (processAlert.sendAlert(alertRequest)) {
                                    logger.info("Loan SMS sent to::=> {}", alertRequest.getMobileNo());
                                }
                            }
                        } else {
                            alertRequest.setMessageType("SV");
                            String savingsUrlEndpoint = "savingsaccounts/" + tenant.getId() + "/transactions?command=deposit";
                            logger.info("urlEndpoint::=> {}", exernalUrl + savingsUrlEndpoint);

                            // Construct JSON request body
                            //https://evolve.bgs.co.ke/fineract-provider/api/v1/paymenttypes -  to get payment types
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

                            logger.info("Loan Request to CBS::=> {}", jsonBody);

                            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
                            Request loanRequest = new Request.Builder()
                                    .url(exernalUrl.concat(savingsUrlEndpoint))
                                    .post(body)
                                    .addHeader("Authorization", Credentials.basic(username, password))
                                    .addHeader("Fineract-Platform-TenantId", tenantId)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("ACCEPT", "application/json")
                                    .build();

                            try (Response savingsResponse = client.newCall(loanRequest).execute()) {
                                if (!savingsResponse.isSuccessful()) {
                                    throw new IOException("Unexpected code " + savingsResponse);
                                }
                                handleIpnRequest(mpesaRequest, savingsResponse.isSuccessful() ? "S" : "F");
                                logger.info("tenantDto response to CBS::=> {}", savingsResponse.body().string());
                                if (processAlert.sendAlert(alertRequest)) {
                                    logger.info("saving SMS sent to::=> {}", alertRequest.getMobileNo());
                                }
                            }
                        }
                        logger.info("tenantDto to CBS::=> {}", tenantDto.toString());
                    }
                } catch (JSONException | NumberFormatException | IOException e) {
                    tenantDto.setId(0L);
                    handleIpnRequest(mpesaRequest, "F");
                    logger.error("No tenant Found {}", e.getMessage());

                }

            } catch (Exception ex) {
                tenantDto.setId(0L);
                handleIpnRequest(mpesaRequest, "F");
                logger.error("Main error {}", ex.getMessage());
            }

        } else {
            logger.error("No tenant Found");
            tenantDto.setId(0L);
            handleIpnRequest(mpesaRequest, "F");
        }
        return tenantDto;
    }


    private JsonNode convertResponseToJson(ResponseEntity<String> response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public TenantEntity checkClient(MpesaRequest mpesaRequest) {

        TenantEntity tenantDto = new TenantEntity();
        String clientNo = mpesaRequest.getBillRefNumber().replace(tenantId, "").substring(1);
        String[] parts = clientNo.split("#");

        if (parts.length == 2) {
            String bankCodeAndClientCode = parts[0];
            String accountType = parts[1];

            // Assuming the bank code is always the first two characters
            String bankCode = bankCodeAndClientCode.substring(0, 2);
            String clientCode = bankCodeAndClientCode.substring(2);

            tenantDto.setId(Long.valueOf(clientCode));

            System.out.println("Bank Code: " + bankCode);
            System.out.println("Client Code: " + clientCode);
            System.out.println("Account Type: " + accountType);
        } else {
            System.out.println("Invalid clientNo format.");
        }
        if (!isBlank(tenantDto.getId())) {
            // Call external JSON web service
            RestTemplate restTemplate = new RestTemplate();
            String urlEndpoint = "clients/" + tenantDto.getId();
            ResponseEntity<String> response = restTemplate.postForEntity(exernalUrl + urlEndpoint, tenantDto, String.class);
            return tenantDto;
        } else {
            throw new RuntimeException("No record Found");
        }

    }

    private String formatMpesaDate(String timestamp) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(timestamp, inputFormatter);
        String formattedDate = dateTime.format(outputFormatter);
        return formattedDate;
    }

    public void handleIpnRequest(MpesaRequest mpesaRequest, String status) {
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

    public LocalDateTime DateConverter(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
        System.out.println("Converted LocalDateTime: " + dateTime);
        return dateTime;
    }
    //    public TenantDto validateAccount(MpesaRequest mpesaRequest) {
//        // Implement process logic
//        TenantDto tenantDto =   repository.findByIdNo(mpesaRequest.getBillRefNumber());
//        if (tenantDto != null) {
//            // Call external JSON web service
//            RestTemplate restTemplate = new RestTemplate();
//            String urlEndpoint = exernalUrl;
//            ResponseEntity<String> response = restTemplate.postForEntity(exernalUrl+urlEndpoint, tenantDto, String.class);
//            return tenantDto;
//        }else{
//            throw new RuntimeException("No record Found");
//        }
//    }
//    public TenantDto getAllAccounts(MpesaRequest mpesaRequest) {
//        // Implement process logic
//        TenantDto tenantDto =   repository.findByIdNo(mpesaRequest.getBillRefNumber());
//        if (tenantDto != null) {
//            // Call external JSON web service
//            RestTemplate restTemplate = new RestTemplate();
//            String urlEndpoint = exernalUrl;
//            ResponseEntity<String> response = restTemplate.postForEntity(exernalUrl+urlEndpoint, tenantDto, String.class);
//            return tenantDto;
//        }else{
//            throw new RuntimeException("No record Found");
//        }
//    }


    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public AccountsDto getAccountEntity() {
        return accountEntity;
    }

    public void setAccountEntity(AccountsDto accountEntity) {
        this.accountEntity = accountEntity;
    }
}
