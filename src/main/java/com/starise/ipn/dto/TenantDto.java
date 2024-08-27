package com.starise.ipn.dto;


import java.math.BigDecimal;
import java.sql.Date;

public class TenantDto {

    private String tenantName;
    private String tenantCode;
    private String acctNo;
    private BigDecimal loanBalance;
    private Date dueDate;
    private String acctType;
    private  String idNo;
    private String idType;

       public TenantDto(String tenantName, String tenantCode, String acctNo, BigDecimal loanBalance, Date dueDate, String acctType, String idNo, String idType) {
        this.tenantName = tenantName;
        this.tenantCode = tenantCode;
        this.acctNo = acctNo;
        this.loanBalance = loanBalance;
        this.dueDate = dueDate;
        this.acctType = acctType;
        this.idNo = idNo;
        this.idType = idType;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }
    public String getIdNo() {
        return idNo;
    }

    public void setIdNo(String idNo) {
        this.idNo = idNo;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getAcctNo() {
        return acctNo;
    }

    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }

    public BigDecimal getLoanBalance() {
        return loanBalance;
    }

    public void setLoanBalance(BigDecimal loanBalance) {
        this.loanBalance = loanBalance;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getAcctType() {
        return acctType;
    }

    public void setAcctType(String acctType) {
        this.acctType = acctType;
    }
}
