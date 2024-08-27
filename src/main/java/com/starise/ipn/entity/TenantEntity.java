package com.starise.ipn.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

import javax.persistence.*;

@Entity
@Table(name = "m_client")
public class TenantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "display_name")
    private String display_name;
    @Column(name = "account_no")
    private String account_no;
    @Column(name = "external_id")
    private String external_id;
    @Column(name = "mobile_no")
    private String mobile_no;


    public TenantEntity() {    }
    public TenantEntity(Long id, String display_name, String account_no, String external_id, String mobile_no) {
        this.id = id;
        this.display_name = display_name;
        this.account_no = account_no;
        this.external_id = external_id;
        this.mobile_no = mobile_no;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public String getAccount_no() {
        return account_no;
    }

    public void setAccount_no(String account_no) {
        this.account_no = account_no;
    }

    public String getExternal_id() {
        return external_id;
    }

    public void setExternal_id(String external_id) {
        this.external_id = external_id;
    }

    public String getMobile_no() {
        return mobile_no;
    }

    public void setMobile_no(String mobile_no) {
        this.mobile_no = mobile_no;
    }
}
