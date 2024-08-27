package com.starise.ipn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.starise.ipn.entity.TenantIdEntity;

public interface TenantIdRepository extends JpaRepository<TenantIdEntity, Long> {

    @Query("select t from TenantIdEntity t where t.document_key = :document_key")
    TenantIdEntity findByIdNo(@Param("document_key") String documentKey );

    @Query("select t from TenantIdEntity t where t.client_id = :client_id")
    TenantIdEntity findClientByIdNo(@Param("client_id") Long clientId);

}
