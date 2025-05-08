package com.malikyasir.landlawassist.Modelss;

public class User {
    private String fullName;   // Matches fullNameInput
    private String email;      // Matches emailInput
    private String password;   // Matches passwordInput
    private String phone;      // Matches phoneInput
    private String userType;   // Matches userTypeSpinner
    private String streetAddress;
    private String country;
    private int profileCompletion;
    // New fields for lawyer
    private String lawyerId;    // Unique ID for lawyer
    private String city;        // City where lawyer practices
    private String specialization; // Lawyer's specialization
    private String experience;  // Years of experience
    private String barCouncilNumber; // Bar council registration number
    private boolean isAvailable; // Whether lawyer is available for new cases
    private double rating;      // Lawyer's rating
    private int totalCases;     // Total cases handled
    private String profileImage; // Add this field
    private String id; // Add this field

    // Default constructor required for Firestore or Realtime Database
    public User() {
    }

    public User(String fullName, String email, String password, String phone, String userType) {
        this.fullName = fullName;
        this.email = email;
        this.password = password; // Password added to align with XML
        this.phone = phone;
        this.userType = userType;
        this.profileCompletion = 50; // 50% when first registered
        if ("Lawyer".equals(userType)) {
            this.lawyerId = generateLawyerId(); // Generate unique ID for lawyer
            this.isAvailable = true;
            this.rating = 0.0;
            this.totalCases = 0;
        }
    }

    private String generateLawyerId() {
        // Generate a unique ID for lawyer (e.g., LAW-XXXXX)
        return "LAW-" + System.currentTimeMillis();
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

    public String getStreetAddress() { return streetAddress; }
    public String getCountry() { return country; }
    public int getProfileCompletion() { return profileCompletion; }

    // New getters and setters for lawyer fields
    public String getLawyerId() { return lawyerId; }
    public void setLawyerId(String lawyerId) { this.lawyerId = lawyerId; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getBarCouncilNumber() { return barCouncilNumber; }
    public void setBarCouncilNumber(String barCouncilNumber) { this.barCouncilNumber = barCouncilNumber; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getTotalCases() { return totalCases; }
    public void setTotalCases(int totalCases) { this.totalCases = totalCases; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }
    public void setCountry(String country) { this.country = country; }
    public void setProfileCompletion(int profileCompletion) { this.profileCompletion = profileCompletion; }
}
