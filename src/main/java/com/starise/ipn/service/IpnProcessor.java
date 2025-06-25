package com.starise.ipn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starise.ipn.Util.AXCaller;
import com.starise.ipn.Util.AXWorker;
import com.starise.ipn.dto.AccountsDto;
import com.starise.ipn.dto.ClientInfo;
import com.starise.ipn.dto.TransactionResult;
import com.starise.ipn.dto.VerifiedAccount;
import com.starise.ipn.entity.IpnLog;
import com.starise.ipn.entity.MpesaRequest;
import com.starise.ipn.entity.TenantEntity;
import com.starise.ipn.entity.TenantIdEntity;
import com.starise.ipn.model.AccountInfo;
import com.starise.ipn.model.AlertRequest;
import com.starise.ipn.model.IpnRequest;
import com.starise.ipn.repository.TenantRepository;
import com.starise.ipn.sms.ProcessAlert;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class IpnProcessor {
    private static final ConcurrentMap<String, Lock> transactionLocks = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(IpnProcessor.class);

    private final AccountService accountService;
    private final TenantRepository tenantRepository;
    private final ProcessAlert processAlert;
    private final AXWorker axWorker;
    private final IpnLogService ipnLogService;
    private AXCaller caller;

    AccountsDto accountEntity = new AccountsDto();

    @Autowired
    public IpnProcessor(AccountService accountService, TenantRepository tenantRepository,
                        ProcessAlert processAlert,
                        AXWorker axWorker,
                        IpnLogService ipnLogService,
                        AXCaller caller) {
        this.accountService = accountService;
        this.tenantRepository = tenantRepository;
        this.processAlert = processAlert;
        this.axWorker = axWorker;
        this.ipnLogService = ipnLogService;
        this.caller =caller;
    }

    @Async
    public CompletableFuture<TenantIdEntity> processMpesaTxnAsync(MpesaRequest mpesaRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
               setCaller(new AXCaller());
                String lockKey = mpesaRequest.getBillRefNumber() + "_" + mpesaRequest.getTransID();
                Lock lock = transactionLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
                return postWithIdNo(mpesaRequest, lock, lockKey);
            } catch (Exception e) {
                logger.error("Error processing transaction", e);
                updateTransactionStatus(mpesaRequest, "F");
                throw new RuntimeException(e); // Propagate the exception
            }
        });
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TenantIdEntity postWithIdNo(MpesaRequest mpesaRequest, Lock lock, String lockKey) throws Exception {

        try {
            // 1. Initialization & Validation
            validateRequest(mpesaRequest);
            acquireLock(lock);

            updateTransactionStatus(mpesaRequest, "P");


            // 2. Check idempotency
//            if (isDuplicateTransaction(mpesaRequest)) {
//                return getExistingTransaction(mpesaRequest);
//            }

            // 3. Client Resolution
            ClientInfo clientInfo = resolveClient(mpesaRequest);

            // 4. Account Verification
            VerifiedAccount account = verifyAccount(clientInfo, mpesaRequest);

            // 5. Transaction Processing
            TransactionResult result = processTransaction(account);

            return buildResponse(result);
        } catch (Exception e) {
            updateTransactionStatus(mpesaRequest, "F");
            throw e;
        } finally {
            releaseLock(lock, lockKey);
        }
    }

    private TenantIdEntity buildResponse(TransactionResult result) {
        return result.getTenantIdEntity();
    }

    private void releaseLock(Lock lock, String lockKey) {
        lock.unlock();
        transactionLocks.remove(lockKey); // Clean up
    }

    private TransactionResult processTransaction(VerifiedAccount account) {
        TransactionResult transactionResult = new TransactionResult();
        try {
            logger.info("================ START PROCESSING LOANS {} - {} ================", account.getClientInfo().getAccountsDto().getProductPrefix(), account.getClientInfo().getAccountsDto().getAccount_no());
            if (account.getRepaymentAmount().compareTo(account.getLoanAmount()) > 0) {
                account.setExcessPayment(account.getRepaymentAmount().subtract(account.getLoanAmount()));
                account.setRepaymentAmount(account.getLoanAmount());

                logger.info("=============================Excess amount evaluation =================================");
                logger.info("Repayment amount {}", account.getRepaymentAmount());
                logger.info("Excess amount for SV {}", account.getExcessPayment());
                logger.info("customer id {}", account.getClientInfo().getTenantDto().getId());
                logger.info("=============================Excess amount evaluation end =================================");
            }

            account.getAlertRequest().setMessageType("LN");
            boolean loanProcessed = processLoan(account);
            if (loanProcessed) {
                transactionResult.setResponseCode("00");
                transactionResult.setResponseMessage("Approved");
                transactionResult.setTenantIdEntity(account.getClientInfo().getTenantDto());
            } else {
                transactionResult.setResponseCode("01");
                transactionResult.setResponseMessage("Failed");

                account.getClientInfo().getTenantDto().setId(0L);
                transactionResult.setTenantIdEntity(account.getClientInfo().getTenantDto());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return transactionResult; //review respnse
    }

    private boolean processLoan(VerifiedAccount txnDetail) {
        try {
            String loanUrlEndpoint = "loans/" + txnDetail.getClientInfo().getAccountsDto().getId() + "/transactions?command=repayment";
            logger.info("urlEndpoint::=> {}", axWorker.externalUrl + loanUrlEndpoint);
            String mpesaDate = axWorker.convertMifosDate(txnDetail.getMpesaRequest().getTransTime());
            String txnDate = new SimpleDateFormat("dd MMM yyyy").format(new Date());

            // Construct JSON request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("dateFormat", "dd MMMM yyyy");
            requestBody.put("locale", "en");
            requestBody.put("transactionDate", txnDate); //txnDate//mpesaDate
            requestBody.put("transactionAmount", txnDetail.getRepaymentAmount().toPlainString());
            requestBody.put("paymentTypeId", "2");
            requestBody.put("note", "mpesa loan payment");
            requestBody.put("accountNumber", txnDetail.getClientInfo().getAccountsDto().getAccount_no());
            requestBody.put("checkNumber", txnDetail.getMpesaRequest().getBillRefNumber());
            requestBody.put("routingCode", txnDetail.getMpesaRequest().getFirstName());
            requestBody.put("receiptNumber", txnDetail.getMpesaRequest().getTransID());
            requestBody.put("bankNumber", "mpesa");

            // Convert requestBody map to JSON string
            ObjectMapper loanObjectMapper = new ObjectMapper();
            String jsonBody = loanObjectMapper.writeValueAsString(requestBody);
            logger.info("Mpesa Request to CBS::=> {}", jsonBody);

            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
            Request loanRequest = new Request.Builder()
                    .url(axWorker.externalUrl.concat(loanUrlEndpoint))
                    .post(body)
                    .addHeader("Authorization", Credentials.basic(axWorker.username, axWorker.password))
                    .addHeader("Fineract-Platform-TenantId", axWorker.tenantId)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("ACCEPT", "application/json")
                    .build();

            try (Response loanResponse = axWorker.getUnsafeOkHttpClient().newCall(loanRequest).execute()) {
                if (!loanResponse.isSuccessful()) {
                    updateTransactionStatus(txnDetail.getMpesaRequest(), "F");
                    throw new IOException("Unexpected code " + loanResponse);
                }
                updateTransactionStatus(txnDetail.getMpesaRequest(), "S");
                if (processAlert.sendAlert(txnDetail.getAlertRequest(),caller)) {
                    logger.info("Loan SMS sent to::=> {}", txnDetail.getAlertRequest().getMobileNo());
                }

                logger.info("=============== PREPARE TO PROCESS SAVINGS FOR EXCESS PAYMENT OF {} FOR {} ", txnDetail.getExcessPayment(), txnDetail.getClientInfo().getTenantDto().getClient_id());
                logger.info("Account has excess payment process deposit::=> {}, amount greater than zero {}", txnDetail.getExcessPayment(), txnDetail.getExcessPayment().compareTo(BigDecimal.ZERO));
                if (txnDetail.getExcessPayment().compareTo(BigDecimal.ZERO) > 0) {
                    logger.info("Found Excess payment for::=> {}, {}, {}, {}, {}, {}, {}, {}, {}", txnDetail.getClientInfo().getTenantDto().getClient_id(), txnDate, txnDetail.getExcessPayment(), txnDetail.getClientInfo().getAccountsDto().getAccount_no(), txnDetail.getMpesaRequest().getBillRefNumber(), txnDetail.getMpesaRequest().getFirstName(), txnDetail.getMpesaRequest().getTransID(), txnDetail.getAlertRequest().toString(), txnDetail.getMpesaRequest().toString());
                    if (processExcessPaymentToSavings(txnDetail.getClientInfo().getTenantDto().getId(), txnDate, txnDetail.getExcessPayment().toPlainString(), txnDetail.getClientInfo().getAccountsDto().getAccount_no(), txnDetail.getMpesaRequest().getBillRefNumber(), txnDetail.getMpesaRequest().getFirstName(), txnDetail.getMpesaRequest().getTransID(), txnDetail.getAlertRequest(), txnDetail.getMpesaRequest())) {
                        logger.info("Processing to savings account for account {} was successful for  amount::=> {}", txnDetail.getClientInfo().getTenantDto().getClient_id(), txnDetail.getExcessPayment().toPlainString());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private boolean processPaymentToSavings(VerifiedAccount txnDetail,AXCaller caller) throws JsonProcessingException {
        logger.info("================ START PROCESSING DEPOSITS {} - {} ================", txnDetail.getClientInfo().getAccountsDto().getProductPrefix(), txnDetail.getClientInfo().getAccountsDto().getAccount_no());
        txnDetail.getAlertRequest().setMessageType("SV");
        txnDetail.getAlertRequest().setAmount(new BigDecimal(txnDetail.getMpesaRequest().getTransAmount()));

        String savingsUrlEndpoint = "savingsaccounts/" + txnDetail.getClientInfo().getAccountsDto().getId() + "/transactions?command=deposit";
        logger.info("SV urlEndpoint::=> {}", axWorker.externalUrl + savingsUrlEndpoint);

        String mpesaDate = axWorker.convertMifosDate(txnDetail.getMpesaRequest().getTransTime());
        String txnDate = new SimpleDateFormat("dd MMM yyyy").format(new Date());


        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("dateFormat", "dd MMMM yyyy");
        requestBody.put("locale", "en");
        requestBody.put("transactionDate", txnDate);
        requestBody.put("transactionAmount", txnDetail.getMpesaRequest().getTransAmount());
        requestBody.put("paymentTypeId", "2"); //2 =Paybill Collection Account
        // requestBody.put("note", "mpesa Deposit");
        requestBody.put("accountNumber", txnDetail.getClientInfo().getAccountsDto().getAccount_no());
        requestBody.put("checkNumber", txnDetail.getMpesaRequest().getBillRefNumber());
        requestBody.put("routingCode", txnDetail.getMpesaRequest().getFirstName());
        requestBody.put("receiptNumber", txnDetail.getMpesaRequest().getTransID());
        requestBody.put("bankNumber", "mpesa");

        // Convert requestBody map to JSON string
        ObjectMapper loanObjectMapper = new ObjectMapper();
        String jsonBody = loanObjectMapper.writeValueAsString(requestBody);

        logger.info("Deposit Request to CBS::=> {}", jsonBody);

        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request loanRequest = new Request.Builder()
                .url(axWorker.externalUrl.concat(savingsUrlEndpoint))
                .post(body)
                .addHeader("Authorization", Credentials.basic(axWorker.username, axWorker.password))
                .addHeader("Fineract-Platform-TenantId", axWorker.tenantId)
                .addHeader("Content-Type", "application/json")
                .addHeader("ACCEPT", "application/json")
                .build();

        try (Response savingsResponse = axWorker.getUnsafeOkHttpClient().newCall(loanRequest).execute()) {
            boolean isTxnSuccessful = savingsResponse.isSuccessful();
            logger.info("is txn successful::=> {}", isTxnSuccessful);

            if (!isTxnSuccessful) {
                updateTransactionStatus(txnDetail.getMpesaRequest(), "F");
                throw new IOException("Unexpected code " + savingsResponse);
            }

            updateTransactionStatus(txnDetail.getMpesaRequest(), "S");
            logger.info("tenantDto response from CBS::=> {}", savingsResponse.body().string());
            if (processAlert.sendAlert(txnDetail.getAlertRequest(),getCaller())) {
                logger.info("saving SMS sent to::=> {}", txnDetail.getAlertRequest().getMobileNo());
            }

            logger.info("tenantDto to CBS::=> {}", txnDetail.toString());
            return true;
        } catch (IOException e) {
            logger.error("ProcessPaymentToDepsoit ", e);
        }
        return false;
    }

    private boolean processExcessPaymentToSavings(Long clientID, String txnDate, String txnAmt, String tenantacctNo,
                                                  String billerRefNo, String firstName, String transId, AlertRequest alertRequest, MpesaRequest mpesaRequest) {
        try {
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
            logger.error("++++++++++++++++++++END LOG dp ++++++++++++++ ");

            OkHttpClient client = axWorker.getUnsafeOkHttpClient();

            alertRequest.setMessageType("SV");

            String savingsUrlEndpoint = "savingsaccounts/" + tenant.getId() + "/transactions?command=deposit";
            logger.info("process dp urlEndpoint::=> {}", axWorker.externalUrl + savingsUrlEndpoint);

            // Construct JSON request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("dateFormat", "dd MMMM yyyy");
            requestBody.put("locale", "en");
            requestBody.put("transactionDate", txnDate);
            requestBody.put("transactionAmount", txnAmt);
            requestBody.put("paymentTypeId", "2"); //2 =Paybill Collection Account
            requestBody.put("accountNumber", tenantacctNo);
            requestBody.put("checkNumber", billerRefNo);
            requestBody.put("routingCode", firstName);
            requestBody.put("receiptNumber", transId);
            requestBody.put("bankNumber", "mpesa");

            // Convert requestBody map to JSON string
            ObjectMapper loanObjectMapper = new ObjectMapper();
            String jsonBody = loanObjectMapper.writeValueAsString(requestBody);

            logger.info("process Dp Request to CBS::=> {}", jsonBody);

            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
            Request loanRequest = new Request.Builder()
                    .url(axWorker.externalUrl.concat(savingsUrlEndpoint))
                    .post(body)
                    .addHeader("Authorization", Credentials.basic(axWorker.username, axWorker.password))
                    .addHeader("Fineract-Platform-TenantId", axWorker.tenantId)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("ACCEPT", "application/json")
                    .build();

            try (Response savingsResponse = client.newCall(loanRequest).execute()) {
                if (!savingsResponse.isSuccessful()) {
                    throw new IOException("Unexpected code " + savingsResponse);
                }
                updateTransactionStatus(mpesaRequest, "S");
                logger.info("SV response to CBS::=> {}", savingsResponse.body().string());
                alertRequest.setAmount(new BigDecimal(txnAmt));
                if (processAlert.sendAlert(alertRequest,getCaller())) {
                    logger.info("SV SMS sent to::=> {}", alertRequest.getMobileNo());
                }
                return true;
            }
        } catch (Exception ex) {
            logger.error("process DP error {}", ex.getMessage());
        }
        return false;
    }

    private VerifiedAccount verifyAccount(ClientInfo clientInfo, MpesaRequest mpesaRequest) {
        VerifiedAccount verifiedAccount = new VerifiedAccount();
        verifiedAccount.setMpesaRequest(mpesaRequest);

        OkHttpClient client = axWorker.getUnsafeOkHttpClient();

        if (!axWorker.isBlank(clientInfo.getTenantDto().getId()) && clientInfo.getTenantDto().getId() > 0) {
            String urlEndpoint = "clients/" + clientInfo.getTenantDto().getId();
            logger.info("client url::=>{}{}", axWorker.externalUrl, urlEndpoint);
            Request request = new Request.Builder()
                    .url(axWorker.externalUrl.concat(urlEndpoint))
                    .addHeader("Authorization", axWorker.basicAuthToken())
                    .addHeader("Fineract-Platform-TenantId", axWorker.tenantId)
                    .build();


            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                logger.info("API Response {}", responseBody);

                if (!response.isSuccessful()) {
                    clientInfo.getTenantDto().setId(0L);
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

                    AlertRequest alertRequest = setAlertRequest(jsonObject, clientInfo);
                    clientInfo.getTenantDto().setId(alertRequest.getClientId());

                    if (!alertRequest.isClientActive()) {
                        clientInfo.getTenantDto().setId(0L);
                    } else {
                        Long tenatId = clientInfo.getTenantDto().getId();
                        //customerId = tenatId;
                        AccountsDto tenant = getAccounts(tenatId, clientInfo.getAccountType()); // get the acounts based on suffix
                        clientInfo.setAccountsDto(tenant);

                        if (axWorker.isBlank(tenant.getId())) {
                            logger.info("Found no Loans for EM::=> resolve to process Deposit {}", tenatId);
                            tenant = getAccounts(tenatId, "SV");
                            clientInfo.setAccountsDto(tenant);
                        }

                        alertRequest.setBalance(axWorker.isBlank(tenant.getAccount_balance()) ? BigDecimal.ZERO : BigDecimal.valueOf(tenant.getAccount_balance()).subtract(verifiedAccount.getClientInfo().getMpesaAmount()));
                        logger.info("ACCOUNT Balance {}", alertRequest.getBalance());

                        clientInfo.setLoan(isLoan(tenant.getProductPrefix()) && tenant.isLoan());
                        if (clientInfo.isLoan()) {
                            BigDecimal loanAmount = fetchLoanAmount(tenant.getId());
                            BigDecimal repaymentAmount = new BigDecimal(mpesaRequest.getTransAmount());
                            BigDecimal excessPayment = BigDecimal.ZERO;

                            verifiedAccount.setLoanAmount(loanAmount);
                            verifiedAccount.setRepaymentAmount(repaymentAmount);
                            verifiedAccount.setExcessPayment(excessPayment);
                            alertRequest.setAmount(repaymentAmount);
                        }
                        verifiedAccount.setAlertRequest(alertRequest);
                        verifiedAccount.setClientInfo(clientInfo);
                    }

                } catch (Exception ex) {
                    clientInfo.getTenantDto().setId(0L);
                    updateTransactionStatus(mpesaRequest, "F");
                    logger.error("Main error {}", ex.getMessage());
                }

            } catch (Exception ex) {
                clientInfo.getTenantDto().setId(0L);
                updateTransactionStatus(mpesaRequest, "F");
                logger.error("Main error {}", ex.getMessage());
            }
        }
        return verifiedAccount;
    }

    private BigDecimal fetchLoanAmount(Long loanId) {
        try {
            OkHttpClient client = new OkHttpClient();

            String getLoanEndpoint = "loans/" + loanId;

            System.out.println("url::=>" + axWorker.externalUrl + getLoanEndpoint);
            logger.info("loans balance ::=> {}{}", axWorker.externalUrl, getLoanEndpoint);
            String basicAuth = Credentials.basic(axWorker.username, axWorker.password);

            Request request = new Request.Builder()
                    .url(axWorker.externalUrl.concat(getLoanEndpoint))
                    .get()
                    .addHeader("Authorization", basicAuth)
                    .addHeader("Fineract-Platform-TenantId", axWorker.tenantId)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String responseBody = response.body().string();
                System.out.println("API Response: " + responseBody);
                logger.info("loans balance API Response::=> {} ", responseBody);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                JsonNode totalOutstandingNode = jsonResponse.path("summary").path("totalOutstanding");

                double totalOutstanding = 0.0;
                if (totalOutstandingNode.isDouble()) {
                    totalOutstanding = totalOutstandingNode.asDouble();
                    System.out.println("Total Outstanding: " + totalOutstanding);
                } else {
                    System.out.println("Total Outstanding not found or not a double.");
                }

                return BigDecimal.valueOf(totalOutstanding);

            } catch (Exception e) {
                logger.error("Error in fetching loan {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("main loans Error in fetching loan {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private AlertRequest setAlertRequest(JSONObject jsonObject, ClientInfo clientInfo) {
        AlertRequest alertRequest = new AlertRequest();
        Long id = Long.valueOf(jsonObject.optString("id"));
        String accountNo = jsonObject.optString("accountNo");
        String statusValue = jsonObject.getJSONObject("status").optString("value");
        boolean active = jsonObject.optBoolean("active");
        String displayName = jsonObject.optString("displayName");
        String mobileNo = axWorker.formatMobileNumber(jsonObject.optString("mobileNo"));
        String externalId = jsonObject.optString("externalId");

        //set alert details
        alertRequest.setAccountNo(accountNo);
        alertRequest.setMobileNo(mobileNo);
        alertRequest.setReceipt(clientInfo.getMpesaReceiptNo());
        alertRequest.setClientName(displayName);
        alertRequest.setSenderName(clientInfo.getMpesaSender());
        alertRequest.setTxnDate(clientInfo.getMpesaTransTime());
        alertRequest.setClientId(id);
        alertRequest.setClientActive(active);
        return alertRequest;
    }

    private ClientInfo resolveClient(MpesaRequest mpesaRequest) {
        ClientInfo clientInfo = new ClientInfo();
        TenantIdEntity tenantDto = new TenantIdEntity();
        clientInfo.setClientNo(mpesaRequest.getBillRefNumber().trim());
        clientInfo.setBankCode(mpesaRequest.getBusinessShortCode());
        clientInfo.setMpesaReceiptNo(mpesaRequest.getTransID());
        clientInfo.setMpesaSender(mpesaRequest.getFirstName());
        clientInfo.setMpesaTransTime(axWorker.formatMpesaDate(mpesaRequest.getTransTime()));
        clientInfo.setMpesaAmount(new BigDecimal(mpesaRequest.getTransAmount()));

        long clientCode = 0L;
        String accountType = "";
        logger.info("clientNo: {}", clientInfo.getClientNo());
        if (axWorker.hasSuffix(clientInfo.getClientNo())) {
            String[] parts = axWorker.splitClientId(clientInfo.getClientNo());

            if (parts != null && parts.length == 2) {
                String clientCodePart = parts[0].trim();
                accountType = parts[1].trim();
                if (accountType.equalsIgnoreCase("EL")) {
                    clientInfo.setAccountType("EM");
                }
                logger.info("clientNo: {}, account type: {}", clientInfo.getClientNo(), accountType);

                clientInfo.setClientNo(clientCodePart); //bankCodeAndClientCode.substring(2);
                clientCode = searchCustomerById(clientInfo.getClientNo()); //getIdbyExternalId(clientNo);//Long.parseLong(clientNo);

                tenantDto.setId(clientCode);
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("clientNo: {}", clientInfo.getClientNo());
                    logger.info("Invalid clientNo format.");
                }
                tenantDto.setId(0L);
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("clientNo-NoSuffix: {}", clientInfo.getClientNo());
            }
            clientCode = searchCustomerById(clientInfo.getClientNo()); //getIdbyExternalId(clientNo);
            tenantDto.setId(clientCode);
            clientInfo.setAccountType("EM");
        }
        clientInfo.setTenantDto(tenantDto);
        return clientInfo;
    }

    private synchronized Long searchCustomerById(String idNo) {
        TenantEntity tenant = tenantRepository.fetchByExternalId(idNo);
        return tenant.getId();
    }

//    private TenantIdEntity getExistingTransaction(MpesaRequest mpesaRequest) {
//
//    }
//
//    private boolean isDuplicateTransaction(MpesaRequest mpesaRequest) {
//        if (transactionRepository.existsByTransId(mpesaRequest.getTransID())) {
//            logger.info("Transaction {} already processed", mpesaRequest.getTransID());
//            return transactionRepository.findByTransId(mpesaRequest.getTransID());
//        }
//    }

    private void acquireLock(Lock lock) {
        lock.lock();
        logger.debug("Active locks: {}", transactionLocks.size());
    }

    private void validateRequest(MpesaRequest mpesaRequest) {
        if (mpesaRequest.getTransID() == null || mpesaRequest.getBillRefNumber() == null) {
            throw new IllegalArgumentException("Missing transaction identifiers");
        }
    }

    public AccountsDto getAccounts(Long clientID, String acctPrefix) {
        System.out.println("Getting accounts ::=> " + clientID);
        setAccountEntity(new AccountsDto());
        OkHttpClient client = new OkHttpClient();

        if (clientID == null || clientID == 0L) {
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

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            logger.info("Accounts API Response::=> {}", responseBody);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            // Step 1: Process loan accounts if acctPrefix is not SV
            if (!acctPrefix.equalsIgnoreCase("SV") && isLoan(acctPrefix)) {
                List<AccountInfo> activeLoanSavingsAccounts = accountService.extractLoanAccounts(jsonResponse.toString());
                AccountInfo prioritizedAccount = null;

                for (AccountInfo accountInfo : activeLoanSavingsAccounts) {
                    getAccountEntity().setLoan(true);

                    // Step 1.1: Exact match for `acctPrefix`
                    if (accountInfo.getShortProductName().equalsIgnoreCase(acctPrefix)
                            && accountInfo.getStatus().equalsIgnoreCase("Active")) {
                        logger.error(" PROCESSING Loan For Passed Prefix: client ID {}, account prefix {}", clientID, acctPrefix);
                        populateAccountEntity(accountInfo);
                        return getAccountEntity(); // Found exact match, exit early
                    }

                    // Step 1.2: Prioritize `EM` accounts
                    if (prioritizedAccount == null
                            && accountInfo.getShortProductName().equalsIgnoreCase("EM")
                            && accountInfo.getStatus().equalsIgnoreCase("Active")) {
                        logger.error(" PROCESSING Loan For EM: client ID {}, account prefix {}", clientID, acctPrefix);
                        prioritizedAccount = accountInfo; // Store `EM` account
                    }

                    // Step 1.3: Fallback to the first active account
                    if (prioritizedAccount == null && accountInfo.getStatus().equalsIgnoreCase("Active")) {
                        logger.error("This is the fall back: client ID {}, account prefix {}", clientID, acctPrefix);
                        prioritizedAccount = accountInfo; // Store the first active account as fallback
                    }
                }

                // Step 1.4: Use prioritized account if it exists
                if (prioritizedAccount != null) {
                    populateAccountEntity(prioritizedAccount);
                    logger.error("This is the prioritized account Client ID {}, PriorityEntity{}", clientID, getAccountEntity().toString());
                    return getAccountEntity();
                }
            }

            // Step 2: Process savings accounts if no loans found or acctPrefix is `SV`
            if (acctPrefix.equalsIgnoreCase("SV") || getAccountEntity().getAccount_no() == null) {
                logger.error(" PROCESSING DEPOSIT: client ID {}, account prefix {}", clientID, acctPrefix);

                List<AccountInfo> activeSavingsAccounts = accountService.extractSavingsAccounts(jsonResponse.toString());
                for (AccountInfo accountInfo : activeSavingsAccounts) {
                    if (accountInfo.getStatus().equalsIgnoreCase("Active")
                            && !isLoan(accountInfo.getShortProductName())) {
                        populateAccountEntity(accountInfo);
                        getAccountEntity().setLoan(false);
                        return getAccountEntity(); // Exit after finding the first active savings account
                    }
                }
            }

            logger.info("Fetched account::=> {}", getAccountEntity());

        } catch (Exception e) {
            logger.error("Error fetching accounts: {}", e.getMessage(), e);
        }

        // Step 3: Validate and return account entity (if no loans or savings found)
        return getAccountEntity(); //validateTxn(clientID, acctPrefix, getAccountEntity());
    }

    private void populateAccountEntity(AccountInfo accountInfo) {
        getAccountEntity().setAccount_no(accountInfo.getAccountNo());
        getAccountEntity().setId((long) accountInfo.getId());
        getAccountEntity().setProductName(accountInfo.getProductName());
        getAccountEntity().setProductPrefix(accountInfo.getShortProductName());
        getAccountEntity().setAccount_balance(accountInfo.getAccountBalance());
    }

    public void updateTransactionStatus(MpesaRequest mpesaRequest, String status) {
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
        request.setTransTime(axWorker.DateConverter(mpesaRequest.getTransTime()));
        request.setStatus(status);

        IpnLog log = getIpnLog(request);
        IpnLog ipnLog = ipnLogService.insertOrUpdate(log);
        logger.info(ipnLog.toString());
    }

    @NotNull
    private static IpnLog getIpnLog(IpnRequest request) {
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
        return log;
    }

    private boolean isLoan(String productPrefix) {
        return productPrefix.equalsIgnoreCase("BL")
                || productPrefix.equalsIgnoreCase("EM")
                || productPrefix.equalsIgnoreCase("EL")
                || productPrefix.equalsIgnoreCase("ML")
                || productPrefix.equalsIgnoreCase("SB");
    }

    public AccountsDto getAccountEntity() {
        return accountEntity;
    }

    public void setAccountEntity(AccountsDto accountEntity) {
        this.accountEntity = accountEntity;
    }

    public AXCaller getCaller()
    {
        return caller;
    }

    public void setCaller(AXCaller caller)
    {
        this.caller = caller;
    }
}
