package com.starise.ipn.model;

import lombok.Data;

@Data
public class SmsTemplate {
    private int templateId;
    private String templateCode;
    private String template;
    private String status;
}
