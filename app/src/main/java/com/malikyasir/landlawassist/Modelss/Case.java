package com.malikyasir.landlawassist.Modelss;

public class Case {
    private String userId;
    private String title;
    private String description;
    private String court;
    private String caseNumber;
    private long filingDate;
    private String id;
    private String status;

    public Case() {
    }

    public Case(String userId, String title, String description, String court) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.court = court;
        this.status = "ACTIVE";
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCourt() {
        return court;
    }

    public void setCourt(String court) {
        this.court = court;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public long getFilingDate() {
        return filingDate;
    }

    public void setFilingDate(long filingDate) {
        this.filingDate = filingDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
