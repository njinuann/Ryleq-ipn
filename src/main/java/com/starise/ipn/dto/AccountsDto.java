package com.starise.ipn.dto;



public class AccountsDto {
    private Long id;
    private String display_name;
    private String account_no;
    private String external_id;
    private String mobile_no;
    private String productName;
    private String productPrefix;
    private Double account_balance;

    public Double getAccount_balance() {
        return account_balance;
    }

    public void setAccount_balance(Double account_balance) {
        this.account_balance = account_balance;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductPrefix() {
        return productPrefix;
    }

    public void setProductPrefix(String productPrefix) {
        this.productPrefix = productPrefix;
    }

    public AccountsDto() {    }
    public AccountsDto(Long id, String display_name, String account_no, String external_id, String mobile_no) {
        this.id = id;
        this.display_name = display_name;
        this.account_no = account_no;
        this.external_id = external_id;
        this.mobile_no = mobile_no;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public String getAccount_no() {
        return account_no;
    }

    public void setAccount_no(String account_no) {
        this.account_no = account_no;
    }

    public String getExternal_id() {
        return external_id;
    }

    public void setExternal_id(String external_id) {
        this.external_id = external_id;
    }

    public String getMobile_no() {
        return mobile_no;
    }

    public void setMobile_no(String mobile_no) {
        this.mobile_no = mobile_no;
    }
}
