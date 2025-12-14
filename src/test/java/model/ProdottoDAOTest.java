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
        List<Variante> varianteList = new ArrayList<>();
        varianteList.add(new Variante()); // Add a variant

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> {
                            when(mock.doRetrieveVariantiByIdProdotto(idProd)).thenReturn(varianteList);
                        })) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getString("id_prodotto")).thenReturn(idProd);
            when(mockResultSet.getString("nome")).thenReturn("Proteine Whey");
            when(mockResultSet.getString("descrizione")).thenReturn("Ottime");
            when(mockResultSet.getString("categoria")).thenReturn("Proteine");
            when(mockResultSet.getString("immagine")).thenReturn("img.jpg");
            when(mockResultSet.getInt("calorie")).thenReturn(150);
            when(mockResultSet.getInt("carboidrati")).thenReturn(5);
            when(mockResultSet.getInt("proteine")).thenReturn(30);
            when(mockResultSet.getInt("grassi")).thenReturn(2);

            Prodotto result = prodottoDAO.doRetrieveById(idProd);

            assertNotNull(result);
            assertEquals(idProd, result.getIdProdotto());
            assertEquals("Proteine Whey", result.getNome());
            assertEquals("Ottime", result.getDescrizione());
            assertEquals("Proteine", result.getCategoria());
            assertEquals("img.jpg", result.getImmagine());
            assertEquals(150, result.getCalorie());
            assertEquals(5, result.getCarboidrati());
            assertEquals(30, result.getProteine());
            assertEquals(2, result.getGrassi());
            assertEquals(1, result.getVarianti().size()); // Kill setVarianti line 35

            verify(mockPreparedStatement).setString(1, idProd); // Kill setString line 15
            verify(mockedVarianteDAO.constructed().get(0)).doRetrieveVariantiByIdProdotto(idProd);
        }
    }

    @Test
    void doSave_Success() throws SQLException {
        Prodotto p = new Prodotto();
        p.setIdProdotto("P1");
        p.setNome("Test");
        p.setDescrizione("Desc");
        p.setCategoria("Cat");
        p.setImmagine("img.jpg");
        p.setCalorie(100);
        p.setCarboidrati(50);
        p.setProteine(20);
        p.setGrassi(10);

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            prodottoDAO.doSave(p);

            verify(mockPreparedStatement).setString(1, "P1");
            verify(mockPreparedStatement).setString(2, "Test");
            verify(mockPreparedStatement).setString(3, "Desc");
            verify(mockPreparedStatement).setString(4, "Cat");
            verify(mockPreparedStatement).setString(5, "img.jpg");
            verify(mockPreparedStatement).setInt(6, 100);
            verify(mockPreparedStatement).setInt(7, 50);
            verify(mockPreparedStatement).setInt(8, 20);
            verify(mockPreparedStatement).setInt(9, 10);

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
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P_Snack");
            when(mockResultSet.getString("nome")).thenReturn("Barretta");
            when(mockResultSet.getString("descrizione")).thenReturn("Gustosa");
            when(mockResultSet.getString("categoria")).thenReturn("Snack");
            when(mockResultSet.getString("immagine")).thenReturn("snack.jpg");
            when(mockResultSet.getInt("calorie")).thenReturn(200);
            when(mockResultSet.getInt("carboidrati")).thenReturn(20);
            when(mockResultSet.getInt("proteine")).thenReturn(10);
            when(mockResultSet.getInt("grassi")).thenReturn(5);

            List<Prodotto> result = prodottoDAO.doRetrieveByCriteria(attr, val);

            assertEquals(1, result.size());
            Prodotto p = result.get(0);
            assertEquals("P_Snack", p.getIdProdotto());
            assertEquals("Barretta", p.getNome());
            assertEquals("Gustosa", p.getDescrizione());
            assertEquals("Snack", p.getCategoria());
            assertEquals("snack.jpg", p.getImmagine());
            assertEquals(200, p.getCalorie());
            assertEquals(20, p.getCarboidrati());
            assertEquals(10, p.getProteine());
            assertEquals(5, p.getGrassi());
            assertNotNull(p.getVarianti());
            assertEquals(1, p.getVarianti().size());

            verify(mockPreparedStatement).setString(1, val);
        }
    }

    @Test
    void updateProduct_Success() throws SQLException {
        Prodotto p = new Prodotto();
        p.setIdProdotto("P_UPD");
        p.setNome("Updated");
        p.setDescrizione("DescUpd");
        p.setCategoria("CatUpd");
        p.setImmagine("ImgUpd");
        p.setCalorie(300);
        p.setCarboidrati(30);
        p.setProteine(30);
        p.setGrassi(30);

        String idOld = "P_OLD";

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            prodottoDAO.updateProduct(p, idOld);

            verify(mockPreparedStatement).setString(1, "P_UPD");
            verify(mockPreparedStatement).setString(2, "Updated");
            verify(mockPreparedStatement).setString(3, "DescUpd");
            verify(mockPreparedStatement).setString(4, "CatUpd");
            verify(mockPreparedStatement).setString(5, "ImgUpd");
            verify(mockPreparedStatement).setInt(6, 300);
            verify(mockPreparedStatement).setInt(7, 30);
            verify(mockPreparedStatement).setInt(8, 30);
            verify(mockPreparedStatement).setInt(9, 30);
            verify(mockPreparedStatement).setString(10, idOld);
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
            when(mockResultSet.getString("nome")).thenReturn("Name");
            when(mockResultSet.getString("descrizione")).thenReturn("Desc");
            when(mockResultSet.getString("categoria")).thenReturn("Cat");
            when(mockResultSet.getString("immagine")).thenReturn("Img");
            when(mockResultSet.getInt("calorie")).thenReturn(100);
            when(mockResultSet.getInt("carboidrati")).thenReturn(10);
            when(mockResultSet.getInt("proteine")).thenReturn(10);
            when(mockResultSet.getInt("grassi")).thenReturn(10);

            List<Prodotto> result = prodottoDAO.doRetrieveAll();

            assertEquals(1, result.size());
            Prodotto p = result.get(0);
            assertEquals("ALL_1", p.getIdProdotto());
            assertEquals("Name", p.getNome());
            assertEquals("Desc", p.getDescrizione());
            assertEquals("Cat", p.getCategoria());
            assertEquals("Img", p.getImmagine());
            assertEquals(100, p.getCalorie());
            assertEquals(10, p.getCarboidrati());
            assertEquals(10, p.getProteine());
            assertEquals(10, p.getGrassi());
            assertNotNull(p.getVarianti());
            assertEquals(1, p.getVarianti().size());
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
            assertEquals("P_NAME", result.get(0).getIdProdotto());
            // Assert extra fields to kill extractProductFromResultSet mutants
            // Note: we need to ensure the mocked ResultSet returns these values first.
            // In the setup above, we only mocked id_prodotto. Let's add more.
        }
    }

    @Test
    void filterProducts_FieldsVerification() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> {
                            when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(anyString(), any(), any(),
                                    anyBoolean()))
                                    .thenReturn(new Variante());
                        })) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P_FIELDS");
            when(mockResultSet.getString("nome")).thenReturn("ProdName");
            when(mockResultSet.getString("categoria")).thenReturn("ProdCat");
            when(mockResultSet.getString("immagine")).thenReturn("ProdImg");
            when(mockResultSet.getInt("calorie")).thenReturn(123);

            List<Prodotto> result = prodottoDAO.filterProducts(null, null, null, null, null);

            assertEquals(1, result.size());
            Prodotto p = result.get(0);
            assertEquals("P_FIELDS", p.getIdProdotto());
            assertEquals("ProdName", p.getNome()); // Kill setNome
            assertEquals("ProdCat", p.getCategoria()); // Kill setCategoria
            assertEquals("ProdImg", p.getImmagine()); // Kill setImmagine
            assertEquals(123, p.getCalorie());
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
        List<Prodotto> list = new ArrayList<>();
        list.add(new Prodotto());
        doReturn(list).when(spyDao).doRetrieveAll();

        List<Prodotto> result = spyDao.doRetrieveByCriteria("any", "Tutto");

        verify(spyDao).doRetrieveAll();
        assertEquals(1, result.size()); // Kill EmptyObjectReturnValsMutator
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
    void filterProducts_SortingPriceAsc_StrictAndRobust() throws SQLException {
        // Targeted Test to kill MathMutators in getLowestPrice
        // P1: Price 100, Disc 20 -> Net 80.
        // P2: Price 85, Disc 0 -> Net 85.
        // P3: Price 50, Disc 0 -> Net 50.
        // Expected Asc Order: P3 (50), P1 (80), P2 (85).

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> {
                            Variante v1 = new Variante();
                            v1.setPrezzo(100.0f);
                            v1.setSconto(20);
                            Variante v2 = new Variante();
                            v2.setPrezzo(85.0f);
                            v2.setSconto(0);
                            Variante v3 = new Variante();
                            v3.setPrezzo(50.0f);
                            v3.setSconto(0);

                            when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(eq("P1"), any(), any(),
                                    anyBoolean())).thenReturn(v1);
                            when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(eq("P2"), any(), any(),
                                    anyBoolean())).thenReturn(v2);
                            when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(eq("P3"), any(), any(),
                                    anyBoolean())).thenReturn(v3);
                        })) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, true, true, false);
            when(mockResultSet.getString("id_prodotto")).thenReturn("P1", "P2", "P3");
            when(mockResultSet.getInt("calorie")).thenReturn(100, 100, 100);

            List<Prodotto> result = prodottoDAO.filterProducts(null, "PriceAsc", null, null, null);

            assertEquals(3, result.size());
            assertEquals("P3", result.get(0).getIdProdotto()); // 50
            assertEquals("P1", result.get(1).getIdProdotto()); // 80
            assertEquals("P2", result.get(2).getIdProdotto()); // 85
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

    @Test
    void filterProducts_SystemOutCapture() throws SQLException {
        // Test to kill System.out.println mutants
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
                MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                        (mock, context) -> when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(anyString(), any(),
                                any(), anyBoolean()))
                                .thenReturn(new Variante()))) {

            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(false);

            prodottoDAO.filterProducts("Cat", null, null, null, "Name");

            String output = outContent.toString();
            // Verify debug print statements
            assertTrue(output.contains("nameFilterDAO: Name"));
            // Verify SQL print statement to kill mutant at line 77
            // logic: if (!conditions.isEmpty()) -> print sql
            assertTrue(output.contains("SELECT p.* FROM prodotto p WHERE"));

        } finally {
            System.setOut(originalOut);
        }
    }
}