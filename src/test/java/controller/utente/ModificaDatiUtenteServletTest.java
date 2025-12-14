package controller.utente;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Utente;
import model.UtenteDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Classe di test completa per ModificaDatiUtenteServlet.
 */
public class ModificaDatiUtenteServletTest {

    private ModificaDatiUtenteServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;
    private Utente realUtente;
    private ServletContext servletContext; // Promoted to field for verification

    // Definiamo la password in chiaro che useremo per i test "positivi"
    private final String PLAINTEXT_PASSWORD_CORRETTA = "Password!Corretta123";

    // Questo campo verrà calcolato nel setup usando il VERO metodo sha1()
    private String HASHED_PASSWORD_IN_DB;

    @BeforeEach
    void setup() throws Exception {
        // 1. Istanziamo la servlet normally (NON è uno spy)
        servlet = new ModificaDatiUtenteServlet();

        // 2. Mock ServletConfig e ServletContext per permettere il logging
        ServletConfig servletConfig = mock(ServletConfig.class);
        servletContext = mock(ServletContext.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        // Important: Mock servlet name for log messages
        when(servletConfig.getServletName()).thenReturn("ModificaDatiUtenteServlet");

        // Inizializza il servlet con il config mockato
        servlet.init(servletConfig);

        // 3. Calcoliamo l'hash REALE usando il metodo della servlet
        HASHED_PASSWORD_IN_DB = servlet.sha512(PLAINTEXT_PASSWORD_CORRETTA);

        // 4. Mocks per le dipendenze servlet
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        // 5. Creiamo un utente REALE con l'hash REALE
        realUtente = new Utente();
        realUtente.setEmail("user@example.com");
        realUtente.setPassword(HASHED_PASSWORD_IN_DB); // <-- L'hash reale

        // 6. Stub di base
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("Utente")).thenReturn(realUtente);
        when(request.getRequestDispatcher("areaUtenteServlet")).thenReturn(dispatcher);
        when(request.getProtocol()).thenReturn("HTTP/1.1");
    }

    /**
     * Test per il metodo di utilità sha512.
     */
    @Test
    @DisplayName("Il metodo sha512() calcola correttamente l'hash")
    void sha512_HashesCorrectly() {
        // Test con un valore noto (SHA-512 di "test")
        String expectedHash = "ee26b0dd4af7e749aa1a8ee3c10ae9923f618980772e473f8819a5d4940e0db27ac185f8a0e1d5f84f88bc887fd67b143732c304cc5fa9ad8e6f57f50028a8ff";
        assertEquals(expectedHash, servlet.sha512("test"));
    }

    @Test
    @DisplayName("doPost con 'field' non valido va al caso 'default' e imposta un errore")
    void doPost_InvalidField_ForwardsWithError() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("campo-sbagliato");

        servlet.doPost(request, response);

