package controller.Admin;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * Classe di test per AdminServlet.
 * Verifica i 3 scenari: Admin, Utente normale, e Guest (non loggato).
 */
public class AdminServletTest {

    private AdminServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;

    @BeforeEach
    void setup() {
        servlet = new AdminServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        // Stub di base
        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher("WEB-INF/Admin/AreaAdmin.jsp")).thenReturn(dispatcher);
    }

    // --- Test 1: Delega doPost ---

    @Test
    @DisplayName("doPost deve delegare a doGet")
    void doPost_delegatesToDoGet() throws ServletException, IOException {
        AdminServlet spyServlet = spy(new AdminServlet());

        // Disattiviamo il doGet reale per testare solo la delega
        doNothing().when(spyServlet).doGet(any(HttpServletRequest.class), any(HttpServletResponse.class));

        spyServlet.doPost(request, response);

        verify(spyServlet).doGet(request, response);
    }

    // --- Test 2: Happy Path (Admin) ---

    @Test
    @DisplayName("Utente Admin -> Esegue il forward a AreaAdmin.jsp")
    void doGet_adminUser_forwardsToAdminPage() throws ServletException, IOException {
        // Prepara un utente mock che è admin
        Utente adminUtente = mock(Utente.class);
        when(adminUtente.getPoteri()).thenReturn(true);

        // Collega l'utente alla sessione
        when(session.getAttribute("Utente")).thenReturn(adminUtente);

        servlet.doGet(request, response);

        // Verifica che il forward sia avvenuto
        verify(request).getRequestDispatcher("WEB-INF/Admin/AreaAdmin.jsp");
        verify(dispatcher).forward(request, response);
    }

    // --- Test 3: Failure Path (Utente non Admin) ---

    @Test
    @DisplayName("Utente Non-Admin -> Non fa nulla (nessun forward)")
    void doGet_nonAdminUser_doesNothing() throws ServletException, IOException {
        // Prepara un utente mock che NON è admin
        Utente normalUser = mock(Utente.class);
        when(normalUser.getPoteri()).thenReturn(false);

        // Collega l'utente alla sessione
        when(session.getAttribute("Utente")).thenReturn(normalUser);

        servlet.doGet(request, response);

        // Verifica che il forward NON sia avvenuto
        verify(request, never()).getRequestDispatcher(anyString());
        verify(dispatcher, never()).forward(request, response);
    }

    // --- Test 4: Failure Path (Guest) ---

    @Test
    @DisplayName("Utente Non Loggato (Guest) -> Non fa nulla (nessun forward)")
    void doGet_guestUser_doesNothing() throws ServletException, IOException {
        // Simula un utente non loggato
        when(session.getAttribute("Utente")).thenReturn(null);

        servlet.doGet(request, response);

        // Verifica che il forward NON sia avvenuto
        verify(request, never()).getRequestDispatcher(anyString());
        verify(dispatcher, never()).forward(request, response);
    }
}