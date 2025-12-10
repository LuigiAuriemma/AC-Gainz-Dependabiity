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
 * Verifica che la servlet gestisca correttamente i prodotti senza varianti.
 */
public class SearchBarServletTest {

    private SearchBarServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    // Per catturare l'output JSON
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setup() throws Exception {
        servlet = new SearchBarServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);

        // Mock ServletConfig e ServletContext per permettere il logging
        ServletConfig servletConfig = mock(ServletConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        
        // Inizializza il servlet con il config mockato
        servlet.init(servletConfig);

        // Prepariamo un writer in memoria per catturare l'output JSON
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);

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

    // --- Test 1: Generali ---

    @Test
    @DisplayName("doPost deve delegare a doGet")
    void doPost_delegatesToDoGet() throws ServletException, IOException {
        SearchBarServlet spyServlet = spy(new SearchBarServlet());
        // Disattiviamo il doGet reale per testare solo la delega
        doNothing().when(spyServlet).doGet(any(HttpServletRequest.class), any(HttpServletResponse.class));

        spyServlet.doPost(request, response);

        verify(spyServlet).doGet(request, response);
    }

    // --- Test 2: Verifica della Correzione della Faglia ---

    @Test
    @DisplayName("Ignora prodotti senza varianti nell'output JSON")
    void search_skipsProductsWithoutVariants() throws ServletException, IOException, SQLException {
        when(request.getParameter("name")).thenReturn("Tutto"); // Attiva il ramo 1

        // Prodotto Buono (ha varianti)
        Prodotto pBuono = new Prodotto();
        pBuono.setNome("Prodotto Buono");
        pBuono.setImmagine("img1.png"); // Aggiungi tutti i campi necessari per getJsonObject
        pBuono.setCategoria("Cat1");
        Variante vBuona = new Variante(); vBuona.setIdVariante(1);
        pBuono.setVarianti(List.of(vBuona));

        // Prodotto Fallato (varianti vuote)
        Prodotto pFallato = new Prodotto();
        pFallato.setNome("Prodotto Fallato");
        pFallato.setVarianti(new ArrayList<>()); // LISTA VUOTA

        List<Prodotto> productList = List.of(pBuono, pFallato);

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            // Il DAO restituisce entrambi i prodotti
            when(mock.filterProducts(any(), any(), any(), any(), any())).thenReturn(productList);
        })) {

            // Verifica che NON ci sia nessun crash
            assertDoesNotThrow(() -> {
                servlet.doGet(request, response);
            });

            String json = getJsonOutput();

            // Verifica che il prodotto buono ci sia
            assertTrue(json.contains("\"nome\":\"Prodotto Buono\""));
            // Verifica che il prodotto fallato (che restituiva null) sia stato ignorato
            assertFalse(json.contains("\"nome\":\"Prodotto Fallato\""));
        }
    }


    // --- Test 3: Ramo 1 (Ricerca per Nome) ---

    @Test
    @DisplayName("Ramo 1 (Happy Path): Ricerca per Nome")
    void searchByName_happyPath() throws ServletException, IOException, SQLException {
        when(request.getParameter("name")).thenReturn("Whey");

        // Setup mock product (deve avere varianti per passare il test!)
        Prodotto p1 = new Prodotto(); p1.setNome("Whey Gold");
        p1.setImmagine("img.png"); p1.setCategoria("Prot");
        Variante v1 = new Variante(); v1.setIdVariante(1);
        p1.setVarianti(List.of(v1));
        List<Prodotto> productList = List.of(p1);

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.filterProducts("", "", "", "", "Whey")).thenReturn(productList);
        })) {

            servlet.doGet(request, response);

            // Verifiche
            verify(dao.constructed().get(0)).filterProducts("", "", "", "", "Whey");
            verify(session).removeAttribute("categoria");
            verify(session).setAttribute("searchBarName", "Whey");
            verify(session).setAttribute("filteredProducts", productList);

            String json = getJsonOutput();
            assertTrue(json.contains("\"nome\":\"Whey Gold\""));
        }
    }

    @Test
    @DisplayName("Ramo 1 (Faglia): Ricerca per Nome con SQLException invia errore")
    void searchByName_sqlException_sendsError() throws SQLException, ServletException, IOException {
        when(request.getParameter("name")).thenReturn("Whey");

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.filterProducts(any(), any(), any(), any(), any())).thenThrow(new SQLException("DB Fail"));
        })) {

            servlet.doGet(request, response);

            // Verifica che sia stato inviato un errore INTERNAL_SERVER_ERROR
            verify(response).sendError(eq(500), anyString());
        }
    }


    // --- Test 4: Ramo 2 (Ricerca per Categoria) ---

    @Test
    @DisplayName("Ramo 2 (Happy Path): Ricerca per Categoria (name=null)")
    void searchByCategory_nameNull_happyPath() throws ServletException, IOException, SQLException {
        when(request.getParameter("name")).thenReturn(null);
        when(session.getAttribute("categoriaRecovery")).thenReturn("Integratori");

        // Setup mock product
        Prodotto p1 = new Prodotto(); p1.setNome("Creatina");
        p1.setImmagine("img.png"); p1.setCategoria("Int");
        Variante v1 = new Variante(); v1.setIdVariante(1);
        p1.setVarianti(List.of(v1));
        List<Prodotto> productList = List.of(p1);

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.filterProducts("Integratori", "", "", "", "")).thenReturn(productList);
        })) {

            servlet.doGet(request, response);

            // Verifiche
            verify(dao.constructed().get(0)).filterProducts("Integratori", "", "", "", "");
            verify(session).removeAttribute("searchBarName");
            verify(session).setAttribute("categoria", "Integratori");

            String json = getJsonOutput();
            assertTrue(json.contains("\"nome\":\"Creatina\""));
        }
    }

    @Test
    @DisplayName("Ramo 2 (Happy Path): Ricerca per Categoria (name=\"\")")
    void searchByCategory_nameEmpty_happyPath() throws ServletException, IOException, SQLException {
        when(request.getParameter("name")).thenReturn(""); // Rientra nel ramo "else"
        when(session.getAttribute("categoriaRecovery")).thenReturn("Proteine");

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.filterProducts("Proteine", "", "", "", "")).thenReturn(new ArrayList<>());
        })) {

            servlet.doGet(request, response);

            // Verifiche
            verify(dao.constructed().get(0)).filterProducts("Proteine", "", "", "", "");
            verify(session).removeAttribute("searchBarName");
            verify(session).setAttribute("categoria", "Proteine");
        }
    }

    @Test
    @DisplayName("Ramo 2 (Faglia): Ricerca per Categoria con SQLException invia errore")
    void searchByCategory_sqlException_sendsError() throws SQLException, ServletException, IOException {
        when(request.getParameter("name")).thenReturn(null); // Ramo categoria

        try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.filterProducts(any(), any(), any(), any(), any())).thenThrow(new SQLException("DB Fail"));
        })) {

            servlet.doGet(request, response);

            // Verifica che sia stato inviato un errore INTERNAL_SERVER_ERROR
            verify(response).sendError(eq(500), anyString());
        }
    }
}