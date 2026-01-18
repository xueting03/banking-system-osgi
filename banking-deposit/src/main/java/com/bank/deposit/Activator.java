package com.bank.deposit;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.bank.api.ICustomerService;
import com.bank.api.IDepositAccountService;

public class Activator implements BundleActivator {
    
    private ServiceRegistration<IDepositAccountService> registration;
    private ICustomerService customerService;
    
    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("=== Deposit Account Service Bundle Started ===");
        
        // Get Customer Service
        ServiceReference<ICustomerService> ref = 
            context.getServiceReference(ICustomerService.class);
        
        if (ref == null) {
            System.out.println("ERROR: Customer Service not found!");
            return;
        }
        
        customerService = context.getService(ref);
        
        // Register Deposit Account Service
        IDepositAccountService depositService = new DepositAccountServiceImpl(customerService);
        registration = context.registerService(
            IDepositAccountService.class, 
            depositService, 
            null
        );
        
        System.out.println("Deposit Account Service registered successfully");
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("=== Deposit Account Service Bundle Stopped ===");
        
        if (registration != null) {
            registration.unregister();
        }
    }
}
