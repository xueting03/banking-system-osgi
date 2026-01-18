package com.bank.api;

public interface ICustomerService {
    Customer createCustomer(String name, String email);
    Customer updateCustomer(String id, String name, String email);
    Customer getCustomer(String id);
    boolean verifyLogin(String id, String password);
}
