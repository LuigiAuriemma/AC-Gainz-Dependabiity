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

            verify(mockPreparedStatement).setInt(1, id); // Killed VoidMethodCallMutator
        }
    }

    // ... (keep doRetrieveById_NotFound_ReturnsEmptyObject) ...

    @Test
    void doRetrieveByIdVariante_Found() throws SQLException {
        int idVariante = 5;
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(contains("JOIN variante"))).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("id_gusto")).thenReturn(10);
            when(mockResultSet.getString("nome")).thenReturn("Vaniglia");

            Gusto result = gustoDAO.doRetrieveByIdVariante(idVariante);

            assertNotNull(result);
            assertEquals(10, result.getIdGusto()); // Killed check on id_gusto setter
            assertEquals("Vaniglia", result.getNomeGusto());

            verify(mockPreparedStatement).setInt(1, idVariante); // Killed checks on setInt
        }
    }

    // ... (keep doRetrieveByIdVariante_NotFound_ReturnsNull) ...

    @Test
    void doRetrieveAll_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, true, false); // 2 risultati
            when(mockResultSet.getInt("id_gusto")).thenReturn(1, 2); // Simula ID diversi
            when(mockResultSet.getString("nomeGusto")).thenReturn("Gusto1", "Gusto2");

            List<Gusto> result = gustoDAO.doRetrieveAll();

            assertEquals(2, result.size());
            assertEquals("Gusto1", result.get(0).getNomeGusto());
            assertEquals(1, result.get(0).getIdGusto()); // Killed check on id_gusto
            assertEquals("Gusto2", result.get(1).getNomeGusto());
            assertEquals(2, result.get(1).getIdGusto());
        }
    }

    @Test
    void updateGusto_Success() throws SQLException {
        Gusto g = new Gusto();
        g.setIdGusto(1);
        g.setNome("NuovoNome");

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            gustoDAO.updateGusto(g, 1);

            verify(mockPreparedStatement).setString(2, "NuovoNome");
            verify(mockPreparedStatement).setInt(1, 1); // Verify setInt(1, g.getIdGusto())
            verify(mockPreparedStatement).setInt(3, 1); // WHERE id
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doSaveGusto_FullObject_GeneratesCorrectSQL() throws SQLException {
        // Capture stdout
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));

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

            // Verifica Esatta della Query
            assertEquals("INSERT INTO gusto (id_gusto, nomeGusto) VALUES (?, ?)", sql);

            // Verifica i parametri passati
            verify(mockPreparedStatement).setObject(eq(1), eq(100));
            verify(mockPreparedStatement).setObject(eq(2), eq("Pistacchio"));
            verify(mockPreparedStatement).executeUpdate();

            // Verifica stdout
            assertTrue(outContent.toString().contains("INSERT INTO gusto"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void doSaveGusto_OnlyName_GeneratesCorrectSQL() throws SQLException {
        // Capture stdout
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));

        // Testiamo l'inserimento standard (senza ID specificato, es. auto-increment)
        Gusto g = new Gusto();
        g.setIdGusto(0);
        g.setNome("Fragola");

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);

            gustoDAO.doSaveGusto(g);

            String sql = sqlCaptor.getValue();

            // Verifica Esatta della Query
            assertEquals("INSERT INTO gusto (nomeGusto) VALUES (?)", sql);

            // Verifica che ci sia solo 1 parametro settato
            verify(mockPreparedStatement).setObject(eq(1), eq("Fragola"));
            verify(mockPreparedStatement).executeUpdate();

            // Verifica stdout
            assertTrue(outContent.toString().contains("INSERT INTO gusto"));
        } finally {
            System.setOut(originalOut);
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

    // --- NEW TESTS ---

    @Test
    void doRetrieveById_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> gustoDAO.doRetrieveById(1));
        }
    }

    @Test
    void doRetrieveByIdVariante_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> gustoDAO.doRetrieveByIdVariante(1));
        }
    }

    @Test
    void doRetrieveAll_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> gustoDAO.doRetrieveAll());
        }
    }

    @Test
    void updateGusto_SQLException() throws SQLException {
        Gusto g = new Gusto();
        g.setIdGusto(1);
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> gustoDAO.updateGusto(g, 1));
        }
    }

    @Test
    void doSaveGusto_SQLException() throws SQLException {
        // Capture stderr to kill VoidMethodCallMutator on printStackTrace
        java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalErr = System.err;
        System.setErr(new java.io.PrintStream(errContent));

        Gusto g = new Gusto();
        g.setNome("Test");
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            // doSaveGusto catches SQLException and prints stack trace, does NOT throw
            // RuntimeException
            assertDoesNotThrow(() -> gustoDAO.doSaveGusto(g));

            // Verify that printStackTrace was called
            assertTrue(errContent.toString().contains("java.sql.SQLException: DB Error"),
                    "Should print stack trace to stderr");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void doRemoveGusto_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> gustoDAO.doRemoveGusto(1));
        }
    }

    @Test
    void doSaveGusto_NoFields() throws SQLException {
        // Capture stdout
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));

        Gusto g = new Gusto(); // id=0, nome=null
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);

            gustoDAO.doSaveGusto(g);

            String sql = sqlCaptor.getValue();

            // Verifiche per il caso "Tutto vuoto"
            assertFalse(sql.contains("id_gusto"), "Non deve contenere id_gusto");
            assertFalse(sql.contains("nomeGusto"), "Non deve contenere nomeGusto");
            assertTrue(sql.contains("VALUES ("), "Deve contenere VALUES");
            // Se vuoto parameters.size() == 0, quindi loop non esegue

            verify(mockPreparedStatement, never()).setObject(anyInt(), any());
            verify(mockPreparedStatement).executeUpdate();

            assertTrue(outContent.toString().contains("INSERT INTO gusto"));
            assertTrue(outContent.toString().contains("[]"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void doSaveGusto_BoundaryId() throws SQLException {
        // Test id=1 per killare mutanti di boundary (es. > 1)
        Gusto g = new Gusto();
        g.setIdGusto(1);
        g.setNome("Caffe");

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);

            gustoDAO.doSaveGusto(g);
            String sql = sqlCaptor.getValue();

            assertTrue(sql.contains("id_gusto"), "Deve contenere id_gusto anche se id=1");
        }

        // Test id=-1 per killare mutanti (es. >= 0 se failano su 0)
        Gusto gNeg = new Gusto();
        gNeg.setIdGusto(-1);
        gNeg.setNome("Caffe");
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);

            gustoDAO.doSaveGusto(gNeg);
            String sql = sqlCaptor.getValue();

            assertFalse(sql.contains("id_gusto"), "Non deve contenere id_gusto se id=-1");
        }
    }
}