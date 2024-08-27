package com.starise.ipn.entity;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MpesaRequest {

    @JsonProperty("TransactionType")
    private String transactionType;

    @JsonProperty("BillRefNumber")
    private String billRefNumber;

    @JsonProperty("OrgAccountBalance")
    private String orgAccountBalance;

    @JsonProperty("MSISDN")
    private String msisdn;

    @JsonProperty("FirstName")
    private String firstName;

    @JsonProperty("TransAmount")
    private String transAmount;

    @JsonProperty("ThirdPartyTransID")
    private String thirdPartyTransID;

    @JsonProperty("InvoiceNumber")
    private String invoiceNumber;

    @JsonProperty("TransID")
    private String transID;

    @JsonProperty("TransTime")
    private String transTime;

    @JsonProperty("BusinessShortCode")
    private String businessShortCode;

    // Getters and Setters

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

    public String getOrgAccountBalance() {
        return orgAccountBalance;
    }

    public void setOrgAccountBalance(String orgAccountBalance) {
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

    public String getTransAmount() {
        return transAmount;
    }

    public void setTransAmount(String transAmount) {
        this.transAmount = transAmount;
    }

    public String getThirdPartyTransID() {
        return thirdPartyTransID;
    }

    public void setThirdPartyTransID(String thirdPartyTransID) {
        this.thirdPartyTransID = thirdPartyTransID;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getTransID() {
        return transID;
    }

    public void setTransID(String transID) {
        this.transID = transID;
    }

    public String getTransTime() {
        return transTime;
    }

    public void setTransTime(String transTime) {
        this.transTime = transTime;
    }

    public String getBusinessShortCode() {
        return businessShortCode;
    }

    public void setBusinessShortCode(String businessShortCode) {
        this.businessShortCode = businessShortCode;
    }

    @Override
    public String toString() {
        return "MpesaRequest{" +
                "transactionType='" + transactionType + '\'' +
                ", billRefNumber='" + billRefNumber + '\'' +
                ", orgAccountBalance='" + orgAccountBalance + '\'' +
                ", msisdn='" + msisdn + '\'' +
                ", firstName='" + firstName + '\'' +
                ", transAmount='" + transAmount + '\'' +
                ", thirdPartyTransID='" + thirdPartyTransID + '\'' +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", transID='" + transID + '\'' +
                ", transTime='" + transTime + '\'' +
                ", businessShortCode='" + businessShortCode + '\'' +
                '}';
    }
}
