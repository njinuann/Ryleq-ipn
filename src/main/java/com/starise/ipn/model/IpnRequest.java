package com.starise.ipn.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class IpnRequest {
    private String transactionType;
    private String billRefNumber;
    private BigDecimal orgAccountBalance;
    private String msisdn;
    private String firstName;
    private BigDecimal transAmount;
    private String thirdPartyTransId;
    private String invoiceNumber;
    private String transId;
    private LocalDateTime transTime;
    private String businessShortCode;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private String status;


    @Override
    public String toString() {
        return "IpnRequest{" +
                "transactionType='" + transactionType + '\'' +
                ", billRefNumber='" + billRefNumber + '\'' +
                ", orgAccountBalance=" + orgAccountBalance +
                ", msisdn='" + msisdn + '\'' +
                ", firstName='" + firstName + '\'' +
                ", transAmount=" + transAmount +
                ", thirdPartyTransId='" + thirdPartyTransId + '\'' +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", transId='" + transId + '\'' +
                ", transTime=" + transTime +
                ", businessShortCode='" + businessShortCode + '\'' +
                '}';
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getBillRefNumber() {
        return billRefNumber;
    }

    public void setBillRefNumber(String billRefNumber) {
        this.billRefNumber = billRefNumber;
    }

    public BigDecimal getOrgAccountBalance() {
        return orgAccountBalance;
    }

    public void setOrgAccountBalance(BigDecimal orgAccountBalance) {
        this.orgAccountBalance = orgAccountBalance;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public BigDecimal getTransAmount() {
        return transAmount;
    }

    public void setTransAmount(BigDecimal transAmount) {
        this.transAmount = transAmount;
    }

    public String getThirdPartyTransId() {
        return thirdPartyTransId;
    }

    public void setThirdPartyTransId(String thirdPartyTransId) {
        this.thirdPartyTransId = thirdPartyTransId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getTransId() {
        return transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public LocalDateTime getTransTime() {
        return transTime;
    }

    public void setTransTime(LocalDateTime transTime) {
        this.transTime = transTime;
    }

    public String getBusinessShortCode() {
        return businessShortCode;
    }

    public void setBusinessShortCode(String businessShortCode) {
        this.businessShortCode = businessShortCode;
    }
}

