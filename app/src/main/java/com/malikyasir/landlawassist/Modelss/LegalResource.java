package com.malikyasir.landlawassist.Modelss;

public class LegalResource {
    private String caseNumber;
    private String title;
    private String description;
    private String date;
    private String pdfUrl;
    private String court;

    public LegalResource() {
        // Required empty constructor for Firebase
    }

    public LegalResource(String caseNumber, String title, String description, String date, String pdfUrl, String court) {
        this.caseNumber = caseNumber;
        this.title = title;
        this.description = description;
        this.date = date;
        this.pdfUrl = pdfUrl;
        this.court = court;
    }

    // Getters and Setters
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public String getCourt() { return court; }
    public void setCourt(String court) { this.court = court; }
} 