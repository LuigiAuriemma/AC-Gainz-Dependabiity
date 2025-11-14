package controller.Filters;

import jakarta.servlet.RequestDispatcher;
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
 * Testa la faglia (NPE) e i due rami logici ("tutto" e categoria).
 */
public class CategoriesServletTest {

    private CategoriesServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;

    @BeforeEach
    void setup() {
        servlet = new CategoriesServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        // Stub di base
        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher("FilterProducts.jsp")).thenReturn(dispatcher);
    }

    // --- Test 1: Delega doPost ---

    @Test
    @DisplayName("doPost deve delegare a doGet")
    void doPost_delegatesToDoGet() throws ServletException, IOException {
        CategoriesServlet spyServlet = spy(new CategoriesServlet());

        // Disattiviamo il vero doGet per evitare il NPE
        doNothing().when(spyServlet).doGet(any(HttpServletRequest.class), any(HttpServletResponse.class));

        spyServlet.doPost(request, response);
        verify(spyServlet).doGet(request, response);
    }

    // --- Test 2: FAGLIA (NullPointerException) ---

    @Test
    @DisplayName("doGet con 'category' null viene gestito (chiama criteria con null)")
    void doGet_nullCategory_isHandledSafely() throws ServletException, IOException {
        // Simula il parametro mancante
        when(request.getParameter("category")).thenReturn(null);

        List<Prodotto> emptyList = new ArrayList<>(); // Lista fittizia

        try (MockedConstruction<ProdottoDAO> mockedDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            // Stub: ci aspettiamo che il DAO venga chiamato con 'null'
            when(mock.doRetrieveByCriteria("categoria", null)).thenReturn(emptyList);
        })) {

            // 1. Verifichiamo che NON ci sia nessun crash
            assertDoesNotThrow(() -> {
                servlet.doGet(request, response);
            });

            // 2. Verifichiamo il nuovo percorso
            ProdottoDAO dao = mockedDao.constructed().get(0);

            // Verifica che sia stato chiamato il metodo GIUSTO (il ramo 'else')
            verify(dao).doRetrieveByCriteria("categoria", null);
            verify(dao, never()).doRetrieveAll();

            // Verifica che gli attributi siano stati impostati (anche se con null)
            verify(session).setAttribute("categoria", null);
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
}