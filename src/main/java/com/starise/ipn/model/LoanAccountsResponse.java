package com.starise.ipn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanAccountsResponse {
    @JsonProperty("loanAccounts")
    public List<LoanAccount> loanAccounts;
}
