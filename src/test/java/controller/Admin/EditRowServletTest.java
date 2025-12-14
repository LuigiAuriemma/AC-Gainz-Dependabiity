package controller.Admin;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Prodotto;
import model.ProdottoDAO;
import model.Utente;
import model.UtenteDAO;
import model.Variante;
import model.VarianteDAO;
import model.Ordine;
import model.OrdineDao;
import model.DettaglioOrdine;
import model.DettaglioOrdineDAO;
import model.Gusto;
import model.GustoDAO;
import model.Confezione;
import model.ConfezioneDAO;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Classe di test (aggiornata) per editRowServlet.
 * Verifica che la servlet gestisca input non validi in modo sicuro
 * e che la logica di modifica funzioni.
 */
public class EditRowServletTest {
    private editRowServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private RequestDispatcher dispatcher;
    private ServletConfig servletConfig;
    private ServletContext servletContext;

    private final java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
    private final java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
    private final java.io.PrintStream originalErr = System.err;
    private final java.io.PrintStream originalOut = System.out;

    @BeforeEach
    void setup() throws ServletException {
        System.setErr(new java.io.PrintStream(errContent));
        System.setOut(new java.io.PrintStream(outContent));
        servlet = new editRowServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        dispatcher = mock(RequestDispatcher.class);
        servletConfig = mock(ServletConfig.class);
        servletContext = mock(ServletContext.class);

        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletConfig.getServletName()).thenReturn("editRowServlet");
        servlet.init(servletConfig);
    }

    @org.junit.jupiter.api.AfterEach
    void restoreStreams() {
        System.setErr(originalErr);
        System.setOut(originalOut);
    }

    /**
     * Helper per impostare tutti i parametri validi per un test 'utente'.
     */
    private void setupValidUtenteParams() {
        when(request.getParameter("email")).thenReturn("user@example.com");
        when(request.getParameter("nome")).thenReturn("Mario");
        when(request.getParameter("cognome")).thenReturn("Rossi");
        when(request.getParameter("codiceFiscale")).thenReturn("RSSMRA80A01H501U");
        when(request.getParameter("dataDiNascita")).thenReturn("1980-01-01");
        when(request.getParameter("indirizzo")).thenReturn("Via Roma 1");
        when(request.getParameter("telefono")).thenReturn("3331234567");
    }

    /**
     * Helper per impostare tutti i parametri validi per un test 'prodotto'.
     */
    private void setupValidProdottoParams() {
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("nome")).thenReturn("Proteine");
        when(request.getParameter("descrizione")).thenReturn("Descrizione test");
        when(request.getParameter("categoria")).thenReturn("Integratori");
        when(request.getParameter("immagine")).thenReturn("img.png");
        when(request.getParameter("calorie")).thenReturn("100");
        when(request.getParameter("carboidrati")).thenReturn("10");
        when(request.getParameter("proteine")).thenReturn("80");
        when(request.getParameter("grassi")).thenReturn("5");
    }

    // --- Test 1: Generali ---

    @Test
    @DisplayName("doGet deve fallire con Method Not Allowed (405)")
    void doGet_shouldFailWith405() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getProtocol()).thenReturn("HTTP/1.1");

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), anyString());
    }

    @Test
    @DisplayName("Eccezione in doGet -> Logga errore e invia 500 se non commesso")
    void doGet_exception_logsAndSendsError() throws ServletException, IOException {
        editRowServlet spyServlet = spy(new editRowServlet());
        spyServlet.init(servletConfig);

        try {
            spyServlet.doGet(null, response);
        } catch (Exception e) {
            // ignore
        }
        verify(servletContext, atLeastOnce()).log(anyString(), any(Throwable.class));
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @Test
    @DisplayName("Eccezione in doPost -> Logga errore e invia 500")
    void doPost_exception_logsAndSendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenThrow(new RuntimeException("Test Error"));

        servlet.doPost(request, response);

        verify(servletContext).log(eq("editRowServlet: Errore in editRowServlet doPost"), any(RuntimeException.class));
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    // --- Test 2: Verifica Correzione Faglie (Input non validi) ---

    @Test
    @DisplayName("tableName nullo -> Gestito e invia 400")
    void doPost_nullTableName_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn(null);
        when(request.getParameter("primaryKey")).thenReturn("123");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Parametri 'tableName' o 'primaryKey' mancanti.");
        verify(dispatcher, never()).forward(any(), any());
    }

    @Test
    @DisplayName("primaryKey nulla -> Gestito e invia 400")
    void doPost_nullPrimaryKey_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn(null);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Parametri 'tableName' o 'primaryKey' mancanti.");
        verify(dispatcher, never()).forward(any(), any());
    }

    @Test
    @DisplayName("Input non numerico (Prodotto) -> Gestito e invia 400")
    void doPost_editProdotto_nonNumeric_sendsError() throws ServletException, IOException {
        setupValidProdottoParams();
        when(request.getParameter("tableName")).thenReturn("prodotto");
        when(request.getParameter("primaryKey")).thenReturn("P1");
        when(request.getParameter("calorie")).thenReturn("abc");

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class)) {
            servlet.doPost(request, response);
            verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
            verify(dispatcher, never()).forward(any(), any());
            assertEquals(0, dao.constructed().size());
            assertFalse(errContent.toString().isEmpty(), "Should print stack trace to stderr");
        }
    }

    @Test
    @DisplayName("Input data non valido (Utente) -> Gestito e invia 400")
    void doPost_editUtente_invalidDate_sendsError() throws ServletException, IOException {
        setupValidUtenteParams();
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("old@email.com");
        when(request.getParameter("dataDiNascita")).thenReturn("data-sbagliata");

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);
            verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
            verify(dispatcher, never()).forward(any(), any());
            assertEquals(0, dao.constructed().size());
            assertFalse(errContent.toString().isEmpty(), "Should print stack trace to stderr");
        }
    }

    // --- Test 3: Happy Path (Modifica Utente) ---

    @Test
    @DisplayName("Modifica 'utente' (Happy Path) -> Chiama DAO e fa forward")
    void doPost_editUtente_happyPath_forwards() throws ServletException, IOException, ParseException {
        setupValidUtenteParams(); // Imposta tutti i parametri validi
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("old@email.com");

        when(request.getRequestDispatcher("showTable?tableName=utente")).thenReturn(dispatcher);

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            doNothing().when(mock).doUpdateCustomer(any(Utente.class), anyString());
        })) {

            servlet.doPost(request, response);

            UtenteDAO mockDao = dao.constructed().get(0);
            ArgumentCaptor<Utente> utenteCaptor = ArgumentCaptor.forClass(Utente.class);
            verify(mockDao).doUpdateCustomer(utenteCaptor.capture(), eq("old@email.com"));

            Utente savedUser = utenteCaptor.getValue();
            // KILL MUTANTS: Verify ALL setters
            assertEquals("Mario", savedUser.getNome());
            assertEquals("Rossi", savedUser.getCognome());
            assertEquals("RSSMRA80A01H501U", savedUser.getCodiceFiscale());
            assertEquals("user@example.com", savedUser.getEmail());
            assertEquals("Via Roma 1", savedUser.getIndirizzo());
            assertEquals("3331234567", savedUser.getTelefono());

            Date expectedDate = new SimpleDateFormat("yyyy-MM-dd").parse("1980-01-01");
            assertEquals(expectedDate, savedUser.getDataNascita());

            verify(dispatcher).forward(request, response);
            verify(response, never()).sendError(anyInt(), anyString());

            // Validate stdout output
            assertTrue(outContent.toString().contains("tableName: utente"), "Stdout must contain 'tableName: utente'");
        }
    }

    // --- Test 4: Sad Path (Logica di business) ---

    @Test
    @DisplayName("Modifica 'utente' (Sad Path) -> 'isValid' false -> Invia 400")
    void doPost_editUtente_invalidParam_sendsError() throws ServletException, IOException {
        setupValidUtenteParams();
        when(request.getParameter("nome")).thenReturn(""); // isBlank
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("old@email.com");

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);
            verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
            verify(dispatcher, never()).forward(any(), any());
            assertEquals(0, dao.constructed().size());
        }
    }

    // --- Test 5: Prodotto ---

    @Test
    @DisplayName("Modifica 'prodotto' (Happy Path) -> Chiama DAO e fa forward")
    void doPost_editProdotto_happyPath_forwards() throws ServletException, IOException {
        setupValidProdottoParams();
        when(request.getParameter("tableName")).thenReturn("prodotto");
        when(request.getParameter("primaryKey")).thenReturn("P1");

        when(request.getRequestDispatcher("showTable?tableName=prodotto")).thenReturn(dispatcher);

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            doNothing().when(mock).updateProduct(any(Prodotto.class), anyString());
        })) {
            servlet.doPost(request, response);

            ProdottoDAO mockDao = dao.constructed().get(0);
            ArgumentCaptor<Prodotto> captor = ArgumentCaptor.forClass(Prodotto.class);
            verify(mockDao).updateProduct(captor.capture(), eq("P1"));

            Prodotto p = captor.getValue();
            assertEquals("P1", p.getIdProdotto());
            assertEquals("Proteine", p.getNome());
            assertEquals("Descrizione test", p.getDescrizione());
            assertEquals("Integratori", p.getCategoria());
            assertEquals("img.png", p.getImmagine());
            assertEquals(100, p.getCalorie());
            assertEquals(10, p.getCarboidrati());
            assertEquals(80, p.getProteine());
            assertEquals(5, p.getGrassi());

            verify(dispatcher).forward(request, response);
        }
    }

    // --- Test 6: Variante ---

    @Test
    @DisplayName("Modifica 'variante' (Happy Path) -> Chiama DAO e fa forward")
    void doPost_editVariante_happyPath_forwards() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("variante");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(request.getParameter("idVariante")).thenReturn("1");
        when(request.getParameter("idProdottoVariante")).thenReturn("P1");
        when(request.getParameter("idGusto")).thenReturn("1");
        when(request.getParameter("idConfezione")).thenReturn("1");
        when(request.getParameter("prezzo")).thenReturn("10.5");
        when(request.getParameter("quantity")).thenReturn("100");
        when(request.getParameter("sconto")).thenReturn("10"); // Non-default to kill mutant
        when(request.getParameter("evidenza")).thenReturn("1");

        when(request.getRequestDispatcher("showTable?tableName=variante")).thenReturn(dispatcher);

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            doNothing().when(mock).updateVariante(any(Variante.class), anyInt());
        })) {
            servlet.doPost(request, response);

            ArgumentCaptor<Variante> captor = ArgumentCaptor.forClass(Variante.class);
            verify(dao.constructed().get(0)).updateVariante(captor.capture(), eq(1));

            Variante v = captor.getValue();
            assertEquals(1, v.getIdVariante());
            assertEquals("P1", v.getIdProdotto());
            assertEquals(1, v.getIdGusto());
            assertEquals(1, v.getIdConfezione());
            assertEquals(10.5f, v.getPrezzo());
            assertEquals(100, v.getQuantita());
            assertEquals(10, v.getSconto()); // Verified non-default
            assertEquals(true, v.isEvidenza());

            verify(dispatcher).forward(request, response);
        }
    }

    @Test
    @DisplayName("Modifica 'variante' (Sad Path: Invalid Number) -> Invia 400")
    void doPost_editVariante_invalidInput_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("variante");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(request.getParameter("idVariante")).thenReturn("1");
        when(request.getParameter("idProdottoVariante")).thenReturn("P1");
        when(request.getParameter("idGusto")).thenReturn("1");
        when(request.getParameter("idConfezione")).thenReturn("1");
        when(request.getParameter("prezzo")).thenReturn("invalid"); // Invalid number
        when(request.getParameter("quantity")).thenReturn("100");
        when(request.getParameter("sconto")).thenReturn("0");
        when(request.getParameter("evidenza")).thenReturn("1");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
        assertFalse(errContent.toString().isEmpty(), "Should print stack trace to stderr");
    }

    // --- Test 7: Ordine ---

    @Test
    @DisplayName("Modifica 'ordine' (Happy Path) -> Chiama DAO e fa forward")
    void doPost_editOrdine_happyPath_forwards() throws ServletException, IOException, ParseException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("emailUtente")).thenReturn("user@example.com");
        when(request.getParameter("data")).thenReturn("2023-01-01");
        when(request.getParameter("stato")).thenReturn("Spedito");
        when(request.getParameter("totale")).thenReturn("50.0");

        when(request.getRequestDispatcher("showTable?tableName=ordine")).thenReturn(dispatcher);

        try (MockedConstruction<OrdineDao> dao = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            doNothing().when(mock).doUpdateOrder(any(Ordine.class), anyInt());
        })) {
            servlet.doPost(request, response);

            ArgumentCaptor<Ordine> captor = ArgumentCaptor.forClass(Ordine.class);
            verify(dao.constructed().get(0)).doUpdateOrder(captor.capture(), eq(1));

            Ordine o = captor.getValue();
            assertEquals(1, o.getIdOrdine());
            assertEquals("user@example.com", o.getEmailUtente());
            assertEquals("Spedito", o.getStato());
            assertEquals(50.0f, o.getTotale());
            assertEquals(new SimpleDateFormat("yyyy-MM-dd").parse("2023-01-01"), o.getDataOrdine());

            verify(dispatcher).forward(request, response);
        }
    }

    @Test
    @DisplayName("Modifica 'ordine' (Sad Path: Negative Total) -> Invia 400")
    void doPost_editOrdine_negativeTotal_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("emailUtente")).thenReturn("user@example.com");
        when(request.getParameter("data")).thenReturn("2023-01-01");
        when(request.getParameter("stato")).thenReturn("Spedito");
        when(request.getParameter("totale")).thenReturn("-10.0"); // Negative

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
    }

    @Test
    @DisplayName("Modifica 'ordine' (Boundary: Zero Total) -> Happy Path")
    void doPost_editOrdine_zeroTotal_updates() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("emailUtente")).thenReturn("user@example.com");
        when(request.getParameter("data")).thenReturn("2023-01-01");
        when(request.getParameter("stato")).thenReturn("Spedito");
        when(request.getParameter("totale")).thenReturn("0.0");

        when(request.getRequestDispatcher("showTable?tableName=ordine")).thenReturn(dispatcher);

        try (MockedConstruction<OrdineDao> dao = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            doNothing().when(mock).doUpdateOrder(any(Ordine.class), anyInt());
        })) {
            servlet.doPost(request, response);

            verify(dispatcher).forward(request, response);
            verify(response, never()).sendError(anyInt(), anyString());
        }
    }

    // --- Test 8: DettaglioOrdine ---

    @Test
    @DisplayName("Modifica 'dettaglioOrdine' (Happy Path) -> Chiama DAO e fa forward")
    void doPost_editDettaglioOrdine_happyPath_forwards() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, P1, 1");
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("idVariante")).thenReturn("1");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("quantity")).thenReturn("5");

        when(request.getRequestDispatcher("showTable?tableName=dettaglioOrdine")).thenReturn(dispatcher);

        try (MockedConstruction<DettaglioOrdineDAO> dao = mockConstruction(DettaglioOrdineDAO.class, (mock, ctx) -> {
            doNothing().when(mock).doUpdateDettaglioOrdine(any(DettaglioOrdine.class), anyInt(), anyString(), anyInt());
        })) {
            servlet.doPost(request, response);

            ArgumentCaptor<DettaglioOrdine> captor = ArgumentCaptor.forClass(DettaglioOrdine.class);
            verify(dao.constructed().get(0)).doUpdateDettaglioOrdine(captor.capture(), eq(1), eq("P1"), eq(1));

            DettaglioOrdine details = captor.getValue();
            assertEquals(1, details.getIdOrdine());
            assertEquals("P1", details.getIdProdotto());
            assertEquals(1, details.getIdVariante());
            assertEquals(5, details.getQuantita());

            verify(dispatcher).forward(request, response);
        }
    }

    @Test
    @DisplayName("Modifica 'dettaglioOrdine' (Sad Path: Negative Quantity) -> Invia 400")
    void doPost_editDettaglioOrdine_negativeQuantity_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, P1, 1");
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("idVariante")).thenReturn("1");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("quantity")).thenReturn("-5");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
    }

    @Test
    @DisplayName("Modifica 'dettaglioOrdine' (Boundary: Zero Quantity) -> Happy Path")
    void doPost_editDettaglioOrdine_zeroQuantity_updates() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, P1, 1");
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("idVariante")).thenReturn("1");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("quantity")).thenReturn("0");

        when(request.getRequestDispatcher("showTable?tableName=dettaglioOrdine")).thenReturn(dispatcher);

        try (MockedConstruction<DettaglioOrdineDAO> dao = mockConstruction(DettaglioOrdineDAO.class, (mock, ctx) -> {
            doNothing().when(mock).doUpdateDettaglioOrdine(any(DettaglioOrdine.class), anyInt(), anyString(), anyInt());
        })) {
            servlet.doPost(request, response);

            verify(dispatcher).forward(request, response);
            verify(response, never()).sendError(anyInt(), anyString());
        }
    }

    // --- Test 9: Gusto ---

    @Test
    @DisplayName("Modifica 'gusto' (Happy Path) -> Chiama DAO e fa forward")
    void doPost_editGusto_happyPath_forwards() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("gusto");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(request.getParameter("idGusto")).thenReturn("1");
        when(request.getParameter("nomeGusto")).thenReturn("Cioccolato");

        when(request.getRequestDispatcher("showTable?tableName=gusto")).thenReturn(dispatcher);

        try (MockedConstruction<GustoDAO> dao = mockConstruction(GustoDAO.class, (mock, ctx) -> {
            doNothing().when(mock).updateGusto(any(Gusto.class), anyInt());
        })) {
            servlet.doPost(request, response);

            ArgumentCaptor<Gusto> captor = ArgumentCaptor.forClass(Gusto.class);
            verify(dao.constructed().get(0)).updateGusto(captor.capture(), eq(1));

            Gusto g = captor.getValue();
            assertEquals(1, g.getIdGusto());
            assertEquals("Cioccolato", g.getNomeGusto());

            verify(dispatcher).forward(request, response);
        }
    }

    // --- Test 10: Confezione ---

    @Test
    @DisplayName("Modifica 'confezione' (Happy Path) -> Chiama DAO e fa forward")
    void doPost_editConfezione_happyPath_forwards() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("confezione");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(request.getParameter("idConfezione")).thenReturn("1");
        when(request.getParameter("pesoConfezione")).thenReturn("500");

        when(request.getRequestDispatcher("showTable?tableName=confezione")).thenReturn(dispatcher);

        try (MockedConstruction<ConfezioneDAO> dao = mockConstruction(ConfezioneDAO.class, (mock, ctx) -> {
            doNothing().when(mock).doUpdateConfezione(any(Confezione.class), anyInt());
        })) {
            servlet.doPost(request, response);

            ArgumentCaptor<Confezione> captor = ArgumentCaptor.forClass(Confezione.class);
            verify(dao.constructed().get(0)).doUpdateConfezione(captor.capture(), eq(1));

            Confezione c = captor.getValue();
            assertEquals(1, c.getIdConfezione());
            assertEquals(500, c.getPeso());

            verify(dispatcher).forward(request, response);
        }
    }

    @Test
    @DisplayName("Modifica 'confezione' (Sad Path: Zero Weight) -> Invia 400")
    void doPost_editConfezione_zeroWeight_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("confezione");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(request.getParameter("idConfezione")).thenReturn("1");
        when(request.getParameter("pesoConfezione")).thenReturn("0");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
    }

    @Test
    @DisplayName("tableName non valido (Default switch) -> Invia 400")
    void doPost_invalidTableName_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("tabellaSbagliata");
        when(request.getParameter("primaryKey")).thenReturn("123");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid table name.");
        verify(dispatcher, never()).forward(any(), any());
    }

    // --- Test 11: Exception Handling (Coverage Improvement) ---

    @Test
    @DisplayName("Modifica 'ordine' (Sad Path: Invalid Date) -> Invia 400")
    void doPost_editOrdine_invalidDate_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("emailUtente")).thenReturn("user@example.com");
        when(request.getParameter("data")).thenReturn("invalid-date"); // ParseException
        when(request.getParameter("stato")).thenReturn("Spedito");
        when(request.getParameter("totale")).thenReturn("50.0");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
        assertFalse(errContent.toString().isEmpty(), "Should print stack trace");
    }

    @Test
    @DisplayName("Modifica 'ordine' (Sad Path: Invalid Totale) -> Invia 400")
    void doPost_editOrdine_invalidTotale_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("emailUtente")).thenReturn("user@example.com");
        when(request.getParameter("data")).thenReturn("2023-01-01");
        when(request.getParameter("stato")).thenReturn("Spedito");
        when(request.getParameter("totale")).thenReturn("abc"); // NumberFormatException

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
        assertFalse(errContent.toString().isEmpty(), "Should print stack trace");
    }

    @Test
    @DisplayName("Modifica 'ordine' (Sad Path: Invalid IdOrdine) -> Invia 400")
    void doPost_editOrdine_invalidIdOrdine_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(request.getParameter("idOrdine")).thenReturn("abc"); // NumberFormatException
        when(request.getParameter("emailUtente")).thenReturn("user@example.com");
        when(request.getParameter("data")).thenReturn("2023-01-01");
        when(request.getParameter("stato")).thenReturn("Spedito");
        when(request.getParameter("totale")).thenReturn("50.0");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
        assertFalse(errContent.toString().isEmpty(), "Should print stack trace");
    }

    @Test
    @DisplayName("Modifica 'dettaglioOrdine' (Sad Path: Invalid Quantity) -> Invia 400")
    void doPost_editDettaglioOrdine_invalidQuantity_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, P1, 1");
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("idVariante")).thenReturn("1");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("quantity")).thenReturn("abc"); // NumberFormatException

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
        assertFalse(errContent.toString().isEmpty(), "Should print stack trace");
    }

    @Test
    @DisplayName("Modifica 'dettaglioOrdine' (Sad Path: Malformed PK) -> Invia 400")
    void doPost_editDettaglioOrdine_malformedPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, P1"); // Missing 3rd part -> ArrayIndexOutOfBounds
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("idVariante")).thenReturn("1");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("quantity")).thenReturn("5");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
    }

    @Test
    @DisplayName("Modifica 'gusto' (Sad Path: Invalid IdGusto) -> Invia 400")
    void doPost_editGusto_invalidIdGusto_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("gusto");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(request.getParameter("idGusto")).thenReturn("abc"); // NumberFormatException
        when(request.getParameter("nomeGusto")).thenReturn("Cioccolato");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
        assertFalse(errContent.toString().isEmpty(), "Should print stack trace");
    }

    @Test
    @DisplayName("Modifica 'confezione' (Sad Path: Invalid Peso) -> Invia 400")
    void doPost_editConfezione_invalidPeso_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("confezione");
        when(request.getParameter("primaryKey")).thenReturn("1");
        when(request.getParameter("idConfezione")).thenReturn("1");
        when(request.getParameter("pesoConfezione")).thenReturn("abc"); // NumberFormatException

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
        assertFalse(errContent.toString().isEmpty(), "Should print stack trace");
    }

    // --- Test 12: Primary Key Parsing Exceptions (New Coverage) ---

    @Test
    @DisplayName("Modifica 'ordine' (Sad Path: Invalid PrimaryKey) -> Invia 400")
    void doPost_editOrdine_invalidPrimaryKey_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("ordine");
        when(request.getParameter("primaryKey")).thenReturn("abc"); // NumberFormatException
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("emailUtente")).thenReturn("user@example.com");
        when(request.getParameter("data")).thenReturn("2023-01-01");
        when(request.getParameter("stato")).thenReturn("Spedito");
        when(request.getParameter("totale")).thenReturn("50.0");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
        assertFalse(errContent.toString().isEmpty(), "Should print stack trace");
    }

    @Test
    @DisplayName("Modifica 'gusto' (Sad Path: Invalid PrimaryKey) -> Invia 400")
    void doPost_editGusto_invalidPrimaryKey_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("gusto");
        when(request.getParameter("primaryKey")).thenReturn("abc"); // NumberFormatException
        when(request.getParameter("idGusto")).thenReturn("1");
        when(request.getParameter("nomeGusto")).thenReturn("Cioccolato");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
        assertFalse(errContent.toString().isEmpty(), "Should print stack trace");
    }

    @Test
    @DisplayName("Modifica 'confezione' (Sad Path: Invalid PrimaryKey) -> Invia 400")
    void doPost_editConfezione_invalidPrimaryKey_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("confezione");
        when(request.getParameter("primaryKey")).thenReturn("abc"); // NumberFormatException
        when(request.getParameter("idConfezione")).thenReturn("1");
        when(request.getParameter("pesoConfezione")).thenReturn("500");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
        assertFalse(errContent.toString().isEmpty(), "Should print stack trace");
    }

    @Test
    @DisplayName("Modifica 'variante' (Sad Path: Invalid PrimaryKey) -> Invia 400")
    void doPost_editVariante_invalidPrimaryKey_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("variante");
        when(request.getParameter("primaryKey")).thenReturn("abc"); // NumberFormatException
        when(request.getParameter("idVariante")).thenReturn("1");
        when(request.getParameter("idProdottoVariante")).thenReturn("P1");
        when(request.getParameter("idGusto")).thenReturn("1");
        when(request.getParameter("idConfezione")).thenReturn("1");
        when(request.getParameter("prezzo")).thenReturn("10.5");
        when(request.getParameter("quantity")).thenReturn("100");
        when(request.getParameter("sconto")).thenReturn("0");
        when(request.getParameter("evidenza")).thenReturn("1");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
        assertFalse(errContent.toString().isEmpty(), "Should print stack trace");
    }

    @Test
    @DisplayName("tableName vuoto (blank) -> Gestito e invia 400")
    void doPost_blankTableName_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("   "); // Blank
        when(request.getParameter("primaryKey")).thenReturn("123");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Parametri 'tableName' o 'primaryKey' mancanti.");
    }

    @Test
    @DisplayName("primaryKey vuoto (blank) -> Gestito e invia 400")
    void doPost_blankPrimaryKey_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("   "); // Blank

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Parametri 'tableName' o 'primaryKey' mancanti.");
    }

    @Test
    @DisplayName("Modifica 'utente' (Sad Path: Parametro mancante/null) -> Invia 400")
    void doPost_editUtente_missingParam_sendsError() throws ServletException, IOException {
        setupValidUtenteParams();
        when(request.getParameter("nome")).thenReturn(null); // Missing param
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("old@email.com");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
    }

    @Test
    @DisplayName("Modifica 'dettaglioOrdine' (Sad Path: Invalid PK Format) -> Invia 400")
    void doPost_editDettaglioOrdine_invalidPKFormat_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("A, P1, 1"); // Invalid number in PK
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("idVariante")).thenReturn("1");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("quantity")).thenReturn("5");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
        assertFalse(errContent.toString().isEmpty(), "Should print stack trace");
    }
}
