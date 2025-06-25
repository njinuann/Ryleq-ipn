package com.starise.ipn.dto;

import com.starise.ipn.entity.TenantIdEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TransactionResult {
private String responseMessage;
private String responseCode;
private TenantIdEntity tenantIdEntity;
}
