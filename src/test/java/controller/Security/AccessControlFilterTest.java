package controller.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * Classe di test per AccessControlFilter.
 * Testa i tre scenari principali: Utente Guest, Utente Loggato (non admin) e
 * Utente Admin.
 */
public class AccessControlFilterTest {

    private AccessControlFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private FilterChain chain;

    // Definiamo un context path fittizio per i test
    private final String CONTEXT_PATH = "/progetto-test";

    @BeforeEach
    void setup() {
        // Creiamo i mock per ogni test
        filter = new AccessControlFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        chain = mock(FilterChain.class);

        // Stub di base: ogni test ha bisogno di una sessione e di un context path
        when(request.getSession()).thenReturn(session);
        when(request.getContextPath()).thenReturn(CONTEXT_PATH);
    }

    /**
     * Helper per verificare che la richiesta sia stata reindirizzata.
     * Verifica che sendRedirect sia stato chiamato e che la catena sia stata
     * interrotta.
     */
    private void assertRedirectedTo(String targetPath) throws IOException, ServletException {
        // Verifica che il redirect sia avvenuto all'URL completo
        verify(response).sendRedirect(CONTEXT_PATH + targetPath);
        // VERIFICA CHIAVE: la richiesta non deve proseguire (non deve chiamare il
        // filtro successivo)
        verify(chain, never()).doFilter(request, response);
    }

    /**
     * Helper per verificare che la richiesta sia passata.
     * Verifica che la catena sia stata invocata e che non ci siano stati redirect.
     */
    private void assertPassedThrough() throws IOException, ServletException {
        // VERIFICA CHIAVE: la richiesta deve proseguire
        verify(chain).doFilter(request, response);
        // Verifica che non sia avvenuto alcun redirect
        verify(response, never()).sendRedirect(anyString());
    }

    /**
     * Simula un utente Guest (non loggato).
     * l'attributo "Utente" in sessione è null.
     */
    @Nested
    @DisplayName("👤 Test come Utente GUEST (non loggato)")
    class GuestTests {

        @BeforeEach
        void setupGuest() {
            // L'utente non è in sessione
            when(session.getAttribute("Utente")).thenReturn(null);
        }

        @Test
        @DisplayName("Vede /index.jsp (Passa)")
        void guest_index_passes() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/index.jsp");
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        @DisplayName("Vede /Login.jsp (Passa)")
        void guest_login_passes() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/Login.jsp");
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        @DisplayName("Vede /Registrazione.jsp (Passa)")
        void guest_registrazione_passes() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/Registrazione.jsp");
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        @DisplayName("Vede /AreaUtente.jsp -> Redirect a Login")
        void guest_areaUtente_redirectsToLogin() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/AreaUtente.jsp");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/Login.jsp");
        }

        @Test
        @DisplayName("Vede /Ordine.jsp -> Redirect a Index")
        void guest_ordine_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/Ordine.jsp");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /logOut -> Redirect a Index")
        void guest_logOut_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/logOut");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /admin/page -> Redirect a Index")
        void guest_adminPage_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/admin/page");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /showTable -> Redirect a Index")
        void guest_showTable_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/showTable");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /deleteRow -> Redirect a Index")
        void guest_deleteRow_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/deleteRow");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /editRow -> Redirect a Index")
        void guest_editRow_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/editRow");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /insertRow -> Redirect a Index")
        void guest_insertRow_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/insertRow");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /showRowForm -> Redirect a Index")
        void guest_showRowForm_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/showRowForm");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }
    }

    /**
     * Simula un utente standard (loggato, non admin).
     * L'utente è in sessione e utente.getPoteri() == false.
     */
    @Nested
    @DisplayName("🧑 Test come UTENTE (loggato, non admin)")
    class UserTests {

        @BeforeEach
        void setupUser() {
            // Mockiamo l'utente per non dipendere dalla classe reale
            Utente mockUser = mock(Utente.class);
            when(mockUser.getPoteri()).thenReturn(false); // Non è admin
            when(session.getAttribute("Utente")).thenReturn(mockUser);
        }

        @Test
        @DisplayName("Vede /AreaUtente.jsp (Passa)")
        void user_areaUtente_passes() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/AreaUtente.jsp");
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        @DisplayName("Vede /Ordine.jsp (Passa)")
        void user_ordine_passes() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/Ordine.jsp");
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        @DisplayName("Vede /logOut (Passa)")
        void user_logOut_passes() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/logOut");
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        @DisplayName("Vede /Login.jsp -> Redirect a Index")
        void user_login_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/Login.jsp");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /Registrazione.jsp -> Redirect a Index")
        void user_registrazione_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/Registrazione.jsp");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /admin/page -> Redirect a Index")
        void user_adminPage_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/admin/page");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /deleteRow -> Redirect a Index")
        void user_deleteRow_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/deleteRow");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /showTable -> Redirect a Index")
        void user_showTable_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/showTable");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /editRow -> Redirect a Index")
        void user_editRow_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/editRow");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /insertRow -> Redirect a Index")
        void user_insertRow_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/insertRow");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }

        @Test
        @DisplayName("Vede /showRowForm -> Redirect a Index")
        void user_showRowForm_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/showRowForm");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }
    }

    /**
     * Simula un utente ADMIN (loggato, admin).
     * L'utente è in sessione e utente.getPoteri() == true.
     */
    @Nested
    @DisplayName("👩‍💼 Test come ADMIN (loggato, admin)")
    class AdminTests {

        @BeforeEach
        void setupAdmin() {
            // Mockiamo l'utente
            Utente mockAdmin = mock(Utente.class);
            when(mockAdmin.getPoteri()).thenReturn(true); // È admin
            when(session.getAttribute("Utente")).thenReturn(mockAdmin);
        }

        @Test
        @DisplayName("Vede /admin/page (Passa)")
        void admin_adminPage_passes() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/admin/page");
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        @DisplayName("Vede /insertRow (Passa)")
        void admin_insertRow_passes() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/insertRow");
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        @DisplayName("Vede /AreaUtente.jsp (Passa)")
        void admin_areaUtente_passes() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/AreaUtente.jsp");
            filter.doFilter(request, response, chain);
            assertPassedThrough();
        }

        @Test
        @DisplayName("Vede /Login.jsp -> Redirect a Index")
        void admin_login_redirectsToIndex() throws IOException, ServletException {
            when(request.getServletPath()).thenReturn("/Login.jsp");
            filter.doFilter(request, response, chain);
            assertRedirectedTo("/index.jsp");
        }
    }
}