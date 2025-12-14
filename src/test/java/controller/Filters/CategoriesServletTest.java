package controller.Filters;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Prodotto;
import model.ProdottoDAO;
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
 * Classe di test per CategoriesServlet.
 * Testa la faglia (NPE), i rami logici ("tutto" e categoria) e la gestione
 * eccezioni.
 */
public class CategoriesServletTest {

    private CategoriesServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;
    private ServletContext servletContext;

    @BeforeEach
    void setup() throws ServletException {
        // Use spy to allow mocking internal calls (like doGet from doPost)
        servlet = spy(new CategoriesServlet());

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        // Mock configuration for log() support
        ServletConfig config = mock(ServletConfig.class);
        servletContext = mock(ServletContext.class);
        when(config.getServletContext()).thenReturn(servletContext);
        when(config.getServletName()).thenReturn("CategoriesServlet");
        servlet.init(config);

        // Stub di base
        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher("FilterProducts.jsp")).thenReturn(dispatcher);
    }

    // --- Test 1: Delega doPost ---

    @Test
    @DisplayName("doPost deve delegare a doGet")
    void doPost_delegatesToDoGet() throws ServletException, IOException {
        // Since 'servlet' is already a spy, we can verify calling it directly
        // But to verify delegation without executing doGet logic (which might crash on
        // mocks),
        // we usually suppress doGet.
        // However, we want to test that doPost CALLS doGet.

        // Suppress doGet execution to verify delegation only
        doNothing().when(servlet).doGet(any(), any());

        servlet.doPost(request, response);
        verify(servlet).doGet(request, response);
    }

    // --- Test 2: FAGLIA (NullPointerException) ---

    @Test
    @DisplayName("doGet con 'category' null viene gestito (default a 'tutto' e chiama doRetrieveAll)")
    void doGet_nullCategory_isHandledSafely() throws ServletException, IOException {
        // Simula il parametro mancante (null)
        when(request.getParameter("category")).thenReturn(null);

        List<Prodotto> emptyList = new ArrayList<>(); // Lista fittizia

        try (MockedConstruction<ProdottoDAO> mockedDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            // Stub: Ora ci aspettiamo che il DAO chiami doRetrieveAll perché il default è
            // "tutto"
            when(mock.doRetrieveAll()).thenReturn(emptyList);
        })) {

            // 1. Eseguiamo la servlet
            assertDoesNotThrow(() -> {
                servlet.doGet(request, response);
            });

            // 2. Verifichiamo il nuovo percorso
            ProdottoDAO dao = mockedDao.constructed().get(0);

            // CORREZIONE: Verifica che sia stato chiamato doRetrieveAll()
            verify(dao).doRetrieveAll();

            // CORREZIONE: Verifica che NON sia stato chiamato il metodo con i criteri
            verify(dao, never()).doRetrieveByCriteria(anyString(), any());

            // CORREZIONE: Verifica che in sessione finisca il valore di default "tutto",
            // NON null
            verify(session).setAttribute("categoria", "tutto");
            verify(session).setAttribute("categoriaRecovery", "tutto");

            verify(request).setAttribute("originalProducts", emptyList);

            // Verifica il forward
            verify(dispatcher).forward(request, response);
        }
    }

    // --- Test 3: Happy Path (category = "tutto") ---

    @Test
    @DisplayName("doGet con category='tutto' chiama doRetrieveAll")
    void doGet_categoryTutto_callsDoRetrieveAll() throws ServletException, IOException {
        when(request.getParameter("category")).thenReturn("tutto");

        List<Prodotto> listA = new ArrayList<>(); // Lista fittizia
        listA.add(new Prodotto());

        try (MockedConstruction<ProdottoDAO> mockedDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            // Stub: doRetrieveAll restituisce la nostra lista
            when(mock.doRetrieveAll()).thenReturn(listA);
        })) {

            servlet.doGet(request, response);

            // Verifica che sia stato creato 1 DAO
            assertEquals(1, mockedDao.constructed().size());
            ProdottoDAO dao = mockedDao.constructed().get(0);

            // Verifica che sia stato chiamato il metodo GIUSTO
            verify(dao).doRetrieveAll();
            verify(dao, never()).doRetrieveByCriteria(anyString(), anyString());

            // Verifica pulizia sessione
            verify(session).removeAttribute("products");
            verify(session).removeAttribute("searchBarName");

            // Verifica impostazione attributi
            verify(request).setAttribute("originalProducts", listA);
            verify(session).setAttribute("categoria", "tutto");
            verify(session).setAttribute("categoriaRecovery", "tutto");
            verify(session).setAttribute("filteredProducts", listA);

            // Verifica forward
            verify(dispatcher).forward(request, response);
        }
    }

    // --- Test 4: Happy Path (category = "proteine") ---

    @Test
    @DisplayName("doGet con category='proteine' chiama doRetrieveByCriteria")
    void doGet_categoryProteine_callsDoRetrieveByCriteria() throws ServletException, IOException {
        when(request.getParameter("category")).thenReturn("proteine");

        List<Prodotto> listB = new ArrayList<>(); // Lista fittizia
        listB.add(new Prodotto());

        try (MockedConstruction<ProdottoDAO> mockedDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            // Stub: doRetrieveByCriteria restituisce la nostra lista
            when(mock.doRetrieveByCriteria("categoria", "proteine")).thenReturn(listB);
        })) {

            servlet.doGet(request, response);

            // Verifica che sia stato creato 1 DAO
            assertEquals(1, mockedDao.constructed().size());
            ProdottoDAO dao = mockedDao.constructed().get(0);

            // Verifica che sia stato chiamato il metodo GIUSTO
            verify(dao).doRetrieveByCriteria("categoria", "proteine");
            verify(dao, never()).doRetrieveAll();

            // Verifica pulizia sessione
            verify(session).removeAttribute("products");
            verify(session).removeAttribute("searchBarName");

            // Verifica impostazione attributi
            verify(request).setAttribute("originalProducts", listB);
            verify(session).setAttribute("categoria", "proteine");
            verify(session).setAttribute("categoriaRecovery", "proteine");
            verify(session).setAttribute("filteredProducts", listB);

            // Verifica forward
            verify(dispatcher).forward(request, response);
        }
    }

    // --- EXCEPTION HANDLING TESTS ---

    @Test
    @DisplayName("doGet throws Exception -> Logs and sends 500")
    void doGet_exception_sendsError500() throws ServletException, IOException {
        // Trigger checked exception caught by servlet
        doThrow(new ServletException("Crash doGet")).when(dispatcher).forward(any(), any());

        servlet.doGet(request, response);

        verify(servletContext).log(eq("CategoriesServlet: Errore in CategoriesServlet doGet"), any(Exception.class));
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Errore interno durante il recupero delle categorie.");
    }

    @Test
    @DisplayName("doGet throws Exception but committed -> Logs but NO 500")
    void doGet_exception_committed_doesNotSendError() throws ServletException, IOException {
        when(response.isCommitted()).thenReturn(true);
        doThrow(new ServletException("Crash doGet")).when(dispatcher).forward(any(), any());

        servlet.doGet(request, response);

        verify(servletContext).log(eq("CategoriesServlet: Errore in CategoriesServlet doGet"), any(Exception.class));
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    @DisplayName("doPost throws Exception (via doGet) -> Logs and sends 500")
    void doPost_exception_sendsError500() throws ServletException, IOException {
        // Stub doGet to throw exception
        doThrow(new ServletException("Crash via doGet")).when(servlet).doGet(any(), any());

        servlet.doPost(request, response);

        verify(servletContext).log(eq("CategoriesServlet: Errore in CategoriesServlet doPost"), any(Exception.class));
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
    }

    @Test
    @DisplayName("doPost throws Exception but committed -> Logs but NO 500")
    void doPost_exception_committed_doesNotSendError() throws ServletException, IOException {
        when(response.isCommitted()).thenReturn(true);
        doThrow(new ServletException("Crash via doGet")).when(servlet).doGet(any(), any());

        servlet.doPost(request, response);

        verify(servletContext).log(eq("CategoriesServlet: Errore in CategoriesServlet doPost"), any(Exception.class));
        verify(response, never()).sendError(anyInt(), anyString());
    }
}