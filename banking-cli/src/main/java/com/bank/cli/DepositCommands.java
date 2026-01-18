package com.bank.cli;

import java.math.BigDecimal;

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
            
            return depositService.createDepositAccount(ic, password, initialBalance);
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
            return depositService.getDepositAccount(ic, password);
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
            return depositService.depositFunds(ic, password, amountValue);
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
            return depositService.withdrawFunds(ic, password, amountValue);
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
            return depositService.updateDepositAccountStatus(ic, password, "FREEZE");
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
            return depositService.updateDepositAccountStatus(ic, password, "UNFREEZE");
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
            return depositService.closeDepositAccount(ic, password);
        } catch (Exception e) {
            return "Error closing deposit account: " + e.getMessage();
        }
    }
}
