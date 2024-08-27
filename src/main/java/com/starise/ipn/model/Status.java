package com.starise.ipn.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Status {
    public int id;
    public String code;
    public String value;
    public boolean pendingApproval;
    public boolean waitingForDisbursal;
    public boolean active;
    public boolean closedObligationsMet;
    public boolean closedWrittenOff;
    public boolean closedRescheduled;
    public boolean closed;
    public boolean overpaid;
}