package com.starise.ipn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanAccount {
    public int id;
    public String accountNo;
    public int productId;
    public String productName;
    public String shortProductName;
    public Status status;
    public LoanType loanType;
    public int loanCycle;
    public Timeline timeline;
    public boolean inArrears;
    public double originalLoan;
    public double amountPaid;
    public double loanBalance;
}



