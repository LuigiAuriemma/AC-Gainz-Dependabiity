package controller.Admin;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Confezione;
import model.ConfezioneDAO;
import model.DettaglioOrdine;
import model.DettaglioOrdineDAO;
import model.Gusto;
import model.GustoDAO;
import model.Ordine;
import model.OrdineDao;
import model.Prodotto;
import model.ProdottoDAO;
import model.Utente;
import model.UtenteDAO;
import model.Variante;
import model.VarianteDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    void setup() throws Exception {
        servlet = new showRowForm();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        // Mock ServletConfig e ServletContext per permettere il logging
        ServletConfig servletConfig = mock(ServletConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        
        // Inizializza il servlet con il config mockato
        servlet.init(servletConfig);

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

    @Test
    @DisplayName("doGet 'prodotto' (Happy Path) -> Restituisce JSON Prodotto")
    void doGet_prodotto_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("prodotto");
        when(request.getParameter("primaryKey")).thenReturn("P1");

        Prodotto prodotto = new Prodotto();
        prodotto.setIdProdotto("P1");
        prodotto.setNome("Protein Bar");
        prodotto.setDescrizione("Delicious");
        prodotto.setCategoria("Snack");
        prodotto.setImmagine("img.jpg");
        prodotto.setCalorie(200);
        prodotto.setCarboidrati(20);
        prodotto.setProteine(20);
        prodotto.setGrassi(5);

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveById("P1")).thenReturn(prodotto);
        })) {
            servlet.doGet(request, response);
            String json = getJsonOutput();
            assertTrue(json.contains("\"idProdotto\":\"P1\""));
            assertTrue(json.contains("\"nome\":\"Protein Bar\""));
        }
    }

    @Test
    @DisplayName("doGet 'variante' (Happy Path) -> Restituisce JSON Variante")
    void doGet_variante_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("variante");
        when(request.getParameter("primaryKey")).thenReturn("1");

        Variante variante = new Variante();
        variante.setIdVariante(1);
        variante.setIdProdotto("P1");
        variante.setIdGusto(1);
        variante.setIdConfezione(1);
        variante.setPrezzo(10.0f);
        variante.setQuantita(100);
        variante.setSconto(0);
        variante.setEvidenza(true);

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveVarianteByIdVariante(1)).thenReturn(variante);
        })) {
            servlet.doGet(request, response);
            String json = getJsonOutput();
            assertTrue(json.contains("\"idVariante\":1"));
            assertTrue(json.contains("\"prezzo\":10.0"));
        }
    }

    @Test
    @DisplayName("doGet 'ordine' (Happy Path) -> Restituisce JSON Ordine")
    void doGet_ordine_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("1");

        Ordine ordine = new Ordine();
        ordine.setIdOrdine(1);
        ordine.setEmailUtente("user@example.com");
        ordine.setStato("Spedito");
        ordine.setTotale(50.0f);

        try (MockedConstruction<OrdineDao> dao = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            when(mock.doRetrieveById(1)).thenReturn(ordine);
        })) {
            servlet.doGet(request, response);
            String json = getJsonOutput();
            assertTrue(json.contains("\"idOrdine\":1"));
            assertTrue(json.contains("\"emailUtente\":\"user@example.com\""));
        }
    }

    @Test
    @DisplayName("doGet 'dettaglioOrdine' (Happy Path) -> Restituisce JSON DettaglioOrdine")
    void doGet_dettaglioOrdine_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, P1, 1");

        DettaglioOrdine dettaglio = new DettaglioOrdine();
        dettaglio.setIdOrdine(1);
        dettaglio.setIdProdotto("P1");
        dettaglio.setIdVariante(1);
        dettaglio.setQuantita(5);
        dettaglio.setPrezzo(10.0f);

        try (MockedConstruction<DettaglioOrdineDAO> dao = mockConstruction(DettaglioOrdineDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByIdOrderAndIdVariant(1, 1)).thenReturn(dettaglio);
        })) {
            servlet.doGet(request, response);
            String json = getJsonOutput();
            assertTrue(json.contains("\"idOrdine\":1"));
            assertTrue(json.contains("\"quantity\":5"));
        }
    }

    @Test
    @DisplayName("doGet 'gusto' (Happy Path) -> Restituisce JSON Gusto")
    void doGet_gusto_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("gusto");
        when(request.getParameter("primaryKey")).thenReturn("1");

        Gusto gusto = new Gusto();
        gusto.setIdGusto(1);
        gusto.setNome("Cioccolato");

        try (MockedConstruction<GustoDAO> dao = mockConstruction(GustoDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveById(1)).thenReturn(gusto);
        })) {
            servlet.doGet(request, response);
            String json = getJsonOutput();
            assertTrue(json.contains("\"idGusto\":1"));
            assertTrue(json.contains("\"nomeGusto\":\"Cioccolato\""));
        }
    }

    @Test
    @DisplayName("doGet 'confezione' (Happy Path) -> Restituisce JSON Confezione")
    void doGet_confezione_happyPath_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("confezione");
        when(request.getParameter("primaryKey")).thenReturn("1");

        Confezione confezione = new Confezione();
        confezione.setIdConfezione(1);
        confezione.setPeso(500);

        try (MockedConstruction<ConfezioneDAO> dao = mockConstruction(ConfezioneDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveById(1)).thenReturn(confezione);
        })) {
            servlet.doGet(request, response);
            String json = getJsonOutput();
            assertTrue(json.contains("\"idConfezione\":1"));
            assertTrue(json.contains("\"pesoConfezione\":500"));
        }
    }

    // --- Test 5: Additional Edge Cases (Coverage Improvement) ---

    @Test
    @DisplayName("doGet con 'tableName' non valido -> Invia errore BAD_REQUEST")
    void doGet_invalidTableName_sendsBadRequest() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("invalidTable");
        when(request.getParameter("primaryKey")).thenReturn("1");

        servlet.doGet(request, response);

        // Verifica che sia stato inviato un errore BAD_REQUEST
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    @DisplayName("doGet 'ordine' con PK non numerica -> Restituisce []")
    void doGet_ordine_nonNumericKey_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("abc");

        servlet.doGet(request, response);

        assertEquals("[]", getJsonOutput());
    }

    @Test
    @DisplayName("doGet 'variante' con PK non numerica -> Restituisce []")
    void doGet_variante_nonNumericKey_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("variante");
        when(request.getParameter("primaryKey")).thenReturn("abc");

        servlet.doGet(request, response);

        assertEquals("[]", getJsonOutput());
    }

    @Test
    @DisplayName("doGet 'gusto' con PK non numerica -> Restituisce []")
    void doGet_gusto_nonNumericKey_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("gusto");
        when(request.getParameter("primaryKey")).thenReturn("abc");

        servlet.doGet(request, response);

        assertEquals("[]", getJsonOutput());
    }

    // --- Test 6: Blank Parameters ---

    @Test
    @DisplayName("doGet con 'tableName' vuoto -> Restituisce []")
    void doGet_blankTableName_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("");
        when(request.getParameter("primaryKey")).thenReturn("1");

        servlet.doGet(request, response);

        assertEquals("[]", getJsonOutput());
    }

    @Test
    @DisplayName("doGet con 'primaryKey' vuoto -> Restituisce []")
    void doGet_blankPrimaryKey_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("");

        servlet.doGet(request, response);

        assertEquals("[]", getJsonOutput());
    }

    // --- Test 7: Record Not Found (DAO returns null) ---

    @Test
    @DisplayName("doGet 'utente' non trovato -> Restituisce []")
    void doGet_utente_notFound_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("unknown@example.com");

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("unknown@example.com")).thenReturn(null);
        })) {
            servlet.doGet(request, response);
            assertEquals("[]", getJsonOutput());
        }
    }

    @Test
    @DisplayName("doGet 'variante' non trovata -> Restituisce []")
    void doGet_variante_notFound_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("variante");
        when(request.getParameter("primaryKey")).thenReturn("99");

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveVarianteByIdVariante(99)).thenReturn(null);
        })) {
            servlet.doGet(request, response);
            assertEquals("[]", getJsonOutput());
        }
    }

    @Test
    @DisplayName("doGet 'ordine' non trovato -> Restituisce []")
    void doGet_ordine_notFound_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("99");

        try (MockedConstruction<OrdineDao> dao = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            when(mock.doRetrieveById(99)).thenReturn(null);
        })) {
            servlet.doGet(request, response);
            assertEquals("[]", getJsonOutput());
        }
    }

    @Test
    @DisplayName("doGet 'dettaglioOrdine' non trovato -> Restituisce []")
    void doGet_dettaglioOrdine_notFound_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, P1, 1");

        try (MockedConstruction<DettaglioOrdineDAO> dao = mockConstruction(DettaglioOrdineDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByIdOrderAndIdVariant(1, 1)).thenReturn(null);
        })) {
            servlet.doGet(request, response);
            assertEquals("[]", getJsonOutput());
        }
    }

    @Test
    @DisplayName("doGet 'gusto' non trovato -> Restituisce []")
    void doGet_gusto_notFound_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("gusto");
        when(request.getParameter("primaryKey")).thenReturn("99");

        try (MockedConstruction<GustoDAO> dao = mockConstruction(GustoDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveById(99)).thenReturn(null);
        })) {
            servlet.doGet(request, response);
            assertEquals("[]", getJsonOutput());
        }
    }

    @Test
    @DisplayName("doGet 'confezione' non trovata -> Restituisce []")
    void doGet_confezione_notFound_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("confezione");
        when(request.getParameter("primaryKey")).thenReturn("99");

        try (MockedConstruction<ConfezioneDAO> dao = mockConstruction(ConfezioneDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveById(99)).thenReturn(null);
        })) {
            servlet.doGet(request, response);
            assertEquals("[]", getJsonOutput());
        }
    }

    // --- Test 8: Conditional Branches in Helpers ---

    @Test
    @DisplayName("doGet 'ordine' con data -> JSON con data")
    void doGet_ordine_withDate_returnsJsonWithDate() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("1");

        Ordine ordine = new Ordine();
        ordine.setIdOrdine(1);
        ordine.setDataOrdine(new java.sql.Date(System.currentTimeMillis()));

        try (MockedConstruction<OrdineDao> dao = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            when(mock.doRetrieveById(1)).thenReturn(ordine);
        })) {
            servlet.doGet(request, response);
            String json = getJsonOutput();
            assertTrue(json.contains("\"data\":"));
            assertFalse(json.contains("\"data\":\"\""));
        }
    }

    @Test
    @DisplayName("doGet 'utente' con data nascita -> JSON con data")
    void doGet_utente_withDate_returnsJsonWithDate() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("user@example.com");

        Utente utente = new Utente();
        utente.setEmail("user@example.com");
        utente.setDataNascita(new java.sql.Date(System.currentTimeMillis()));

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(utente);
        })) {
            servlet.doGet(request, response);
            String json = getJsonOutput();
            assertTrue(json.contains("\"dataDiNascita\":"));
            assertFalse(json.contains("\"dataDiNascita\":\"\""));
        }
    }

    @Test
    @DisplayName("doGet 'variante' non in evidenza -> JSON con evidenza 0")
    void doGet_variante_notEvidenza_returnsJson() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("variante");
        when(request.getParameter("primaryKey")).thenReturn("1");

        Variante variante = new Variante();
        variante.setIdVariante(1);
        variante.setEvidenza(false);

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveVarianteByIdVariante(1)).thenReturn(variante);
        })) {
            servlet.doGet(request, response);
            String json = getJsonOutput();
            assertTrue(json.contains("\"evidenza\":0"));
        }
    }
}
