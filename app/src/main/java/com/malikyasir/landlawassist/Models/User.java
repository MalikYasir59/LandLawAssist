package com.malikyasir.landlawassist.Models;

public class User {
    private String fullName;   // Matches fullNameInput
    private String email;      // Matches emailInput
    private String password;   // Matches passwordInput
    private String phone;      // Matches phoneInput
    private String userType;   // Matches userTypeSpinner

    // Default constructor required for Firestore or Realtime Database
    public User() {
    }

    public User(String fullName, String email, String password, String phone, String userType) {
        this.fullName = fullName;
        this.email = email;
        this.password = password; // Password added to align with XML
        this.phone = phone;
        this.userType = userType;
    }

    // Getters
    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPhone() {
        return phone;
    }

    public String getUserType() {
        return userType;
    }

    // Setters
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
