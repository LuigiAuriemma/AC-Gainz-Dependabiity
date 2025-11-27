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

class DettaglioOrdineDAOTest {

    private DettaglioOrdineDAO dao;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        dao = new DettaglioOrdineDAO();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
    }

    @Test
    void doRetrieveById_JoinMapping_Success() throws SQLException {
        // Testiamo che i dati della JOIN vengano letti correttamente
        int idOrdine = 1;
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            // Usiamo contains perché la query è molto lunga
            when(mockConnection.prepareStatement(contains("SELECT dettaglio_ordine.*")))
                    .thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);

            // Mock dei dati base
            when(mockResultSet.getInt("id_ordine")).thenReturn(idOrdine);
            when(mockResultSet.getString("id_prodotto")).thenReturn("PROD1");

            // Mock dei dati da JOIN (campi che non sono nella tabella dettaglio_ordine
            // base)
            when(mockResultSet.getString("nomeGusto")).thenReturn("Cioccolato");
            when(mockResultSet.getInt("peso")).thenReturn(1000);
            when(mockResultSet.getString("nome")).thenReturn("Protein Powder"); // Nome prodotto

            List<DettaglioOrdine> result = dao.doRetrieveById(idOrdine);

            assertNotNull(result);
            assertEquals(1, result.size());
            DettaglioOrdine item = result.get(0);

            assertEquals("Cioccolato", item.getGusto());
            assertEquals(1000, item.getPesoConfezione());
            assertEquals("Protein Powder", item.getNomeProdotto());
        }
    }

    @Test
    void doRetrieveByIdOrderAndIdVariant_Found() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("id_ordine")).thenReturn(10);
            when(mockResultSet.getInt("id_variante")).thenReturn(5);

            DettaglioOrdine result = dao.doRetrieveByIdOrderAndIdVariant(10, 5);

            assertNotNull(result);
            assertEquals(10, result.getIdOrdine());
        }
    }

    @Test
    void doRetrieveByIdOrderAndIdVariant_NotFound_ReturnsNull() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(false);

            DettaglioOrdine result = dao.doRetrieveByIdOrderAndIdVariant(10, 5);

            assertNull(result);
        }
    }

    @Test
    void doUpdateDettaglioOrdine_Success() throws SQLException {
        DettaglioOrdine d = new DettaglioOrdine();
        d.setIdOrdine(1);
        d.setIdVariante(2);
        d.setIdProdotto("P1");
        d.setQuantita(5);
        d.setPrezzo(10.5f);

        int oldIdOrdine = 1;
        String oldIdProdotto = "P1";
        int oldIdVariante = 2;

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            dao.doUpdateDettaglioOrdine(d, oldIdOrdine, oldIdProdotto, oldIdVariante);

            // Verifica parametri SET
            verify(mockPreparedStatement).setInt(1, 1); // d.getIdOrdine()
            verify(mockPreparedStatement).setFloat(5, 10.5f); // d.getPrezzo()

            // Verifica parametri WHERE (indici 6, 7, 8)
            verify(mockPreparedStatement).setInt(6, oldIdOrdine);
            verify(mockPreparedStatement).setString(7, oldIdProdotto);
            verify(mockPreparedStatement).setInt(8, oldIdVariante);

            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doRemoveDettaglioOrdine_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            dao.doRemoveDettaglioOrdine(10, 5);

            verify(mockPreparedStatement).setInt(1, 10);
            verify(mockPreparedStatement).setInt(2, 5);
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doSave_FullObject_GeneratesCorrectSQL() throws SQLException {
        // Caso: Quantità > 0 e Prezzo > 0
        DettaglioOrdine d = new DettaglioOrdine();
        d.setIdOrdine(1);
        d.setIdProdotto("P1");
        d.setIdVariante(2);
        d.setQuantita(10);
        d.setPrezzo(9.99f);

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            dao.doSave(d);

            String sql = sqlCaptor.getValue();

            // Verifiche Query
            assertTrue(sql.contains("quantità"), "La query deve includere 'quantità'"); // Nota l'accento nel tuo DAO
            assertTrue(sql.contains("prezzo"), "La query deve includere 'prezzo'");

            // Verifiche Parametri (5 parametri totali)
            verify(mockPreparedStatement).setObject(eq(1), eq(1));
            verify(mockPreparedStatement).setObject(eq(4), eq(10)); // Quantità è il 4° aggiunto
            verify(mockPreparedStatement).setObject(eq(5), eq(9.99f)); // Prezzo è il 5°
        }
    }

    @Test
    void doSave_MinimalObject_GeneratesCorrectSQL() throws SQLException {
        // Caso: Quantità = 0 e Prezzo = 0 (non vengono aggiunti alla query)
        DettaglioOrdine d = new DettaglioOrdine();
        d.setIdOrdine(1);
        d.setIdProdotto("P1");
        d.setIdVariante(2);
        d.setQuantita(0); // Non deve essere inserito
        d.setPrezzo(0.0f); // Non deve essere inserito

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            dao.doSave(d);

            String sql = sqlCaptor.getValue();

            // Verifiche Query Negativa
            assertFalse(sql.contains("quantità"), "La query NON deve includere 'quantità' se 0");
            assertFalse(sql.contains("prezzo"), "La query NON deve includere 'prezzo' se 0");

            // Verifiche Parametri: Solo 3 parametri attesi (id_ordine, id_prodotto,
            // id_variante)
            verify(mockPreparedStatement).setObject(eq(1), eq(1));
            verify(mockPreparedStatement).setObject(eq(2), eq("P1"));
            verify(mockPreparedStatement).setObject(eq(3), eq(2));

            // Verifica che non tenti di settare un 4° parametro
            verify(mockPreparedStatement, never()).setObject(eq(4), any());
        }
    }

    @Test
    void doRetrieveAll_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("id_ordine")).thenReturn(99);

            List<DettaglioOrdine> result = dao.doRetrieveAll();

            assertEquals(1, result.size());
            assertEquals(99, result.get(0).getIdOrdine());
        }
    }

    // --- NEW TESTS ---

    @Test
    void doRetrieveById_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRetrieveById(1));
        }
    }

    @Test
    void doRetrieveByIdOrderAndIdVariant_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRetrieveByIdOrderAndIdVariant(1, 1));
        }
    }

    @Test
    void doRetrieveAll_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRetrieveAll());
        }
    }

    @Test
    void doUpdateDettaglioOrdine_SQLException() throws SQLException {
        DettaglioOrdine d = new DettaglioOrdine();
        d.setIdOrdine(1);
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doUpdateDettaglioOrdine(d, 1, "P1", 1));
        }
    }

    @Test
    void doRemoveDettaglioOrdine_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRemoveDettaglioOrdine(1, 1));
        }
    }

    @Test
    void doSave_SQLException() throws SQLException {
        DettaglioOrdine d = new DettaglioOrdine();
        d.setIdOrdine(1);
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doSave(d));
        }
    }

    @Test
    void doSave_InsertError() throws SQLException {
        DettaglioOrdine d = new DettaglioOrdine();
        d.setIdOrdine(1);
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(0); // Simulate failure

            RuntimeException e = assertThrows(RuntimeException.class, () -> dao.doSave(d));
            assertEquals("INSERT error.", e.getMessage());
        }
    }
}