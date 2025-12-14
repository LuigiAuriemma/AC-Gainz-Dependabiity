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
import model.Variante;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * Classe di test per GenericFilterServlet.
 * Testa le due modalità (nameForm/JSP e Filtro/JSON),
 * la sanitizzazione degli input, e la gestione eccezioni.
 */
public class GenericFilterServletTest {

    private GenericFilterServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;
    private ServletContext servletContext;

    // Per catturare l'output JSON e verificare flush
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setup() throws Exception {
        servlet = spy(new GenericFilterServlet());

        // Mock ServletConfig e ServletContext per permettere il logging
        ServletConfig servletConfig = mock(ServletConfig.class);
        servletContext = mock(ServletContext.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletConfig.getServletName()).thenReturn("GenericFilterServlet");

        // Inizializza il servlet con il config mockato
        servlet.init(servletConfig);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        // Prepariamo un writer in memoria per catturare l'output JSON
        // USIAMO SPY per verificare flush()
        stringWriter = new StringWriter();
        printWriter = spy(new PrintWriter(stringWriter));

        // Stub di base
        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher("FilterProducts.jsp")).thenReturn(dispatcher);
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

        verify(servletContext).log(eq("GenericFilterServlet: Errore in GenericFilterServlet doPost"),
                any(Exception.class));
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
    }

    @Test
    @DisplayName("doPost exceptions with already committed response -> No error 500")
    void doPost_exception_committed_doesNotSendError() throws ServletException, IOException {
        when(response.isCommitted()).thenReturn(true);
        doThrow(new ServletException("Crash doGet")).when(servlet).doGet(any(), any());

        servlet.doPost(request, response);

        verify(servletContext).log(anyString(), any(Exception.class));
        verify(response, never()).sendError(anyInt(), anyString());
    }

    // --- Test 2: Test del metodo helper getJsonObject ---

    @Nested
    @DisplayName("Test Metodo Statico: getJsonObject")
    class GetJsonObjectTests {

        @Test
        @DisplayName("(Happy Path) Prodotto con Variante -> Restituisce JSON")
        void getJsonObject_happyPath_returnsJson() {
            Prodotto p = new Prodotto();
            Variante v = new Variante();
            v.setIdVariante(101);
            v.setSconto(20);
            p.setNome("Proteine");
            p.setVarianti(List.of(v));

            JSONObject json = GenericFilterServlet.getJsonObject(p);

            assertNotNull(json);
            assertEquals("Proteine", json.get("nome"));
            assertEquals(20, json.get("sconto"));
        }

        @Test
        @DisplayName("Prodotto con Varianti null (Mocked) -> Restituisce null")
        void getJsonObject_mockedNullVariants_returnsNull() {
            Prodotto p = mock(Prodotto.class);
            when(p.getVarianti()).thenReturn(null);

            JSONObject json = GenericFilterServlet.getJsonObject(p);

            assertNull(json);
        }

        @Test
        @DisplayName("Verifica tutti i campi del JSON")
        void getJsonObject_verifyAllFields() {
            Prodotto p = new Prodotto();
            p.setIdProdotto("1");
            p.setNome("Prodotto Test");
            p.setCategoria("Integratori");
            p.setCalorie(100);
            p.setImmagine("img.jpg");

            Variante v = new Variante();
            v.setIdVariante(10);
            v.setPrezzo(50.0f);
            v.setGusto("Vaniglia");
            v.setPesoConfezione(1000);
            v.setSconto(0);

            p.setVarianti(List.of(v));

            JSONObject json = GenericFilterServlet.getJsonObject(p);

            assertNotNull(json);
            assertEquals("1", json.get("id"));
            assertEquals("Prodotto Test", json.get("nome"));
            assertEquals("Integratori", json.get("categoria"));
            assertEquals(100, json.get("calorie"));
            assertEquals("img.jpg", json.get("immagine"));
            assertEquals(10, json.get("idVariante"));
            assertEquals(50.0f, json.get("prezzo"));
            assertEquals("Vaniglia", json.get("gusto"));
            assertEquals(1000, json.get("peso"));
            assertNull(json.get("sconto"));
        }

        @Test
        @DisplayName("(CORRETTO) Prodotto senza Varianti -> Restituisce null")
        void getJsonObject_noVariant_returnsNull() {
            Prodotto p = new Prodotto();
            p.setNome("Prodotto Fallato");
            p.setVarianti(new ArrayList<>());

            JSONObject json = GenericFilterServlet.getJsonObject(p);

            assertNull(json);
        }
    }

    // --- Test 3: Modalità nameForm (JSP) ---

    @Nested
    @DisplayName("Modalità: nameForm (JSP)")
    class NameFormTests {

        @Test
        @DisplayName("nameForm con valore -> Chiama filterProducts e gestisce attributi sessione")
        void nameForm_withValue_forwardsToJSP() throws ServletException, IOException, SQLException {
            when(request.getParameter("nameForm")).thenReturn("Proteine");
            List<Prodotto> listA = List.of(new Prodotto());

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.filterProducts("", "", "", "", "Proteine")).thenReturn(listA);
            })) {

                servlet.doGet(request, response);

                verify(dao.constructed().get(0)).filterProducts("", "", "", "", "Proteine");

                // VERIFICA MUTANTE: setAttribute filteredProducts
                verify(request).setAttribute("originalProducts", listA);
                verify(session).setAttribute("searchBarName", "Proteine");
                verify(session).setAttribute("filteredProducts", listA);

                verify(dispatcher).forward(request, response);
                assertTrue(getJsonOutput().isEmpty());
            }
        }

        @Test
        @DisplayName("nameForm INPUT INVALIDO -> Sanitizzato e non cerca nel DB")
        void nameForm_invalidInput_isSanitized() throws ServletException, IOException, SQLException {
            // "Droga!!!" contiene '!' che non è nel pattern SAFE_TEXT_PATTERN
            // (a-zA-Z0-9\s\-%]+)
            when(request.getParameter("nameForm")).thenReturn("Droga!!!");

            // Se sanitizzato -> diventa ""

            List<Prodotto> listB = new ArrayList<>();

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveAll()).thenReturn(listB);
            })) {

                servlet.doGet(request, response);

                // Deve comportarsi come se il nome fosse vuoto -> doRetrieveAll
                verify(dao.constructed().get(0)).doRetrieveAll();
                verify(dao.constructed().get(0), never()).filterProducts(any(), any(), any(), any(), any());
            }
        }

        @Test
        @DisplayName("nameForm vuoto -> Chiama doRetrieveAll e fa forward")
        void nameForm_blank_callsDoRetrieveAll() throws ServletException, IOException, SQLException {
            when(request.getParameter("nameForm")).thenReturn("");
            List<Prodotto> listB = List.of(new Prodotto());

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveAll()).thenReturn(listB);
            })) {

                servlet.doGet(request, response);

                verify(dao.constructed().get(0)).doRetrieveAll();
                verify(session).removeAttribute("categoria");
                verify(dispatcher).forward(request, response);
            }
        }

        @Test
        @DisplayName("nameForm lancia SQLException -> Logga e Invia errore 500")
        void nameForm_sqlException_sendsError() throws Exception {
            when(request.getParameter("nameForm")).thenReturn("Proteine");

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.filterProducts(any(), any(), any(), any(), any())).thenThrow(new SQLException("DB Error"));
            })) {

                servlet.doGet(request, response);

                // VERIFICA MUTANTE: log chiamato
                verify(servletContext).log(eq("GenericFilterServlet: Errore in handleNameForm"), any(Exception.class));
                verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
            }
        }
    }

    // --- Test 4: Modalità Filtro AJAX (JSON) ---

    @Nested
    @DisplayName("Modalità: Filtro AJAX (JSON)")
    class AjaxFilterTests {

        @Test
        @DisplayName("Filtro AJAX ignora prodotti senza varianti e imposta ContentType/Flush")
        void ajaxFilter_skipsNullVariantProducts() throws ServletException, IOException, SQLException {
            when(request.getParameter("nameForm")).thenReturn(null);

            Prodotto pBuono = new Prodotto();
            pBuono.setNome("Prodotto Buono");
            pBuono.setVarianti(List.of(new Variante()));

            Prodotto pFallato = new Prodotto();
            pFallato.setNome("Prodotto Fallato");
            pFallato.setVarianti(new ArrayList<>());

            List<Prodotto> listFromDB = List.of(pBuono, pFallato);

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.filterProducts(any(), any(), any(), any(), any())).thenReturn(listFromDB);
            })) {

                servlet.doGet(request, response);

                verify(dispatcher, never()).forward(request, response);

                // VERIFICA MUTANTE: setContentType + flush
                verify(response).setContentType("application/json");
                verify(printWriter).flush();

                String json = getJsonOutput();
                assertTrue(json.contains("\"nome\":\"Prodotto Buono\""));
                assertFalse(json.contains("\"nome\":\"Prodotto Fallato\""));
            }
        }

        @Test
        @DisplayName("getWriter throws IOException -> Logs and Sends Error 500")
        void ajaxFilter_ioException_sendsError() throws Exception {
            when(request.getParameter("nameForm")).thenReturn(null);

            // Stub Exception on getWriter
            when(response.getWriter()).thenThrow(new IOException("Output blocked"));

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.filterProducts(any(), any(), any(), any(), any())).thenReturn(new ArrayList<>());
            })) {

                servlet.doGet(request, response);

                // Verify Log called for sendJsonResponse error
                verify(servletContext).log(eq("GenericFilterServlet: Errore in sendJsonResponse"),
                        any(Exception.class));
                verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Errore durante l'invio della risposta JSON.");
            }
        }

        @Test
        @DisplayName("Filtro AJAX lancia SQLException -> Logga e Invia errore 500")
        void ajaxFilter_sqlException_sendsError() throws Exception {
            when(request.getParameter("nameForm")).thenReturn(null);

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.filterProducts(any(), any(), any(), any(), any())).thenThrow(new SQLException("DB Error"));
            })) {

                servlet.doGet(request, response);

                verify(servletContext).log(eq("GenericFilterServlet: Errore in filterProducts"), any(Exception.class));
                verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
            }
        }

        @Test
        @DisplayName("Filtro AJAX usa parametri sessione e request (con valori in Whitelist)")
        void ajaxFilter_usesSessionAndRequestParameters() throws ServletException, IOException, SQLException {
            when(request.getParameter("nameForm")).thenReturn(null);

            when(session.getAttribute("categoria")).thenReturn("Integratori");
            when(session.getAttribute("searchBarName")).thenReturn("Whey");

            when(request.getParameter("weight")).thenReturn("1kg");
            when(request.getParameter("taste")).thenReturn("Cioccolato");
            when(request.getParameter("sorting")).thenReturn("PriceAsc");

            List<Prodotto> emptyList = new ArrayList<>();

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.filterProducts(anyString(), anyString(), anyString(), anyString(), anyString()))
                        .thenReturn(emptyList);
            })) {

                servlet.doGet(request, response);

                verify(dao.constructed().get(0)).filterProducts(
                        eq("Integratori"),
                        eq("PriceAsc"),
                        eq("1kg"),
                        eq("Cioccolato"),
                        eq("Whey"));

                verify(session).setAttribute("filteredProducts", emptyList);
            }
        }

        @Test
        @DisplayName("Filtro AJAX con parametri vuoti -> Passati come stringhe vuote (Kill Mutant isValidInput)")
        void ajaxFilter_emptyParams_passedAsEmptyString() throws ServletException, IOException, SQLException {
            // Setup: nameForm=null (AJAX mode)
            when(request.getParameter("nameForm")).thenReturn(null);

            // Parametri vuoti (ma non null)
            when(request.getParameter("weight")).thenReturn("");
            when(request.getParameter("taste")).thenReturn("");

            // Session default
            when(session.getAttribute("categoria")).thenReturn(null);
            when(session.getAttribute("searchBarName")).thenReturn(null);

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.filterProducts(any(), any(), any(), any(), any())).thenReturn(new ArrayList<>());
            })) {

                servlet.doGet(request, response);

                // VERIFICA: Il DAO deve ricevere "" e "" (stringhe vuote), NON null.
                // Se il mutante "return true -> return false" in isValidInput è attivo,
                // isValidInput("") ritornerebbe false, causando la logica ternaria
                // (? rawWeight : null) a restituire null.

                verify(dao.constructed().get(0)).filterProducts(
                        isNull(), // category
                        eq("default"), // sorting
                        eq(""), // weight
                        eq(""), // taste
                        eq("") // nameFilter (default "")
                );
            }
        }
    }
}