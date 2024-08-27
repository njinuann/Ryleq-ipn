package com.starise.ipn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Timeline {
    public int[] submittedOnDate;
    public String submittedByUsername;
    public String submittedByFirstname;
    public String submittedByLastname;
    public int[] approvedOnDate;
    public String approvedByUsername;
    public String approvedByFirstname;
    public String approvedByLastname;
    public int[] expectedDisbursementDate;
    public int[] actualDisbursementDate;
    public String disbursedByUsername;
    public String disbursedByFirstname;
    public String disbursedByLastname;
    public int[] closedOnDate;
    public int[] expectedMaturityDate;
}
