package com.starise.ipn.entity;


import javax.persistence.*;
import java.sql.Date;

@Entity
@Table(name = "ipn_alert")
public class IpnAlertEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "alert_type")
    private String alertType;
    @Column(name = "alert_message")
    private String alertMessage;
    @Column(name = "status")
    private String status;
    @Column(name = "alert_date")
    private Date alertDate;
    @Column(name = "trans_id")
    private String transId;
}
