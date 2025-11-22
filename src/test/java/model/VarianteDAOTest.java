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
        Prodotto p1 = new Prodotto(); p1.setIdProdotto("A");
        Prodotto p2 = new Prodotto(); p2.setIdProdotto("B");
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
}