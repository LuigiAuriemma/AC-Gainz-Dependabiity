package controller.Admin;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Prodotto;
import model.ProdottoDAO;
import model.Utente;
import model.UtenteDAO;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Parametri 'tableName' o 'primaryKey' mancanti.");
        verify(dispatcher, never()).forward(any(), any());
    }

    @Test
    @DisplayName("primaryKey nulla -> Gestito e invia 400")
    void doPost_nullPrimaryKey_sendsError() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");
        when(request.getParameter("primaryKey")).thenReturn(null);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Parametri 'tableName' o 'primaryKey' mancanti.");
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
}
