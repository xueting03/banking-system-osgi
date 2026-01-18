package com.bank.customer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import com.bank.api.ICustomerService;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    private ServiceRegistration<?> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        registration = context.registerService(
            ICustomerService.class.getName(),
            new CustomerServiceImpl(),
            null
        );
        System.out.println("=== Customer Service Bundle Started ===");
        System.out.println("Customer Service registered successfully");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("=== Customer Service Bundle Stopped ===");
        if (registration != null) {
            registration.unregister();
        }
    }
}