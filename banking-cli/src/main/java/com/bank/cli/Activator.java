package com.bank.cli;

import java.util.Dictionary;
import java.util.Hashtable;

import com.bank.api.ICardService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.bank.api.ICustomerService;
import com.bank.api.ISupportTicketService;
import com.bank.api.IDepositAccountService;

public class Activator implements BundleActivator {

    private ServiceTracker<IDepositAccountService, IDepositAccountService> depositServiceTracker;
    private ServiceTracker<ICustomerService, ICustomerService> customerServiceTracker;
    private ServiceRegistration<?> depositCommandServiceRegistration;
    private ServiceRegistration<?> customerCommandServiceRegistration;
    private ServiceRegistration<?> supportCommandServiceRegistration;
    private ServiceTracker<ISupportTicketService, ISupportTicketService> supportServiceTracker;
    private ServiceRegistration<?> cardCommandServiceRegistration;
    private ServiceTracker<ICardService, ICardService> cardServiceTracker;

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

        // Track ISupportTicketService
        supportServiceTracker = new ServiceTracker<>(
            context,
            ISupportTicketService.class,
            new SupportServiceTrackerCustomizer(context)
        );
        supportServiceTracker.open();

        // Tract ICardService
        cardServiceTracker = new ServiceTracker<>(
            context,
            ICardService.class,
            new CardServiceTrackerCustomizer(context)
        );
        cardServiceTracker.open();
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

        if (supportCommandServiceRegistration != null) {
            supportCommandServiceRegistration.unregister();
            supportCommandServiceRegistration = null;
        }

        if (supportServiceTracker != null) {
            supportServiceTracker.close();
            supportServiceTracker = null;
        }

        if (cardCommandServiceRegistration != null) {
            cardCommandServiceRegistration.unregister();
            cardCommandServiceRegistration = null;
        }

        if (cardServiceTracker != null) {
            cardServiceTracker.close();
            cardServiceTracker = null;
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
                    "create", "get", "update", "login"
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

    /**
     * Tracks the lifecycle of the Support Ticket service.
     */
    private class SupportServiceTrackerCustomizer implements ServiceTrackerCustomizer<ISupportTicketService, ISupportTicketService> {

        private final BundleContext context;

        public SupportServiceTrackerCustomizer(BundleContext context) {
            this.context = context;
        }

        @Override
        public ISupportTicketService addingService(ServiceReference<ISupportTicketService> reference) {
            ISupportTicketService supportService = context.getService(reference);

            if (supportService != null) {
                System.out.println("Support service detected - Registering Gogo commands...");

                SupportCommands commands = new SupportCommands(supportService);

                Dictionary<String, Object> properties = new Hashtable<>();
                properties.put("osgi.command.scope", "support");
                properties.put("osgi.command.function", new String[] {
                    "create", "update", "assign", "status", "list", "get"
                });

                supportCommandServiceRegistration = context.registerService(
                    SupportCommands.class.getName(),
                    commands,
                    properties
                );

                System.out.println("Gogo support commands registered successfully!");
            }

            return supportService;
        }

        @Override
        public void modifiedService(ServiceReference<ISupportTicketService> reference, ISupportTicketService service) {
            // No action needed on modification
        }

        @Override
        public void removedService(ServiceReference<ISupportTicketService> reference, ISupportTicketService service) {
            System.out.println("Support service removed - Unregistering commands...");

            if (supportCommandServiceRegistration != null) {
                supportCommandServiceRegistration.unregister();
                supportCommandServiceRegistration = null;
            }

            context.ungetService(reference);
        }
    }

    /**
     * Tracks the lifecycle of the Card service.
     */
    private class CardServiceTrackerCustomizer implements ServiceTrackerCustomizer<ICardService, ICardService> {

        private final BundleContext context;

        public CardServiceTrackerCustomizer(BundleContext context) {
            this.context = context;
        }

        @Override
        public ICardService addingService(ServiceReference<ICardService> reference) {
            ICardService cardService = context.getService(reference);

            if (cardService != null) {
                System.out.println("Card service detected - Registering Gogo commands...");

                CardCommands commands = new CardCommands(cardService);

                Dictionary<String, Object> properties = new Hashtable<>();
                properties.put("osgi.command.scope", "card");
                properties.put("osgi.command.function", new String[]{
                        "create", "get", "status", "pin", "limit"
                });

                cardCommandServiceRegistration = context.registerService(
                        CardCommands.class.getName(),
                        commands,
                        properties
                );

                System.out.println("Gogo card commands registered successfully!");
            }

            return cardService;
        }

        @Override
        public void modifiedService(ServiceReference<ICardService> reference, ICardService service) {
            // No action needed on modification
        }

        @Override
        public void removedService(ServiceReference<ICardService> reference, ICardService service) {
            System.out.println("Card service removed - Unregistering commands...");

            if (cardCommandServiceRegistration != null) {
                cardCommandServiceRegistration.unregister();
                cardCommandServiceRegistration = null;
            }

            context.ungetService(reference);
        }
    }
}
