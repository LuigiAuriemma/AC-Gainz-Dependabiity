package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CarrelloDAOTest {

    private CarrelloDAO carrelloDAO;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        carrelloDAO = new CarrelloDAO();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
    }

    @Test
    void doSave_Success() throws SQLException {
        Carrello c = new Carrello();
        c.setEmailUtente("user@test.com");
        c.setIdProdotto("PROD_1");
        c.setIdVariante(10);
        c.setQuantita(2);
        c.setPrezzo(50.0f);

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            // Nota: il DAO usa Statement.RETURN_GENERATED_KEYS
            when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(mockPreparedStatement);

            carrelloDAO.doSave(c);

            // Verifica il setting dei parametri (ordine 1-5)
            verify(mockPreparedStatement).setString(1, "user@test.com");
            verify(mockPreparedStatement).setString(2, "PROD_1");
            verify(mockPreparedStatement).setInt(3, 10);
            verify(mockPreparedStatement).setInt(4, 2);
            verify(mockPreparedStatement).setFloat(5, 50.0f);

            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doRemoveCartByUser_Success() throws SQLException {
        String email = "delete@test.com";

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            // Simuliamo che vengano cancellate 5 righe
            when(mockPreparedStatement.executeUpdate()).thenReturn(5);

            carrelloDAO.doRemoveCartByUser(email);

            verify(mockPreparedStatement).setString(1, email);
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doRetrieveCartItemsByUser_Success_WithJoins() throws SQLException {
        String email = "user@test.com";

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            // Usiamo contains perché la query è molto lunga e contiene JOIN
            when(mockConnection.prepareStatement(contains("SELECT * FROM carrello join variante")))
                    .thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            // Simuliamo 1 prodotto nel carrello
            when(mockResultSet.next()).thenReturn(true, false);

            // --- MOCK DEI DATI ---
            // 1. Dati tabella 'carrello'
            when(mockResultSet.getString("email_utente")).thenReturn(email);
            when(mockResultSet.getString("id_prodotto")).thenReturn("PROD_A");
            when(mockResultSet.getInt("id_variante")).thenReturn(5);
            when(mockResultSet.getInt("quantità")).thenReturn(3); // Nota l'accento come nel DAO
            when(mockResultSet.getFloat("prezzo")).thenReturn(15.50f);

            // 2. Dati dalle JOIN (gusto, confezione, prodotto)
            when(mockResultSet.getString("nomeGusto")).thenReturn("Fragola");
            when(mockResultSet.getInt("peso")).thenReturn(1000);
            when(mockResultSet.getString("nome")).thenReturn("Protein Shake"); // Nome prodotto
            when(mockResultSet.getString("immagine")).thenReturn("img.jpg");

            // Esecuzione
            List<Carrello> result = carrelloDAO.doRetrieveCartItemsByUser(email);

            // Verifiche
            assertNotNull(result);
            assertEquals(1, result.size());
            Carrello item = result.get(0);

            // Verifica dati diretti
            assertEquals("PROD_A", item.getIdProdotto());
            assertEquals(3, item.getQuantita());

            // Verifica dati JOIN (importante!)
            assertEquals("Fragola", item.getGusto());
            assertEquals(1000, item.getPesoConfezione());
            assertEquals("Protein Shake", item.getNomeProdotto());
            assertEquals("img.jpg", item.getImmagineProdotto());

            verify(mockPreparedStatement).setString(1, email);
        }
    }

    @Test
    void doRetrieveCartItemsByUser_Empty() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(false);

            List<Carrello> result = carrelloDAO.doRetrieveCartItemsByUser("nobody@test.com");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void exceptionHandling_WrapsSQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> {
                carrelloDAO.doSave(new Carrello());
            });
        }
    }

    // --- NEW TESTS ---

    @Test
    void doRemoveCartByUser_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> carrelloDAO.doRemoveCartByUser("email"));
        }
    }

    @Test
    void doRetrieveCartItemsByUser_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> carrelloDAO.doRetrieveCartItemsByUser("email"));
        }
    }

    @Test
    void doRemoveCartByUser_NoDelete() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(0);

            // Should not throw exception, just print to stdout
            carrelloDAO.doRemoveCartByUser("nodelete@test.com");

            verify(mockPreparedStatement).executeUpdate();
        }
    }
}