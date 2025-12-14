package controller.Filters;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Prodotto;
import model.ProdottoDAO;
import model.Variante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Classe di test (aggiornata) per SearchBarServlet.
 * Verifica che la servlet gestisca correttamente i prodotti senza varianti,
 * il logging, il flush del writer e le eccezioni.
 */
public class SearchBarServletTest {

    private SearchBarServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private ServletContext servletContext;

    // Per catturare l'output JSON
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setup() throws Exception {
        servlet = spy(new SearchBarServlet());
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);

        // Mock ServletConfig e ServletContext per permettere il logging
        ServletConfig servletConfig = mock(ServletConfig.class);
        servletContext = mock(ServletContext.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletConfig.getServletName()).thenReturn("SearchBarServlet");

        // Inizializza il servlet con il config mockato
        servlet.init(servletConfig);

        // Prepariamo un writer in memoria per catturare l'output JSON
        // SPY per verificare flush()
        stringWriter = new StringWriter();
        printWriter = spy(new PrintWriter(stringWriter));

        // Stub di base
        when(request.getSession()).thenReturn(session);
        when(response.getWriter()).thenReturn(printWriter);
    }

    /**
     * Helper per ottenere l'output JSON catturato.
     */
    private String getJsonOutput() {
        printWriter.flush();
        return stringWriter.toString().trim();
    }

    // --- Test 1: Generali & doPost ---

    @Test
    @DisplayName("doPost deve delegare a doGet")
    void doPost_delegatesToDoGet() throws ServletException, IOException {
        // Disattiviamo il doGet reale per testare solo la delega
        doNothing().when(servlet).doGet(any(HttpServletRequest.class), any(HttpServletResponse.class));

        servlet.doPost(request, response);

        verify(servlet).doGet(request, response);
    }

    @Test
    @DisplayName("doPost exceptions are caught and logged")
    void doPost_exception_sendsError500() throws ServletException, IOException {
        // Force doGet failure
        doThrow(new ServletException("Crash doGet")).when(servlet).doGet(any(), any());

        servlet.doPost(request, response);

        // Kill NO_COVERAGE mutant in doPost catch
        verify(servletContext).log(eq("SearchBarServlet: Errore in SearchBarServlet doPost"), any(Exception.class));
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
    }

    @Test
    @DisplayName("doPost with committed response -> No error 500")
    void doPost_exception_committed_doesNotSendError() throws ServletException, IOException {
        when(response.isCommitted()).thenReturn(true);
        doThrow(new ServletException("Crash doGet")).when(servlet).doGet(any(), any());

        servlet.doPost(request, response);

        verify(servletContext).log(anyString(), any(Exception.class));
        verify(response, never()).sendError(anyInt(), anyString());
    }

    // --- Test 2: Verifica della Correzione della Faglia (Varianti) ---

    @Test
    @DisplayName("Ignora prodotti senza varianti nell'output JSON")
    void search_skipsProductsWithoutVariants() throws ServletException, IOException, SQLException {
        when(request.getParameter("name")).thenReturn("Tutto"); // Attiva il ramo 1

        // Prodotto Buono (ha varianti)
        Prodotto pBuono = new Prodotto();
        pBuono.setNome("Prodotto Buono");
        pBuono.setImmagine("img1.png");
        pBuono.setCategoria("Cat1");
        Variante vBuona = new Variante();
        vBuona.setIdVariante(1);
        pBuono.setVarianti(List.of(vBuona));

        // Prodotto Fallato (varianti vuote)
        Prodotto pFallato = new Prodotto();
        pFallato.setNome("Prodotto Fallato");
        pFallato.setVarianti(new ArrayList<>()); // LISTA VUOTA

        List<Prodotto> productList = List.of(pBuono, pFallato);

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.filterProducts(any(), any(), any(), any(), any())).thenReturn(productList);
        })) {

            assertDoesNotThrow(() -> {
                servlet.doGet(request, response);
            });

            String json = getJsonOutput();

            assertTrue(json.contains("\"nome\":\"Prodotto Buono\""));
            assertFalse(json.contains("\"nome\":\"Prodotto Fallato\""));
        }
    }

    // --- Test 3: Ramo 1 (Ricerca per Nome) ---

    @Test
    @DisplayName("Ramo 1 (Happy Path): Ricerca per Nome -> JSON, ContentType e Flush")
    void searchByName_happyPath() throws ServletException, IOException, SQLException {
        when(request.getParameter("name")).thenReturn("Whey");

        Prodotto p1 = new Prodotto();
        p1.setNome("Whey Gold");
        p1.setImmagine("img.png");
        p1.setCategoria("Prot");
        Variante v1 = new Variante();
        v1.setIdVariante(1);
        p1.setVarianti(List.of(v1));
        List<Prodotto> productList = List.of(p1);

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.filterProducts("", "", "", "", "Whey")).thenReturn(productList);
        })) {

            servlet.doGet(request, response);

            verify(dao.constructed().get(0)).filterProducts("", "", "", "", "Whey");
            verify(session).removeAttribute("categoria");
            verify(session).setAttribute("searchBarName", "Whey");
            verify(session).setAttribute("filteredProducts", productList);

            // KILL MUTANTS: setContentType + flush
            verify(response).setContentType("application/json");
            verify(printWriter).flush();

            String json = getJsonOutput();
            assertTrue(json.contains("\"nome\":\"Whey Gold\""));
        }
    }

    @Test
    @DisplayName("Ramo 1 (Faglia): Ricerca per Nome con SQLException -> Logga e Invia errore")
    void searchByName_sqlException_sendsError() throws SQLException, ServletException, IOException {
        when(request.getParameter("name")).thenReturn("Whey");

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.filterProducts(any(), any(), any(), any(), any())).thenThrow(new SQLException("DB Fail"));
        })) {

            servlet.doGet(request, response);

            // KILL MUTANT: Log verification
            verify(servletContext).log(eq("SearchBarServlet: Errore SQL ricerca per nome"), any(Exception.class));
            verify(response).sendError(eq(500), anyString());
        }
    }

    // --- Test 4: Ramo 2 (Ricerca per Categoria) ---

    @Test
    @DisplayName("Ramo 2 (Happy Path): Ricerca per Categoria -> JSON, ContentType e Flush")
    void searchByCategory_nameNull_happyPath() throws ServletException, IOException, SQLException {
        when(request.getParameter("name")).thenReturn(null);
        when(session.getAttribute("categoriaRecovery")).thenReturn("Integratori");

        Prodotto p1 = new Prodotto();
        p1.setNome("Creatina");
        p1.setImmagine("img.png");
        p1.setCategoria("Int");
        Variante v1 = new Variante();
        v1.setIdVariante(1);
        p1.setVarianti(List.of(v1));
        List<Prodotto> productList = List.of(p1);

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.filterProducts("Integratori", "", "", "", "")).thenReturn(productList);
        })) {

            servlet.doGet(request, response);

            verify(dao.constructed().get(0)).filterProducts("Integratori", "", "", "", "");
            verify(session).removeAttribute("searchBarName");
            verify(session).setAttribute("categoria", "Integratori");

            // KILL MUTANTS: Check flush here too, why not
            verify(response).setContentType("application/json");
            verify(printWriter).flush();

            String json = getJsonOutput();
            assertTrue(json.contains("\"nome\":\"Creatina\""));
        }
    }

    @Test
    @DisplayName("Ramo 2 (Faglia): Ricerca per Categoria con SQLException -> Logga e Invia errore")
    void searchByCategory_sqlException_sendsError() throws SQLException, ServletException, IOException {
        when(request.getParameter("name")).thenReturn(null);

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.filterProducts(any(), any(), any(), any(), any())).thenThrow(new SQLException("DB Fail"));
        })) {

            servlet.doGet(request, response);

            // KILL MUTANT: Log verification
            verify(servletContext).log(eq("SearchBarServlet: Errore SQL ricerca per categoria"), any(Exception.class));
            verify(response).sendError(eq(500), anyString());
        }
    }

    @Test
    @DisplayName("doGet GENERIC Exception (Outer Catch) -> Logs and Sends Error 500")
    void doGet_genericException_sendsError500() throws Exception {
        when(request.getParameter("name")).thenReturn(null); // Goes to Category branch
        when(session.getAttribute("categoriaRecovery")).thenReturn("Integratori");

        // Setup: DAO works, but JSON generation fails (e.g. getWriter throws
        // IOException)
        // This exception is NOT caught by the inner SQL blocks, so it goes to the outer
        // block

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.filterProducts(any(), any(), any(), any(), any())).thenReturn(new ArrayList<>());
        })) {

            // Force IOException during addToJson
            when(response.getWriter()).thenThrow(new IOException("Writer exploded"));

            servlet.doGet(request, response);

            // KILL NO_COVERAGE MUTANT: Outer catch block
            verify(servletContext).log(eq("SearchBarServlet: Errore in SearchBarServlet doGet"), any(Exception.class));
            verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore interno durante la ricerca.");
        }
    }
}