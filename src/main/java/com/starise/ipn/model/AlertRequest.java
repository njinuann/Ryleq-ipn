package com.starise.ipn.model;



import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
public class AlertRequest {
    private int smsId;
    private String mobileNo;
    private String message;
    private String accountNo;
    private String clientId;
    private String clientName;
    private String messageType;
    private BigDecimal amount;
    private BigDecimal balance;
    private String receipt;
    private String detail;
    private String senderName;
    private String txnDate;
    private BigDecimal threshold;
    private SmsTemplate smsTemplate;

 }
