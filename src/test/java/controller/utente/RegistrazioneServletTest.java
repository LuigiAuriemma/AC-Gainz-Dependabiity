package controller.utente;

import controller.utente.RegistrazioneServlet;
import jakarta.servlet.RequestDispatcher;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.*;

/**
 * Test di unità per la RegistrazioneServlet.
 * Copre i rami di validazione, i rami che dipendono dal DAO, il caso di successo e la delega doGet->doPost.
 */
public class RegistrazioneServletTest {

    private RegistrazioneServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;

    @BeforeEach
    void setup() {
        servlet = new RegistrazioneServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher("Registrazione.jsp")).thenReturn(dispatcher);

        // Valori "validi" predefiniti per tutti i parametri, così i singoli test
        // possono cambiare solo il campo che vogliono invalidare.
        when(request.getParameter("email")).thenReturn("user@example.com");
        when(request.getParameter("password")).thenReturn("Password1!");
        when(request.getParameter("nome")).thenReturn("Mario");
        when(request.getParameter("cognome")).thenReturn("Rossi");
        when(request.getParameter("codiceFiscale")).thenReturn("RSSMRA85T10A562S"); // CF valido
        when(request.getParameter("dataDiNascita")).thenReturn("1995-01-15");
        when(request.getParameter("indirizzo")).thenReturn("Via Roma 1, Napoli");
        when(request.getParameter("numCellulare")).thenReturn("3331234567");
    }

    @Test
    @DisplayName("Email non valida -> forward a Registrazione.jsp con messaggio d'errore")
    void invalidEmail_forwardsWithError() throws ServletException, IOException {
        when(request.getParameter("email")).thenReturn("not-an-email");

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("error"), eq("Pattern email non rispettato"));
        verify(dispatcher).forward(request, response);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("Password non valida -> forward a Registrazione.jsp con messaggio d'errore")
    void invalidPassword_forwardsWithError() throws ServletException, IOException {
        // Facciamo in modo che il DAO non blocchi il flusso prima della password
        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(null);
        })) {
            when(request.getParameter("password")).thenReturn("short"); // manca maiuscola, numero, simbolo

            servlet.doPost(request, response);

            verify(request).setAttribute(eq("error"), eq("Pattern password non rispettato"));
            verify(dispatcher).forward(request, response);
            verify(response, never()).sendRedirect(anyString());
        }
    }

    @Test
    @DisplayName("Codice Fiscale non valido -> forward a Registrazione.jsp con messaggio d'errore")
    void invalidCodiceFiscale_forwardsWithError() throws Exception {
        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(null);
        })) {
            when(request.getParameter("codiceFiscale")).thenReturn("RSSMRA85T10A56"); // troppo corto

            servlet.doPost(request, response);

            verify(request).setAttribute(eq("error"), eq("Pattern codice fiscale non rispettato"));
            verify(dispatcher).forward(request, response);
            verify(response, never()).sendRedirect(anyString());
        }
    }

    @Test
    @DisplayName("Data di nascita non valida -> forward a Registrazione.jsp con messaggio d'errore")
    void invalidDataDiNascita_forwardsWithError() throws Exception {
        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(null);
        })) {
            when(request.getParameter("dataDiNascita")).thenReturn("15/01/1995"); // formato errato

            servlet.doPost(request, response);

            verify(request).setAttribute(eq("error"), eq("Pattern data non rispettato"));
            verify(dispatcher).forward(request, response);
            verify(response, never()).sendRedirect(anyString());
        }
    }

    @Test
    @DisplayName("Numero di cellulare non valido -> forward a Registrazione.jsp con messaggio d'errore")
    void invalidNumCellulare_forwardsWithError() throws Exception {
        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(null);
        })) {
            when(request.getParameter("numCellulare")).thenReturn("0212345678"); // non inizia per 3

            servlet.doPost(request, response);

            verify(request).setAttribute(eq("error"), eq("Pattern numero di telefono non rispettato"));
            verify(dispatcher).forward(request, response);
            verify(response, never()).sendRedirect(anyString());
        }
    }

    @Test
    @DisplayName("Email già registrata -> forward a Registrazione.jsp con messaggio d'errore")
    void emailGiaRegistrata_forwardsWithError() throws Exception {
        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class, (mock, context) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(new Utente()); // esiste già
        })) {
            servlet.doPost(request, response);

            verify(request).setAttribute(eq("error"), eq("Email già registrata."));
            verify(dispatcher).forward(request, response);
            verify(response, never()).sendRedirect(anyString());

            UtenteDAO costruito = mocked.constructed().get(0);
            verify(costruito, never()).doSave(any(Utente.class));
        }
    }

    @Test
    @DisplayName("Registrazione valida -> doSave, sessione popolata e redirect a index.jsp")
    void validRegistration_redirectsToIndex_andSavesUser() throws Exception {
        try (MockedConstruction<UtenteDAO> mocked = mockConstruction(UtenteDAO.class, (mock, context) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(null); // non esiste
        })) {
            servlet.doPost(request, response);

            // Redirect
            verify(response).sendRedirect("index.jsp");
            verify(dispatcher, never()).forward(request, response);

            // Sessione
            verify(session).setAttribute(eq("Utente"), any(Utente.class));

            // Salvataggio con utente popolato correttamente
            UtenteDAO daoCostruito = mocked.constructed().get(0);
            ArgumentCaptor<Utente> utenteCaptor = ArgumentCaptor.forClass(Utente.class);
            verify(daoCostruito).doSave(utenteCaptor.capture());
            Utente salvato = utenteCaptor.getValue();

            assertEquals("user@example.com", salvato.getEmail());
            assertEquals("RSSMRA85T10A562S", salvato.getCodiceFiscale());
            assertEquals("Mario", salvato.getNome());
            assertEquals("Rossi", salvato.getCognome());
            assertEquals("Via Roma 1, Napoli", salvato.getIndirizzo());
            assertEquals("3331234567", salvato.getTelefono());
            assertNotEquals("Password1!", salvato.getPassword());
        }
    }

    @Test
    @DisplayName("doGet delega a doPost (qui verifichiamo un errore per semplicità)")
    void doGet_delegatesToDoPost() throws Exception {
        when(request.getParameter("email")).thenReturn("invalid"); // forza errore email
        servlet.doGet(request, response);

        verify(request).setAttribute(eq("error"), eq("Pattern email non rispettato"));
        verify(dispatcher).forward(request, response);
        verify(response, never()).sendRedirect(anyString());
    }
}
