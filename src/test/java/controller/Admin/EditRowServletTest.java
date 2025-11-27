package controller.Admin;

import jakarta.servlet.RequestDispatcher;
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

    @BeforeEach
    void setup() {
        servlet = new editRowServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        dispatcher = mock(RequestDispatcher.class);
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

    // --- Test 2: Verifica Correzione Faglie (Input non validi) ---

    @Test
    @DisplayName("tableName nullo -> Gestito e invia 400")
    void doPost_nullTableName_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn(null);
        when(request.getParameter("primaryKey")).thenReturn("123");

        servlet.doPost(request, response);

        // Verifica che la correzione (controllo di guardia) invii un errore
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
        setupValidProdottoParams(); // Imposta tutti i parametri validi
        when(request.getParameter("tableName")).thenReturn("prodotto");
        when(request.getParameter("primaryKey")).thenReturn("P1");
        when(request.getParameter("calorie")).thenReturn("abc"); // ... tranne questo

        // Il DAO non verrà mai creato perché il try-catch nel metodo corretto fallisce
        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class)) {

            servlet.doPost(request, response);

            // Il metodo 'editProdotto' (corretto) cattura NFE, restituisce false.
            // La servlet quindi invia un errore.
            verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
            verify(dispatcher, never()).forward(any(), any());
            assertEquals(0, dao.constructed().size()); // DAO non creato
        }
    }

    @Test
    @DisplayName("Input data non valido (Utente) -> Gestito e invia 400")
    void doPost_editUtente_invalidDate_sendsError() throws ServletException, IOException {
        setupValidUtenteParams(); // Imposta tutti i parametri validi
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("old@email.com");
        when(request.getParameter("dataDiNascita")).thenReturn("data-sbagliata"); // ... tranne questo

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class)) {

            servlet.doPost(request, response);

            // 'editUtente' cattura ParseException, restituisce false. Servlet invia errore.
            verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
            verify(dispatcher, never()).forward(any(), any());
            assertEquals(0, dao.constructed().size()); // DAO non creato
        }
    }

    // --- Test 3: Happy Path (Modifica Utente) ---

    @Test
    @DisplayName("Modifica 'utente' (Happy Path) -> Chiama DAO e fa forward")
    void doPost_editUtente_happyPath_forwards() throws ServletException, IOException, ParseException {
        setupValidUtenteParams(); // Imposta tutti i parametri validi
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("old@email.com");

        // Stub del dispatcher per il forward
        when(request.getRequestDispatcher("showTable?tableName=utente")).thenReturn(dispatcher);

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            // Stub: doUpdate non fa nulla
            doNothing().when(mock).doUpdateCustomer(any(Utente.class), anyString());
        })) {

            servlet.doPost(request, response);

            // Verifica che il DAO sia stato creato e chiamato
            UtenteDAO mockDao = dao.constructed().get(0);

            // Cattura l'oggetto Utente passato al DAO
            ArgumentCaptor<Utente> utenteCaptor = ArgumentCaptor.forClass(Utente.class);
            verify(mockDao).doUpdateCustomer(utenteCaptor.capture(), eq("old@email.com"));

            // Verifica che l'oggetto Utente catturato abbia i dati corretti
            Utente savedUser = utenteCaptor.getValue();
            assertEquals("Mario", savedUser.getNome());
            assertEquals("user@example.com", savedUser.getEmail());

            // Verifica la data (per assicurarsi che il parsing sia corretto)
            Date expectedDate = new SimpleDateFormat("yyyy-MM-dd").parse("1980-01-01");
            assertEquals(expectedDate, savedUser.getDataNascita());

            // Verifica che il forward sia avvenuto
            verify(dispatcher).forward(request, response);
            verify(response, never()).sendError(anyInt(), anyString());
        }
    }

    // --- Test 4: Sad Path (Logica di business) ---

    @Test
    @DisplayName("Modifica 'utente' (Sad Path) -> 'isValid' false -> Invia 400")
    void doPost_editUtente_invalidParam_sendsError() throws ServletException, IOException {
        setupValidUtenteParams(); // Imposta tutti i parametri validi
        when(request.getParameter("nome")).thenReturn(""); // ... tranne questo (isBlank)
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn("old@email.com");

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class)) {

            servlet.doPost(request, response);

            // Il metodo 'isValid' restituisce false, 'editUtente' restituisce false.
            verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
            verify(dispatcher, never()).forward(any(), any());
            // Il DAO non deve essere creato perché il controllo fallisce prima
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
            verify(mockDao).updateProduct(any(Prodotto.class), eq("P1"));
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
        when(request.getParameter("sconto")).thenReturn("0");
        when(request.getParameter("evidenza")).thenReturn("1");

        when(request.getRequestDispatcher("showTable?tableName=variante")).thenReturn(dispatcher);

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            doNothing().when(mock).updateVariante(any(Variante.class), anyInt());
        })) {
            servlet.doPost(request, response);

            verify(dao.constructed().get(0)).updateVariante(any(Variante.class), eq(1));
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

        // Since editVariante does not catch NumberFormatException, this might fail with
        // an exception
        // But let's see if we can catch it or if the servlet should be fixed.
        // For now, assuming the test expects 400, but it might crash.
        // If it crashes, we should fix the servlet.
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
    }

    // --- Test 7: Ordine ---

    @Test
    @DisplayName("Modifica 'ordine' (Happy Path) -> Chiama DAO e fa forward")
    void doPost_editOrdine_happyPath_forwards() throws ServletException, IOException {
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

            verify(dao.constructed().get(0)).doUpdateOrder(any(Ordine.class), eq(1));
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

            verify(dao.constructed().get(0)).doUpdateDettaglioOrdine(any(DettaglioOrdine.class), eq(1), eq("P1"),
                    eq(1));
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

            verify(dao.constructed().get(0)).updateGusto(any(Gusto.class), eq(1));
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

            verify(dao.constructed().get(0)).doUpdateConfezione(any(Confezione.class), eq(1));
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

        // Lo switch va in 'default'
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
    }

    @Test
    @DisplayName("Modifica 'dettaglioOrdine' (Sad Path: Malformed PK) -> Invia 400")
    void doPost_editDettaglioOrdine_malformedPK_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("dettaglioOrdine");
        when(request.getParameter("primaryKey")).thenReturn("1, P1"); // Missing 3rd part ->
                                                                      // ArrayIndexOutOfBoundsException
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
    }

    // --- Test 13: Boolean Logic & Edge Cases (New Coverage) ---

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
    }
}
