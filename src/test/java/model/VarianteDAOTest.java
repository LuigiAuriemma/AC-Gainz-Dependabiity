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

            // Verifica Parametri e Parsing con InOrder per uccidere i mutanti di incremento
            org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(mockPreparedStatement);
            inOrder.verify(mockPreparedStatement).setString(1, idProd);
            // Il DAO fa split(" ")[0] su "500 g" -> "500" -> parseInt
            inOrder.verify(mockPreparedStatement).setInt(2, 500);
            // Il DAO fa split(" \\(")[0] su "Cioccolato (Best)" -> "Cioccolato"
            inOrder.verify(mockPreparedStatement).setString(3, "Cioccolato");
            inOrder.verify(mockPreparedStatement).executeQuery();
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

            // Setup mock result to insure list is not empty
            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("id_variante")).thenReturn(99);
            when(mockResultSet.getString("id_prodotto_variante")).thenReturn("A");
            when(mockResultSet.getInt("id_gusto")).thenReturn(2);
            when(mockResultSet.getInt("id_confezione")).thenReturn(3);
            when(mockResultSet.getInt("quantità")).thenReturn(50);
            when(mockResultSet.getFloat("prezzo")).thenReturn(20.0f);
            when(mockResultSet.getInt("sconto")).thenReturn(10);
            when(mockResultSet.getString("nomeGusto")).thenReturn("Fragola");
            when(mockResultSet.getInt("peso")).thenReturn(100);
            when(mockResultSet.getBoolean("evidenza")).thenReturn(true);

            List<Variante> result = dao.doRetrieveVariantiByProdotti(prodotti);

            String sql = sqlCaptor.getValue();

            // Verifica che la clausola IN abbia due placeholder
            assertTrue(sql.contains("IN (?, ?)"));

            // Verifica parametri
            verify(mockPreparedStatement).setString(1, "A");
            verify(mockPreparedStatement).setString(2, "B");

            // KILL MUTANT: replaced return value with Collections.emptyList
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            Variante v = result.get(0);
            assertEquals(99, v.getIdVariante());
            assertEquals("A", v.getIdProdotto());
            assertEquals(2, v.getIdGusto());
            assertEquals(3, v.getIdConfezione());
            assertEquals(50, v.getQuantita());
            assertEquals(20.0f, v.getPrezzo());
            assertEquals(10, v.getSconto());
            assertEquals("Fragola", v.getGusto());
            assertEquals(100, v.getPesoConfezione());
            assertTrue(v.isEvidenza());
        }
    }

    @Test
    void doRetrieveVariantiByProdotti_EmptyList_ReturnsMutableList() {
        List<Prodotto> emptyList = new ArrayList<>();
        List<Variante> result = dao.doRetrieveVariantiByProdotti(emptyList);

        assertTrue(result.isEmpty());
        // Verify mutability to kill Collections.emptyList mutant
        assertDoesNotThrow(() -> result.add(new Variante()));
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
            when(mockResultSet.getString("id_prodotto_variante")).thenReturn("P1");
            when(mockResultSet.getInt("id_gusto")).thenReturn(2);
            when(mockResultSet.getInt("id_confezione")).thenReturn(3);
            when(mockResultSet.getInt("quantità")).thenReturn(50);
            when(mockResultSet.getFloat("prezzo")).thenReturn(20.0f);
            when(mockResultSet.getInt("sconto")).thenReturn(10);
            when(mockResultSet.getString("nomeGusto")).thenReturn("Fragola");
            when(mockResultSet.getInt("peso")).thenReturn(100);
            when(mockResultSet.getBoolean("evidenza")).thenReturn(true);

            List<Variante> result = dao.doRetrieveVariantiByIdProdotto("P1");

            assertEquals(1, result.size());
            Variante v = result.get(0);
            assertEquals(1, v.getIdVariante());
            assertEquals("P1", v.getIdProdotto());
            assertEquals(2, v.getIdGusto());
            assertEquals(3, v.getIdConfezione());
            assertEquals(50, v.getQuantita());
            assertEquals(20.0f, v.getPrezzo());
            assertEquals(10, v.getSconto());
            assertEquals("Fragola", v.getGusto());
            assertEquals(100, v.getPesoConfezione());
            assertTrue(v.isEvidenza());

            verify(mockPreparedStatement).setString(1, "P1");
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
            verify(mockPreparedStatement).setInt(2, 1);
            verify(mockPreparedStatement).setInt(3, 2);
            verify(mockPreparedStatement).setFloat(4, 10.0f);
            verify(mockPreparedStatement).setInt(5, 100);
            verify(mockPreparedStatement).setInt(6, 5);
            verify(mockPreparedStatement).setBoolean(7, true);
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
            when(mockResultSet.getInt("id_variante")).thenReturn(77);
            when(mockResultSet.getString("id_prodotto_variante")).thenReturn("P1");
            when(mockResultSet.getInt("id_gusto")).thenReturn(1);
            when(mockResultSet.getInt("id_confezione")).thenReturn(2);
            when(mockResultSet.getInt("quantità")).thenReturn(33);
            when(mockResultSet.getFloat("prezzo")).thenReturn(9.99f);
            when(mockResultSet.getInt("sconto")).thenReturn(5);
            when(mockResultSet.getBoolean("evidenza")).thenReturn(true);
            when(mockResultSet.getString("nomeGusto")).thenReturn("Cheapest");
            when(mockResultSet.getInt("peso")).thenReturn(150);

            Variante result = dao.doRetrieveCheapestVariant("P1");

            assertEquals(77, result.getIdVariante());
            assertEquals("P1", result.getIdProdotto());
            assertEquals(1, result.getIdGusto());
            assertEquals(2, result.getIdConfezione());
            assertEquals(33, result.getQuantita());
            assertEquals(9.99f, result.getPrezzo());
            assertEquals(5, result.getSconto());
            assertTrue(result.isEvidenza());
            assertEquals("Cheapest", result.getGusto());
            assertEquals(150, result.getPesoConfezione());

            verify(mockPreparedStatement).setString(1, "P1");
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
            when(mockResultSet.getString("id_prodotto_variante")).thenReturn("P1", "P2");
            when(mockResultSet.getInt("id_gusto")).thenReturn(2, 3);
            when(mockResultSet.getInt("id_confezione")).thenReturn(3, 4);
            when(mockResultSet.getInt("quantità")).thenReturn(50, 60);
            when(mockResultSet.getFloat("prezzo")).thenReturn(20.0f, 30.0f);
            when(mockResultSet.getInt("sconto")).thenReturn(10, 15);
            when(mockResultSet.getBoolean("evidenza")).thenReturn(true, false);

            List<Variante> result = dao.doRetrieveAll();

            assertEquals(2, result.size());

            Variante v1 = result.get(0);
            assertEquals(1, v1.getIdVariante());
            assertEquals("P1", v1.getIdProdotto());
            assertEquals(2, v1.getIdGusto());
            assertEquals(3, v1.getIdConfezione());
            assertEquals(50, v1.getQuantita());
            assertEquals(20.0f, v1.getPrezzo());
            assertEquals(10, v1.getSconto());
            assertTrue(v1.isEvidenza());

            Variante v2 = result.get(1);
            assertEquals(2, v2.getIdVariante());
            assertEquals("P2", v2.getIdProdotto());
            assertEquals(3, v2.getIdGusto());
            assertEquals(4, v2.getIdConfezione());
            assertEquals(60, v2.getQuantita());
            assertEquals(30.0f, v2.getPrezzo());
            assertEquals(15, v2.getSconto());
            assertFalse(v2.isEvidenza());
        }
    }

    @Test
    void doRetrieveVariantByFlavourAndWeight_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("id_variante")).thenReturn(99);
            when(mockResultSet.getString("nomeGusto")).thenReturn("Fragola");
            when(mockResultSet.getFloat("prezzo")).thenReturn(5.5f);

            // Mock other fields with NON-DEFAULT values
            when(mockResultSet.getString("id_prodotto_variante")).thenReturn("P1");
            when(mockResultSet.getInt("id_gusto")).thenReturn(11);
            when(mockResultSet.getInt("id_confezione")).thenReturn(22);
            when(mockResultSet.getInt("quantità")).thenReturn(33);
            when(mockResultSet.getInt("sconto")).thenReturn(44);
            when(mockResultSet.getBoolean("evidenza")).thenReturn(true);
            when(mockResultSet.getInt("peso")).thenReturn(555);

            List<Variante> result = dao.doRetrieveVariantByFlavourAndWeight("P1", "Fragola", 500);

            assertEquals(1, result.size());
            Variante v = result.get(0);
            assertEquals("Fragola", v.getGusto());
            assertEquals(99, v.getIdVariante());
            assertEquals(5.5f, v.getPrezzo());

            // Assert all fields
            assertEquals("P1", v.getIdProdotto());
            assertEquals(11, v.getIdGusto());
            assertEquals(22, v.getIdConfezione());
            assertEquals(33, v.getQuantita());
            assertEquals(44, v.getSconto());
            assertTrue(v.isEvidenza());
            assertEquals(555, v.getPesoConfezione());

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
            when(mockResultSet.getString("id_prodotto_variante")).thenReturn("P1");
            when(mockResultSet.getInt("id_gusto")).thenReturn(2);
            when(mockResultSet.getInt("id_confezione")).thenReturn(3);
            when(mockResultSet.getInt("quantità")).thenReturn(50);
            when(mockResultSet.getFloat("prezzo")).thenReturn(15.0f);

            // Mock NON-DEFAULT values
            when(mockResultSet.getInt("sconto")).thenReturn(10); // Not 0
            when(mockResultSet.getBoolean("evidenza")).thenReturn(true); // Not false

            when(mockResultSet.getString("nomeGusto")).thenReturn("A");
            when(mockResultSet.getInt("peso")).thenReturn(100);

            Variante result = dao.doRetrieveVarianteByIdVariante(123);

            assertNotNull(result);
            assertEquals(123, result.getIdVariante());
            assertEquals("P1", result.getIdProdotto());
            assertEquals(2, result.getIdGusto());
            assertEquals(3, result.getIdConfezione());
            assertEquals(50, result.getQuantita());
            assertEquals(15.0f, result.getPrezzo());

            // Assert NON-DEFAULT values
            assertEquals(10, result.getSconto());
            assertTrue(result.isEvidenza());

            assertEquals("A", result.getGusto());
            assertEquals(100, result.getPesoConfezione());

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
            verify(mockPreparedStatement).setInt(3, 2);
            verify(mockPreparedStatement).setInt(4, 3);
            verify(mockPreparedStatement).setFloat(5, 15.0f);
            verify(mockPreparedStatement).setInt(6, 50);
            verify(mockPreparedStatement).setInt(7, 0);
            verify(mockPreparedStatement).setBoolean(8, false);
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
    void doRetrieveFilteredVariantiByIdProdotto_ReturnsData() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            // Return 2 rows to check logic
            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getInt("id_variante")).thenReturn(10, 20);
            when(mockResultSet.getString("id_prodotto_variante")).thenReturn("P1", "P1");
            when(mockResultSet.getInt("id_gusto")).thenReturn(1, 2);
            when(mockResultSet.getInt("id_confezione")).thenReturn(1, 2);
            when(mockResultSet.getInt("quantità")).thenReturn(5, 5);
            when(mockResultSet.getFloat("prezzo")).thenReturn(10f, 20f);
            when(mockResultSet.getInt("sconto")).thenReturn(0, 5);
            when(mockResultSet.getBoolean("evidenza")).thenReturn(false, true);
            when(mockResultSet.getString("nomeGusto")).thenReturn("G1", "G2");
            when(mockResultSet.getInt("peso")).thenReturn(100, 200);

            List<Variante> result = dao.doRetrieveFilteredVariantiByIdProdotto("P1", null, null);

            assertEquals(2, result.size());
            Variante v1 = result.get(0);
            assertEquals(10, v1.getIdVariante());
            assertEquals("P1", v1.getIdProdotto());
            assertEquals(1, v1.getIdGusto());
            assertEquals(1, v1.getIdConfezione());
            assertEquals(5, v1.getQuantita());
            assertEquals(10f, v1.getPrezzo());
            assertEquals(0, v1.getSconto());
            assertFalse(v1.isEvidenza());
            assertEquals("G1", v1.getGusto());
            assertEquals(100, v1.getPesoConfezione());

            // Check v2 which has non-default values to kill setter mutants (sconto=5,
            // evidenza=true)
            Variante v2 = result.get(1);
            assertEquals(20, v2.getIdVariante());
            assertEquals("P1", v2.getIdProdotto());
            assertEquals(2, v2.getIdGusto());
            assertEquals(2, v2.getIdConfezione());
            assertEquals(5, v2.getQuantita());
            assertEquals(20f, v2.getPrezzo());
            assertEquals(5, v2.getSconto());
            assertTrue(v2.isEvidenza());
            assertEquals("G2", v2.getGusto());
            assertEquals(200, v2.getPesoConfezione());

            // Kill EmptyObjectReturnValsMutator
            assertFalse(result.isEmpty());
        }
    }

    @Test
    void doRetrieveVariantByCriteria_ReturnsData() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("id_variante")).thenReturn(55);
            when(mockResultSet.getString("id_prodotto_variante")).thenReturn("P1");
            when(mockResultSet.getInt("id_gusto")).thenReturn(1);
            when(mockResultSet.getInt("id_confezione")).thenReturn(1);
            when(mockResultSet.getInt("quantità")).thenReturn(10);
            when(mockResultSet.getFloat("prezzo")).thenReturn(100f);
            when(mockResultSet.getInt("sconto")).thenReturn(5);
            when(mockResultSet.getBoolean("evidenza")).thenReturn(true);
            when(mockResultSet.getString("nomeGusto")).thenReturn("G1");
            when(mockResultSet.getInt("peso")).thenReturn(500);

            List<Variante> result = dao.doRetrieveVariantByCriteria("P1", "flavour", "Vanilla");

            assertEquals(1, result.size());
            // Kill EmptyObjectReturnValsMutator and setter mutants
            Variante v = result.get(0);
            assertEquals(55, v.getIdVariante());
            assertEquals("P1", v.getIdProdotto());
            assertEquals(1, v.getIdGusto());
            assertEquals(1, v.getIdConfezione());
            assertEquals(10, v.getQuantita());
            assertEquals(100f, v.getPrezzo());
            assertEquals(5, v.getSconto());
            assertTrue(v.isEvidenza());
            assertEquals("G1", v.getGusto());
            assertEquals(500, v.getPesoConfezione());
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
    void doRetrieveCheapestFilteredVarianteByIdProdotto_ReturnsCorrectData() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("id_variante")).thenReturn(88);
            when(mockResultSet.getString("id_prodotto_variante")).thenReturn("P1");
            when(mockResultSet.getInt("id_gusto")).thenReturn(1);
            when(mockResultSet.getInt("id_confezione")).thenReturn(2);
            when(mockResultSet.getInt("quantità")).thenReturn(10);
            when(mockResultSet.getFloat("prezzo")).thenReturn(12.5f);
            when(mockResultSet.getInt("sconto")).thenReturn(5);
            when(mockResultSet.getBoolean("evidenza")).thenReturn(true);
            when(mockResultSet.getString("nomeGusto")).thenReturn("CheapestFiltered");
            when(mockResultSet.getInt("peso")).thenReturn(300);

            Variante result = dao.doRetrieveCheapestFilteredVarianteByIdProdotto("P1", "300 g",
                    "CheapestFiltered (Best)", true);

            assertNotNull(result);
            assertEquals(88, result.getIdVariante());
            assertEquals("P1", result.getIdProdotto());
            assertEquals(1, result.getIdGusto());
            assertEquals(2, result.getIdConfezione());
            assertEquals(10, result.getQuantita());
            assertEquals(12.5f, result.getPrezzo());
            assertEquals(5, result.getSconto());
            assertTrue(result.isEvidenza());
            assertEquals("CheapestFiltered", result.getGusto());
            assertEquals(300, result.getPesoConfezione());

            // Verify filters were applied (checks correct parsing too) with InOrder
            org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(mockPreparedStatement);
            inOrder.verify(mockPreparedStatement).setString(1, "P1");
            // The order depends on implementation, usually weight then taste
            inOrder.verify(mockPreparedStatement).setInt(2, 300);
            inOrder.verify(mockPreparedStatement).setString(3, "CheapestFiltered");
            inOrder.verify(mockPreparedStatement).executeQuery();
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