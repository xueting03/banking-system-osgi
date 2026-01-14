package com.bank.transaction;

import com.bank.api.IAccountService;
import com.bank.api.ITransactionService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    
    private ServiceRegistration<ITransactionService> registration;
    private IAccountService accountService;
    
    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("=== Transaction Service Bundle Started ===");
        
        // Get Account Service from OSGi registry
        ServiceReference<IAccountService> ref = 
            context.getServiceReference(IAccountService.class);
        
        if (ref == null) {
            System.out.println("ERROR: Account Service not found!");
            return;
        }
        
        accountService = context.getService(ref);
        
        // Register Transaction Service
        ITransactionService transactionService = new TransactionServiceImpl(accountService);
        registration = context.registerService(
            ITransactionService.class, 
            transactionService, 
            null
        );
        
        System.out.println("Transaction Service registered successfully");
        
        // Demo: Create some test accounts and do a transfer
        demoTransactions();
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("=== Transaction Service Bundle Stopped ===");
        
        if (registration != null) {
            registration.unregister();
        }
    }
    
    private void demoTransactions() {
        System.out.println("%n========== BANKING SYSTEM DEMO ==========");
        
        // Create accounts
        accountService.createAccount("ACC001", "Alice Johnson", 1000.0);
        accountService.createAccount("ACC002", "Bob Smith", 500.0);
        
        System.out.println("%nInitial Balances:");
        System.out.printf("Alice: $%.2f%n", accountService.getBalance("ACC001"));
        System.out.printf("Bob: $%.2f%n", accountService.getBalance("ACC002"));
        
        // Deposit
        accountService.deposit("ACC001", 200.0);
        
        // Withdraw
        accountService.withdraw("ACC002", 100.0);
        
        System.out.println("%n=========================================%n");
    }
}