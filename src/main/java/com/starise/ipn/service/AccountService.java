package com.starise.ipn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starise.ipn.model.AccountInfo;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class AccountService {

    private final ObjectMapper objectMapper;

    public AccountService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<AccountInfo> extractAccounts(String jsonResponse) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        List<AccountInfo> accountInfoList = new ArrayList<>();

        // Extract loan accounts
        JsonNode loanAccountsNode = rootNode.path("loanAccounts");
        extractAccountDetails(loanAccountsNode, accountInfoList);

        // Extract savings accounts if no loan accounts are present
        if (accountInfoList.isEmpty()) {
            JsonNode savingsAccountsNode = rootNode.path("savingsAccounts");
            extractAccountDetails(savingsAccountsNode, accountInfoList);
        }

        return accountInfoList;
    }

    private void extractAccountDetails(JsonNode accountsNode, List<AccountInfo> accountInfoList) {
        if (accountsNode.isArray()) {
            for (JsonNode accountNode : accountsNode) {
                AccountInfo accountInfo = new AccountInfo();
                accountInfo.setId(accountNode.path("id").asLong());
                accountInfo.setAccountNo(accountNode.path("accountNo").asText());
                accountInfo.setProductId(accountNode.path("productId").asLong());
                accountInfo.setProductName(accountNode.path("productName").asText());
                accountInfo.setShortProductName(accountNode.path("shortProductName").asText());
                accountInfo.setStatus(accountNode.path("status").path("value").asText());
                accountInfo.setAccountBalance(isLoan(accountInfo.getShortProductName()) ? accountNode.path("loanBalance").asDouble() : accountNode.path("accountBalance").asDouble());

                accountInfoList.add(accountInfo);
            }
        }
    }

    private boolean isLoan(String productPrefix) {
        return productPrefix.equalsIgnoreCase("BL")
                || productPrefix.equalsIgnoreCase("EM")
                || productPrefix.equalsIgnoreCase("EL")
                || productPrefix.equalsIgnoreCase("ML");
    }
}