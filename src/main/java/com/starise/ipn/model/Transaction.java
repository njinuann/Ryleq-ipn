package com.starise.ipn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transaction {
    private Integer id;
    private String transDate;
    private String valueDate;
    private String memberAcctNo;
    private BigDecimal transAmt;
    private String transRef;
    private Integer chargeType;
    private Integer estateId;
    private String description;
    private Boolean successFlag = false;
    private String failureReason;
    private String postedBy;
    private String mobilePhone;
    private Integer houseId;
    private String reversalFlag;
    private String reversalReason;
    private String mpesaReceiptNo;
    private String mpesaReference;
    private String mpesaStatus;
    private boolean excludeReversed;
    private Date startDate;
    private Date endDate;



}