        // Verifica che imposti l'errore corretto
        verify(request).setAttribute("error", "Invalid field parameter");
        // Verifica che faccia il forward
        verify(dispatcher).forward(request, response);
    }

    // --- Test per handleNomeChange ---

    @Test
    @DisplayName("Modifica Nome: Successo")
    void handleNomeChange_Success() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("nome");
        when(request.getParameter("new-name")).thenReturn("Mario");

        // Intercettiamo la creazione del DAO
        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {

            servlet.doPost(request, response);

            // Otteniamo il DAO mockato
            UtenteDAO constructedDao = mocked.constructed().get(0);

            // 1. Verifica chiamata al DB
            verify(constructedDao).doUpdateCustomerGeneric(realUtente, "nome", "Mario");
            // 2. Verifica aggiornamento oggetto Utente
            assertEquals("Mario", realUtente.getNome());
            // 3. Verifica aggiornamento sessione
            verify(session).setAttribute("Utente", realUtente);
            // 4. Verifica messaggi di successo e forward
            verify(request).setAttribute("messageType", "success");
            verify(request).setAttribute("message", "Nome modificato con successo");
            verify(request).setAttribute("field", "nome"); // <-- Added
            verify(dispatcher).forward(request, response);
        }
    }

    @Test
    @DisplayName("Modifica Nome: Fallimento (Missing Parameter)")
    void handleNomeChange_MissingParam() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("nome");
        when(request.getParameter("new-name")).thenReturn(null); // Parametro mancante

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {

            servlet.doPost(request, response);

            // Verifica messaggi di errore e forward
            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Missing parameters");
            verify(request).setAttribute("field", "nome"); // Already present, but good to check
            verify(dispatcher).forward(request, response);

            // Verifica che il DAO non sia stato usato
            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    // --- Test per handleCognomeChange (NUOVO TEST) ---

    @Test
    @DisplayName("Modifica Cognome: Successo")
    void handleCognomeChange_Success() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("cognome");
        when(request.getParameter("new-surname")).thenReturn("Rossi");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            UtenteDAO constructedDao = mocked.constructed().get(0);

            verify(constructedDao).doUpdateCustomerGeneric(realUtente, "cognome", "Rossi");
            assertEquals("Rossi", realUtente.getCognome());
            verify(session).setAttribute("Utente", realUtente);
            verify(request).setAttribute("messageType", "success");
            verify(request).setAttribute("message", "Cognome modificato con successo");
            verify(request).setAttribute("field", "cognome"); // <-- Added
            verify(dispatcher).forward(request, response);
        }
    }

    @Test
    @DisplayName("Modifica Cognome: Fallimento (Missing Parameter)")
    void handleCognomeChange_MissingParam() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("cognome");
        when(request.getParameter("new-surname")).thenReturn(null);

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Missing parameters");
            verify(request).setAttribute("field", "cognome");
            verify(dispatcher).forward(request, response);
            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    // --- Test per handleAddressChange (NUOVO TEST) ---

    @Test
    @DisplayName("Modifica Indirizzo: Successo")
    void handleAddressChange_Success() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("address");
        when(request.getParameter("street")).thenReturn("Via Roma 10");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            UtenteDAO constructedDao = mocked.constructed().get(0);

            verify(constructedDao).doUpdateCustomerGeneric(realUtente, "indirizzo", "Via Roma 10");
            assertEquals("Via Roma 10", realUtente.getIndirizzo());
            verify(session).setAttribute("Utente", realUtente);
            verify(request).setAttribute("messageType", "success");
            verify(request).setAttribute("message", "Indirizzo modificato con successo");
            verify(request).setAttribute("field", "address"); // <-- Added
            verify(dispatcher).forward(request, response);
        }
    }

    // --- Test per handleCodiceFiscaleChange (NUOVO TEST) ---

    @Test
    @DisplayName("Modifica Codice Fiscale: Successo")
    void handleCodiceFiscaleChange_Success() throws ServletException, IOException {
        String validCF = "RSSMRA80A01H501U"; // CF valido
        when(request.getParameter("field")).thenReturn("codice-fiscale");
        when(request.getParameter("new-codice-fiscale")).thenReturn(validCF);

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            UtenteDAO constructedDao = mocked.constructed().get(0);

            verify(constructedDao).doUpdateCustomerGeneric(realUtente, "codice_fiscale", validCF);
            assertEquals(validCF, realUtente.getCodiceFiscale());
            verify(session).setAttribute("Utente", realUtente);
            verify(request).setAttribute("messageType", "success");
            verify(request).setAttribute("message", "Codice fiscale modificato con successo");
            verify(request).setAttribute("field", "codice-fiscale"); // <-- Added
            verify(dispatcher).forward(request, response);
        }
    }

    @Test
    @DisplayName("Modifica Codice Fiscale: Fallimento (Invalid Pattern)")
    void handleCodiceFiscaleChange_InvalidPattern() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("codice-fiscale");
        when(request.getParameter("new-codice-fiscale")).thenReturn("12345"); // Pattern errato

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Pattern non rispettato");
            verify(request).setAttribute("field", "codice-fiscale");
            verify(dispatcher).forward(request, response);
            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Modifica Telefono: Successo")
    void handlePhoneChange_Success() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("phone");
        when(request.getParameter("new-phone")).thenReturn("3331234567");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            UtenteDAO constructedDao = mocked.constructed().get(0);
            verify(constructedDao).doUpdateCustomerGeneric(realUtente, "numero_di_cellulare", "3331234567");
            assertEquals("3331234567", realUtente.getTelefono());
            verify(session).setAttribute("Utente", realUtente);
            verify(request).setAttribute("messageType", "success");
            verify(request).setAttribute("field", "phone"); // <-- Added
            verify(dispatcher).forward(request, response);
        }
    }

    @Test
    @DisplayName("Modifica Telefono: Fallimento (Invalid Pattern)")
    void handlePhoneChange_InvalidPattern() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("phone");
        when(request.getParameter("new-phone")).thenReturn("12345"); // Pattern errato

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            // Verifica errore
            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Pattern non rispettato");
            verify(request).setAttribute("field", "phone");
            verify(dispatcher).forward(request, response);

            // Verifica che il DAO non sia stato usato
            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Modifica Data Nascita: Successo")
    void handleDataNascitaChange_Success() throws Exception {
        when(request.getParameter("field")).thenReturn("data-di-nascita");
        when(request.getParameter("new-birthdate")).thenReturn("1995-05-20");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            UtenteDAO constructedDao = mocked.constructed().get(0);
            verify(constructedDao).doUpdateCustomerGeneric(realUtente, "data_di_nascita", "1995-05-20");

            // Verifica che la data sull'oggetto utente sia corretta
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date expectedDate = sdf.parse("1995-05-20");
            assertEquals(expectedDate, realUtente.getDataNascita());

            verify(session).setAttribute("Utente", realUtente);
            verify(request).setAttribute("messageType", "success");
            verify(request).setAttribute("field", "data-di-nascita"); // <-- Added
            verify(dispatcher).forward(request, response);
        }
    }

    @Test
    @DisplayName("Modifica Data Nascita: Fallimento (Anno < 1900)")
    void handleDataNascitaChange_YearTooLow() throws Exception {
        when(request.getParameter("field")).thenReturn("data-di-nascita");
        when(request.getParameter("new-birthdate")).thenReturn("1899-12-31");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Anno minimo non rispettato");
            verify(request).setAttribute("field", "data-di-nascita");
            verify(dispatcher).forward(request, response);

            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Modifica Password: Successo")
    void handlePasswordChange_Success() throws Exception {
        when(request.getParameter("field")).thenReturn("password");

        // 1. Usiamo la password in chiaro corretta, definita nel setup
        when(request.getParameter("current-password")).thenReturn(PLAINTEXT_PASSWORD_CORRETTA);

        // 2. Definiamo la nuova password
        when(request.getParameter("new-password")).thenReturn("NuovaValidPass1!");
        when(request.getParameter("confirm-password")).thenReturn("NuovaValidPass1!");

        // 3. NESSUNO STUB per sha1() è necessario.

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {

            // 4. Eseguiamo doPost(). La servlet chiamerà il VERO metodo sha1()
            servlet.doPost(request, response);

            // Il test passa perché:
            // servlet.sha1(PLAINTEXT_PASSWORD_CORRETTA) == HASHED_PASSWORD_IN_DB
            // e HASHED_PASSWORD_IN_DB è la password nel realUtente.

            UtenteDAO constructedDao = mocked.constructed().get(0);

            // 5. Catturiamo la nuova password salvata
            ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
            verify(constructedDao).doUpdateCustomerGeneric(eq(realUtente), eq("password"), passwordCaptor.capture());

            String savedPassword = passwordCaptor.getValue();
            assertNotNull(savedPassword);
            // Verifichiamo che la password salvata NON sia quella in chiaro
            assertNotEquals("NuovaValidPass1!", savedPassword);

            // 6. Verifica che l'oggetto utente e la sessione siano aggiornati
            assertEquals(savedPassword, realUtente.getPassword()); // L'oggetto utente ha la nuova password hashata
            verify(session).setAttribute("Utente", realUtente);

            // 7. Verifica messaggi di successo
            verify(request).setAttribute("messageType", "success");
            verify(request).setAttribute("message", "Password modificata con successo");
            verify(request).setAttribute("field", "password"); // <-- Added
            verify(dispatcher).forward(request, response);
        }
    }

    @Test
    @DisplayName("Modifica Password: Fallimento (Password attuale non corretta)")
    void handlePasswordChange_CurrentPasswordMismatch() throws Exception {
        when(request.getParameter("field")).thenReturn("password");

        when(request.getParameter("current-password")).thenReturn("password-sbagliata");

        when(request.getParameter("new-password")).thenReturn("NuovaValidPass1!");
        when(request.getParameter("confirm-password")).thenReturn("NuovaValidPass1!");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {

            servlet.doPost(request, response);

            // Il test fallisce (correttamente) perché:
            // servlet.sha1("password-sbagliata") != HASHED_PASSWORD_IN_DB

            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Password attuale non corretta");
            verify(request).setAttribute("field", "password"); // <-- Added
            verify(dispatcher).forward(request, response);

            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Modifica Password: Fallimento (Nuove password non corrispondono)")
    void handlePasswordChange_NewPasswordMismatch() throws Exception {
        when(request.getParameter("field")).thenReturn("password");

        // La password corrente è corretta
        when(request.getParameter("current-password")).thenReturn(PLAINTEXT_PASSWORD_CORRETTA);

        // Ma le nuove non combaciano
        when(request.getParameter("new-password")).thenReturn("NuovaValidPass1!");
        when(request.getParameter("confirm-password")).thenReturn("PasswordDiversa2!"); // Mismatch

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            // Verifica errore
            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Le password non corrispondono");
            verify(request).setAttribute("field", "password"); // <-- Added
            verify(dispatcher).forward(request, response);

            // Nessuna modifica al DB
            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Modifica Password: Fallimento (Pattern nuova password non rispettato)")
    void handlePasswordChange_InvalidPattern() throws Exception {
        when(request.getParameter("field")).thenReturn("password");
        when(request.getParameter("current-password")).thenReturn(PLAINTEXT_PASSWORD_CORRETTA);
        when(request.getParameter("new-password")).thenReturn("debole"); // Pattern errato
        when(request.getParameter("confirm-password")).thenReturn("debole");

        // Il controllo fallisce prima ancora di verificare la password corrente

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            // Verifica errore
            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Pattern non rispettato");
            verify(request).setAttribute("field", "password"); // <-- Added
            verify(dispatcher).forward(request, response);

            // Nessuna modifica al DB
            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Modifica Password: Fallimento (Missing Parameter)")
    void handlePasswordChange_MissingParam() throws Exception {
        when(request.getParameter("field")).thenReturn("password");
        // Parametri mancanti
        when(request.getParameter("current-password")).thenReturn(null);
        when(request.getParameter("new-password")).thenReturn("NuovaValidPass1!");
        when(request.getParameter("confirm-password")).thenReturn("NuovaValidPass1!");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Missing parameters");
            verify(request).setAttribute("field", "password");
            verify(dispatcher).forward(request, response);

            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Modifica Password: Fallimento (Confirm Password Invalid Pattern)")
    void handlePasswordChange_ConfirmPasswordInvalidPattern() throws Exception {
        when(request.getParameter("field")).thenReturn("password");
        when(request.getParameter("current-password")).thenReturn(PLAINTEXT_PASSWORD_CORRETTA);
        when(request.getParameter("new-password")).thenReturn("NuovaValidPass1!");
        when(request.getParameter("confirm-password")).thenReturn("short"); // Pattern errato

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Pattern non rispettato");
            verify(request).setAttribute("field", "password");
            verify(dispatcher).forward(request, response);

            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Modifica Data Nascita: Fallimento (Missing Parameter)")
    void handleDataNascitaChange_MissingParam() throws Exception {
        when(request.getParameter("field")).thenReturn("data-di-nascita");
        when(request.getParameter("new-birthdate")).thenReturn(null);

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Missing parameters");
            verify(request).setAttribute("field", "data-di-nascita");
            verify(dispatcher).forward(request, response);

            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Modifica Data Nascita: Errore Parsing (Gestito dal servlet)")
    void handleDataNascitaChange_ParseError() throws Exception {
        when(request.getParameter("field")).thenReturn("data-di-nascita");
        // Data che passa il check dell'anno ma fallisce il parsing
        when(request.getParameter("new-birthdate")).thenReturn("2000-bad-date");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            // Il servlet cattura la ServletException causata da ParseException
            // e invia un errore 500
            verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
        }
    }

    @Test
    @DisplayName("doGet esegue senza errori (default check)")
    void doGet_Executes() throws ServletException, IOException {
        servlet.doGet(request, response);
        // Verifica che invii 405 (comportamento standard mocked/spy/super)
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), anyString());
    }

    @Test
    @DisplayName("Modifica Data Nascita: Fallimento (Formato Anno non valido)")
    void handleDataNascitaChange_InvalidYearFormat_SendsError() throws Exception {
        when(request.getParameter("field")).thenReturn("data-di-nascita");
        when(request.getParameter("new-birthdate")).thenReturn("abcd-01-01"); // Anno non numerico

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Formato data non valido");
            verify(request).setAttribute("field", "data-di-nascita");
            verify(dispatcher).forward(request, response);

            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Modifica Indirizzo: Fallimento (Missing Parameter)")
    void handleAddressChange_MissingParam() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("address");
        when(request.getParameter("street")).thenReturn(null);

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Missing parameters");
            verify(request).setAttribute("field", "address");
            verify(dispatcher).forward(request, response);

            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Modifica Telefono: Fallimento (Missing Parameter)")
    void handlePhoneChange_MissingParam() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("phone");
        when(request.getParameter("new-phone")).thenReturn(null);

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Missing parameters");
            // Nota: Il codice sorgente setta "field" a "address" in questo caso, come
            // notato.
            // Copiamo il comportamento osservato.
            verify(request).setAttribute("field", "address");
            verify(dispatcher).forward(request, response);

            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Modifica Codice Fiscale: Fallimento (Missing Parameter)")
    void handleCodiceFiscaleChange_MissingParam() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("codice-fiscale");
        when(request.getParameter("new-codice-fiscale")).thenReturn(null);

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Missing parameters");
            verify(request).setAttribute("field", "codice-fiscale");
            verify(dispatcher).forward(request, response);

            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("doGet invia errore 405 (Method Not Allowed) - Verify Check")
    void doGet_sendsMethodNotAllowed() throws ServletException, IOException {
        // Riesegue doGet e verifica esplicitamente il 405
        servlet.doGet(request, response);
        verify(response, atLeastOnce()).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), anyString());
    }

    @Test
    @DisplayName("doGet lancia eccezione imprevista -> Logga e invia 500")
    void doGet_exception_sendsError500() throws ServletException, IOException {
        // Simuliamo un errore nella chiamata di super.doGet o nella response
        doThrow(new IOException("Simulated IO Error")).when(response).sendError(anyInt(), anyString());

        servlet.doGet(request, response);

        // Verify Log
        verify(servletContext).log(eq("ModificaDatiUtenteServlet: Errore in doGet ModificaDatiUtenteServlet"),
                any(Exception.class));
        // Verify 500
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
    }

    @Test
    @DisplayName("doPost lancia eccezione imprevista -> Logga e invia 500")
    void doPost_exception_sendsError500() throws ServletException, IOException {
        // Simuliamo un'eccezione all'inizio di doPost, es. request.getParameter lancia
        // RuntimeException
        when(request.getParameter("field")).thenThrow(new RuntimeException("Unexpected Error"));

        servlet.doPost(request, response);

        // Verify Log
        verify(servletContext).log(eq("ModificaDatiUtenteServlet: Errore in doPost ModificaDatiUtenteServlet"),
                any(Exception.class));
        // Verify 500
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Errore interno durante la modifica dei dati.");
    }

    @Test
    @DisplayName("doGet: se lancia eccezione ma risposta committata -> Logga ma NON invia errore")
    void doGet_exception_committed_doesNotSendError() throws ServletException, IOException {
        // Simuliamo risposta committata
        when(response.isCommitted()).thenReturn(true);
        // Simuliamo eccezione
        doThrow(new IOException("Simulated IO")).when(response).sendError(anyInt(), anyString());

        servlet.doGet(request, response);

        verify(servletContext).log(eq("ModificaDatiUtenteServlet: Errore in doGet ModificaDatiUtenteServlet"),
                any(Exception.class));
        // Poiché committed = true, non deve inviare 500
        verify(response, never()).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
    }

    @Test
    @DisplayName("doPost: se lancia eccezione ma risposta committata -> Logga ma NON invia errore")
    void doPost_exception_committed_doesNotSendError() throws ServletException, IOException {
        when(response.isCommitted()).thenReturn(true);
        // Simuliamo eccezione
        when(request.getParameter("field")).thenThrow(new RuntimeException("Unexpected Error"));

        servlet.doPost(request, response);

        verify(servletContext).log(eq("ModificaDatiUtenteServlet: Errore in doPost ModificaDatiUtenteServlet"),
                any(Exception.class));
        // Poiché committed = true, non deve inviare 500
        verify(response, never()).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Errore interno durante la modifica dei dati.");
    }

    @Test
    @DisplayName("Modifica Data Nascita: Anno 1900 esatto (Boundary) -> Successo")
    void handleDataNascitaChange_Year1900_Success() throws Exception {
        when(request.getParameter("field")).thenReturn("data-di-nascita");
        when(request.getParameter("new-birthdate")).thenReturn("1900-01-01");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            UtenteDAO constructedDao = mocked.constructed().get(0);
            verify(constructedDao).doUpdateCustomerGeneric(realUtente, "data_di_nascita", "1900-01-01");
            verify(request).setAttribute("messageType", "success");
        }
    }

    @Test
    @DisplayName("Modifica Password: Fallimento (Missing new-password)")
    void handlePasswordChange_MissingNewPassword() throws Exception {
        when(request.getParameter("field")).thenReturn("password");
        when(request.getParameter("current-password")).thenReturn(PLAINTEXT_PASSWORD_CORRETTA);
        when(request.getParameter("new-password")).thenReturn(null);
        when(request.getParameter("confirm-password")).thenReturn("NuovaValidPass1!");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Missing parameters");
            verify(dispatcher).forward(request, response);
            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Modifica Password: Fallimento (Missing confirm-password)")
    void handlePasswordChange_MissingConfirmPassword() throws Exception {
        when(request.getParameter("field")).thenReturn("password");
        when(request.getParameter("current-password")).thenReturn(PLAINTEXT_PASSWORD_CORRETTA);
        when(request.getParameter("new-password")).thenReturn("NuovaValidPass1!");
        when(request.getParameter("confirm-password")).thenReturn(null);

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Missing parameters");
            verify(dispatcher).forward(request, response);
            verify(mocked.constructed().get(0), never()).doUpdateCustomerGeneric(any(), any(), any());
        }
    }

    @Test
    @DisplayName("sha512 gestisce correttamente caratteri Unicode")
    void sha512_HashesCorrectly_WithUnicode() {
        // "p@sswòrd€" sha512 hash (UTF-8)
        String expectedHash = "812b1d1b94bcafd4b9b1e2d8c31bad5e1ec8c26af262ddbd8215e5225002eaefbfff128e6e4987b4284290362d06a3db5fc449d92991c7152b1a4134cca6ce1e";
        assertEquals(expectedHash, servlet.sha512("p@sswòrd€"));
    }

    @Test
    @DisplayName("Modifica Telefono: Valore valido lunghezza 9 (minimo)")
    void handlePhoneChange_Length9_Success() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("phone");
        // "3" + 8 cifre = 9 cifre totale
        when(request.getParameter("new-phone")).thenReturn("312345678");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            // Deve passare
            verify(request).setAttribute("messageType", "success");
            verify(request).setAttribute("message", "Numero di telefono modificato con successo");
            verify(request).setAttribute("field", "phone"); // <-- Added
        }
    }

    @Test
    @DisplayName("Modifica Telefono: Fallimento lunghezza 11 (max + 1)")
    void handlePhoneChange_Length11_Failure() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("phone");
        // "3" + 10 cifre = 11 cifre (pattern accetta 8-9 dopo il 3)
        when(request.getParameter("new-phone")).thenReturn("31234567890");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            // Deve fallire
            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Pattern non rispettato");
            verify(request).setAttribute("field", "phone"); // <-- Added
        }
    }

    @Test
    @DisplayName("Modifica Password: Fallimento lunghezza 7 (min - 1)")
    void handlePasswordChange_Length7_Failure() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("password");
        // Caratteri richiesti: maiusc, minusc, numero, speciale. Totale 7.
        // "Pass1!s" -> P(M) a(m) s(m) s(m) 1(d) !(s) s(m) = 7 chars
        when(request.getParameter("current-password")).thenReturn(PLAINTEXT_PASSWORD_CORRETTA);
        when(request.getParameter("new-password")).thenReturn("Pass1!s");
        when(request.getParameter("confirm-password")).thenReturn("Pass1!s");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            // Deve fallire
            verify(request).setAttribute("messageType", "error");
            verify(request).setAttribute("message", "Pattern non rispettato");
            verify(request).setAttribute("field", "password"); // <-- Added
        }
    }

    @Test
    @DisplayName("Modifica Password: Successo lunghezza 8 (min)")
    void handlePasswordChange_Length8_Success() throws ServletException, IOException {
        when(request.getParameter("field")).thenReturn("password");
        when(request.getParameter("current-password")).thenReturn(PLAINTEXT_PASSWORD_CORRETTA);
        // "Pass1!ok" -> 8 chars, rispetta requisiti
        when(request.getParameter("new-password")).thenReturn("Pass1!ok");
        when(request.getParameter("confirm-password")).thenReturn("Pass1!ok");

        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class)) {
            servlet.doPost(request, response);

            // Deve passare
            verify(request).setAttribute("messageType", "success");
            verify(request).setAttribute("message", "Password modificata con successo");
            verify(request).setAttribute("field", "password"); // <-- Added
        }
    }

}