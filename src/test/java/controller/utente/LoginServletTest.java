package controller.utente;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Carrello;
import model.CarrelloDAO;
import model.Utente;
import model.UtenteDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class LoginServletTest {

    private LoginServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;
    private ServletContext context;

    @BeforeEach
    void setup() throws ServletException {
        servlet = new LoginServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        // Init servlet with mocked config to support log() calls
        ServletConfig config = mock(ServletConfig.class);
        context = mock(ServletContext.class);
        when(config.getServletContext()).thenReturn(context);
        when(config.getServletName()).thenReturn("LoginServlet");
        servlet.init(config);

        // usa un dispatcher unico per QUALSIASI JSP (Login.jsp, index.jsp, ecc.)
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        when(request.getSession()).thenReturn(session);

        // parametri "validi" di default
        when(request.getParameter("email")).thenReturn("user@example.com");
        when(request.getParameter("password")).thenReturn("Password1!");
        when(request.getProtocol()).thenReturn("HTTP/1.1");
    }

    @Test
    @DisplayName("Email non valida → forward a Login.jsp con attribute patternEmail e rimozione attributo")
    void invalidEmail_forwardsWithError() throws Exception {
        when(request.getParameter("email")).thenReturn("not-an-email");

        servlet.doPost(request, response);

        verify(request).setAttribute("patternEmail", "Pattern email non rispettato!");
        verify(request).getRequestDispatcher("Login.jsp");
        verify(dispatcher).forward(request, response);
        verify(request).removeAttribute("patternEmail"); // Verify removal to kill mutant
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("Email non trovata → forward a Login.jsp con attribute userNotFound e rimozione attributo")
    void emailNotFound_forwardsWithError() throws Exception {
        try (MockedConstruction<UtenteDAO> mockUtenteDAO = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(null);
        })) {
            servlet.doPost(request, response);

            verify(request).setAttribute("userNotFound", "Utente non registrato!");
            verify(request).getRequestDispatcher("Login.jsp");
            verify(dispatcher).forward(request, response);
            verify(request).removeAttribute("userNotFound"); // Verify removal to kill mutant
            verify(response, never()).sendRedirect(anyString());
        }
    }

    @Test
    @DisplayName("Password errata → forward a Login.jsp con attribute wrongPassword e rimozione attributo")
    void wrongPassword_forwardsWithError() throws Exception {
        Utente dbUser = new Utente();
        dbUser.setEmail("user@example.com");

        try (MockedConstruction<UtenteDAO> mockUtenteDAO = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(dbUser);
            // la servlet poi chiama doRetrieveByEmailAndPassword: ritorna null per simulare
            // pwd errata
            when(mock.doRetrieveByEmailAndPassword("user@example.com", "Password1!")).thenReturn(null);
        })) {
            servlet.doPost(request, response);

            verify(request).setAttribute("wrongPassword", "Password errata!");
            verify(request).getRequestDispatcher("Login.jsp");
            verify(dispatcher).forward(request, response);
            verify(request).removeAttribute("wrongPassword"); // Verify removal to kill mutant
            verify(response, never()).sendRedirect(anyString());
        }
    }

    @Test
    @DisplayName("Login corretto → sessione Utente, merge carrello e forward a index.jsp")
    void correctLogin_forwardsToIndex_andSetsSessionAndCart() throws Exception {
        // utente trovato e password corretta
        Utente x = new Utente();
        x.setEmail("user@example.com");

        List<Carrello> dbCart = new ArrayList<>(); // carrello DB
        // opzionale: simulare un item nella sessione
        when(session.getAttribute("cart")).thenReturn(null); // o una lista se vuoi testare il merge

        try (MockedConstruction<UtenteDAO> mockUtenteDAO = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(x);
            when(mock.doRetrieveByEmailAndPassword("user@example.com", "Password1!")).thenReturn(x);
        });
                MockedConstruction<CarrelloDAO> mockCarrelloDAO = mockConstruction(CarrelloDAO.class, (mock, ctx) -> {
                    when(mock.doRetrieveCartItemsByUser("user@example.com")).thenReturn(dbCart);
                })) {

            servlet.doPost(request, response);

            // sessione utente
            verify(session).setMaxInactiveInterval(1800);
            verify(session).setAttribute("Utente", x);

            // carrello in sessione impostato con quello dal DB (eventualmente mergiato)
            verify(session).setAttribute(eq("cart"), any());

            // forward a index.jsp (non redirect)
            verify(request).getRequestDispatcher("index.jsp");
            verify(dispatcher).forward(request, response);
            verify(response, never()).sendRedirect(anyString());
        }
    }

    @Test
    @DisplayName("Password non valida (pattern) → forward a Login.jsp con attribute patternPassword e rimozione attributo")
    void invalidPasswordPattern_forwardsWithError() throws Exception {
        when(request.getParameter("password")).thenReturn("short"); // Password troppo corta/semplice

        servlet.doPost(request, response);

        verify(request).setAttribute("patternPassword", "Pattern password non rispettato!");
        verify(request).getRequestDispatcher("Login.jsp");
        verify(dispatcher).forward(request, response);
        verify(request).removeAttribute("patternPassword"); // Verify removal to kill mutant
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("SQLException durante il recupero utente → invia errore 500 e logga errore")
    void sqlException_sendsError500() throws Exception {
        try (MockedConstruction<UtenteDAO> mockUtenteDAO = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail(anyString())).thenReturn(new Utente()); // Email trovata
            when(mock.doRetrieveByEmailAndPassword(anyString(), anyString()))
                    .thenThrow(new java.sql.SQLException("DB Error"));
        })) {
            servlet.doPost(request, response);

            verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore database.");

            // KILL MUTANT: Verifica che venga loggato l'errore (riga 82)
            verify(context).log(eq("LoginServlet: Errore SQL durante il login"), any(java.sql.SQLException.class));
        }
    }

    @Test
    @DisplayName("Login corretto con merge carrello (Overlap) → quantità sommate")
    void correctLogin_cartMergeOverlap_sumsQuantities() throws Exception {
        Utente x = new Utente();
        x.setEmail("user@example.com");

        // Carrello DB: Item 1 (qty 2)
        Carrello dbItem = new Carrello();
        dbItem.setIdVariante(1);
        dbItem.setQuantita(2);
        dbItem.setPrezzo(10.0f);
        List<Carrello> dbCart = new ArrayList<>();
        dbCart.add(dbItem);

        // Carrello Sessione: Item 1 (qty 3)
        Carrello sessionItem = new Carrello();
        sessionItem.setIdVariante(1);
        sessionItem.setQuantita(3);
        sessionItem.setPrezzo(15.0f);
        List<Carrello> sessionCart = new ArrayList<>();
        sessionCart.add(sessionItem);

        when(session.getAttribute("cart")).thenReturn(sessionCart);

        try (MockedConstruction<UtenteDAO> mockUtenteDAO = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(x);
            when(mock.doRetrieveByEmailAndPassword("user@example.com", "Password1!")).thenReturn(x);
        });
                MockedConstruction<CarrelloDAO> mockCarrelloDAO = mockConstruction(CarrelloDAO.class, (mock, ctx) -> {
                    when(mock.doRetrieveCartItemsByUser("user@example.com")).thenReturn(dbCart);
                })) {

            servlet.doPost(request, response);

            // Verifica che l'item nel DB sia stato aggiornato (2 + 3 = 5)
            assertEquals(5, dbItem.getQuantita());
            assertEquals(25.0, dbItem.getPrezzo()); // 10 + 15

            // Verifica che la lista finale contenga ancora 1 solo elemento (merge avvenuto)
            assertEquals(1, dbCart.size());

            verify(session).setAttribute("cart", dbCart);
        }
    }

    @Test
    @DisplayName("Login corretto con merge carrello (No Overlap) → item aggiunto")
    void correctLogin_cartMergeNoOverlap_addsItem() throws Exception {
        Utente x = new Utente();
        x.setEmail("user@example.com");

        // Carrello DB: Item 1
        Carrello dbItem = new Carrello();
        dbItem.setIdVariante(1);
        List<Carrello> dbCart = new ArrayList<>();
        dbCart.add(dbItem);

        // Carrello Sessione: Item 2
        Carrello sessionItem = new Carrello();
        sessionItem.setIdVariante(2);
        List<Carrello> sessionCart = new ArrayList<>();
        sessionCart.add(sessionItem);

        when(session.getAttribute("cart")).thenReturn(sessionCart);

        try (MockedConstruction<UtenteDAO> mockUtenteDAO = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(x);
            when(mock.doRetrieveByEmailAndPassword("user@example.com", "Password1!")).thenReturn(x);
        });
                MockedConstruction<CarrelloDAO> mockCarrelloDAO = mockConstruction(CarrelloDAO.class, (mock, ctx) -> {
                    when(mock.doRetrieveCartItemsByUser("user@example.com")).thenReturn(dbCart);
                })) {

            servlet.doPost(request, response);

            // Verifica che la lista finale contenga 2 elementi
            assertEquals(2, dbCart.size());
            assertTrue(dbCart.contains(dbItem));
            assertTrue(dbCart.contains(sessionItem));

            verify(session).setAttribute("cart", dbCart);
        }
    }

    @Test
    @DisplayName("doGet → invia errore 405 (chiama super.doGet)")
    void doGet_sendsMethodNotAllowed() throws Exception {
        // Mock response per catturare sendError
        // In HttpServlet.doGet, viene chiamato sendError(405)
        servlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), anyString());
    }

    @Test
    @DisplayName("doGet exception → invia errore 500 e logga errore")
    void doGet_throwsException_sendsErrorSafe() throws Exception {
        // Force super.doGet to throw exception by manipulating response
        // or since we can't easily mock super.doGet, we trigger exception in
        // response.sendError
        // which happens inside super.doGet
        doThrow(new IOException("Simulated IO Error in doGet")).when(response)
                .sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), anyString());

        servlet.doGet(request, response);

        // Verify catch block execution
        verify(context).log(eq("LoginServlet: Errore in LoginServlet doGet"), any(Exception.class));
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), eq("Errore interno."));
    }

    @Test
    @DisplayName("Eccezione generica in doPost → invia errore 500 e logga errore")
    void genericException_sendsError500() throws Exception {
        when(request.getParameter("email")).thenThrow(new RuntimeException("Unexpected error"));

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno durante il login.");

        // KILL MUTANT: Verifica che venga loggato l'errore critico (riga 136)
        verify(context).log(eq("LoginServlet: Errore critico in LoginServlet doPost"), any(Exception.class));
    }

    @Test
    @DisplayName("CarrelloDAO restituisce null → gestito come lista vuota")
    void nullDbCart_handledAsEmptyList() throws Exception {
        Utente x = new Utente();
        x.setEmail("user@example.com");

        try (MockedConstruction<UtenteDAO> mockUtenteDAO = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(x);
            when(mock.doRetrieveByEmailAndPassword("user@example.com", "Password1!")).thenReturn(x);
        });
                MockedConstruction<CarrelloDAO> mockCarrelloDAO = mockConstruction(CarrelloDAO.class, (mock, ctx) -> {
                    when(mock.doRetrieveCartItemsByUser("user@example.com")).thenReturn(null); // Return NULL
                })) {

            servlet.doPost(request, response);

            // Se dbCart è null, viene inizializzato a new ArrayList()
            // Il codice non deve esplodere e deve settare un carrello
            verify(session).setAttribute(eq("cart"), anyList());
        }
    }
}
