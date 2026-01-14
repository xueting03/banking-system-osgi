package com.bank.account;

import com.bank.api.IAccountService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    
    private ServiceRegistration<IAccountService> registration;
    
    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("=== Account Service Bundle Started ===");
        
        IAccountService accountService = new AccountServiceImpl();
        registration = context.registerService(
            IAccountService.class, 
            accountService, 
            null
        );
        
        System.out.println("Account Service registered successfully");
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("=== Account Service Bundle Stopped ===");
        
        if (registration != null) {
            registration.unregister();
        }
    }
}