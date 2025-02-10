package com.starise.ipn.repository;

import com.starise.ipn.entity.IpnAlertEntity;
import com.starise.ipn.entity.IpnLog;
import com.starise.ipn.entity.TenantIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;

@Repository
public interface IpnAlertRepository extends JpaRepository<IpnAlertEntity, Long> {
    IpnAlertEntity findByTransId(Long id);

//    @Query("select t from ipn_alert t where t.trans_id = :trans_id")
//    IpnAlertEntity findAlertByTransId(@Param("trans_id") String trans_id);

    @Modifying
    @Query(value = "insert into ipn_alert (id, mobile_number, alert_type, alert_message,status,alert_date,trans_id)" +
            "values(:id, :mobile_number, :alert_type, :alert_message, :status, :alert_date, :trans_id )", nativeQuery = true)
    void insertAlert(
            @Param("id") Long id,
            @Param("mobile_number") String mobile_number,
            @Param("alert_type") String alert_type,
            @Param("alert_message") String alert_message,
            @Param("status") String status,
            @Param("alert_date") Date alert_date,
            @Param("trans_id") String trans_id
    );

    @Modifying
    @Query(value = "update ipn_alert  set id=:id, mobile_number =:mobile_number, alert_type=:alert_type, alert_message=:alert_message,status=:status,alert_date=:alert_date" +
            "where id = :id", nativeQuery = true)
    void updateAlert(
            @Param("id") Long id,
            @Param("mobile_number") String mobile_number,
            @Param("alert_type") String alert_type,
            @Param("alert_message") String alert_message,
            @Param("status") String status,
            @Param("alert_date") Date alert_date
    );

    @Modifying
    @Query(value = "update ipn_alert  set id=:id, mobile_number =:mobile_number, alert_type=:alert_type, alert_message=:alert_message,status=:status,alert_date=:alert_date" +
            "where alert_type = :alert_type", nativeQuery = true)
    void updateAlertByType(
            @Param("id") Long id,
            @Param("mobile_number") String mobile_number,
            @Param("alert_type") String alert_type,
            @Param("alert_message") String alert_message,
            @Param("status") String status,
            @Param("alert_date") Date alert_date
    );

}
