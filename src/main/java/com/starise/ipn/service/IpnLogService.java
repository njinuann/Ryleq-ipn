package com.starise.ipn.service;

import com.starise.ipn.entity.IpnLog;
import com.starise.ipn.repository.IpnLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;

@Service
public class IpnLogService {

    @Autowired
    private IpnLogRepository repository;

    @Transactional
    public IpnLog insertOrUpdate(IpnLog log) {
        IpnLog existingLog = repository.findByTransId(log.getTransId());
        if (existingLog != null) {
            // Update existing entry
            existingLog.setTransactionType(log.getTransactionType());
            existingLog.setBillRefNumber(log.getBillRefNumber());
            existingLog.setOrgAccountBalance(log.getOrgAccountBalance());
            existingLog.setMsisdn(log.getMsisdn());
            existingLog.setFirstName(log.getFirstName());
            existingLog.setTransAmount(log.getTransAmount());
            existingLog.setThirdPartyTransId(log.getThirdPartyTransId());
            existingLog.setInvoiceNumber(log.getInvoiceNumber());
            existingLog.setTransTime(log.getTransTime());
            existingLog.setBusinessShortCode(log.getBusinessShortCode());
            existingLog.setStatus(log.getStatus());
            return repository.save(existingLog);
        } else {
            // Insert new entry
            return repository.save(log);
        }
    }
}

