package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrdineDAOTest {
    private OrdineDao ordineDao;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private Statement mockStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        ordineDao = new OrdineDao();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockStatement = mock(Statement.class);
        mockResultSet = mock(ResultSet.class);
    }

    @Test
    void doRetrieveById_Found() throws SQLException {
        int idCercato = 10;
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            // Simuliamo che il record esista
            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("id_ordine")).thenReturn(idCercato);
            when(mockResultSet.getString("email_utente")).thenReturn("user@test.com");
            when(mockResultSet.getDate("data")).thenReturn(new java.sql.Date(System.currentTimeMillis()));
            when(mockResultSet.getString("stato")).thenReturn("Spedito");
            when(mockResultSet.getFloat("totale")).thenReturn(50.5f);
            when(mockResultSet.getString("descrizione")).thenReturn("Desc");

            Ordine result = ordineDao.doRetrieveById(idCercato);

            assertNotNull(result);
            assertEquals(idCercato, result.getIdOrdine());
            assertEquals("user@test.com", result.getEmailUtente());
            assertNotNull(result.getDataOrdine());
            assertEquals("Spedito", result.getStato());
            assertEquals(50.5f, result.getTotale());
            assertEquals("Desc", result.getDescrizione());

            verify(mockPreparedStatement).setInt(1, idCercato);
        }
    }

    @Test
    void doRetrieveById_NotFound() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            // Simuliamo che il record NON esista
            when(mockResultSet.next()).thenReturn(false);

            Ordine result = ordineDao.doRetrieveById(999);

            assertNull(result);
            verify(mockPreparedStatement).setInt(1, 999);
        }
    }

    @Test
    void doRetrieveByEmail_Success() throws SQLException {
        String email = "user@test.com";
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(contains("email_utente = ?"))).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            // Simuliamo 2 ordini trovati
            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getInt("id_ordine")).thenReturn(1, 2);
            when(mockResultSet.getString("email_utente")).thenReturn("user@test.com", "user@test.com");
            // Default returns for other fields to prevent NPE in assertions if needed,
            // though Setters handle null ok usually.
            // But let's mock one field to kill mutants
            when(mockResultSet.getFloat("totale")).thenReturn(10.0f, 20.0f);
            when(mockResultSet.getString("descrizione")).thenReturn("D1", "D2");
            when(mockResultSet.getDate("data")).thenReturn(new java.sql.Date(System.currentTimeMillis()));
            when(mockResultSet.getString("stato")).thenReturn("Spedito", "Nuovo");

            List<Ordine> result = ordineDao.doRetrieveByEmail(email);

            assertEquals(2, result.size());
            assertEquals(1, result.get(0).getIdOrdine());
            assertEquals("user@test.com", result.get(0).getEmailUtente()); // Assumed from mocked logic or set field?
                                                                           // method sets it from result set
            // In DAO: ordine.setEmailUtente(resultSet.getString("email_utente"));
            // We need to mock getString("email_utente"). It returns null by default?
            // Actually verification of setString(1, email) is done, but result object
            // population is what we check.

            assertEquals("Spedito", result.get(0).getStato());
            assertEquals(10.0f, result.get(0).getTotale()); // Kil mutant index 60
            assertEquals("D1", result.get(0).getDescrizione()); // Kill mutant index 67
            assertNotNull(result.get(0).getDataOrdine()); // too if needed.

            verify(mockPreparedStatement).setString(1, email);
        }
    }

    // ... (keep doSave tests for later strictifying) ...

    @Test
    void doRetrieveAll_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.createStatement()).thenReturn(mockStatement);
            when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("id_ordine")).thenReturn(10);
            when(mockResultSet.getString("stato")).thenReturn("Nuovo");
            when(mockResultSet.getString("descrizione")).thenReturn("Desc");
            when(mockResultSet.getDate("data")).thenReturn(new java.sql.Date(System.currentTimeMillis()));
            // Mock missing fields
            when(mockResultSet.getString("email_utente")).thenReturn("user@test.com");
            when(mockResultSet.getFloat("totale")).thenReturn(99.9f);

            List<Ordine> result = ordineDao.doRetrieveAll();

            assertEquals(1, result.size());
            assertEquals(10, result.get(0).getIdOrdine());
            assertEquals("Nuovo", result.get(0).getStato());
            assertEquals("Desc", result.get(0).getDescrizione());
            assertNotNull(result.get(0).getDataOrdine());
            assertEquals("user@test.com", result.get(0).getEmailUtente());
            assertEquals(99.9f, result.get(0).getTotale());
        }
    }

    @Test
    void getLastInsertedId_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement("SELECT LAST_INSERT_ID()")).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt(1)).thenReturn(123); // L'ID generato

            int id = ordineDao.getLastInsertedId();

            assertEquals(123, id);
        }
    }

    @Test
    void doUpdateOrder_Success() throws SQLException {
        Ordine o = new Ordine();
        o.setIdOrdine(1);
        o.setEmailUtente("updated@test.com");
        o.setStato("Consegnato");
        o.setDataOrdine(new java.util.Date()); // Util Date
        o.setTotale(200.0f);
        o.setDescrizione("Update");

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            ordineDao.doUpdateOrder(o, 1);

            verify(mockPreparedStatement).setInt(1, 1); // Kill index 17
            verify(mockPreparedStatement).setString(2, "updated@test.com");
            verify(mockPreparedStatement).setString(3, "Consegnato"); // Kill index 31
            verify(mockPreparedStatement).setDate(eq(4), any(java.sql.Date.class));
            verify(mockPreparedStatement).setFloat(5, 200.0f); // Kill index 56
            verify(mockPreparedStatement).setString(6, "Update");
            verify(mockPreparedStatement).setInt(7, 1);

            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doSave_FullObject_ConstructsCorrectQuery() throws SQLException {
        // Testiamo la generazione dinamica della query quando TUTTI i campi sono
        // presenti
        Ordine ordine = new Ordine();
        ordine.setIdOrdine(1);
        ordine.setEmailUtente("test@test.com");
        ordine.setDataOrdine(new java.util.Date());
        ordine.setStato("Nuovo");
        ordine.setTotale(100.0f);
        ordine.setDescrizione("Note");

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            // Usiamo ArgumentCaptor per intercettare la query SQL generata dallo
            // StringBuilder
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(mockPreparedStatement);

            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            ordineDao.doSave(ordine);

            String executedQuery = sqlCaptor.getValue();

            // Verifiche sulla Query Dinamica (Struttura ESATTA)
            // Ordine campi aggiunto: id, email, data, stato, totale, descrizione
            // 6 campi totali.
            // VALUES (?, ?, ?, ?, ?, ?)

            String expectedPart = "INSERT INTO ordine (id_ordine, email_utente, data, stato, totale, descrizione) VALUES (?, ?, ?, ?, ?, ?)";
            assertEquals(expectedPart, executedQuery, "La query generata deve corrispondere esattamente");

            // Verifichiamo parametri
            verify(mockPreparedStatement).setObject(eq(1), eq(1));
            verify(mockPreparedStatement).setObject(eq(2), eq("test@test.com"));
            // data
            verify(mockPreparedStatement).setObject(eq(4), eq("Nuovo"));
            verify(mockPreparedStatement).setObject(eq(5), eq(100.0f));
            verify(mockPreparedStatement).setObject(eq(6), eq("Note"));
        }
    }

    @Test
    void doSave_PartialObject_ConstructsReducedQuery() throws SQLException {
        // Testiamo la generazione dinamica quando MANCANO dei campi (es. descrizione e
        // stato null)
        Ordine ordine = new Ordine();
        ordine.setIdOrdine(5);
        ordine.setTotale(50.0f);
        // Email, Data, Stato, Descrizione sono NULL o vuoti di default

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(mockPreparedStatement);

            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            ordineDao.doSave(ordine);

            String executedQuery = sqlCaptor.getValue();

            // Verifiche: la query esatta
            // Campi: id, totale. 2 campi
            // VALUES (?, ?)
            String expected = "INSERT INTO ordine (id_ordine, totale) VALUES (?, ?)";
            assertEquals(expected, executedQuery);

            verify(mockPreparedStatement).setObject(eq(1), eq(5));
            verify(mockPreparedStatement).setObject(eq(2), eq(50.0f));

            // Verifica che non chiami indici superiori
            verify(mockPreparedStatement, never()).setObject(eq(3), any());
        }
    }

    @Test
    void doSave_ZeroTotal() throws SQLException {
        Ordine ordine = new Ordine();
        ordine.setIdOrdine(5);
        ordine.setTotale(0.0f); // Totale is 0, so it should NOT be added (condition > 0)

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(mockPreparedStatement);

            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            ordineDao.doSave(ordine);

            String executedQuery = sqlCaptor.getValue();

            // Totale should NOT be present if 0
            assertFalse(executedQuery.contains("totale"));

            // Only ID should be present (assuming other fields null)
            String expected = "INSERT INTO ordine (id_ordine) VALUES (?)";
            assertEquals(expected, executedQuery);

            verify(mockPreparedStatement).setObject(eq(1), eq(5));
        }
    }

    @Test
    void doDeleteOrder_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            ordineDao.doDeleteOrder(5);

            verify(mockPreparedStatement).setInt(1, 5);
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    // --- NEW TESTS ---

    @Test
    void doRetrieveById_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> ordineDao.doRetrieveById(1));
        }
    }

    @Test
    void doRetrieveByEmail_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> ordineDao.doRetrieveByEmail("email"));
        }
    }

    @Test
    void getLastInsertedId_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> ordineDao.getLastInsertedId());
        }
    }

    @Test
    void getLastInsertedId_NoResult() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            int id = ordineDao.getLastInsertedId();
            assertEquals(0, id);
        }
    }

    @Test
    void doSave_SQLException() throws SQLException {
        Ordine o = new Ordine();
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> ordineDao.doSave(o));
        }
    }

    @Test
    void doSave_InsertError() throws SQLException {
        Ordine o = new Ordine();
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(0);

            assertThrows(RuntimeException.class, () -> ordineDao.doSave(o));
        }
    }

    @Test
    void doRetrieveAll_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.createStatement()).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> ordineDao.doRetrieveAll());
        }
    }

    @Test
    void doUpdateOrder_SQLException() throws SQLException {
        Ordine o = new Ordine();
        o.setDataOrdine(new java.util.Date());
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> ordineDao.doUpdateOrder(o, 1));
        }
    }

    @Test
    void doDeleteOrder_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> ordineDao.doDeleteOrder(1));
        }
    }
}