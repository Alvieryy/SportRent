package com.example.sportsrentfx.model;

public class Customer {
    private int customerId;
    private String firstName;
    private String lastName;
    private String contactNumber;
    private String email;
    private int rewardPoints;

    public Customer(int customerId, String firstName, String lastName, String contactNumber, String email, int rewardPoints) {
        this.customerId = customerId;
        this.firstName = firstName != null ? firstName.trim() : "";
        this.lastName = lastName != null ? lastName.trim() : "";
        this.contactNumber = contactNumber != null ? contactNumber.trim() : "";
        this.email = email != null ? email.trim() : "";
        this.rewardPoints = rewardPoints;
    }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName != null ? firstName.trim() : ""; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName != null ? lastName.trim() : ""; }

    public String getFullName() {
        return (firstName + " " + lastName).trim();
    }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber != null ? contactNumber.trim() : ""; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email != null ? email.trim() : ""; }

    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

    @Override
    public String toString() {
        return "#" + customerId + " - " + getFullName();
    }
}