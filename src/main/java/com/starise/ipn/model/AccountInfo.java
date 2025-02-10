package com.starise.ipn.model;

public class AccountInfo {
    private Long id;
    private String accountNo;
    private Long productId;
    private String productName;
    private String shortProductName;
    private String status;
    private Double accountBalance;


    public AccountInfo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getShortProductName() {
        return shortProductName;
    }

    public void setShortProductName(String shortProductName) {
        this.shortProductName = shortProductName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(Double accountBalance) {
        this.accountBalance = accountBalance;
    }

    @Override
    public String toString() {
        return "AccountInfo{" +
                "id=" + id +
                ", accountNo='" + accountNo + '\'' +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", shortProductName='" + shortProductName + '\'' +
                ", status='" + status + '\'' +
                ", accountBalance=" + accountBalance +
                '}';
    }

    public AccountInfo(Long id, String accountNo, Long productId, String productName, String shortProductName, String status, Double accountBalance) {
        this.id = id;
        this.accountNo = accountNo;
        this.productId = productId;
        this.productName = productName;
        this.shortProductName = shortProductName;
        this.status = status;
        this.accountBalance = accountBalance;
    }
}
