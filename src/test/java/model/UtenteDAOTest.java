package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UtenteDAOTest {

    private UtenteDAO utenteDAO;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private Statement mockStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        utenteDAO = new UtenteDAO();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockStatement = mock(Statement.class);
        mockResultSet = mock(ResultSet.class);
    }

    @Test
    void doRetrieveByEmailAndPassword_Success() throws SQLException {
        String email = "test@example.com";
        String password = "password123";

        // Simuliamo il ConPool statico
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            // Configuriamo il comportamento dei mock JDBC
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true); // Troviamo un utente

            // Simuliamo i dati restituiti dal DB
            when(mockResultSet.getString("email")).thenReturn(email);
            when(mockResultSet.getString("password")).thenReturn(password);
            when(mockResultSet.getString("nome")).thenReturn("Mario");
            when(mockResultSet.getString("cognome")).thenReturn("Rossi");
            when(mockResultSet.getBoolean("poteri")).thenReturn(false);
            // ... altri campi se necessari

            Utente result = utenteDAO.doRetrieveByEmailAndPassword(email, password);

            assertNotNull(result);
            assertEquals("Mario", result.getNome());
            assertEquals(email, result.getEmail());

            // Verifichiamo che i parametri siano stati settati correttamente nel
            // PreparedStatement
            verify(mockPreparedStatement).setString(1, email);
            verify(mockPreparedStatement).setString(2, password);
        }
    }

    @Test
    void doRetrieveByEmailAndPassword_Failure() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false); // Nessun utente trovato

            Utente result = utenteDAO.doRetrieveByEmailAndPassword("wrong@email.com", "wrongpass");

            assertNull(result);
        }
    }

    @Test
    void doRetrieveByEmail_Success() throws SQLException {
        String email = "test@example.com";

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true, false); // Un risultato, poi fine

            when(mockResultSet.getString("email")).thenReturn(email);
            when(mockResultSet.getString("nome")).thenReturn("Luigi");

            Utente result = utenteDAO.doRetrieveByEmail(email);

            assertNotNull(result);
            assertEquals("Luigi", result.getNome());
            verify(mockPreparedStatement).setString(1, email);
        }
    }

    @Test
    void doSave_Success() throws SQLException {
        Utente u = new Utente();
        u.setEmail("new@example.com");
        u.setPassword("pass");
        u.setNome("New");
        u.setCognome("User");
        u.setCodiceFiscale("CF123");
        u.setDataNascita(new java.util.Date());
        u.setIndirizzo("Via Roma");
        u.setTelefono("123456");

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1); // 1 riga inserita

            utenteDAO.doSave(u);

            verify(mockPreparedStatement).executeUpdate();
            verify(mockPreparedStatement).setString(1, u.getEmail());
        }
    }

    @Test
    void doSave_Failure_ThrowsException() throws SQLException {
        Utente u = new Utente();
        // ... setup minimo ...
        u.setDataNascita(new java.util.Date()); // Necessario per evitare NullPointerException nel DAO prima del try

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(0); // 0 righe inserite, errore

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                utenteDAO.doSave(u);
            });

            assertEquals("INSERT error.", exception.getMessage());
        }
    }

    @Test
    void doRetrieveAll_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.createStatement()).thenReturn(mockStatement);
            when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);

            // Simuliamo 2 utenti nel DB
            when(mockResultSet.next()).thenReturn(true, true, false);

            // Mock dei dati (semplificato per brevità, usando indici come nel DAO)
            when(mockResultSet.getString(1)).thenReturn("u1@test.com", "u2@test.com");
            when(mockResultSet.getString(3)).thenReturn("Nome1", "Nome2");
            when(mockResultSet.getString(5)).thenReturn("CF1", "CF2");

            List<Utente> result = utenteDAO.doRetrieveAll();

            assertEquals(2, result.size());
            assertEquals("u1@test.com", result.get(0).getEmail());
            assertEquals("u2@test.com", result.get(1).getEmail());
        }
    }

    @Test
    void doUpdateCustomer_Success() throws SQLException {
        Utente u = new Utente();
        u.setEmail("update@test.com");
        u.setNome("Updated");
        u.setCognome("Name");
        u.setCodiceFiscale("CFUPD");
        u.setDataNascita(new java.util.Date());
        u.setIndirizzo("New Address");
        u.setTelefono("999");
        u.setPoteri(true);

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            utenteDAO.doUpdateCustomer(u, "old@test.com");

            verify(mockPreparedStatement).executeUpdate();
            verify(mockPreparedStatement).setString(2, "Updated"); // Controllo parametro nome
            verify(mockPreparedStatement).setString(9, "old@test.com"); // Controllo WHERE
        }
    }

    @Test
    void doUpdateCustomerGeneric_DateLogic() throws SQLException {
        Utente u = new Utente();
        u.setEmail("test@test.com");
        String dateString = "2000-01-01";

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            utenteDAO.doUpdateCustomerGeneric(u, "dataDiNascita", dateString);

            // Verifica che sia stato chiamato setDate (logica specifica del case
            // "dataDiNascita")
            verify(mockPreparedStatement).setDate(eq(1), any(java.sql.Date.class));
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doUpdateCustomerGeneric_PoteriTrue() throws SQLException {
        Utente u = new Utente();
        u.setEmail("test@test.com");

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            utenteDAO.doUpdateCustomerGeneric(u, "poteri", "true");

            // Verifica logica booleana
            verify(mockPreparedStatement).setBoolean(1, true);
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doRemoveUserByEmail_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            utenteDAO.doRemoveUserByEmail("delete@me.com");

            verify(mockPreparedStatement).setString(1, "delete@me.com");
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void exceptionHandling_WrapsSQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> {
                utenteDAO.doRetrieveAll();
            });
        }
    }

    // --- NEW TESTS ---

    @Test
    void doRetrieveByEmailAndPassword_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> utenteDAO.doRetrieveByEmailAndPassword("email", "pass"));
        }
    }

    @Test
    void doRetrieveByEmail_NotFound() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            Utente result = utenteDAO.doRetrieveByEmail("notfound@test.com");
            assertNull(result);
        }
    }

    @Test
    void doRetrieveByEmail_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> utenteDAO.doRetrieveByEmail("email"));
        }
    }

    @Test
    void doSave_SQLException() throws SQLException {
        Utente u = new Utente();
        u.setDataNascita(new java.util.Date());
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> utenteDAO.doSave(u));
        }
    }

    @Test
    void doUpdateCustomer_SQLException() throws SQLException {
        Utente u = new Utente();
        u.setDataNascita(new java.util.Date());
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> utenteDAO.doUpdateCustomer(u, "email"));
        }
    }

    @Test
    void doUpdateCustomerGeneric_PoteriFalse() throws SQLException {
        Utente u = new Utente();
        u.setEmail("test@test.com");
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            utenteDAO.doUpdateCustomerGeneric(u, "poteri", "false");

            verify(mockPreparedStatement).setBoolean(1, false);
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doUpdateCustomerGeneric_Default() throws SQLException {
        Utente u = new Utente();
        u.setEmail("test@test.com");
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            utenteDAO.doUpdateCustomerGeneric(u, "nome", "NewName");

            verify(mockPreparedStatement).setString(1, "NewName");
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doUpdateCustomerGeneric_ParseException() throws SQLException {
        Utente u = new Utente();
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            assertThrows(RuntimeException.class,
                    () -> utenteDAO.doUpdateCustomerGeneric(u, "dataDiNascita", "invalid-date"));
        }
    }

    @Test
    void doUpdateCustomerGeneric_SQLException() throws SQLException {
        Utente u = new Utente();
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> utenteDAO.doUpdateCustomerGeneric(u, "nome", "val"));
        }
    }

    @Test
    void doRemoveUserByEmail_NoDelete() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(0);

            // Should not throw exception, just print to stdout
            utenteDAO.doRemoveUserByEmail("nodelete@me.com");

            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doRemoveUserByEmail_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> utenteDAO.doRemoveUserByEmail("email"));
        }
    }
}