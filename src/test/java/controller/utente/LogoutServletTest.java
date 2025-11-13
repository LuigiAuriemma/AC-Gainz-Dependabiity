package controller.utente;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Carrello;
import model.CarrelloDAO;
import model.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests per LogoutServlet (basati esattamente sul codice fornito).
 */
public class LogoutServletTest {

    private LogoutServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;

    @BeforeEach
    void setup() {
        servlet = new LogoutServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        // Default stubs
        when(request.getSession()).thenReturn(session);
        // quando la servlet chiama req.getRequestDispatcher("index.jsp")
        when(request.getRequestDispatcher("index.jsp")).thenReturn(dispatcher);
    }

    @Test
    @DisplayName("Se non c'è utente in sessione la servlet non fa nulla")
    void noUser_doesNothing() throws ServletException, IOException {
        when(session.getAttribute("Utente")).thenReturn(null);

        servlet.doGet(request, response);

        // Non deve invalidare la sessione né fare forward né usare il DAO
        verify(session, never()).invalidate();
        verify(session, never()).removeAttribute(anyString());
        verify(request, never()).getRequestDispatcher("index.jsp");
        verify(dispatcher, never()).forward(request, response);
    }

    @Test
    @DisplayName("Utente presente e carrello null -> rimuove carrello DB, invalida sessione e forward a index.jsp")
    void userWithoutCart_removesCartAndInvalidatesAndForwards() throws Exception {
        // Prepara user
        Utente user = new Utente();
        user.setEmail("user@example.com");
        when(session.getAttribute("Utente")).thenReturn(user);

        // session cart null
        when(session.getAttribute("cart")).thenReturn(null);

        // Intercettiamo new CarrelloDAO()
        try (MockedConstruction<CarrelloDAO> mocked = mockConstruction(CarrelloDAO.class, (mock, ctx) -> {
            // doRemoveCartByUser non deve lanciare eccezioni
            doNothing().when(mock).doRemoveCartByUser("user@example.com");
        })) {
            servlet.doGet(request, response);

            // Il DAO costruito deve aver ricevuto la chiamata per rimuovere il carrello dell'utente
            CarrelloDAO constructedDao = mocked.constructed().get(0);
            verify(constructedDao).doRemoveCartByUser("user@example.com");

            // Non ci sono salvataggi (carrello vuoto)
            verify(constructedDao, never()).doSave(any(Carrello.class));

            // Sessione: rimozione attributo Utente e invalidazione
            verify(session).removeAttribute("Utente");
            verify(session).invalidate();

            // Forward a index.jsp
            verify(request).getRequestDispatcher("index.jsp");
            verify(dispatcher).forward(request, response);
            verify(response, never()).sendRedirect(anyString());
        }
    }

    @Test
    @DisplayName("Utente con carrello -> setEmailUtente su ogni item, doSave per ogni item, poi invalidate e forward")
    void userWithCart_savesCartItems_thenInvalidatesAndForwards() throws Exception {
        // Prepara utente
        Utente user = new Utente();
        user.setEmail("user@example.com");
        when(session.getAttribute("Utente")).thenReturn(user);

        // Prepara carrello in sessione con 2 item
        Carrello item1 = new Carrello();
        Carrello item2 = new Carrello();
        List<Carrello> sessionCart = new ArrayList<>();
        sessionCart.add(item1);
        sessionCart.add(item2);
        when(session.getAttribute("cart")).thenReturn(sessionCart);

        try (MockedConstruction<CarrelloDAO> mocked = mockConstruction(CarrelloDAO.class, (mock, ctx) -> {
            doNothing().when(mock).doRemoveCartByUser("user@example.com");
            // doSave non fa nulla di particolare
        })) {
            servlet.doGet(request, response);

            // DAO costruito
            CarrelloDAO constructedDao = mocked.constructed().get(0);

            // Verifica removeCart chiamato
            verify(constructedDao).doRemoveCartByUser("user@example.com");

            // Catturiamo gli oggetti passati a doSave
            ArgumentCaptor<Carrello> captor = ArgumentCaptor.forClass(Carrello.class);
            verify(constructedDao, times(2)).doSave(captor.capture());
            List<Carrello> saved = captor.getAllValues();

            // Controlliamo che ogni Carrello salvato abbia l'email utente impostata
            for (Carrello c : saved) {
                assertEquals("user@example.com", c.getEmailUtente());
            }

            // Sessione: rimozione attributo Utente e invalidazione
            verify(session).removeAttribute("Utente");
            verify(session).invalidate();

            // Forward a index.jsp
            verify(request).getRequestDispatcher("index.jsp");
            verify(dispatcher).forward(request, response);
            verify(response, never()).sendRedirect(anyString());
        }
    }

    @Test
    @DisplayName("doPost deve chiamare doGet")
    void doPost_callsDoGet() throws ServletException, IOException {
        // Creiamo uno "spy" della servlet.
        // È un oggetto LogoutServlet reale, ma possiamo verificare le chiamate ai suoi metodi.
        LogoutServlet spyServlet = spy(new LogoutServlet());

        // Chiamiamo doPost
        spyServlet.doPost(request, response);

        // Verifichiamo che lo spy abbia chiamato il suo stesso metodo doGet
        // Dobbiamo usare "verify(spyServlet)" e non "verify(servlet)"
        verify(spyServlet).doGet(request, response);
    }
}
