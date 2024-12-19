package com.malikyasir.landlawassist.Modelss;
public class Case {
    private String userId;
    private String title;
    private String description;
    private String court;
    private String caseNumber;
    private long filingDate;

    public Case(String userId, String title, String description, String court) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.court = court;
    }

    // Getter and setter methods...

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public void setFilingDate(long filingDate) {
        this.filingDate = filingDate;
    }

}