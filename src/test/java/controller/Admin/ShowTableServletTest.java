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
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Classe di test (aggiornata) per showTableServlet.
 * Verifica che la servlet gestisca 'tableName' nullo
 * e i rami dello switch.
 */
public class ShowTableServletTest {
    private showTableServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;

    // Non mockiamo il dispatcher qui perché il nome cambia
    // in base al 'case' dello switch.

    @BeforeEach
    void setup() {
        servlet = new showTableServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    // --- Test 1: Generali ---

    @Test
    @DisplayName("doPost deve delegare a doGet")
    void doPost_delegatesToDoGet() throws ServletException, IOException {
        showTableServlet spyServlet = spy(new showTableServlet());
        // Disattiviamo il doGet reale per testare solo la delega
        doNothing().when(spyServlet).doGet(any(HttpServletRequest.class), any(HttpServletResponse.class));

        spyServlet.doPost(request, response);

        verify(spyServlet).doGet(request, response);
    }

    // --- Test 2: Verifica della Correzione della Faglia (tableName null) ---

    @Test
    @DisplayName("(CORRETTO) doGet con 'tableName' nullo -> Redirect ad admin")
    void doGet_nullTableName_redirectsToAdmin() throws ServletException, IOException {
        // Simula il parametro mancante
        when(request.getParameter("tableName")).thenReturn(null);

        servlet.doGet(request, response);

        // Verifica che il redirect di sicurezza sia stato chiamato
        verify(response).sendRedirect("admin");
        // Verifica che NESSUN forward sia avvenuto
        verify(request, never()).getRequestDispatcher(anyString());
    }

    // --- Test 3: Verifica della Correzione della Faglia (tableName blank) ---

    @Test
    @DisplayName("(CORRETTO) doGet con 'tableName' vuoto -> Redirect ad admin")
    void doGet_blankTableName_redirectsToAdmin() throws ServletException, IOException {
        // Simula parametro vuoto
        when(request.getParameter("tableName")).thenReturn(""); // .isBlank() copre questo

        servlet.doGet(request, response);

        // Verifica che il redirect di sicurezza sia stato chiamato
        verify(response).sendRedirect("admin");
        verify(request, never()).getRequestDispatcher(anyString());
    }

    // --- Test 4: Happy Path (tableName = "utente") ---

    @Test
    @DisplayName("doGet con tableName='utente' -> Forward a tableUtente.jsp")
    void doGet_tableNameUtente_forwardsToUtente() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("utente");

        List<Utente> listUtenti = new ArrayList<>();
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("WEB-INF/Admin/tableUtente.jsp")).thenReturn(dispatcher);

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveAll()).thenReturn(listUtenti);
        })) {

            servlet.doGet(request, response);

            // Verifica che il DAO corretto sia stato chiamato
            verify(dao.constructed().get(0)).doRetrieveAll();
            // Verifica che l'attributo sia stato impostato
            verify(request).setAttribute("tableUtente", listUtenti);
            // Verifica il forward
            verify(dispatcher).forward(request, response);
            // Verifica che non ci sia stato redirect
            verify(response, never()).sendRedirect(anyString());
        }
    }

    // --- Test 5: Happy Path (tableName = "prodotto") ---

    @Test
    @DisplayName("doGet con tableName='prodotto' -> Forward a tableProdotto.jsp")
    void doGet_tableNameProdotto_forwardsToProdotto() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("prodotto");

        List<Prodotto> listProdotti = new ArrayList<>();
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("WEB-INF/Admin/tableProdotto.jsp")).thenReturn(dispatcher);

        // Dobbiamo mockare anche UtenteDAO? No, perché entra solo nel case "prodotto".
        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveAll()).thenReturn(listProdotti);
        })) {

            servlet.doGet(request, response);

            verify(dao.constructed().get(0)).doRetrieveAll();
            verify(request).setAttribute("tableProdotto", listProdotti);
            verify(dispatcher).forward(request, response);
            verify(response, never()).sendRedirect(anyString());
        }
    }

    // --- Test 6: Sad Path (tableName non valido) ---

    @Test
    @DisplayName("doGet con 'tableName' non valido -> Non fa nulla")
    void doGet_invalidTableName_doesNothing() throws ServletException, IOException {
        when(request.getParameter("tableName")).thenReturn("tabellaSbagliata");

        // Mockiamo un DAO solo per essere sicuri che non venga chiamato
        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class)) {

            servlet.doGet(request, response);

            // Verifica che nessun DAO sia stato creato (nessun case combacia)
            assertEquals(0, dao.constructed().size());
            // Verifica che non ci sia stato forward
            verify(request, never()).getRequestDispatcher(anyString());
            // Verifica che non ci sia stato redirect
            verify(response, never()).sendRedirect(anyString());
        }
    }
}
