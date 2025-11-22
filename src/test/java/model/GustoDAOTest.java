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

class GustoDAOTest {

    private GustoDAO gustoDAO;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        gustoDAO = new GustoDAO();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
    }

    @Test
    void doRetrieveById_Found() throws SQLException {
        int id = 1;
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("id_gusto")).thenReturn(id);
            when(mockResultSet.getString("nomeGusto")).thenReturn("Cioccolato");

            Gusto result = gustoDAO.doRetrieveById(id);

            assertNotNull(result);
            assertEquals(id, result.getIdGusto());
            assertEquals("Cioccolato", result.getNomeGusto());
        }
    }

    @Test
    void doRetrieveById_NotFound_ReturnsEmptyObject() throws SQLException {
        // Nota: Il tuo DAO restituisce new Gusto() (vuoto) se non trova nulla, NON null.
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(false);

            Gusto result = gustoDAO.doRetrieveById(999);

            assertNotNull(result); // Non è null
            assertEquals(0, result.getIdGusto()); // ID è 0 (default int)
            assertNull(result.getNomeGusto()); // Nome è null
        }
    }

    @Test
    void doRetrieveByIdVariante_Found() throws SQLException {
        int idVariante = 5;
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(contains("JOIN variante"))).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("id_gusto")).thenReturn(10);
            // ATTENZIONE: Il tuo codice chiama rs.getString("nome") anche se la query seleziona "nomeGusto"
            // Simulo il comportamento del codice Java:
            when(mockResultSet.getString("nome")).thenReturn("Vaniglia");

            Gusto result = gustoDAO.doRetrieveByIdVariante(idVariante);

            assertNotNull(result);
            assertEquals("Vaniglia", result.getNomeGusto()); // Nota: il getter è getNome ma nel DAO setti setNome(rs.getString("nome"))? Verifica i getter/setter del bean Gusto
            // Assumendo che il bean abbia getNome() o getNomeGusto()
        }
    }

    @Test
    void doRetrieveByIdVariante_NotFound_ReturnsNull() throws SQLException {
        // Nota: Questo metodo restituisce NULL se non trova nulla (diverso da doRetrieveById)
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(false);

            Gusto result = gustoDAO.doRetrieveByIdVariante(999);

            assertNull(result);
        }
    }

    @Test
    void doRetrieveAll_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, true, false); // 2 risultati
            when(mockResultSet.getString("nomeGusto")).thenReturn("Gusto1", "Gusto2");

            List<Gusto> result = gustoDAO.doRetrieveAll();

            assertEquals(2, result.size());
            assertEquals("Gusto1", result.get(0).getNomeGusto());
        }
    }

    @Test
    void updateGusto_Success() throws SQLException {
        Gusto g = new Gusto();
        g.setIdGusto(1);
        g.setNome("NuovoNome"); // o setNomeGusto a seconda del tuo Bean

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            gustoDAO.updateGusto(g, 1);

            verify(mockPreparedStatement).setString(2, "NuovoNome");
            verify(mockPreparedStatement).setInt(3, 1); // WHERE id
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doSaveGusto_FullObject_GeneratesCorrectSQL() throws SQLException {
        // Testiamo che se c'è ID e NOME, la query li includa entrambi
        Gusto g = new Gusto();
        g.setIdGusto(100);
        g.setNome("Pistacchio");

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            // Captor per la query SQL
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);

            gustoDAO.doSaveGusto(g);

            String sql = sqlCaptor.getValue();

            // Verifica che la query contenga i campi corretti
            assertTrue(sql.contains("id_gusto"), "Deve contenere id_gusto");
            assertTrue(sql.contains("nomeGusto"), "Deve contenere nomeGusto");

            // Verifica i parametri passati
            verify(mockPreparedStatement).setObject(eq(1), eq(100));
            verify(mockPreparedStatement).setObject(eq(2), eq("Pistacchio"));
        }
    }

    @Test
    void doSaveGusto_OnlyName_GeneratesCorrectSQL() throws SQLException {
        // Testiamo l'inserimento standard (senza ID specificato, es. auto-increment)
        Gusto g = new Gusto();
        g.setIdGusto(0); // 0 o -1 solitamente indica "nuovo"
        g.setNome("Fragola");

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);

            gustoDAO.doSaveGusto(g);

            String sql = sqlCaptor.getValue();

            // La query NON deve contenere id_gusto perché id è 0
            assertFalse(sql.contains("id_gusto"), "Non deve contenere id_gusto se è 0");
            assertTrue(sql.contains("nomeGusto"), "Deve contenere nomeGusto");

            // Verifica che ci sia solo 1 parametro settato
            verify(mockPreparedStatement).setObject(eq(1), eq("Fragola"));
            // Verifica che non venga chiamato setObject(2, ...)
            verify(mockPreparedStatement, never()).setObject(eq(2), any());
        }
    }

    @Test
    void doRemoveGusto_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            gustoDAO.doRemoveGusto(5);

            verify(mockPreparedStatement).setInt(1, 5);
            verify(mockPreparedStatement).executeUpdate();
        }
    }
}