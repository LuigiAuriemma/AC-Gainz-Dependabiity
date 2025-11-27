package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VarianteDAOTest {

    private VarianteDAO dao;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        dao = new VarianteDAO();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
    }

    // --- TEST Query Dinamica con Filtri Stringa (Parsing) ---

    @Test
    void doRetrieveFilteredVarianti_WithFilters_ParsesCorrectly() throws SQLException {
        // Il DAO si aspetta formati specifici tipo "100 g" e "Gusto (Info)"
        String idProd = "P1";
        String weightFilter = "500 g";
        String tasteFilter = "Cioccolato (Best)";

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            dao.doRetrieveFilteredVariantiByIdProdotto(idProd, weightFilter, tasteFilter);

            String sql = sqlCaptor.getValue();

            // Verifica SQL
            assertTrue(sql.contains("AND c.peso = ?"), "Deve filtrare per peso");
            assertTrue(sql.contains("AND g.nomeGusto = ?"), "Deve filtrare per gusto");

            // Verifica Parametri e Parsing
            verify(mockPreparedStatement).setString(1, idProd);
            // Il DAO fa split(" ")[0] su "500 g" -> "500" -> parseInt
            verify(mockPreparedStatement).setInt(2, 500);
            // Il DAO fa split(" \\(")[0] su "Cioccolato (Best)" -> "Cioccolato"
            verify(mockPreparedStatement).setString(3, "Cioccolato");
        }
    }

    @Test
    void doRetrieveFilteredVarianti_NoFilters() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            dao.doRetrieveFilteredVariantiByIdProdotto("P1", null, "");

            String sql = sqlCaptor.getValue();

            assertFalse(sql.contains("AND c.peso"), "Non deve filtrare per peso");
            assertFalse(sql.contains("AND g.nomeGusto"), "Non deve filtrare per gusto");

            verify(mockPreparedStatement).setString(1, "P1");
            // Nessun altro parametro deve essere settato
            verify(mockPreparedStatement, never()).setInt(eq(2), anyInt());
        }
    }

    // --- TEST Query Dinamica IN (...) ---

    @Test
    void doRetrieveVariantiByProdotti_ListPopulated_GeneratesInClause() throws SQLException {
        List<Prodotto> prodotti = new ArrayList<>();
        Prodotto p1 = new Prodotto();
        p1.setIdProdotto("A");
        Prodotto p2 = new Prodotto();
        p2.setIdProdotto("B");
        prodotti.add(p1);
        prodotti.add(p2);

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            dao.doRetrieveVariantiByProdotti(prodotti);

            String sql = sqlCaptor.getValue();

            // Verifica che la clausola IN abbia due placeholder
            assertTrue(sql.contains("IN (?, ?)"));

            // Verifica parametri
            verify(mockPreparedStatement).setString(1, "A");
            verify(mockPreparedStatement).setString(2, "B");
        }
    }

    @Test
    void doRetrieveVariantiByProdotti_EmptyList_ReturnsEmptyImmediately() {
        // Verifica ottimizzazione: se la lista è vuota non deve chiamare il DB
        List<Prodotto> emptyList = new ArrayList<>();
        List<Variante> result = dao.doRetrieveVariantiByProdotti(emptyList);

        assertTrue(result.isEmpty());
        // ConPool non dovrebbe essere chiamato se l'ottimizzazione funziona
    }

    // --- TEST Switch Case Criteria ---

    @Test
    void doRetrieveVariantByCriteria_Weight() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(contains("peso = ?"))).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            dao.doRetrieveVariantByCriteria("P1", "weight", "1000");

            verify(mockPreparedStatement).setString(1, "P1");
            // Verifica che per "weight" faccia il parseInt
            verify(mockPreparedStatement).setInt(2, 1000);
        }
    }

    @Test
    void doRetrieveVariantByCriteria_Flavour() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(contains("nomeGusto = ?"))).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            dao.doRetrieveVariantByCriteria("P1", "flavour", "Vanilla");

            verify(mockPreparedStatement).setString(1, "P1");
            // Verifica che per "flavour" usi setString
            verify(mockPreparedStatement).setString(2, "Vanilla");
        }
    }

    // --- TEST Cheapest Filtered (Boolean Logic) ---

    @Test
    void doRetrieveCheapestFiltered_WithEvidenceTrue() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);

            dao.doRetrieveCheapestFilteredVarianteByIdProdotto("P1", null, null, true);

            String sql = sqlCaptor.getValue();
            assertTrue(sql.contains("AND v.evidenza = 1"));
        }
    }

    // --- TEST CRUD Standard ---

    @Test
    void doRetrieveVariantiByIdProdotto_MappingSuccess() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("id_variante")).thenReturn(1);
            when(mockResultSet.getInt("sconto")).thenReturn(10);
            // Testiamo un campo JOIN
            when(mockResultSet.getString("nomeGusto")).thenReturn("Fragola");

            List<Variante> result = dao.doRetrieveVariantiByIdProdotto("P1");

            assertEquals(1, result.size());
            assertEquals("Fragola", result.get(0).getGusto());
            assertEquals(10, result.get(0).getSconto());
        }
    }

    @Test
    void doSaveVariante_Success() throws SQLException {
        Variante v = new Variante();
        v.setIdProdotto("P_NEW");
        v.setIdGusto(1);
        v.setIdConfezione(2);
        v.setPrezzo(10.0f);
        v.setQuantita(100);
        v.setSconto(5);
        v.setEvidenza(true);

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            dao.doSaveVariante(v);

            verify(mockPreparedStatement).setString(1, "P_NEW");
            verify(mockPreparedStatement).setBoolean(7, true); // Evidenza
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doRetrieveCheapestVariant_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            // Verifica che ci sia ORDER BY e LIMIT 1
            when(mockConnection.prepareStatement(contains("limit 1"))).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getFloat("prezzo")).thenReturn(9.99f);

            Variante result = dao.doRetrieveCheapestVariant("P1");

            assertEquals(9.99f, result.getPrezzo());
        }
    }

    // --- NEW TESTS ---

    @Test
    void doRetrieveAll_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement("select * from variante")).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getInt("id_variante")).thenReturn(1, 2);

            List<Variante> result = dao.doRetrieveAll();

            assertEquals(2, result.size());
            assertEquals(1, result.get(0).getIdVariante());
            assertEquals(2, result.get(1).getIdVariante());
        }
    }

    @Test
    void doRetrieveVariantByFlavourAndWeight_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("nomeGusto")).thenReturn("Fragola");

            List<Variante> result = dao.doRetrieveVariantByFlavourAndWeight("P1", "Fragola", 500);

            assertEquals(1, result.size());
            assertEquals("Fragola", result.get(0).getGusto());

            verify(mockPreparedStatement).setString(1, "P1");
            verify(mockPreparedStatement).setString(2, "Fragola");
            verify(mockPreparedStatement).setInt(3, 500);
        }
    }

    @Test
    void doRetrieveVarianteByIdVariante_Found() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("id_variante")).thenReturn(123);

            Variante result = dao.doRetrieveVarianteByIdVariante(123);

            assertNotNull(result);
            assertEquals(123, result.getIdVariante());
            verify(mockPreparedStatement).setInt(1, 123);
        }
    }

    @Test
    void doRetrieveVarianteByIdVariante_NotFound() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(false);

            Variante result = dao.doRetrieveVarianteByIdVariante(999);

            assertNull(result);
        }
    }

    @Test
    void updateVariante_Success() throws SQLException {
        Variante v = new Variante();
        v.setIdVariante(1);
        v.setIdProdotto("P1");
        v.setIdGusto(2);
        v.setIdConfezione(3);
        v.setPrezzo(15.0f);
        v.setQuantita(50);
        v.setSconto(0);
        v.setEvidenza(false);

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            dao.updateVariante(v, 1);

            verify(mockPreparedStatement).setInt(1, 1); // id_variante (set)
            verify(mockPreparedStatement).setString(2, "P1");
            verify(mockPreparedStatement).setFloat(5, 15.0f);
            verify(mockPreparedStatement).setInt(9, 1); // where id_variante
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doRemoveVariante_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            dao.doRemoveVariante(55);

            verify(mockPreparedStatement).setInt(1, 55);
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doRetrieveCheapestFilteredVarianteByIdProdotto_FiltersParsing() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true);

            dao.doRetrieveCheapestFilteredVarianteByIdProdotto("P1", "250 g", "Vaniglia (New)", false);

            String sql = sqlCaptor.getValue();
            assertTrue(sql.contains("AND c.peso = ?"));
            assertTrue(sql.contains("AND g.nomeGusto = ?"));
            assertFalse(sql.contains("AND v.evidenza = 1"));

            verify(mockPreparedStatement).setString(1, "P1");
            verify(mockPreparedStatement).setInt(2, 250);
            verify(mockPreparedStatement).setString(3, "Vaniglia");
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
    void doRetrieveVariantiByIdProdotto_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRetrieveVariantiByIdProdotto("P1"));
        }
    }

    @Test
    void doRetrieveVariantByFlavourAndWeight_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRetrieveVariantByFlavourAndWeight("P1", "Gusto", 100));
        }
    }

    @Test
    void doRetrieveVarianteByIdVariante_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRetrieveVarianteByIdVariante(1));
        }
    }

    @Test
    void doRetrieveCheapestVariant_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRetrieveCheapestVariant("P1"));
        }
    }

    @Test
    void doRetrieveVariantiByProdotti_SQLException() throws SQLException {
        List<Prodotto> prodotti = new ArrayList<>();
        prodotti.add(new Prodotto());
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRetrieveVariantiByProdotti(prodotti));
        }
    }

    @Test
    void doRetrieveFilteredVariantiByIdProdotto_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRetrieveFilteredVariantiByIdProdotto("P1", null, null));
        }
    }

    @Test
    void doRetrieveCheapestFilteredVarianteByIdProdotto_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class,
                    () -> dao.doRetrieveCheapestFilteredVarianteByIdProdotto("P1", null, null, false));
        }
    }

    @Test
    void updateVariante_SQLException() throws SQLException {
        Variante v = new Variante();
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.updateVariante(v, 1));
        }
    }

    @Test
    void doRemoveVariante_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRemoveVariante(1));
        }
    }

    @Test
    void doSaveVariante_SQLException() throws SQLException {
        Variante v = new Variante();
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doSaveVariante(v));
        }
    }

    @Test
    void doRetrieveVariantByCriteria_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRetrieveVariantByCriteria("P1", "flavour", "val"));
        }
    }

    @Test
    void doRetrieveVariantByCriteria_UnknownAttribute() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            dao.doRetrieveVariantByCriteria("P1", "unknown", "val");

            String sql = sqlCaptor.getValue();
            // Should not append any extra condition
            assertFalse(sql.contains("and g.nomeGusto"));
            assertFalse(sql.contains("and c.peso"));

            // Should only set 1 parameter (idProdotto)
            verify(mockPreparedStatement).setString(1, "P1");
            verify(mockPreparedStatement, never()).setString(eq(2), anyString());
        }
    }

    @Test
    void doRetrieveFilteredVariantiByIdProdotto_PartialFilters() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            // Case 1: Only Weight
            dao.doRetrieveFilteredVariantiByIdProdotto("P1", "100 g", null);
            String sql1 = sqlCaptor.getValue();
            assertTrue(sql1.contains("AND c.peso = ?"));
            assertFalse(sql1.contains("AND g.nomeGusto = ?"));
            verify(mockPreparedStatement).setInt(2, 100);

            // Reset mocks for Case 2
            reset(mockPreparedStatement);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            // Case 2: Only Taste
            dao.doRetrieveFilteredVariantiByIdProdotto("P1", null, "Choco (Info)");
            String sql2 = sqlCaptor.getValue();
            assertFalse(sql2.contains("AND c.peso = ?"));
            assertTrue(sql2.contains("AND g.nomeGusto = ?"));
            verify(mockPreparedStatement).setString(2, "Choco");
        }
    }

    @Test
    void doRetrieveCheapestFiltered_EvidenceFalse() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true);

            dao.doRetrieveCheapestFilteredVarianteByIdProdotto("P1", null, null, false);

            String sql = sqlCaptor.getValue();
            assertFalse(sql.contains("AND v.evidenza = 1"));
        }
    }

    @Test
    void doRetrieveCheapestFiltered_NotFound() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(false);

            Variante result = dao.doRetrieveCheapestFilteredVarianteByIdProdotto("P1", null, null, false);

            assertNull(result);
        }
    }

    @Test
    void doRetrieveCheapestFiltered_EmptyFilters() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true);

            // Pass empty strings
            dao.doRetrieveCheapestFilteredVarianteByIdProdotto("P1", "   ", "", false);

            String sql = sqlCaptor.getValue();
            assertFalse(sql.contains("AND c.peso = ?"));
            assertFalse(sql.contains("AND g.nomeGusto = ?"));

            // Only 1 param set (idProdotto)
            verify(mockPreparedStatement).setString(1, "P1");
            verify(mockPreparedStatement, never()).setInt(eq(2), anyInt());
        }
    }

    @Test
    void doRetrieveFilteredVarianti_EmptyFilters() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            dao.doRetrieveFilteredVariantiByIdProdotto("P1", "", "   ");

            String sql = sqlCaptor.getValue();
            assertFalse(sql.contains("AND c.peso = ?"));
            assertFalse(sql.contains("AND g.nomeGusto = ?"));
        }
    }

    @Test
    void doRetrieveVariantiByProdotti_SingleProduct() throws SQLException {
        List<Prodotto> prodotti = new ArrayList<>();
        Prodotto p = new Prodotto();
        p.setIdProdotto("SINGLE");
        prodotti.add(p);

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            dao.doRetrieveVariantiByProdotti(prodotti);

            String sql = sqlCaptor.getValue();
            assertTrue(sql.contains("IN (?)"));
            assertFalse(sql.contains(", ?")); // No commas for single item

            verify(mockPreparedStatement).setString(1, "SINGLE");
        }
    }

    @Test
    void doRetrieveCheapestVariant_NotFound() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(false);

            Variante result = dao.doRetrieveCheapestVariant("P1");

            // Verify it returns an empty object, not null
            assertNotNull(result);
            assertEquals(0, result.getIdVariante()); // Default int value
            assertNull(result.getIdProdotto()); // Default string value
        }
    }

    @Test
    void doRetrieveVariantByCriteria_InvalidWeight() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            assertThrows(NumberFormatException.class, () -> dao.doRetrieveVariantByCriteria("P1", "weight", "invalid"));
        }
    }

    @Test
    void doRetrieveFilteredVarianti_InvalidWeight() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            assertThrows(NumberFormatException.class,
                    () -> dao.doRetrieveFilteredVariantiByIdProdotto("P1", "invalid g", null));
        }
    }

    @Test
    void doRetrieveFilteredVarianti_TasteNoParenthesis() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            dao.doRetrieveFilteredVariantiByIdProdotto("P1", null, "SimpleTaste");

            String sql = sqlCaptor.getValue();
            assertTrue(sql.contains("AND g.nomeGusto = ?"));
            verify(mockPreparedStatement).setString(2, "SimpleTaste");
        }
    }
}