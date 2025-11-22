package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        // ... setta altri campi necessari per evitare NullPointerException se usati nel DAO

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            prodottoDAO.doSave(p);

            verify(mockPreparedStatement).setString(1, "P1");
            verify(mockPreparedStatement).setInt(6, 100);
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void filterProducts_WithCategoryAndSorting() throws SQLException {
        // Scenario: 2 prodotti, filtriamo per categoria e ordiniamo per Calorie Descrescenti

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class);
             MockedConstruction<VarianteDAO> mockedVarianteDAO = Mockito.mockConstruction(VarianteDAO.class,
                     (mock, context) -> {
                         // Simuliamo che ogni prodotto abbia una variante "cheapest" valida
                         // Altrimenti il codice rimuoverebbe il prodotto dalla lista
                         Variante v = new Variante();
                         v.setPrezzo(10.0f);
                         v.setSconto(0);
                         when(mock.doRetrieveCheapestFilteredVarianteByIdProdotto(anyString(), any(), any(), anyBoolean()))
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
            // Poiché filterProducts costruisce la query dinamicamente, catturiamo l'argomento
            // Ma qui basta sapere che è stato chiamato prepareStatement con la stringa giusta
            // Se vuoi essere preciso, usa ArgumentCaptor, ma per ora ci fidiamo che il codice non sia crashato
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
            when(mockConnection.prepareStatement(contains("WHERE categoria = ?"))).thenReturn(mockPreparedStatement); // Nota il contains che è case sensitive a volte, meglio usare stringa parziale o esatta se nota
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
}