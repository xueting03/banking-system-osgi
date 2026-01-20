package com.bank.card;

import com.bank.api.Card;
import com.bank.api.ICardService;
import com.bank.api.ICustomerService;
import com.bank.api.IDepositAccountService;
import com.bank.api.DepositAccount;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.sql.DataSource;

@Component(service = ICardService.class, immediate = true)
public class CardServiceImpl implements ICardService {

    @Reference
    private ICustomerService customerService;

    @Reference
    private IDepositAccountService depositAccountService;

    @Reference
    private DataSource dataSource;

    private static final int DEFAULT_TRANSACTION_LIMIT = 5000;
    private static final int MAX_TRANSACTION_LIMIT = 10000;
    private static final int MIN_TRANSACTION_LIMIT = 100;

    @Activate
    void activate() {
        try (Connection connection = dataSource.getConnection()) {
            initSchema(connection);
            System.out.println("=== Card Service Activated ===");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize CardServiceImpl", e);
        }
    }

    private void initSchema(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS CARD ("
                + "ID VARCHAR(36) PRIMARY KEY, "
                + "ACCOUNT_ID VARCHAR(36) NOT NULL,"
                + "CARD_NUMBER VARCHAR(36) NOT NULL UNIQUE, "
                + "TRANSACTION_LIMIT INT NOT NULL, "
                + "STATUS VARCHAR(32) NOT NULL DEFAULT 'INACTIVE', "
                + "PIN_NUMBER VARCHAR(6) NOT NULL, "
                + "CREATED_AT TIMESTAMP NOT NULL"
                + ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    @Override
    public Card createCard(String identificationNo, String password, String pinNumber) {
        if (isBlank(identificationNo) || isBlank(password) || isBlank(pinNumber)) {
            System.out.println("Card creation failed: identificationNo, password and pin are required.");
            return null;
        }

        if (!isPinValid(pinNumber)) {
            System.out.println("Card creation failed: PIN number must be a 6-digit numeric string.");
            return null;
        }

        // Customer credentials verified when getting deposit account
        DepositAccount acc = getDepositEntity(identificationNo, password);
        if (acc == null || acc.getAccountId() == null) {
            System.out.println("Card creation failed: deposit account not found.");
            return null;
        }

        String status = acc.getStatus();
        if (status == null || !status.equalsIgnoreCase("Active")) {
            System.out.println("Card creation failed: deposit account must be Active.");
            return null;
        }

        String accountId = acc.getAccountId();
        // ensure no existing card for this account
        if (getCardByAccount(acc) != null) {
            System.out.println("Card creation failed: account already has a card.");
            return null;
        }

        String cardId = UUID.randomUUID().toString();
        Card card = new Card(cardId, accountId, generateCardNumber(), DEFAULT_TRANSACTION_LIMIT, Card.CardStatus.INACTIVE, pinNumber);
        String insertSql = "INSERT INTO CARD (ID, ACCOUNT_ID, CARD_NUMBER, TRANSACTION_LIMIT, STATUS, PIN_NUMBER, CREATED_AT) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(insertSql)) {
            ps.setString(1, card.getId());
            ps.setString(2, card.getAccountId());
            ps.setString(3, card.getCardNumber());
            ps.setInt(4, card.getTransactionLimit());
            ps.setString(5, card.getStatus().name());
            ps.setString(6, card.getPinNumber());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Card creation failed: " + e.getMessage());
            return null;
        }
        System.out.println("Card created successfully with card number " + card.getCardNumber());
        return card;
    }

    @Override
    public Card getCard(String identificationNo, String password) {
        if (isBlank(identificationNo) || isBlank(password)) {
            System.out.println("Card retrieval failed: identificationNo and password are required.");
            return null;
        }

        // Customer credentials verified when getting deposit account
        DepositAccount acc = getDepositEntity(identificationNo, password);
        if (acc == null || acc.getAccountId() == null) {
            System.out.println("Card retrieval failed: deposit account not found.");
            return null;
        }
        return getCardByAccount(acc);
    }

    @Override
    public Card updateCardPin(String identificationNo, String password, String currentPin, String newPin) {
        if (isBlank(identificationNo) || isBlank(password) || isBlank(currentPin) || isBlank(newPin)) {
            System.out.println("Update PIN failed: identificationNo, password, currentPin and newPin are required.");
            return null;
        }

        if (!isPinValid(newPin)) {
            System.out.println("Update PIN failed: new PIN must be 6 digits.");
            return null;
        }

        // Customer credentials verified when getting deposit account
        DepositAccount acc = getDepositEntity(identificationNo, password);
        if (acc == null || acc.getAccountId() == null) {
            System.out.println("Update PIN failed: deposit account not found.");
            return null;
        }

        Card card = getCardByAccount(acc);
        if (card == null) {
            System.out.println("Update PIN failed: card not found.");
            return null;
        }
        if (card.getStatus() != Card.CardStatus.ACTIVE) {
            System.out.println("Update PIN failed: card is not ACTIVE.");
            return null;
        }
        if (!Objects.equals(card.getPinNumber(), currentPin)) {
            System.out.println("Update PIN failed: current PIN incorrect.");
            return null;
        }

        card.setPinNumber(newPin);
        String updateSql = "UPDATE CARD SET PIN_NUMBER = ? WHERE ACCOUNT_ID = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setString(1, newPin);
            ps.setString(2, card.getAccountId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Update PIN failed: " + e.getMessage());
            return null;
        }
        System.out.println("PIN updated for card " + card.getCardNumber());
        return card;
    }

    @Override
    public Card updateCardStatus(String identificationNo, String password, UpdateAction action, String pinNumber) {
        if (isBlank(identificationNo) || isBlank(password) || action == null || isBlank(pinNumber)) {
            System.out.println("Update status failed: identificationNo, password, action and pinNumber are required.");
            return null;
        }

        // Customer credentials verified when getting deposit account
        DepositAccount acc = getDepositEntity(identificationNo, password);
        if (acc == null || acc.getAccountId() == null) {
            System.out.println("Update status failed: deposit account not found.");
            return null;
        }
        String depositStatus = acc.getStatus();
        if (depositStatus == null) {
            System.out.println("Update status failed: linked deposit account status unknown.");
            return null;
        }

        Card card = getCardByAccount(acc);
        if (card == null) {
            System.out.println("Update status failed: card not found.");
            return null;
        }
        if (!Objects.equals(card.getPinNumber(), pinNumber)) {
            System.out.println("Update status failed: PIN incorrect.");
            return null;
        }

        switch (action) {
            case ACTIVATE:
                if (!depositStatus.equalsIgnoreCase("Active")) {
                    System.out.println("Activate failed: linked deposit account must be Active.");
                    return null;
                }
                if (card.getStatus() == Card.CardStatus.ACTIVE) {
                    System.out.println("Activate failed: card already ACTIVE.");
                    return null;
                }
                if (card.getStatus() == Card.CardStatus.FROZEN) {
                    System.out.println("Activate failed: frozen card cannot be activated. Please unfreeze the card instead of activating.");
                    return null;
                }
                card.setStatus(Card.CardStatus.ACTIVE);
                break;
            case DEACTIVATE:
                if (card.getStatus() == Card.CardStatus.INACTIVE) {
                    System.out.println("Deactivate failed: card already INACTIVE.");
                    return null;
                }
                if (card.getStatus() == Card.CardStatus.FROZEN) {
                    System.out.println("Deactivate failed: frozen card cannot be deactivated.");
                    return null;
                }
                card.setStatus(Card.CardStatus.INACTIVE);
                break;
            case FREEZE:
                if (card.getStatus() == Card.CardStatus.FROZEN) {
                    System.out.println("Freeze failed: card already FROZEN.");
                    return null;
                }
                if (card.getStatus() != Card.CardStatus.ACTIVE) {
                    System.out.println("Freeze failed: only ACTIVE cards can be frozen.");
                    return null;
                }
                card.setStatus(Card.CardStatus.FROZEN);
                break;
            case UNFREEZE:
                if (!depositStatus.equalsIgnoreCase("Active")) {
                    System.out.println("Unfreeze failed: linked deposit account must be Active.");
                    return null;
                }
                if (card.getStatus() != Card.CardStatus.FROZEN) {
                    System.out.println("Unfreeze failed: only FROZEN cards can be unfrozen.");
                    return null;
                }
                card.setStatus(Card.CardStatus.ACTIVE);
                break;
            default:
                System.out.println("Update status failed: invalid action.");
                return null;
        }

        String updateSql = "UPDATE CARD SET STATUS = ? WHERE ACCOUNT_ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setString(1, card.getStatus().name());
            ps.setString(2, card.getAccountId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Update status failed: " + e.getMessage());
            return null;
        }
        System.out.println("Status updated to " + card.getStatus() + " for card " + card.getCardNumber());
        return card;
    }

    @Override
    public Card updateCardTransactionLimit(String identificationNo, String password, int newLimit, String pinNumber) {
        if (isBlank(identificationNo) || isBlank(password) || isBlank(pinNumber)) {
            System.out.println("Update limit failed: identificationNo, password, newLimit and pinNumber are required.");
            return null;
        }

        // Customer credentials verified when getting deposit account
        DepositAccount acc = getDepositEntity(identificationNo, password);
        if (acc == null || acc.getAccountId() == null) {
            System.out.println("Update limit failed: deposit account not found.");
            return null;
        }
        Card card = getCardByAccount(acc);

        if (card == null) {
            System.out.println("Update limit failed: card not found.");
            return null;
        }
        if (!Objects.equals(card.getPinNumber(), pinNumber)) {
            System.out.println("Update limit failed: PIN incorrect.");
            return null;
        }
        if (card.getStatus() != Card.CardStatus.ACTIVE) {
            System.out.println("Update limit failed: card is not ACTIVE.");
            return null;
        }
        if (newLimit <= MIN_TRANSACTION_LIMIT || newLimit > MAX_TRANSACTION_LIMIT) {
            System.out.println("Update limit failed: limit must be > " + MIN_TRANSACTION_LIMIT + " and <= " + MAX_TRANSACTION_LIMIT);
            return null;
        }

        card.setTransactionLimit(newLimit);
        String updateSql = "UPDATE CARD SET TRANSACTION_LIMIT = ? WHERE ACCOUNT_ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setInt(1, newLimit);
            ps.setString(2, card.getAccountId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Update limit failed: " + e.getMessage());
            return null;
        }
        System.out.println("Transaction limit updated to RM" + newLimit + " for card " + card.getCardNumber());
        return card;
    }

    private boolean verifyCustomer(String identificationNo, String password) {
        return customerService.verifyLogin(identificationNo, password);
    }

    private DepositAccount getDepositEntity(String identificationNo, String password) {
        return depositAccountService.getDepositAccount(identificationNo, password);
    }

    private boolean isPinValid(String pin) {
        return pin != null && pin.matches("\\d{6}");
    }

    private String generateCardNumber() {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(16);
        while (sb.length() < 16) {
            sb.append(rnd.nextInt(10));
        }
        return sb.toString();
    }

    private Card getCardByAccount(DepositAccount account) {
        String selectSql = "SELECT * FROM CARD WHERE ACCOUNT_ID = ?";
        Card card = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setString(1, account.getAccountId());
            var rs = ps.executeQuery();
            if (rs.next()) {
                card = mapCard(rs);
            }
        } catch (SQLException e) {
            System.out.println("Failed to retrieve card: " + e.getMessage());
        }
        if (card != null) {
            return syncCardStatusWithDeposit(account, card);
        }
        return null;
    }

    private Card mapCard(ResultSet rs) throws SQLException {
        return new Card(
                rs.getString("ID"),
                rs.getString("ACCOUNT_ID"),
                rs.getString("CARD_NUMBER"),
                rs.getInt("TRANSACTION_LIMIT"),
                Card.CardStatus.valueOf(rs.getString("STATUS").toUpperCase()),
                rs.getString("PIN_NUMBER")
        );
    }

    private Card syncCardStatusWithDeposit(DepositAccount acc, Card card) {
        if (acc == null || card == null) {
            return card;
        }

        Card.CardStatus targetStatus = null;
        if (acc.isFrozen()) {
            targetStatus = Card.CardStatus.FROZEN;
        } else if (acc.isClosed()) {
            targetStatus = Card.CardStatus.INACTIVE;
        } else {
            return card;
        }

        if (card.getStatus() == targetStatus) {
            return card;
        }

        String updateSql = "UPDATE CARD SET STATUS = ? WHERE ACCOUNT_ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setString(1, targetStatus.name());
            ps.setString(2, card.getAccountId());
            ps.executeUpdate();
            card.setStatus(targetStatus);
        } catch (SQLException e) {
            System.out.println("Failed to sync card status: " + e.getMessage());
        }
        return card;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
