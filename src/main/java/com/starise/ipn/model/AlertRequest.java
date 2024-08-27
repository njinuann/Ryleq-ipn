package com.starise.ipn.model;



import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class AlertRequest {
    private int smsId;
    private String mobileNo;
    private String message;
    private String accountNo;
    private String clientId;
    private String clientName;
    private String messageType;
    private BigDecimal amount;
    private String receipt;
    private String detail;
    private String senderName;
    private String txnDate;
    private SmsTemplate smsTemplate;

 }
