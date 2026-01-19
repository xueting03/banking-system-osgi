package com.bank.cli;

import java.math.BigDecimal;

import com.bank.api.DepositAccount;
import com.bank.api.IDepositAccountService;

public class DepositCommands {

    private final IDepositAccountService depositService;

    public DepositCommands(IDepositAccountService depositService) {
        this.depositService = depositService;
    }

    /**
     * Create a new deposit account
     * deposit:create <ic> <password> [initialBalance]
     */
    public String create(String ic, String password, String... args) {
        try {
            BigDecimal initialBalance = null;
            
            // Parse optional initialBalance
            if (args != null && args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
                try {
                    initialBalance = new BigDecimal(args[0]);
                } catch (NumberFormatException e) {
                    return "Error: Invalid initial balance format. Please provide a valid number.";
                }
            }
            
            DepositAccount account = depositService.createDepositAccount(ic, password, initialBalance);
            if (account == null) {
                return "ERROR: Failed to create deposit account. Please check your credentials and try again.";
            }
            
            return String.format("SUCCESS: Deposit account created (ID: %s, Initial Balance: $%s, Status: %s)",
                account.getAccountId(), account.getBalance(), account.getStatus());
        } catch (Exception e) {
            return "Error creating deposit account: " + e.getMessage();
        }
    }

    /**
     * Get deposit account details
     * deposit:get <ic> <password>
     */
    public String get(String ic, String password) {
        try {
            DepositAccount account = depositService.getDepositAccount(ic, password);
            if (account == null) {
                return "ERROR: No deposit account found or invalid credentials.";
            }
            
            return String.format("SUCCESS: Account ID: %s, Status: %s, Balance: $%s, Created: %s",
                account.getAccountId(), account.getStatus(), account.getBalance(), account.getCreatedAt());
        } catch (Exception e) {
            return "Error retrieving deposit account: " + e.getMessage();
        }
    }

    /**
     * Deposit funds into account
     * deposit:deposit <ic> <password> <amount>
     */
    public String deposit(String ic, String password, String amount) {
        try {
            BigDecimal amountValue = new BigDecimal(amount);
            DepositAccount account = depositService.depositFunds(ic, password, amountValue);
            if (account == null) {
                return "ERROR: Failed to deposit funds. Please check credentials and account status.";
            }
            
            return String.format("SUCCESS: Deposited $%s. New Balance: $%s",
                amountValue, account.getBalance());
        } catch (NumberFormatException e) {
            return "Error: Invalid amount format. Please provide a valid number.";
        } catch (Exception e) {
            return "Error depositing funds: " + e.getMessage();
        }
    }

    /**
     * Withdraw funds from account
     * deposit:withdraw <ic> <password> <amount>
     */
    public String withdraw(String ic, String password, String amount) {
        try {
            BigDecimal amountValue = new BigDecimal(amount);
            DepositAccount account = depositService.withdrawFunds(ic, password, amountValue);
            if (account == null) {
                return "ERROR: Failed to withdraw funds. Please check credentials, balance, and account status.";
            }
            
            return String.format("SUCCESS: Withdrew $%s. New Balance: $%s",
                amountValue, account.getBalance());
        } catch (NumberFormatException e) {
            return "Error: Invalid amount format. Please provide a valid number.";
        } catch (Exception e) {
            return "Error withdrawing funds: " + e.getMessage();
        }
    }

    /**
     * Freeze deposit account
     * deposit:freeze <ic> <password>
     */
    public String freeze(String ic, String password) {
        try {
            DepositAccount account = depositService.updateDepositAccountStatus(ic, password, "FREEZE");
            if (account == null) {
                return "ERROR: Failed to freeze account. Please check credentials and account status.";
            }
            
            return String.format("SUCCESS: Account frozen (ID: %s, Status: %s)",
                account.getAccountId(), account.getStatus());
        } catch (Exception e) {
            return "Error freezing account: " + e.getMessage();
        }
    }

    /**
     * Unfreeze deposit account
     * deposit:unfreeze <ic> <password>
     */
    public String unfreeze(String ic, String password) {
        try {
            DepositAccount account = depositService.updateDepositAccountStatus(ic, password, "UNFREEZE");
            if (account == null) {
                return "ERROR: Failed to unfreeze account. Please check credentials and account status.";
            }
            
            return String.format("SUCCESS: Account unfrozen (ID: %s, Status: %s)",
                account.getAccountId(), account.getStatus());
        } catch (Exception e) {
            return "Error unfreezing account: " + e.getMessage();
        }
    }

    /**
     * Close deposit account
     * deposit:close <ic> <password>
     */
    public String close(String ic, String password) {
        try {
            DepositAccount account = depositService.closeDepositAccount(ic, password);
            if (account == null) {
                return "ERROR: Failed to close account. Please check credentials.";
            }
            
            return String.format("SUCCESS: Deposit account closed (ID: %s, Final Balance: $%s)",
                account.getAccountId(), account.getBalance());
        } catch (Exception e) {
            return "Error closing deposit account: " + e.getMessage();
        }
    }
}
