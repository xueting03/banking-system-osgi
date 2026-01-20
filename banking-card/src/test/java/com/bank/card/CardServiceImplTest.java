package com.bank.card;

import com.bank.api.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class CardServiceImplTest {

    private CardServiceImpl service;
    private ICustomerService mockCustomerService;
    private IDepositAccountService mockDepositService;
    private DataSource mockDataSource;
    private Connection testConnection;

    private static final String ID_NO = "010101-02-0303";
    private static final String PASSWORD = "password";
    private static final String PIN = "123456";

    @BeforeEach
    void setUp() throws Exception {
        String dbUrl = "jdbc:h2:mem:card-test;DB_CLOSE_DELAY=-1";
        mockDataSource = Mockito.mock(DataSource.class);
        Mockito.when(mockDataSource.getConnection()).thenAnswer(invocation ->
                DriverManager.getConnection(dbUrl, "sa", "")
        );

        mockCustomerService = Mockito.mock(ICustomerService.class);
        mockDepositService = Mockito.mock(IDepositAccountService.class);

        service = new CardServiceImpl();
        injectDependency(service, "dataSource", mockDataSource);
        injectDependency(service, "customerService", mockCustomerService);
        injectDependency(service, "depositAccountService", mockDepositService);

        // activate to create schema
        service.activate();

        // a connection used for cleanup
        testConnection = DriverManager.getConnection(dbUrl, "sa", "");
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testConnection != null && !testConnection.isClosed()) {
            testConnection.createStatement().execute("DROP TABLE IF EXISTS CARD");
            testConnection.close();
        }
    }

    private void injectDependency(Object target, String fieldName, Object dependency) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, dependency);
    }

    @Test
    void createCard_success() {
        // given valid card creation request
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);

        DepositAccount deposit = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        deposit.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(deposit);

        // when trigger card creation
        Card created = service.createCard(ID_NO, PASSWORD, PIN);

        // then card is created and save successfully
        assertNotNull(created, "Expected card to be created");
        Card fetched = service.getCard(ID_NO, PASSWORD);
        assertNotNull(fetched, "Expected getCard to return the created card");
        assertEquals(created.getCardNumber(), fetched.getCardNumber());
    }

    @Test
    void createCard_depositNotActive_returnsNull() {
        // given valid card creation request but deposit account is not Active
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);

        DepositAccount deposit = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        deposit.setStatus("Frozen");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(deposit);

        // when trigger card creation
        Card result = service.createCard(ID_NO, PASSWORD, PIN);

        // then expect null result
        assertNull(result, "Expected null when linked deposit account is not Active");
    }

    @Test
    void createCard_existingCard_returnsNull() {
        // given valid card creation request
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);
        DepositAccount deposit = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        deposit.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(deposit);

        // when trigger first card creation should succeed
        Card first = service.createCard(ID_NO, PASSWORD, PIN);
        assertNotNull(first);

        // when trigger second card creation for same account should fail
        Card second = service.createCard(ID_NO, PASSWORD, PIN);
        assertNull(second, "Expected null when a card already exists for the account");
    }

    @Test
    void createCard_invalidPin_returnsNull() {
        // given invalid PIN format for card creation request
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);
        DepositAccount deposit = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        deposit.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(deposit);

        // when trigger card creation
        Card result = service.createCard(ID_NO, PASSWORD, "12AB"); // invalid pin

        // then expect null result
        assertNull(result, "Expected null for invalid PIN format");
    }

    @Test
    void getCard_success_returnsCard() {
        // given valid get card request
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);
        DepositAccount deposit = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        deposit.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(deposit);

        Card created = service.createCard(ID_NO, PASSWORD, PIN);
        assertNotNull(created, "Expected card to be created successfully");

        // when trigger card retrieval
        Card fetched = service.getCard(ID_NO, PASSWORD);

        // then expect to get the created card
        assertNotNull(fetched, "Expected to fetch created card");
        assertEquals(created.getAccountId(), fetched.getAccountId(), "Account IDs should match");
        assertEquals(created.getCardNumber(), fetched.getCardNumber(), "Card numbers should match");
    }

    @Test
    void getCard_invalidCredentials_returnsNull() {
        // given invalid login credentials
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, "bad")).thenReturn(false);

        // when trigger card retrieval
        Card result = service.getCard(ID_NO, "bad");

        // then expect null result
        assertNull(result, "Expected null for invalid credentials");
    }

    @Test
    void updateCardPin_success_updatesPin() {
        // given valid update PIN request
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);
        DepositAccount deposit = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        deposit.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(deposit);

        // create card then activate before updating PIN
        Card created = service.createCard(ID_NO, PASSWORD, PIN);
        assertNotNull(created);

        Card activated = service.updateCardStatus(ID_NO, PASSWORD, ICardService.UpdateAction.ACTIVATE, PIN);
        assertNotNull(activated);

        // when trigger PIN update
        Card updated = service.updateCardPin(ID_NO, PASSWORD, PIN, "111111");

        // then expect PIN to be updated
        assertNotNull(updated);
        assertEquals("111111", updated.getPinNumber());
        Card fetched = service.getCard(ID_NO, PASSWORD);
        assertNotNull(fetched);
        assertEquals("111111", fetched.getPinNumber());
    }

    @Test
    void updateCardPin_wrongCurrent_returnsNull() {
        // given incorrect current PIN in update PIN request
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);
        DepositAccount deposit = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        deposit.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(deposit);

        Card created = service.createCard(ID_NO, PASSWORD, PIN);
        assertNotNull(created);

        // when trigger PIN update with wrong current PIN
        Card result = service.updateCardPin(ID_NO, PASSWORD, "999999", "111111");

        // then expect null result
        assertNull(result, "Expected null when current PIN is incorrect");
    }

    @Test
    void updateCardPin_inactiveCard_returnsNull() {
        // given valid update PIN request but card is not ACTIVE
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);
        DepositAccount deposit = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        deposit.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(deposit);

        // create card without activating
        Card created = service.createCard(ID_NO, PASSWORD, PIN);
        assertNotNull(created);

        // when trigger PIN update
        Card result = service.updateCardPin(ID_NO, PASSWORD, PIN, "111111");

        // then expect null result
        assertNull(result, "Expected null when card is not ACTIVE");
    }

    @Test
    void updateTransactionLimit_success() {
        // given valid update transaction limit request
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);
        DepositAccount deposit = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        deposit.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(deposit);

        Card created = service.createCard(ID_NO, PASSWORD, PIN);
        assertNotNull(created);

        // activate before changing limit
        service.updateCardStatus(ID_NO, PASSWORD, ICardService.UpdateAction.ACTIVATE, PIN);

        // when trigger transaction limit update
        Card updated = service.updateCardTransactionLimit(ID_NO, PASSWORD, 3000, PIN);

        // then expect limit to be updated
        assertNotNull(updated);
        assertEquals(3000, updated.getTransactionLimit());
    }

    @Test
    void updateTransactionLimit_invalid_returnsNull() {
        // given invalid transaction limit request
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);
        DepositAccount deposit = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        deposit.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(deposit);

        Card created = service.createCard(ID_NO, PASSWORD, PIN);
        assertNotNull(created);

        // activate
        service.updateCardStatus(ID_NO, PASSWORD, ICardService.UpdateAction.ACTIVATE, PIN);

        // when trigger transaction limit update with invalid limit
        Card result = service.updateCardTransactionLimit(ID_NO, PASSWORD, 20000, PIN);

        // then expect null result
        assertNull(result, "Expected null for invalid transaction limit");
    }

    @Test
    void updateTransactionLimit_accountNotActive_returnsNull() {
        // given valid update transaction limit request
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);
        DepositAccount deposit = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        deposit.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(deposit);

        // create card without activating
        Card created = service.createCard(ID_NO, PASSWORD, PIN);
        assertNotNull(created);

        // when trigger transaction limit update
        Card result = service.updateCardTransactionLimit(ID_NO, PASSWORD, 3000, PIN);

        // then expect null result
        assertNull(result, "Expected null when linked deposit account is not Active");
    }

    @Test
    void updateCardStatus_activateSuccess() {
        // given valid activate card request
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);
        DepositAccount deposit = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        deposit.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(deposit);

        Card created = service.createCard(ID_NO, PASSWORD, PIN);
        assertNotNull(created);
        assertEquals(Card.CardStatus.INACTIVE, created.getStatus());

        // when trigger activation
        Card activated = service.updateCardStatus(ID_NO, PASSWORD, ICardService.UpdateAction.ACTIVATE, PIN);

        // then expect card to be activated
        assertNotNull(activated);
        assertEquals(Card.CardStatus.ACTIVE, activated.getStatus());
    }

    @Test
    void updateCardStatus_activateAccountNotActive_returnsNull() {
        // given valid activate card request but deposit account is not Active
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);

        // create card with deposit Active
        DepositAccount depositActive = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        depositActive.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(depositActive);

        Card created = service.createCard(ID_NO, PASSWORD, PIN);
        assertNotNull(created);

        // now simulate deposit being not Active
        DepositAccount depositClosed = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        depositClosed.setStatus("Closed");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(depositClosed);

        // when trigger activation
        Card result = service.updateCardStatus(ID_NO, PASSWORD, ICardService.UpdateAction.ACTIVATE, PIN);

        // then expect null result
        assertNull(result, "Expected null when linked deposit account is not Active");
    }

    @Test
    void updateCardStatus_freezeAlreadyFrozen_returnsNull() {
        // given valid freeze card request
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);
        DepositAccount deposit = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        deposit.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(deposit);

        Card created = service.createCard(ID_NO, PASSWORD, PIN);
        assertNotNull(created);

        // activate then freeze
        service.updateCardStatus(ID_NO, PASSWORD, ICardService.UpdateAction.ACTIVATE, PIN);
        Card frozen = service.updateCardStatus(ID_NO, PASSWORD, ICardService.UpdateAction.FREEZE, PIN);
        assertNotNull(frozen);
        assertEquals(Card.CardStatus.FROZEN, frozen.getStatus());

        // when trigger freeze again
        Card again = service.updateCardStatus(ID_NO, PASSWORD, ICardService.UpdateAction.FREEZE, PIN);

        // then expect null result
        assertNull(again, "Expected null when card is already FROZEN");
    }

    @Test
    void getCard_syncsStatusWithDeposit() {
        Mockito.when(mockCustomerService.verifyLogin(ID_NO, PASSWORD)).thenReturn(true);

        // given deposit is Active and card is created
        DepositAccount depositActive = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        depositActive.setStatus("Active");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(depositActive);

        Card created = service.createCard(ID_NO, PASSWORD, PIN);
        assertNotNull(created, "Expected card to be created");

        // when deposit account status changes to Frozen
        DepositAccount depositFrozen = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        depositFrozen.setStatus("Frozen");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(depositFrozen);

        // then card status should sync to FROZEN
        Card fetched = service.getCard(ID_NO, PASSWORD);
        assertNotNull(fetched, "Expected to fetch card after deposit frozen");
        assertEquals(Card.CardStatus.FROZEN, fetched.getStatus(), "Expected card status to be FROZEN after deposit freeze");

        // when deposit account status changes to INACTIVE
        DepositAccount depositClosed = new DepositAccount("DA123", "CUST1", BigDecimal.ZERO);
        depositClosed.setStatus("Closed");
        Mockito.when(mockDepositService.getDepositAccount(ID_NO, PASSWORD)).thenReturn(depositClosed);

        // then card status should sync to INACTIVE
        fetched = service.getCard(ID_NO, PASSWORD);
        assertNotNull(fetched, "Expected to fetch card after deposit closed");
        assertEquals(Card.CardStatus.INACTIVE, fetched.getStatus(), "Expected card status to be INACTIVE after deposit close");
    }
}
