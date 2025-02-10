package com.starise.ipn.entity;



import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;


import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.sql.Date;

@Entity
@Table(name = "m_client_identifier")
public class TenantIdEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "client_id")
    private Long client_id;
    @Column(name = "document_type_id")
    private int document_type_id;
    @Column(name = "document_key")
    private String document_key;
    @Column(name = "description")
    private String description;

    public TenantIdEntity() {
    }

    public TenantIdEntity(Long id, Long client_id, int document_type_id, String document_key, String description) {
        this.id = id;
        this.client_id = client_id;
        this.document_type_id = document_type_id;
        this.document_key = document_key;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClient_id() {
        return client_id;
    }

    public void setClient_id(Long client_id) {
        this.client_id = client_id;
    }

    public int getDocument_type_id() {
        return document_type_id;
    }

    public void setDocument_type_id(int document_type_id) {
        this.document_type_id = document_type_id;
    }

    public String getDocument_key() {
        return document_key;
    }

    public void setDocument_key(String document_key) {
        this.document_key = document_key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "TenantIdEntity{" +
                "id=" + id +
                ", client_id=" + client_id +
                ", document_type_id=" + document_type_id +
                ", document_key='" + document_key + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
