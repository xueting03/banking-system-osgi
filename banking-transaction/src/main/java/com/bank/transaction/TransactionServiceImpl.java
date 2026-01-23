package com.bank.transaction;

import com.bank.api.ITransactionService;
import com.bank.api.ICustomerService;
import com.bank.api.model.Transaction;
import com.bank.api.model.TransactionSummary;
import com.bank.api.model.TransactionType;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


@Component(service = ITransactionService.class, immediate = true)
public class TransactionServiceImpl implements ITransactionService {

    @Reference
    private DataSource dataSource;

    @Reference
    private ICustomerService customerService;

   @Activate
void activate() {
    try (Connection conn = dataSource.getConnection();
         Statement stmt = conn.createStatement()) {

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS TRANSACTION (
                TXN_ID VARCHAR(64) PRIMARY KEY,
                ACCOUNT_ID VARCHAR(64) NOT NULL,
                TYPE VARCHAR(32) NOT NULL,
                AMOUNT DECIMAL(18,2) NOT NULL,
                NOTE VARCHAR(255),
                CREATED_AT TIMESTAMP NOT NULL
            )
        """);

    } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize TRANSACTION table", e);
    }
}


    @Override
public boolean recordTransaction(String identificationNo,
                                 String password,
                                 TransactionType type,
                                 BigDecimal amount,
                                 String note) {
        // Reject zero or negative amounts
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (!customerService.verifyLogin(identificationNo, password)) {
            return false;
        }

        try (Connection conn = dataSource.getConnection()) {

            // 1. Resolve account ID
            String accountId = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT ACCOUNT_ID FROM DEPOSIT_ACCOUNT WHERE CUSTOMER_ID = ?")) {

                ps.setString(1, identificationNo);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        accountId = rs.getString("ACCOUNT_ID");
                    }
                }
            }

            if (accountId == null) {
                return false;
            }

            // 2. Insert transaction
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO TRANSACTION (TXN_ID, ACCOUNT_ID, TYPE, AMOUNT, NOTE, CREATED_AT) " +
                    "VALUES (?, ?, ?, ?, ?, ?)")) {

                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, accountId);
                ps.setString(3, type.name());
                ps.setBigDecimal(4, amount);
                ps.setString(5, note);
                ps.setTimestamp(6, java.sql.Timestamp.valueOf(LocalDateTime.now()));

                ps.executeUpdate();
            }

            return true;

        } catch (Exception e) {
            throw new RuntimeException("Failed to record transaction", e);
        }
}

@Override
public List<Transaction> getTransactionHistory(String identificationNo,
                                                String password) {

    if (!customerService.verifyLogin(identificationNo, password)) {
        return Collections.emptyList();
    }

    try (Connection conn = dataSource.getConnection()) {

        String accountId = null;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ACCOUNT_ID FROM DEPOSIT_ACCOUNT WHERE CUSTOMER_ID = ?")) {

            ps.setString(1, identificationNo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    accountId = rs.getString("ACCOUNT_ID");
                }
            }
        }

        if (accountId == null) {
            return Collections.emptyList();
        }

        List<Transaction> transactions = new java.util.ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM TRANSACTION WHERE ACCOUNT_ID = ? ORDER BY CREATED_AT DESC")) {

            ps.setString(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(new Transaction(
                            rs.getString("TXN_ID"),
                            rs.getString("ACCOUNT_ID"),
                            TransactionType.valueOf(rs.getString("TYPE")),
                            rs.getBigDecimal("AMOUNT"),
                            rs.getString("NOTE"),
                            rs.getTimestamp("CREATED_AT").toLocalDateTime()
                    ));
                }
            }
        }

        return transactions;

    } catch (Exception e) {
        throw new RuntimeException("Failed to fetch transaction history", e);
    }
}


    @Override
public List<Transaction> filterTransactions(String identificationNo,
                                             String password,
                                             TransactionType type,
                                             LocalDateTime from,
                                             LocalDateTime to) {
        if (!customerService.verifyLogin(identificationNo, password)) {
            return Collections.emptyList();
        }
        try (Connection conn = dataSource.getConnection()) {
            String accountId = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT ACCOUNT_ID FROM DEPOSIT_ACCOUNT WHERE CUSTOMER_ID = ?")) {
                ps.setString(1, identificationNo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        accountId = rs.getString("ACCOUNT_ID");
                    }
                }
            }
            if (accountId == null) {
                return Collections.emptyList();
            }
            StringBuilder sql = new StringBuilder(
                    "SELECT * FROM TRANSACTION WHERE ACCOUNT_ID = ?");
            List<Object> params = new java.util.ArrayList<>();
            params.add(accountId);
            if (type != null) {
                sql.append(" AND TYPE = ?");
                params.add(type.name());
            }
            if (from != null) {
                sql.append(" AND CREATED_AT >= ?");
                params.add(java.sql.Timestamp.valueOf(from));
            }
            if (to != null) {
                sql.append(" AND CREATED_AT <= ?");
                params.add(java.sql.Timestamp.valueOf(to));
            }
            sql.append(" ORDER BY CREATED_AT DESC");
            List<Transaction> results = new java.util.ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        results.add(new Transaction(
                                rs.getString("TXN_ID"),
                                rs.getString("ACCOUNT_ID"),
                                TransactionType.valueOf(rs.getString("TYPE")),
                                rs.getBigDecimal("AMOUNT"),
                                rs.getString("NOTE"),
                                rs.getTimestamp("CREATED_AT").toLocalDateTime()
                        ));
                    }
                }
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to filter transactions", e);
        }
    }


    @Override
public TransactionSummary getTransactionSummary(String identificationNo,
                                                 String password,
                                                 LocalDateTime from,
                                                 LocalDateTime to) {

    if (!customerService.verifyLogin(identificationNo, password)) {
        return new TransactionSummary();
    }

    try (Connection conn = dataSource.getConnection()) {

        String accountId = null;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ACCOUNT_ID FROM DEPOSIT_ACCOUNT WHERE CUSTOMER_ID = ?")) {

            ps.setString(1, identificationNo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    accountId = rs.getString("ACCOUNT_ID");
                }
            }
        }

        if (accountId == null) {
            return new TransactionSummary();
        }

        StringBuilder sql = new StringBuilder(
                "SELECT TYPE, SUM(AMOUNT) AS TOTAL FROM TRANSACTION WHERE ACCOUNT_ID = ?");

        List<Object> params = new java.util.ArrayList<>();
        params.add(accountId);

        if (from != null) {
            sql.append(" AND CREATED_AT >= ?");
            params.add(java.sql.Timestamp.valueOf(from));
        }

        if (to != null) {
            sql.append(" AND CREATED_AT <= ?");
            params.add(java.sql.Timestamp.valueOf(to));
        }

        sql.append(" GROUP BY TYPE");

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TransactionType type = TransactionType.valueOf(rs.getString("TYPE"));
                    BigDecimal total = rs.getBigDecimal("TOTAL");

                    if (type == TransactionType.DEPOSIT || type == TransactionType.TRANSFER_IN) {
                        totalDeposits = totalDeposits.add(total);
                    } else if (type == TransactionType.WITHDRAWAL || type == TransactionType.TRANSFER_OUT) {
                        totalWithdrawals = totalWithdrawals.add(total);
                    }
                }
            }
        }

        return new TransactionSummary(
                totalDeposits,
                totalWithdrawals,
                totalDeposits.subtract(totalWithdrawals)
        );

    } catch (Exception e) {
        throw new RuntimeException("Failed to get transaction summary", e);
    }
}


   @Override
public boolean transfer(String fromIdentificationNo,
                        String password,
                        String toIdentificationNo,
                        BigDecimal amount) {
        // Reject zero or negative amounts
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (!customerService.verifyLogin(fromIdentificationNo, password)) {
            return false;
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            String fromAccount = null;
            String toAccount = null;
            BigDecimal fromBalance = BigDecimal.ZERO;

            // 1. Resolve sender account + balance
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT ACCOUNT_ID, BALANCE FROM DEPOSIT_ACCOUNT WHERE CUSTOMER_ID = ?")) {

                ps.setString(1, fromIdentificationNo);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        fromAccount = rs.getString("ACCOUNT_ID");
                        fromBalance = rs.getBigDecimal("BALANCE");
                    }
                }
            }

            // 2. Resolve receiver account
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT ACCOUNT_ID FROM DEPOSIT_ACCOUNT WHERE CUSTOMER_ID = ?")) {

                ps.setString(1, toIdentificationNo);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        toAccount = rs.getString("ACCOUNT_ID");
                    }
                }
            }

            if (fromAccount == null || toAccount == null) {
                conn.rollback();
                return false;
            }

            // 3. Balance check
            if (fromBalance.compareTo(amount) < 0) {
                conn.rollback();
                return false;
            }

            // 4. Update sender balance
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE DEPOSIT_ACCOUNT SET BALANCE = BALANCE - ? WHERE ACCOUNT_ID = ?")) {

                ps.setBigDecimal(1, amount);
                ps.setString(2, fromAccount);
                ps.executeUpdate();
            }

            // 5. Update receiver balance
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE DEPOSIT_ACCOUNT SET BALANCE = BALANCE + ? WHERE ACCOUNT_ID = ?")) {

                ps.setBigDecimal(1, amount);
                ps.setString(2, toAccount);
                ps.executeUpdate();
            }

            // 6. Record TRANSFER_OUT
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO TRANSACTION (TXN_ID, ACCOUNT_ID, TYPE, AMOUNT, NOTE, CREATED_AT) " +
                    "VALUES (?, ?, ?, ?, ?, ?)")) {

                ps.setString(1, java.util.UUID.randomUUID().toString());
                ps.setString(2, fromAccount);
                ps.setString(3, TransactionType.TRANSFER_OUT.name());
                ps.setBigDecimal(4, amount);
                ps.setString(5, "Transfer to " + toIdentificationNo);
                ps.setTimestamp(6, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            }

            // 7. Record TRANSFER_IN
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO TRANSACTION (TXN_ID, ACCOUNT_ID, TYPE, AMOUNT, NOTE, CREATED_AT) " +
                    "VALUES (?, ?, ?, ?, ?, ?)")) {

                ps.setString(1, java.util.UUID.randomUUID().toString());
                ps.setString(2, toAccount);
                ps.setString(3, TransactionType.TRANSFER_IN.name());
                ps.setBigDecimal(4, amount);
                ps.setString(5, "Transfer from " + fromIdentificationNo);
                ps.setTimestamp(6, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            throw new RuntimeException("Transfer failed", e);
        }
}

}
