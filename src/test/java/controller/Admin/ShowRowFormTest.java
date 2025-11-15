package controller.Admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.ConfezioneDAO;
import model.DettaglioOrdineDAO;
import model.ProdottoDAO;
import model.Utente;
import model.UtenteDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Classe di test (aggiornata) per showRowForm.
 * Verifica che la servlet gestisca input non validi in modo sicuro
 * e che la logica di recupero dati funzioni.
 */
public class ShowRowFormTest {
    private showRowForm servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;

    // Per catturare l'output JSON
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setup() throws IOException {
        servlet = new showRowForm();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        // Prepariamo un writer in memoria per catturare l'output JSON
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);

        // Stub di base
        when(response.getWriter()).thenReturn(printWriter);
    }

    /**
     * Helper per ottenere l'output JSON catturato.
     */
    private String getJsonOutput() {
        printWriter.flush();
        return stringWriter.toString().trim();
    }

    // --- Test 1: Generali ---

    @Test
    @DisplayName("doPost deve delegare a doGet")
    void doPost_delegatesToDoGet() throws ServletException, IOException {
        showRowForm spyServlet = spy(new showRowForm());
        // Disattiviamo il doGet reale per testare solo la delega
        doNothing().when(spyServlet).doGet(any(HttpServletRequest.class), any(HttpServletResponse.class));

        spyServlet.doPost(request, response);

        verify(spyServlet).doGet(request, response);
    }

    // --- Test 2: Sad Path (Input non validi) ---

    @Test
    @DisplayName("doGet con 'tableName' nullo -> Restituisce []")
    void doGet_nullTableName_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn(null);
        when(request.getParameter("primaryKey")).thenReturn("123");

        servlet.doGet(request, response);

        // Il 'if' principale fallisce, restituisce JSON vuoto
        assertEquals("[]", getJsonOutput());
    }

    @Test
    @DisplayName("doGet con 'primaryKey' nullo -> Restituisce []")
    void doGet_nullPrimaryKey_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn(null);

        servlet.doGet(request, response);

        // Il 'if' principale fallisce, restituisce JSON vuoto
        assertEquals("[]", getJsonOutput());
    }

    // --- Test 3: Verifica Correzione Faglie ---

    @Test
    @DisplayName("'primaryKey' non numerica -> Gestito e Restituisce []")
    void doGet_nonNumericKey_isHandled_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("confezione");
        when(request.getParameter("primaryKey")).thenReturn("abc"); // Non numerico

        // Mock del DAO (non dovrebbe essere chiamato)
        try (MockedConstruction<ConfezioneDAO> dao = mockConstruction(ConfezioneDAO.class)) {

            // Il 'try-catch' (corretto) cattura NFE e non fa nulla
            assertDoesNotThrow(() -> {
                servlet.doGet(request, response);
            });

            // Restituisce JSON vuoto
            assertEquals("[]", getJsonOutput());
            // Nessun DAO è stato usato
            verify(dao.constructed().get(0), never()).doRetrieveById(anyInt());
        }
    }

    @Test
    @DisplayName("PK composita non numerica -> Gestito e Restituisce []")
    void doGet_nonNumericCompositeKey_isHandled_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, 2, abc"); // Terzo non numerico

        try (MockedConstruction<DettaglioOrdineDAO> dao = mockConstruction(DettaglioOrdineDAO.class)) {

            // Il 'try-catch' (corretto) cattura NFE
            assertDoesNotThrow(() -> {
                servlet.doGet(request, response);
            });

            assertEquals("[]", getJsonOutput());
        }
    }

    @Test
    @DisplayName("PK composita malformata -> Gestito e Restituisce []")
    void doGet_shortCompositeKey_isHandled_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, 2"); // Troppo corto

        assertDoesNotThrow(() -> {
            servlet.doGet(request, response);
        });

        // Il 'if (keys.length == 3)' (corretto) fallisce
        assertEquals("[]", getJsonOutput());
    }

    @Test
    @DisplayName("Record non trovato (DAO restituisce null) -> Restituisce []")
    void doGet_recordNotFound_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("prodotto");
        when(request.getParameter("primaryKey")).thenReturn("P99");

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            // Simula il DAO che non trova il prodotto
            when(mock.doRetrieveById("P99")).thenReturn(null);
        })) {

            servlet.doGet(request, response);

            // L'if (prodotto != null) fallisce
            assertEquals("[]", getJsonOutput());
        }
    }

    // --- Test 4: Happy Path (Recupero Utente) ---

    @Test
    @DisplayName("doGet 'utente' (Happy Path) -> Restituisce JSON Utente")
    void doGet_utente_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("user@example.com");

        // Prepariamo l'utente mockato
        Utente utente = new Utente();
        utente.setEmail("user@example.com");
        utente.setNome("Mario");
        utente.setCognome("Rossi");
        // (Nota: non impostiamo la password, così 'getPassword' restituisce null
        // e non dobbiamo preoccuparci di mockare 'jsonUtenteHelper' per quel campo)

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(utente);
        })) {

            servlet.doGet(request, response);

            String json = getJsonOutput();

            // Verifichiamo che i dati siano nel JSON.
            // Non possiamo usare assertEquals(jsonArray.toJSONString()) perché
            // non abbiamo accesso agli helper statici.
            assertTrue(json.contains("\"email\":\"user@example.com\""));
            assertTrue(json.contains("\"nome\":\"Mario\""));
            assertTrue(json.contains("\"cognome\":\"Rossi\""));
        }
    }
}
