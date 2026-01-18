package com.bank.deposit;

import java.math.BigDecimal;

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
        
        // Demo: Test all deposit account operations
        demoDepositAccounts(depositService);
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("=== Deposit Account Service Bundle Stopped ===");
        
        if (registration != null) {
            registration.unregister();
        }
    }
    
    private void demoDepositAccounts(IDepositAccountService depositService) {
        System.out.println("\n========== DEPOSIT ACCOUNT DEMO ==========");
        
        // Create Deposit Account
        System.out.println("\n--- Create Deposit Account ---");
        System.out.println(depositService.createDepositAccount("030119-08-3006", "alice123", new BigDecimal("100.00")));
        System.out.println(depositService.createDepositAccount("030220-08-3006", "bob456789", new BigDecimal("250.00")));
        
        // Try to create duplicate (should fail)
        System.out.println("\nAttempting to create duplicate account:");
        System.out.println(depositService.createDepositAccount("030119-08-3006", "alice123", new BigDecimal("50.00")));
        
        // Try with wrong password (should fail)
        System.out.println("\nAttempting to create with wrong password:");
        System.out.println(depositService.createDepositAccount("030331-08-3006", "wrongpass", null));
        
        // Get Deposit Account
        System.out.println("\n--- Get Deposit Account ---");
        System.out.println(depositService.getDepositAccount("030119-08-3006", "alice123"));
        
        // Try with wrong password (should fail)
        System.out.println("\nAttempting to get account with wrong password:");
        System.out.println(depositService.getDepositAccount("030119-08-3006", "wrongpass"));
        
        // Deposit Funds
        System.out.println("\n--- Deposit Funds ---");
        System.out.println(depositService.depositFunds("030119-08-3006", "alice123", new BigDecimal("1000.00")));
        System.out.println(depositService.depositFunds("030119-08-3006", "alice123", new BigDecimal("500.50")));
        
        // Try negative amount (should fail)
        System.out.println("\nAttempting to deposit negative amount:");
        System.out.println(depositService.depositFunds("030119-08-3006", "alice123", new BigDecimal("-100")));
        
        // Withdraw Funds
        System.out.println("\n--- Withdraw Funds ---");
        System.out.println(depositService.withdrawFunds("030119-08-3006", "alice123", new BigDecimal("300.00")));
        
        // Try to withdraw more than balance (should fail)
        System.out.println("\nAttempting to withdraw more than balance:");
        System.out.println(depositService.withdrawFunds("030119-08-3006", "alice123", new BigDecimal("5000.00")));
        
        // Update Status (Freeze)
        System.out.println("\n--- Freeze Account ---");
        System.out.println(depositService.updateDepositAccountStatus("030119-08-3006", "alice123", "FREEZE"));
        
        // Try to deposit while frozen (should fail)
        System.out.println("\nAttempting to deposit while frozen:");
        System.out.println(depositService.depositFunds("030119-08-3006", "alice123", new BigDecimal("100.00")));
        
        // Try to freeze again (should fail)
        System.out.println("\nAttempting to freeze already frozen account:");
        System.out.println(depositService.updateDepositAccountStatus("030119-08-3006", "alice123", "FREEZE"));
        
        // Update Status (Unfreeze)
        System.out.println("\n--- Unfreeze Account ---");
        System.out.println(depositService.updateDepositAccountStatus("030119-08-3006", "alice123", "UNFREEZE"));
        
        // Now deposit should work
        System.out.println("\nDepositing after unfreeze:");
        System.out.println(depositService.depositFunds("030119-08-3006", "alice123", new BigDecimal("250.00")));
        
        // Close Deposit Account
        System.out.println("\n--- Close Deposit Account ---");
        System.out.println(depositService.closeDepositAccount("030220-08-3006", "bob456789"));
        
        // Try to deposit to closed account (should fail)
        System.out.println("\nAttempting to deposit to closed account:");
        System.out.println(depositService.depositFunds("030220-08-3006", "bob456789", new BigDecimal("100.00")));
        
        // Try to update status of closed account (should fail)
        System.out.println("\nAttempting to freeze closed account:");
        System.out.println(depositService.updateDepositAccountStatus("030220-08-3006", "bob456789", "FREEZE"));
        
        // Final status check
        System.out.println("\n--- Final Account Status ---");
        System.out.println(depositService.getDepositAccount("030119-08-3006", "alice123"));
        
        System.out.println("\n==========================================\n");
    }
}
