package controller.utente;

import jakarta.servlet.RequestDispatcher;
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

    @BeforeEach
    void setup() {
        servlet = new LoginServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        // usa un dispatcher unico per QUALSIASI JSP (Login.jsp, index.jsp, ecc.)
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        when(request.getSession()).thenReturn(session);

        // parametri "validi" di default
        when(request.getParameter("email")).thenReturn("user@example.com");
        when(request.getParameter("password")).thenReturn("Password1!");
    }

    @Test
    @DisplayName("Email non valida → forward a Login.jsp con attribute patternEmail")
    void invalidEmail_forwardsWithError() throws Exception {
        when(request.getParameter("email")).thenReturn("not-an-email");

        servlet.doPost(request, response);

        verify(request).setAttribute("patternEmail", "Pattern email non rispettato!");
        verify(request).getRequestDispatcher("Login.jsp");
        verify(dispatcher).forward(request, response);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("Email non trovata → forward a Login.jsp con attribute userNotFound")
    void emailNotFound_forwardsWithError() throws Exception {
        try (MockedConstruction<UtenteDAO> mockUtenteDAO = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(null);
        })) {
            servlet.doPost(request, response);

            verify(request).setAttribute("userNotFound", "Utente non registrato!");
            verify(request).getRequestDispatcher("Login.jsp");
            verify(dispatcher).forward(request, response);
            verify(response, never()).sendRedirect(anyString());
        }
    }

    @Test
    @DisplayName("Password errata → forward a Login.jsp con attribute wrongPassword")
    void wrongPassword_forwardsWithError() throws Exception {
        Utente dbUser = new Utente();
        dbUser.setEmail("user@example.com");

        try (MockedConstruction<UtenteDAO> mockUtenteDAO = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(dbUser);
            // la servlet poi chiama doRetrieveByEmailAndPassword: ritorna null per simulare pwd errata
            when(mock.doRetrieveByEmailAndPassword("user@example.com", "Password1!")).thenReturn(null);
        })) {
            servlet.doPost(request, response);

            verify(request).setAttribute("wrongPassword", "Password errata!");
            verify(request).getRequestDispatcher("Login.jsp");
            verify(dispatcher).forward(request, response);
            verify(response, never()).sendRedirect(anyString());
        }
    }

    @Test
    @DisplayName("Login corretto → sessione Utente, merge carrello e forward a index.jsp")
    void correctLogin_forwardsToIndex_andSetsSessionAndCart() throws Exception {
        // utente trovato e password corretta
        Utente x = new Utente();
        x.setEmail("user@example.com");

        List<Carrello> dbCart = new ArrayList<>();  // carrello DB
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
}
