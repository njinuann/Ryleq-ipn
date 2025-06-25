package com.starise.ipn.dto;

import com.starise.ipn.entity.MpesaRequest;
import com.starise.ipn.model.AlertRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class VerifiedAccount {
    private BigDecimal loanAmount;
    private BigDecimal repaymentAmount;
    private BigDecimal excessPayment;
    private AlertRequest alertRequest;
    private ClientInfo clientInfo;
    private MpesaRequest mpesaRequest;

}
