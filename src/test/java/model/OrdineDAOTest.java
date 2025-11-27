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
            when(mockResultSet.getFloat("totale")).thenReturn(50.5f);
            when(mockResultSet.getString("stato")).thenReturn("Spedito");

            Ordine result = ordineDao.doRetrieveById(idCercato);

            assertNotNull(result);
            assertEquals(idCercato, result.getIdOrdine());
            assertEquals(50.5f, result.getTotale());
            assertEquals("Spedito", result.getStato());
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

            List<Ordine> result = ordineDao.doRetrieveByEmail(email);

            assertEquals(2, result.size());
            verify(mockPreparedStatement).setString(1, email);
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

            // Verifiche sulla Query Dinamica
            assertTrue(executedQuery.contains("email_utente"), "La query deve contenere email_utente");
            assertTrue(executedQuery.contains("totale"), "La query deve contenere totale");
            assertTrue(executedQuery.contains("descrizione"), "La query deve contenere descrizione");

            // Verifichiamo che siano stati settati i parametri (setObject viene usato nel
            // loop)
            // Ordine parametri: id, email, data, stato, totale, descrizione (totale 6)
            verify(mockPreparedStatement, atLeastOnce()).setObject(eq(1), eq(1)); // ID
            verify(mockPreparedStatement, atLeastOnce()).setObject(eq(2), eq("test@test.com")); // Email
            verify(mockPreparedStatement, atLeastOnce()).setObject(eq(5), eq(100.0f)); // Totale
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

            // Verifiche: la query NON deve contenere i campi null
            assertFalse(executedQuery.contains("email_utente"), "Non deve inserire email se null");
            assertFalse(executedQuery.contains("descrizione"), "Non deve inserire descrizione se null");
            assertTrue(executedQuery.contains("totale"), "Deve inserire totale");

            // Verifica parametri: Qui ci aspettiamo meno parametri
            // 1 -> id, 2 -> totale. Solo 2 parametri totali.
            verify(mockPreparedStatement).setObject(eq(1), eq(5));
            verify(mockPreparedStatement).setObject(eq(2), eq(50.0f));
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
    void doRetrieveAll_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.createStatement()).thenReturn(mockStatement);
            when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("id_ordine")).thenReturn(10);

            List<Ordine> result = ordineDao.doRetrieveAll();

            assertEquals(1, result.size());
            assertEquals(10, result.get(0).getIdOrdine());
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

            verify(mockPreparedStatement).setString(2, "updated@test.com");
            // Verifica importante: conversione data
            verify(mockPreparedStatement).setDate(eq(4), any(java.sql.Date.class));
            verify(mockPreparedStatement).executeUpdate();
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