package com.starise.ipn.repository;

import com.starise.ipn.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<TenantEntity, Long> {

    @Query("SELECT t  FROM  TenantEntity t where id = :id")
    TenantEntity fetchById(@Param("id") Long id);

    @Query("SELECT t  FROM  TenantEntity t where external_id = :external_id")
    TenantEntity fetchByExternalId(@Param("external_id") String external_id);
}
