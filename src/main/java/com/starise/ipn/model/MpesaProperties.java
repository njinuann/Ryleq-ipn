package com.starise.ipn.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mpesa")
public class MpesaProperties {
    private String paybill;
    private String consumerKey;
    private String consumerSecret;
    private String tokenUrl;
    private String registerUrl;
    private String responseType;
    private String confirmationUrl;
    private String validationUrl;
    private String updateUrl;

    public String getPaybill() {
        return paybill;
    }

    public void setPaybill(String paybill) {
        this.paybill = paybill;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getRegisterUrl() {
        return registerUrl;
    }

    public void setRegisterUrl(String registerUrl) {
        this.registerUrl = registerUrl;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getConfirmationUrl() {
        return confirmationUrl;
    }

    public void setConfirmationUrl(String confirmationUrl) {
        this.confirmationUrl = confirmationUrl;
    }

    public String getValidationUrl() {
        return validationUrl;
    }

    public void setValidationUrl(String validationUrl) {
        this.validationUrl = validationUrl;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl;
    }
}