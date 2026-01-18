package com.bank.api;

import java.util.Date;

public class Customer {
    private String id;
    private String name;
    private String identificationNo;
    private String phoneNo;
    private String address;
    private String email;
    private String password;
    private String status;
    private Date createdAt;

    public Customer() {}

    public Customer(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Customer(String id, String name, String identificationNo, String phoneNo, String address, String email, String password, String status, Date createdAt) {
        this.id = id;
        this.name = name;
        this.identificationNo = identificationNo;
        this.phoneNo = phoneNo;
        this.address = address;
        this.email = email;
        this.password = password;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIdentificationNo() { return identificationNo; }
    public void setIdentificationNo(String identificationNo) { this.identificationNo = identificationNo; }
    public String getPhoneNo() { return phoneNo; }
    public void setPhoneNo(String phoneNo) { this.phoneNo = phoneNo; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
