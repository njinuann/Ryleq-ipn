package com.starise.ipn.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanType {
    public int id;
    public String code;
    public String value;
}
