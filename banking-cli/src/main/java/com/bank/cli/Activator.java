package com.bank.cli;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.bank.api.ICustomerService;
import com.bank.api.IDepositAccountService;

public class Activator implements BundleActivator {

    private ServiceTracker<IDepositAccountService, IDepositAccountService> depositServiceTracker;
    private ServiceTracker<ICustomerService, ICustomerService> customerServiceTracker;
    private ServiceRegistration<?> depositCommandServiceRegistration;
    private ServiceRegistration<?> customerCommandServiceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("Banking CLI Bundle Starting...");

        // Track IDepositAccountService
        depositServiceTracker = new ServiceTracker<>(
            context,
            IDepositAccountService.class,
            new DepositServiceTrackerCustomizer(context)
        );
        depositServiceTracker.open();
        
        // Track ICustomerService
        customerServiceTracker = new ServiceTracker<>(
            context,
            ICustomerService.class,
            new CustomerServiceTrackerCustomizer(context)
        );
        customerServiceTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("Banking CLI Bundle Stopping...");
        
        if (depositCommandServiceRegistration != null) {
            depositCommandServiceRegistration.unregister();
            depositCommandServiceRegistration = null;
        }
        
        if (customerCommandServiceRegistration != null) {
            customerCommandServiceRegistration.unregister();
            customerCommandServiceRegistration = null;
        }
        
        if (depositServiceTracker != null) {
            depositServiceTracker.close();
            depositServiceTracker = null;
        }
        
        if (customerServiceTracker != null) {
            customerServiceTracker.close();
            customerServiceTracker = null;
        }
    }

    /**
     * Tracks the lifecycle of the Deposit service.
     */
    private class DepositServiceTrackerCustomizer implements ServiceTrackerCustomizer<IDepositAccountService, IDepositAccountService> {
        
        private final BundleContext context;

        public DepositServiceTrackerCustomizer(BundleContext context) {
            this.context = context;
        }

        @Override
        public IDepositAccountService addingService(ServiceReference<IDepositAccountService> reference) {
            IDepositAccountService depositService = context.getService(reference);
            
            if (depositService != null) {
                System.out.println("Deposit service detected - Registering Gogo commands...");
                
                // Create command provider with the deposit service
                DepositCommands commands = new DepositCommands(depositService);
                
                // Register the command provider as an OSGi service
                Dictionary<String, Object> properties = new Hashtable<>();
                properties.put("osgi.command.scope", "deposit");
                properties.put("osgi.command.function", new String[] {
                    "create", "get", "deposit", "withdraw", "freeze", "unfreeze", "close"
                });
                
                depositCommandServiceRegistration = context.registerService(
                    DepositCommands.class.getName(),
                    commands,
                    properties
                );
                
                System.out.println("Gogo deposit commands registered successfully!");
            }
            
            return depositService;
        }

        @Override
        public void modifiedService(ServiceReference<IDepositAccountService> reference, IDepositAccountService service) {
            // No action needed on modification
        }

        @Override
        public void removedService(ServiceReference<IDepositAccountService> reference, IDepositAccountService service) {
            System.out.println("Deposit service removed - Unregistering commands...");
            
            if (depositCommandServiceRegistration != null) {
                depositCommandServiceRegistration.unregister();
                depositCommandServiceRegistration = null;
            }
            
            context.ungetService(reference);
        }
    }
    
    /**
     * Tracks the lifecycle of the Customer service.
     */
    private class CustomerServiceTrackerCustomizer implements ServiceTrackerCustomizer<ICustomerService, ICustomerService> {
        
        private final BundleContext context;

        public CustomerServiceTrackerCustomizer(BundleContext context) {
            this.context = context;
        }

        @Override
        public ICustomerService addingService(ServiceReference<ICustomerService> reference) {
            ICustomerService customerService = context.getService(reference);
            
            if (customerService != null) {
                System.out.println("Customer service detected - Registering Gogo commands...");
                
                // Create command provider with the customer service
                CustomerCommands commands = new CustomerCommands(customerService);
                
                // Register the command provider as an OSGi service
                Dictionary<String, Object> properties = new Hashtable<>();
                properties.put("osgi.command.scope", "customer");
                properties.put("osgi.command.function", new String[] {
                    "create", "get"
                });
                
                customerCommandServiceRegistration = context.registerService(
                    CustomerCommands.class.getName(),
                    commands,
                    properties
                );
                
                System.out.println("Gogo customer commands registered successfully!");
            }
            
            return customerService;
        }

        @Override
        public void modifiedService(ServiceReference<ICustomerService> reference, ICustomerService service) {
            // No action needed on modification
        }

        @Override
        public void removedService(ServiceReference<ICustomerService> reference, ICustomerService service) {
            System.out.println("Customer service removed - Unregistering commands...");
            
            if (customerCommandServiceRegistration != null) {
                customerCommandServiceRegistration.unregister();
                customerCommandServiceRegistration = null;
            }
            
            context.ungetService(reference);
        }
    }
}
