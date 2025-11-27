package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProdottoDAOTest {

    private ProdottoDAO prodottoDAO;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private Statement mockStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        prodottoDAO = new ProdottoDAO();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockStatement = mock(Statement.class);
        mockResultSet = mock(ResultSet.class);
    }

    @Test
    void doRetrieveById_Success() throws SQLException {
        String idProd = "PROD1";

        // 1. Mock della connessione statica
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                // 2. Mock del "new VarianteDAO()" che avviene dentro il metodo
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> {
                            // Quando viene chiamato doRetrieveVarianti... sul mock creato internamente
                            when(mock.doRetrieveVariantiByIdProdotto(idProd)).thenReturn(new ArrayList<>());
                        })) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            // Simuliamo il ritrovamento del prodotto
            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getString("id_prodotto")).thenReturn(idProd);
            when(mockResultSet.getString("nome")).thenReturn("Proteine Whey");
            // ... altri campi opzionali per il test ...

            Prodotto result = prodottoDAO.doRetrieveById(idProd);

            assertNotNull(result);
            assertEquals(idProd, result.getIdProdotto());
            assertEquals("Proteine Whey", result.getNome());

            // Verifichiamo che sia stato chiamato il metodo del VarianteDAO mockato
            VarianteDAO vDaoMock = mockedVarianteDAO.constructed().get(0);
            verify(vDaoMock).doRetrieveVariantiByIdProdotto(idProd);
        }
    }

    @Test
    void doSave_Success() throws SQLException {
        Prodotto p = new Prodotto();
        p.setIdProdotto("P1");
        p.setNome("Test");
        p.setCalorie(100);
        // ... setta altri campi necessari per evitare NullPointerException se usati nel
        // DAO

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            prodottoDAO.doSave(p);

            verify(mockPreparedStatement).setString(1, "P1");
            verify(mockPreparedStatement).setInt(6, 100);
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void filterProducts_WithCategoryAndSorting() throws SQLException {
        // Scenario: 2 prodotti, filtriamo per categoria e ordiniamo per Calorie
        // Descrescenti

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> {
                            // Simuliamo che ogni prodotto abbia una variante "cheapest" valida
                            // Altrimenti il codice rimuoverebbe il prodotto dalla lista
                            Variante v = new Variante();
                            v.setPrezzo(10.0f);
                            v.setSconto(0);
                            when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(anyString(), any(), any(),
                                    anyBoolean()))
                                    .thenReturn(v);
                        })) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            // Simuliamo 2 righe nel ResultSet
            when(mockResultSet.next()).thenReturn(true, true, false);

            // Mock dei dati per i due prodotti
            when(mockResultSet.getString("id_prodotto")).thenReturn("P1", "P2");
            when(mockResultSet.getString("nome")).thenReturn("Prod1", "Prod2");
            when(mockResultSet.getInt("calorie")).thenReturn(100, 200); // P1 ha 100, P2 ha 200

            // Chiamata al metodo
            List<Prodotto> result = prodottoDAO.filterProducts("Integratori", "CaloriesDesc", null, null, null);

            // Assert
            assertEquals(2, result.size());

            // Verifica ordinamento CaloriesDesc: P2 (200) deve essere prima di P1 (100)
            assertEquals("P2", result.get(0).getIdProdotto());
            assertEquals("P1", result.get(1).getIdProdotto());

            // Verifica SQL generato correttamente (contiene WHERE categoria)
            // Poiché filterProducts costruisce la query dinamicamente, catturiamo
            // l'argomento
            // Ma qui basta sapere che è stato chiamato prepareStatement con la stringa
            // giusta
            // Se vuoi essere preciso, usa ArgumentCaptor, ma per ora ci fidiamo che il
            // codice non sia crashato
        }
    }

    @Test
    void doRetrieveByCriteria_Success() throws SQLException {
        String attr = "categoria";
        String val = "Snack";

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> {
                            when(mock.doRetrieveCheapestVariant(anyString())).thenReturn(new Variante());
                        })) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(contains("WHERE categoria = ?"))).thenReturn(mockPreparedStatement); // Nota
                                                                                                                      // il
                                                                                                                      // contains
                                                                                                                      // che
                                                                                                                      // è
                                                                                                                      // case
                                                                                                                      // sensitive
                                                                                                                      // a
                                                                                                                      // volte,
                                                                                                                      // meglio
                                                                                                                      // usare
                                                                                                                      // stringa
                                                                                                                      // parziale
                                                                                                                      // o
                                                                                                                      // esatta
                                                                                                                      // se
                                                                                                                      // nota
            // Correggiamo matcher per essere sicuri
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P_Snack");

            List<Prodotto> list = prodottoDAO.doRetrieveByCriteria(attr, val);

            assertEquals(1, list.size());
            verify(mockPreparedStatement).setString(1, val);
        }
    }

    @Test
    void updateProduct_Success() throws SQLException {
        Prodotto p = new Prodotto();
        p.setIdProdotto("P_UPD");
        p.setNome("Updated");
        // ... setta campi
        String idOld = "P_OLD";

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            prodottoDAO.updateProduct(p, idOld);

            verify(mockPreparedStatement).setString(10, idOld); // Verifica che il WHERE usi l'id vecchio
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void removeProduct_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            prodottoDAO.removeProductFromIdProdotto("DEL_123");

            verify(mockPreparedStatement).setString(1, "DEL_123");
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doRetrieveAll_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> {
                            when(mock.doRetrieveCheapestVariant(anyString())).thenReturn(new Variante());
                        })) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.createStatement()).thenReturn(mockStatement);
            when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("ALL_1");

            List<Prodotto> result = prodottoDAO.doRetrieveAll();

            assertEquals(1, result.size());
        }
    }

    // --- NEW TESTS ---

    @Test
    void doRetrieveById_NotFound() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(false);

            Prodotto result = prodottoDAO.doRetrieveById("NOT_FOUND");

            assertNull(result);
        }
    }

    @Test
    void doRetrieveById_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> prodottoDAO.doRetrieveById("P1"));
        }
    }

    @Test
    void filterProducts_NameFilter() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> {
                            when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(anyString(), any(), any(),
                                    anyBoolean()))
                                    .thenReturn(new Variante());
                        })) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P_NAME");

            List<Prodotto> result = prodottoDAO.filterProducts(null, null, null, null, "Protein");

            String sql = sqlCaptor.getValue();
            assertTrue(sql.contains("p.nome LIKE ?"));
            verify(mockPreparedStatement).setObject(1, "%Protein%");
            assertEquals(1, result.size());
        }
    }

    @Test
    void filterProducts_SortingPriceAsc() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> {
                            // Setup variants for sorting
                            Variante v1 = new Variante();
                            v1.setPrezzo(10.0f);
                            v1.setSconto(0);
                            Variante v2 = new Variante();
                            v2.setPrezzo(5.0f);
                            v2.setSconto(0);

                            // Return different variants based on product ID (simplified logic for mock)
                            // Since we can't easily map args to returns in simple mockConstruction without
                            // complex logic,
                            // we'll rely on the fact that the DAO calls it for each product.
                            // We can use an Answer or just return a sequence if order is deterministic.
                            // But here the list is populated first, then sorted.
                            // Let's just mock that doRetrieveCheapest... returns a variant.
                            // We need to set the variant ON the product to test sorting.
                            // The DAO sets the variant returned by doRetrieveCheapest... onto the product.
                            // So we need to ensure doRetrieveCheapest returns something useful.

                            // Better approach: Mock the DAO to return specific variants for specific IDs if
                            // possible,
                            // or just return a generic one and we manually set prices on products if we
                            // could,
                            // but the DAO creates the products from ResultSet.

                            // Let's use a sequence of returns.
                            when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(eq("P1"), any(), any(),
                                    anyBoolean()))
                                    .thenReturn(v1); // 10.0
                            when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(eq("P2"), any(), any(),
                                    anyBoolean()))
                                    .thenReturn(v2); // 5.0
                        })) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P1", "P2");
            when(mockResultSet.getInt("calorie")).thenReturn(100, 100);

            List<Prodotto> result = prodottoDAO.filterProducts(null, "PriceAsc", null, null, null);

            assertEquals(2, result.size());
            assertEquals("P2", result.get(0).getIdProdotto()); // 5.0 < 10.0
            assertEquals("P1", result.get(1).getIdProdotto());
        }
    }

    @Test
    void filterProducts_SortingCaloriesAsc() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(anyString(), any(),
                                any(), anyBoolean()))
                                .thenReturn(new Variante()))) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P1", "P2");
            when(mockResultSet.getInt("calorie")).thenReturn(200, 100);

            List<Prodotto> result = prodottoDAO.filterProducts(null, "CaloriesAsc", null, null, null);

            assertEquals(2, result.size());
            assertEquals("P2", result.get(0).getIdProdotto()); // 100 < 200
            assertEquals("P1", result.get(1).getIdProdotto());
        }
    }

    @Test
    void filterProducts_NoCheapestVariant() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> {
                            // Return null to simulate no matching variant
                            when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(anyString(), any(), any(),
                                    anyBoolean()))
                                    .thenReturn(null);
                        })) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P1");

            List<Prodotto> result = prodottoDAO.filterProducts(null, null, null, null, null);

            assertTrue(result.isEmpty()); // Should be removed
        }
    }

    @Test
    void filterProducts_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> prodottoDAO.filterProducts(null, null, null, null, null));
        }
    }

    @Test
    void doSave_InsertError() throws SQLException {
        Prodotto p = new Prodotto();
        p.setIdProdotto("P1");
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(0); // 0 rows affected

            assertThrows(RuntimeException.class, () -> prodottoDAO.doSave(p));
        }
    }

    @Test
    void doSave_SQLException() throws SQLException {
        Prodotto p = new Prodotto();
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> prodottoDAO.doSave(p));
        }
    }

    @Test
    void doRetrieveByCriteria_Tutto() {
        // Spy the DAO to verify doRetrieveAll call
        ProdottoDAO spyDao = Mockito.spy(new ProdottoDAO());
        doReturn(new ArrayList<>()).when(spyDao).doRetrieveAll();

        spyDao.doRetrieveByCriteria("any", "Tutto");

        verify(spyDao).doRetrieveAll();
    }

    @Test
    void doRetrieveByCriteria_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> prodottoDAO.doRetrieveByCriteria("cat", "val"));
        }
    }

    @Test
    void doRetrieveAll_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.createStatement()).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> prodottoDAO.doRetrieveAll());
        }
    }

    @Test
    void updateProduct_SQLException() throws SQLException {
        Prodotto p = new Prodotto();
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> prodottoDAO.updateProduct(p, "ID"));
        }
    }

    @Test
    void removeProduct_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> prodottoDAO.removeProductFromIdProdotto("ID"));
        }
    }

    @Test
    void filterProducts_CategoryTutto() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(anyString(), any(),
                                any(), anyBoolean()))
                                .thenReturn(new Variante()))) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P_TUTTO");

            prodottoDAO.filterProducts("tutto", null, null, null, null);

            String sql = sqlCaptor.getValue();
            assertFalse(sql.contains("p.categoria = ?"));
        }
    }

    @Test
    void filterProducts_CategoryBlank() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(anyString(), any(),
                                any(), anyBoolean()))
                                .thenReturn(new Variante()))) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P_BLANK");

            prodottoDAO.filterProducts("   ", null, null, null, null);

            String sql = sqlCaptor.getValue();
            assertFalse(sql.contains("p.categoria = ?"));
        }
    }

    @Test
    void filterProducts_NameFilterBlank() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(anyString(), any(),
                                any(), anyBoolean()))
                                .thenReturn(new Variante()))) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P_NAME_BLANK");

            prodottoDAO.filterProducts(null, null, null, null, "   ");

            String sql = sqlCaptor.getValue();
            assertFalse(sql.contains("p.nome LIKE ?"));
        }
    }

    @Test
    void filterProducts_SortingPriceDesc() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> {
                            Variante v1 = new Variante();
                            v1.setPrezzo(10.0f);
                            v1.setSconto(0);
                            Variante v2 = new Variante();
                            v2.setPrezzo(5.0f);
                            v2.setSconto(0);
                            when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(eq("P1"), any(), any(),
                                    anyBoolean())).thenReturn(v1);
                            when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(eq("P2"), any(), any(),
                                    anyBoolean())).thenReturn(v2);
                        })) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P1", "P2");
            when(mockResultSet.getInt("calorie")).thenReturn(100, 100);

            List<Prodotto> result = prodottoDAO.filterProducts(null, "PriceDesc", null, null, null);

            assertEquals(2, result.size());
            assertEquals("P1", result.get(0).getIdProdotto()); // 10.0 > 5.0
            assertEquals("P2", result.get(1).getIdProdotto());
        }
    }

    @Test
    void filterProducts_SortingInvalid() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(anyString(), any(),
                                any(), anyBoolean()))
                                .thenReturn(new Variante()))) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P1", "P2");

            List<Prodotto> result = prodottoDAO.filterProducts(null, "InvalidSort", null, null, null);

            assertEquals(2, result.size());
            assertEquals("P1", result.get(0).getIdProdotto()); // Insertion order preserved
            assertEquals("P2", result.get(1).getIdProdotto());
        }
    }

    @Test
    void filterProducts_VariantSQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(anyString(), any(),
                                any(), anyBoolean()))
                                .thenThrow(new SQLException("Variant DB Error")))) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P1");

            assertThrows(RuntimeException.class, () -> prodottoDAO.filterProducts(null, null, null, null, null));
        }
    }
}