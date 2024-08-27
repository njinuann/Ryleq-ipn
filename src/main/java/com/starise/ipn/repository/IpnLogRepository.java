package com.starise.ipn.repository;

import com.starise.ipn.entity.IpnLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IpnLogRepository extends JpaRepository<IpnLog, Long> {
    IpnLog findByTransId(String transId);
}
