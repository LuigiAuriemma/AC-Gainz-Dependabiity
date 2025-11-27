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

class ConfezioneDAOTest {

    private ConfezioneDAO dao;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        dao = new ConfezioneDAO();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
    }

    // --- TEST doRetrieveById ---

    @Test
    void doRetrieveById_Success() throws SQLException {
        int validId = 10;
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("id_confezione")).thenReturn(validId);
            when(mockResultSet.getInt("peso")).thenReturn(500);

            Confezione result = dao.doRetrieveById(validId);

            assertNotNull(result);
            assertEquals(validId, result.getIdConfezione());
            assertEquals(500, result.getPeso());
        }
    }

    @Test
    void doRetrieveById_InvalidInput_ThrowsException() {
        // Non serve mockare il DB qui, l'eccezione scatta prima
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            dao.doRetrieveById(-5);
        });
        assertEquals("ID confezione non valido: deve essere > 0", e.getMessage());
    }

    @Test
    void doRetrieveById_ZeroInput_ThrowsException() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            dao.doRetrieveById(0);
        });
        assertEquals("ID confezione non valido: deve essere > 0", e.getMessage());
    }

    @Test
    void doRetrieveById_ZeroIdFromDB_ThrowsRuntimeException() throws SQLException {
        // Testiamo il controllo difensivo: if (result == 0) throw RuntimeException
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("id_confezione")).thenReturn(0); // Il DB restituisce 0

            RuntimeException e = assertThrows(RuntimeException.class, () -> {
                dao.doRetrieveById(5);
            });
            assertEquals("ID confezione non può essere 0", e.getMessage());
        }
    }

    @Test
    void doRetrieveById_NotFound_ReturnsNull() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(false);

            Confezione result = dao.doRetrieveById(10);
            assertNull(result);
        }
    }

    // --- TEST doRetrieveAll ---

    @Test
    void doRetrieveAll_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            // Simuliamo 2 risultati
            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getInt("id_confezione")).thenReturn(1, 2);

            List<Confezione> list = dao.doRetrieveAll();

            assertEquals(2, list.size());
        }
    }

    // --- TEST doSaveConfezione ---

    @Test
    void doSaveConfezione_Success() throws SQLException {
        Confezione c = new Confezione();
        c.setIdConfezione(10);
        c.setPeso(100);

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            dao.doSaveConfezione(c);

            verify(mockPreparedStatement).setInt(1, 10);
            verify(mockPreparedStatement).setInt(2, 100);
            // Nota: Il tuo codice usa executeQuery() per un INSERT, quindi verifichiamo
            // quello
            verify(mockPreparedStatement).executeQuery();
        }
    }

    @Test
    void doSaveConfezione_NullInput_ThrowsException() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> {
            dao.doSaveConfezione(null);
        });
        assertEquals("Confezione non può essere nulla", e.getMessage());
    }

    // --- TEST doUpdateConfezione ---

    @Test
    void doUpdateConfezione_Success() throws SQLException {
        Confezione c = new Confezione();
        c.setIdConfezione(99);
        c.setPeso(200);
        int oldId = 88;

        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            dao.doUpdateConfezione(c, oldId);

            // Verifica ordine parametri
            verify(mockPreparedStatement).setInt(1, 99); // SET id
            verify(mockPreparedStatement).setInt(2, 200); // SET peso
            verify(mockPreparedStatement).setInt(3, 88); // WHERE id
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doUpdateConfezione_NullInput_ThrowsException() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> {
            dao.doUpdateConfezione(null, 1);
        });
        assertEquals("La confezione non può essere nulla", e.getMessage());
    }

    // --- TEST doRemoveConfezione ---

    @Test
    void doRemoveConfezione_Success() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

            dao.doRemoveConfezione(5);

            verify(mockPreparedStatement).setInt(1, 5);
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    void doRemoveConfezione_InvalidId_ThrowsException() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            dao.doRemoveConfezione(0);
        });
        assertEquals("ID confezione non valido: deve essere > 0", e.getMessage());
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
    void doRetrieveAll_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRetrieveAll());
        }
    }

    @Test
    void doSaveConfezione_SQLException() throws SQLException {
        Confezione c = new Confezione();
        c.setIdConfezione(1);
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doSaveConfezione(c));
        }
    }

    @Test
    void doUpdateConfezione_SQLException() throws SQLException {
        Confezione c = new Confezione();
        c.setIdConfezione(1);
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doUpdateConfezione(c, 1));
        }
    }

    @Test
    void doRemoveConfezione_SQLException() throws SQLException {
        try (MockedStatic<ConPool> mockedConPool = Mockito.mockStatic(ConPool.class)) {
            mockedConPool.when(ConPool::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

            assertThrows(RuntimeException.class, () -> dao.doRemoveConfezione(1));
        }
    }
}