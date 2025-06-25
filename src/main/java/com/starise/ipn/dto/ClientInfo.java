package com.starise.ipn.dto;

import com.starise.ipn.entity.TenantIdEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class ClientInfo {
    private String clientNo;
    private String bankCode;
    private String mpesaReceiptNo;
    private String mpesaSender;
    private String mpesaTransTime;
    private BigDecimal mpesaAmount;
    private String accountType;
    private AccountsDto accountsDto;
    private TenantIdEntity tenantDto;
    private boolean loan;
}
