package controller.Admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.MockedConstruction;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

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
    // --- Test 6: Utente ---

    @Test
    @DisplayName("Cancellazione 'utente' (Happy Path) -> Restituisce JSON")
    void doGet_deleteUtente_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("user@example.com");

        Utente sessionUser = mock(Utente.class);
        when(sessionUser.getEmail()).thenReturn("admin@example.com"); // Different from deleted user
        when(session.getAttribute("Utente")).thenReturn(sessionUser);

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            Utente u = new Utente();
            u.setEmail("user@example.com");
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(u);
            when(mock.doRetrieveAll()).thenReturn(new ArrayList<>());
        })) {
            servlet.doGet(request, response);

            verify(dao.constructed().get(0)).doRemoveUserByEmail("user@example.com");
            verify(response).getWriter();
            assertTrue(getJsonOutput().equals("[]"));
        }
    }

    @Test
    @DisplayName("Cancellazione 'utente' (Sad Path: Non Trovato) -> Invia 400")
    void doGet_deleteUtente_notFound_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("unknown@example.com");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("unknown@example.com")).thenReturn(null);
        })) {
            servlet.doGet(request, response);

            verify(dao.constructed().get(0), never()).doRemoveUserByEmail(anyString());
            verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
        }
    }

    // --- Test 7: Variante ---

    @Test
    @DisplayName("Cancellazione 'variante' (Happy Path) -> Restituisce JSON")
    void doGet_deleteVariante_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("variante");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveVarianteByIdVariante(1)).thenReturn(new Variante());
            when(mock.doRetrieveAll()).thenReturn(new ArrayList<>());
        })) {
            servlet.doGet(request, response);

            verify(dao.constructed().get(0)).doRemoveVariante(1);
            verify(response).getWriter();
            assertTrue(getJsonOutput().equals("[]"));
        }
    }

    @Test
    @DisplayName("Cancellazione 'variante' (Sad Path: Non Trovato) -> Invia 400")
    void doGet_deleteVariante_notFound_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("variante");
        when(request.getParameter("primaryKey")).thenReturn("99");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveVarianteByIdVariante(99)).thenReturn(null);
        })) {
            servlet.doGet(request, response);

            verify(dao.constructed().get(0), never()).doRemoveVariante(anyInt());
            verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
        }
    }

    // --- Test 8: Ordine ---

    @Test
    @DisplayName("Cancellazione 'ordine' (Happy Path) -> Restituisce JSON")
    void doGet_deleteOrdine_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        try (MockedConstruction<OrdineDao> dao = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            when(mock.doRetrieveAll()).thenReturn(new ArrayList<>());
        })) {
            servlet.doGet(request, response);

            verify(dao.constructed().get(0)).doDeleteOrder(1);
            verify(response).getWriter();
            assertTrue(getJsonOutput().equals("[]"));
        }
    }

    // --- Test 9: DettaglioOrdine ---

    @Test
    @DisplayName("Cancellazione 'dettaglioOrdine' (Happy Path) -> Restituisce JSON")
    void doGet_deleteDettaglioOrdine_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, 2, 3"); // idOrdine, idProdotto(ignored), idVariante
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        try (MockedConstruction<DettaglioOrdineDAO> dao = mockConstruction(DettaglioOrdineDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveAll()).thenReturn(new ArrayList<>());
        })) {
            servlet.doGet(request, response);

            verify(dao.constructed().get(0)).doRemoveDettaglioOrdine(1, 3);
            verify(response).getWriter();
            assertTrue(getJsonOutput().equals("[]"));
        }
    }

    @Test
    @DisplayName("Cancellazione 'dettaglioOrdine' (Sad Path: Malformed PK) -> Invia 400")
    void doGet_deleteDettaglioOrdine_malformedPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, nan, 3"); // 2nd part is not int (though ignored by
                                                                          // logic, logic parses 0 and 2)
        // Wait, logic parses index 0 and 2. Index 1 is ignored?
        // Code: int idOrdine = Integer.parseInt(primaryKeys[0]);
        // int idVariante = Integer.parseInt(primaryKeys[2]);
        // So "1, nan, 3" should work if "nan" is ignored.
        // Let's try "nan, 2, 3" -> fails.

        when(request.getParameter("primaryKey")).thenReturn("nan, 2, 3");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'dettaglioOrdine' (Sad Path: Partial Invalid PK) -> Invia 400")
    void doGet_deleteDettaglioOrdine_partialInvalidPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, 2, nan"); // 3rd part is not int
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'dettaglioOrdine' (Sad Path: Null PK) -> Invia 400")
    void doGet_deleteDettaglioOrdine_nullPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn(null);
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    // --- Test 10: Gusto ---

    @Test
    @DisplayName("Cancellazione 'gusto' (Happy Path) -> Restituisce JSON")
    void doGet_deleteGusto_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("gusto");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        try (MockedConstruction<GustoDAO> dao = mockConstruction(GustoDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveAll()).thenReturn(new ArrayList<>());
        })) {
            servlet.doGet(request, response);

            verify(dao.constructed().get(0)).doRemoveGusto(1);
            verify(response).getWriter();
            assertTrue(getJsonOutput().equals("[]"));
        }
    }

    // --- Test 11: Confezione ---

    @Test
    @DisplayName("Cancellazione 'confezione' (Happy Path) -> Restituisce JSON")
    void doGet_deleteConfezione_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("confezione");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        try (MockedConstruction<ConfezioneDAO> dao = mockConstruction(ConfezioneDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveAll()).thenReturn(new ArrayList<>());
        })) {
            servlet.doGet(request, response);

            verify(dao.constructed().get(0)).doRemoveConfezione(1);
            verify(response).getWriter();
            assertTrue(getJsonOutput().equals("[]"));
        }
    }

    // --- Test 12: Null/Blank PK Edge Cases ---

    @Test
    @DisplayName("Cancellazione 'utente' (Sad Path: Null PK) -> Invia 400")
    void doGet_deleteUtente_nullPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn(null);
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'prodotto' (Sad Path: Null PK) -> Invia 400")
    void doGet_deleteProdotto_nullPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("prodotto");
        when(request.getParameter("primaryKey")).thenReturn(null);
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    // --- Test 13: JSON Helper Edge Cases ---

    @Test
    @DisplayName("Utente con dataNascita null -> JSON corretto")
    void doGet_utenteNullDataNascita_returnsCorrectJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("user@example.com");

        Utente sessionUser = mock(Utente.class);
        when(sessionUser.getEmail()).thenReturn("admin@example.com");
        when(session.getAttribute("Utente")).thenReturn(sessionUser);

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            Utente u = new Utente();
            u.setEmail("user@example.com");
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(u);

            // Utente con dataNascita null per il JSON
            Utente u2 = new Utente();
            u2.setEmail("user2@example.com");
            u2.setDataNascita(null); // Explicitly null
            List<Utente> list = new ArrayList<>();
            list.add(u2);
            when(mock.doRetrieveAll()).thenReturn(list);
        })) {
            servlet.doGet(request, response);

            verify(response).getWriter();
            String json = getJsonOutput();
            assertTrue(json.contains("\"dataDiNascita\":\"\"")); // Verifica che sia gestito come stringa vuota
        }
    }

    // --- Test 14: Additional Edge Cases (Coverage Improvement) ---

    @Test
    @DisplayName("Cancellazione 'confezione' con PK=0 -> Invia 400")
    void doGet_deleteConfezione_zeroPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("confezione");
        when(request.getParameter("primaryKey")).thenReturn("0");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'dettaglioOrdine' con PK vuota -> Invia 400")
    void doGet_deleteDettaglioOrdine_blankPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'utente' con PK vuota -> Invia 400")
    void doGet_deleteUtente_blankPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'prodotto' con PK vuota -> Invia 400")
    void doGet_deleteProdotto_blankPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("prodotto");
        when(request.getParameter("primaryKey")).thenReturn("");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    // --- Test 15: Invalid Table Name & Specific Invalid PKs (New Coverage) ---

    @Test
    @DisplayName("Tabella non valida -> Invia 400")
    void doGet_invalidTableName_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("invalidTable");
        when(request.getParameter("primaryKey")).thenReturn("1");

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'ordine' con PK non valida -> Invia 400")
    void doGet_deleteOrdine_invalidPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("0"); // Invalid ID
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'gusto' con PK non valida -> Invia 400")
    void doGet_deleteGusto_invalidPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("gusto");
        when(request.getParameter("primaryKey")).thenReturn("0"); // Invalid ID
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'variante' con PK non valida -> Invia 400")
    void doGet_deleteVariante_invalidPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("variante");
        when(request.getParameter("primaryKey")).thenReturn("0"); // Invalid ID
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    // --- Test 16: Blank Table Name & Null/Blank PKs (Final Coverage) ---

    @Test
    @DisplayName("tableName vuoto (blank) -> Invia 400")
    void doGet_blankTableName_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("   "); // Blank
        when(request.getParameter("primaryKey")).thenReturn("1");

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'ordine' con PK null -> Invia 400")
    void doGet_deleteOrdine_nullPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn(null);
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'ordine' con PK blank -> Invia 400")
    void doGet_deleteOrdine_blankPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("   ");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'gusto' con PK null -> Invia 400")
    void doGet_deleteGusto_nullPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("gusto");
        when(request.getParameter("primaryKey")).thenReturn(null);
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'gusto' con PK blank -> Invia 400")
    void doGet_deleteGusto_blankPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("gusto");
        when(request.getParameter("primaryKey")).thenReturn("   ");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'variante' con PK null -> Invia 400")
    void doGet_deleteVariante_nullPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("variante");
        when(request.getParameter("primaryKey")).thenReturn(null);
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'variante' con PK blank -> Invia 400")
    void doGet_deleteVariante_blankPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("variante");
        when(request.getParameter("primaryKey")).thenReturn("   ");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'confezione' con PK null -> Invia 400")
    void doGet_deleteConfezione_nullPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("confezione");
        when(request.getParameter("primaryKey")).thenReturn(null);
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("Cancellazione 'confezione' con PK blank -> Invia 400")
    void doGet_deleteConfezione_blankPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("confezione");
        when(request.getParameter("primaryKey")).thenReturn("   ");
        when(session.getAttribute("Utente")).thenReturn(mock(Utente.class));

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }
}
