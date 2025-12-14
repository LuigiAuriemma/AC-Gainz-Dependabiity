package controller.Filters;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Prodotto;
import model.Variante;
import model.VarianteDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Classe di test (aggiornata) per ShowTasteServlet.
 * Verifica Happy Path, Flush, ContentType e GESTIONE ECCEZIONI.
 */
public class ShowTasteServletTest {

    private ShowTasteServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private ServletContext servletContext;

    // Per catturare l'output JSON
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setup() throws Exception {
        servlet = spy(new ShowTasteServlet());
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);

        // MOCK ServletContext per i log
        ServletConfig servletConfig = mock(ServletConfig.class);
        servletContext = mock(ServletContext.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);

        servlet.init(servletConfig);

        // Prepariamo un writer in memoria per catturare l'output JSON
        // USIAMO SPY per verificare flush()
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

    // --- Test 1: Generali ---

    @Test
    @DisplayName("doPost deve delegare a doGet")
    void doPost_delegatesToDoGet() throws ServletException, IOException {
        // Disattiviamo il doGet reale per testare solo la delega
        doNothing().when(servlet).doGet(any(HttpServletRequest.class), any(HttpServletResponse.class));

        servlet.doPost(request, response);

        verify(servlet).doGet(request, response);
    }

    // --- Test 2: Casi Gestiti (Input vuoti) ---

    @Test
    @DisplayName("doGet con 'filteredProducts' nullo in sessione -> Restituisce [] e Flush")
    void doGet_nullProductsInSession_returnsEmptyJson() throws ServletException, IOException {
        when(session.getAttribute("filteredProducts")).thenReturn(null);

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveVariantiByProdotti(any(List.class))).thenReturn(new ArrayList<>());
        })) {

            servlet.doGet(request, response);

            ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
            verify(dao.constructed().get(0)).doRetrieveVariantiByProdotti(captor.capture());
            assertTrue(captor.getValue().isEmpty());

            verify(response).setContentType("application/json");
            verify(printWriter).flush();

            assertEquals("[]", getJsonOutput());
        }
    }

    @Test
    @DisplayName("doGet con 'filteredProducts' vuoto in sessione -> Restituisce [] e Flush")
    void doGet_emptyProductsInSession_returnsEmptyJson() throws ServletException, IOException {
        when(session.getAttribute("filteredProducts")).thenReturn(new ArrayList<Prodotto>());

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveVariantiByProdotti(any(List.class))).thenReturn(new ArrayList<>());
        })) {

            servlet.doGet(request, response);

            ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
            verify(dao.constructed().get(0)).doRetrieveVariantiByProdotti(captor.capture());
            assertTrue(captor.getValue().isEmpty());

            verify(response).setContentType("application/json");
            verify(printWriter).flush();

            assertEquals("[]", getJsonOutput());
        }
    }

    // --- Test 3: Verifica della Correzione della Faglia ---

    @Test
    @DisplayName("(CORRETTO) DAO restituisce 'varianti' null -> Gestito e Restituisce []")
    void doGet_nullVariantiFromDAO_isHandledSafely() throws ServletException, IOException {
        List<Prodotto> productList = List.of(new Prodotto());
        when(session.getAttribute("filteredProducts")).thenReturn(productList);

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveVariantiByProdotti(productList)).thenReturn(null);
        })) {

            assertDoesNotThrow(() -> {
                servlet.doGet(request, response);
            });

            verify(response).setContentType("application/json");
            verify(printWriter).flush();

            assertEquals("[]", getJsonOutput());
        }
    }

    // --- Test 4: Happy Path (Conteggio) ---

    @Test
    @DisplayName("(Happy Path) Conta e formatta i gusti correttamente -> JSON, ContentType e Flush")
    void doGet_happyPath_returnsTasteCounts() throws ServletException, IOException {
        List<Prodotto> productList = List.of(new Prodotto());
        when(session.getAttribute("filteredProducts")).thenReturn(productList);

        Variante v1 = new Variante();
        v1.setGusto("Cioccolato");
        Variante v2 = new Variante();
        v2.setGusto("Vaniglia");
        Variante v3 = new Variante();
        v3.setGusto("Cioccolato");
        List<Variante> variantiFromDB = List.of(v1, v2, v3);

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveVariantiByProdotti(productList)).thenReturn(variantiFromDB);
        })) {

            servlet.doGet(request, response);

            verify(response).setContentType("application/json");
            verify(printWriter).flush();

            String json = getJsonOutput();
            assertTrue(json.contains("\"Cioccolato (2)\""));
            assertTrue(json.contains("\"Vaniglia (1)\""));
        }
    }

    // --- Test 5: Exception Handling (Kill No-Coverage Mutants) ---

    @Test
    @DisplayName("doGet Exception -> Logs error and sends 500")
    void doGet_exception_sendsError500() throws IOException, ServletException {
        // Force an exception during execution, e.g. getWriter throws IOException
        when(response.getWriter()).thenThrow(new IOException("Disk full"));

        servlet.doGet(request, response);

        // Verify Logging
        // GenericServlet appends "ServletName: " prefix, so we check using endsWith
        verify(servletContext).log(argThat(msg -> msg != null && msg.endsWith("Errore in ShowTasteServlet doGet")),
                any(Exception.class));
        // Verify Error Response
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Errore interno nel recupero dei gusti.");
    }

    @Test
    @DisplayName("doGet Exception (Response Committed) -> Logs error but NO 500")
    void doGet_exception_committed_doesNotSendError() throws IOException, ServletException {
        when(response.getWriter()).thenThrow(new IOException("Disk full"));
        when(response.isCommitted()).thenReturn(true);

        servlet.doGet(request, response);

        verify(servletContext).log(argThat(msg -> msg != null && msg.endsWith("Errore in ShowTasteServlet doGet")),
                any(Exception.class));
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    @DisplayName("doPost Exception (via doGet) -> Logs error and sends 500")
    void doPost_exception_sendsError500() throws ServletException, IOException {
        // Force doGet to throw exception
        doThrow(new ServletException("Crash via doGet")).when(servlet).doGet(any(), any());

        servlet.doPost(request, response);

        verify(servletContext).log(argThat(msg -> msg != null && msg.endsWith("Errore in ShowTasteServlet doPost")),
                any(Exception.class));
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
    }
}