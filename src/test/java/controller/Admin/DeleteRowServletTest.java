package controller.Admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Classe di test (aggiornata) per deleteRowServlet.
 * Verifica che la servlet gestisca input non validi in modo sicuro
 * e che la logica di cancellazione funzioni.
 */
public class DeleteRowServletTest {
    private deleteRowServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    // Per catturare l'output JSON
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setup() throws IOException {
        servlet = new deleteRowServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);

        // Prepariamo un writer in memoria per catturare l'output JSON
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);

        // Stub di base
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session); // Per la chiamata invalidate()
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
        deleteRowServlet spyServlet = spy(new deleteRowServlet());
        // Disattiviamo il doGet reale per testare solo la delega
        doNothing().when(spyServlet).doGet(any(HttpServletRequest.class), any(HttpServletResponse.class));

        spyServlet.doPost(request, response);

        verify(spyServlet).doGet(request, response);
    }

    // --- Test 2: Verifica Correzione Faglie (Input non validi) ---

    @Test
    @DisplayName("tableName nullo -> Gestito e invia 400")
    void doGet_nullTableName_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn(null);

        assertDoesNotThrow(() -> {
            servlet.doGet(request, response);
        });

        // Verifica che la correzione (controllo di guardia) invii un errore
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Parametro tableName mancante.");
        verify(response, never()).getWriter(); // Nessun JSON
    }

    @Test
    @DisplayName("primaryKey non numerica -> Gestito e invia 400")
    void doGet_nonNumericKey_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("confezione");
        when(request.getParameter("primaryKey")).thenReturn("abc"); // Non numerico

        assertDoesNotThrow(() -> {
            servlet.doGet(request, response);
        });

        // Il metodo isValidPrimaryKey (corretto) restituisce false,
        // 'success' è false, quindi invia un errore 400
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
        verify(response, never()).getWriter();
    }

    @Test
    @DisplayName("PK composita malformata -> Gestito e invia 400")
    void doGet_malformedCompositeKey_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, 2"); // Manca il 3° pezzo

        assertDoesNotThrow(() -> {
            servlet.doGet(request, response);
        });

        // handleRemoveRowFromDettaglioOrdine (corretto) restituisce false,
        // 'success' è false, quindi invia un errore 400
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Utente in sessione nullo -> Gestito")
    void doGet_nullSessionUser_isHandled() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("admin@example.com");
        when(session.getAttribute("Utente")).thenReturn(null); // Utente non loggato

        // Mock del DAO (necessario per il ramo 'utente')
        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail(anyString())).thenReturn(new Utente());
            doNothing().when(mock).doRemoveUserByEmail(anyString());
        })) {

            assertDoesNotThrow(() -> {
                servlet.doGet(request, response);
            });

            // Verifica che checkIfAdminDeletingSelf (corretto) abbia restituito false
            // e che la servlet abbia proseguito normalmente con l'output JSON
            verify(response, never()).sendRedirect(anyString());
            assertTrue(getJsonOutput().startsWith("[")); // Ha scritto il JSON
        }
    }

    // --- Test 3: Happy Path (Cancellazione Standard) ---

    @Test
    @DisplayName("Cancellazione 'prodotto' (Happy Path) -> Restituisce JSON")
    void doGet_deleteProdotto_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("prodotto");
        when(request.getParameter("primaryKey")).thenReturn("P1");
        // Utente admin in sessione (necessario per non crashare)
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            // Simula che il prodotto esista
            when(mock.doRetrieveById("P1")).thenReturn(new Prodotto());
            // Simula che la cancellazione avvenga
            doNothing().when(mock).removeProductFromIdProdotto("P1");
            // Simula la risposta per il JSON
            when(mock.doRetrieveAll()).thenReturn(new ArrayList<>());
        })) {

            servlet.doGet(request, response);

            // Verifica che la cancellazione sia stata chiamata
            verify(dao.constructed().get(0)).removeProductFromIdProdotto("P1");
            // Verifica che il JSON sia stato inviato
            verify(response).getWriter();
            assertTrue(getJsonOutput().equals("[]")); // JSON di una lista vuota
        }
    }

    // --- Test 4: Percorso Speciale (Auto-Cancellazione) ---

    @Test
    @DisplayName("Admin cancella il proprio account -> Invalida sessione e Redirect")
    void doGet_adminDeletesSelf_redirectsToIndex() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("admin@example.com");

        // Simula l'admin in sessione
        Utente admin = mock(Utente.class);
        when(admin.getEmail()).thenReturn("admin@example.com");
        when(session.getAttribute("Utente")).thenReturn(admin);

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            // Simula che l'utente esista e venga cancellato
            when(mock.doRetrieveByEmail("admin@example.com")).thenReturn(admin);
            // Non serve mockare doRemove...
        })) {

            servlet.doGet(request, response);

            // Verifica che la sessione sia stata invalidata
            verify(session).invalidate();
            // Verifica il redirect
            verify(response).sendRedirect("index.jsp");
            // Verifica che NESSUN JSON sia stato inviato
            verify(response, never()).getWriter();
        }
    }

    // --- Test 5: Sad Path (Logica di cancellazione) ---

    @Test
    @DisplayName("Cancella prodotto inesistente -> Gestito e invia 400")
    void doGet_deleteNonExistentProduct_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("prodotto");
        when(request.getParameter("primaryKey")).thenReturn("P99"); // Prodotto inesistente
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            // Simula che il prodotto NON esista
            when(mock.doRetrieveById("P99")).thenReturn(null);
        })) {

            servlet.doGet(request, response);

            // Verifica che la cancellazione NON sia stata chiamata
            verify(dao.constructed().get(0), never()).removeProductFromIdProdotto(anyString());
            // Il metodo (corretto) restituisce false, 'success' è false
            verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
        }
    }
}
