package com.starise.ipn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starise.ipn.model.LoanAccount;
import com.starise.ipn.model.LoanAccountsResponse;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LoanAccountService {

    public List<LoanAccount> getActiveLoanAccounts(String jsonResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            LoanAccountsResponse response = objectMapper.readValue(jsonResponse, LoanAccountsResponse.class);

            return response.loanAccounts.stream()
                    .filter(loanAccount -> loanAccount.status.active)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
