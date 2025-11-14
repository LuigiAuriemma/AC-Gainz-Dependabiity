package controller.Filters;

import jakarta.servlet.RequestDispatcher;
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
import static org.mockito.Mockito.*;

/**
 * Classe di test (aggiornata) per ShowTasteServlet.
 * Verifica che la servlet gestisca correttamente i 'null'
 * e i percorsi di successo.
 */
public class ShowTasteServletTest {

    private ShowTasteServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    // Per catturare l'output JSON
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setup() throws IOException {
        servlet = new ShowTasteServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);

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
        ShowTasteServlet spyServlet = spy(new ShowTasteServlet());
        // Disattiviamo il doGet reale per testare solo la delega
        doNothing().when(spyServlet).doGet(any(HttpServletRequest.class), any(HttpServletResponse.class));

        spyServlet.doPost(request, response);

        verify(spyServlet).doGet(request, response);
    }

    // --- Test 2: Casi Gestiti (Input vuoti) ---

    @Test
    @DisplayName("doGet con 'filteredProducts' nullo in sessione -> Restituisce []")
    void doGet_nullProductsInSession_returnsEmptyJson() throws ServletException, IOException {
        // Simula la sessione che non ha l'attributo
        when(session.getAttribute("filteredProducts")).thenReturn(null);

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            // Stub: Il DAO viene chiamato con una lista vuota e restituisce vuoto
            when(mock.doRetrieveVariantiByProdotti(any(List.class))).thenReturn(new ArrayList<>());
        })) {

            servlet.doGet(request, response);

            // Verifica che il DAO sia stato chiamato con una lista vuota
            ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
            verify(dao.constructed().get(0)).doRetrieveVariantiByProdotti(captor.capture());
            assertTrue(captor.getValue().isEmpty());

            // Verifica che l'output sia un array JSON vuoto
            assertEquals("[]", getJsonOutput());
        }
    }

    @Test
    @DisplayName("doGet con 'filteredProducts' vuoto in sessione -> Restituisce []")
    void doGet_emptyProductsInSession_returnsEmptyJson() throws ServletException, IOException {
        // Simula la sessione che ha una lista vuota
        when(session.getAttribute("filteredProducts")).thenReturn(new ArrayList<Prodotto>());

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveVariantiByProdotti(any(List.class))).thenReturn(new ArrayList<>());
        })) {

            servlet.doGet(request, response);

            // Verifica che il DAO sia stato chiamato con una lista vuota
            ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
            verify(dao.constructed().get(0)).doRetrieveVariantiByProdotti(captor.capture());
            assertTrue(captor.getValue().isEmpty());

            // Verifica che l'output sia un array JSON vuoto
            assertEquals("[]", getJsonOutput());
        }
    }


    // --- Test 3: Verifica della Correzione della Faglia ---

    @Test
    @DisplayName("(CORRETTO) DAO restituisce 'varianti' null -> Gestito e Restituisce []")
    void doGet_nullVariantiFromDAO_isHandledSafely() throws ServletException, IOException {
        // La sessione ha prodotti validi
        List<Prodotto> productList = List.of(new Prodotto());
        when(session.getAttribute("filteredProducts")).thenReturn(productList);

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            // Ma il DAO restituisce null (simula errore DB o altro)
            when(mock.doRetrieveVariantiByProdotti(productList)).thenReturn(null);
        })) {

            // 1. Verifica che NON ci sia nessun crash
            assertDoesNotThrow(() -> {
                servlet.doGet(request, response);
            });

            // 2. Verifica che l'output sia un array JSON vuoto
            assertEquals("[]", getJsonOutput());
        }
    }


    // --- Test 4: Happy Path (Conteggio) ---

    @Test
    @DisplayName("(Happy Path) Conta e formatta i gusti correttamente")
    void doGet_happyPath_returnsTasteCounts() throws ServletException, IOException {
        // La sessione ha prodotti
        List<Prodotto> productList = List.of(new Prodotto());
        when(session.getAttribute("filteredProducts")).thenReturn(productList);

        // Prepariamo i dati mock
        Variante v1 = new Variante(); v1.setGusto("Cioccolato");
        Variante v2 = new Variante(); v2.setGusto("Vaniglia");
        Variante v3 = new Variante(); v3.setGusto("Cioccolato"); // Duplicato
        List<Variante> variantiFromDB = List.of(v1, v2, v3);

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            // Il DAO restituisce la lista con 3 varianti
            when(mock.doRetrieveVariantiByProdotti(productList)).thenReturn(variantiFromDB);
        })) {

            servlet.doGet(request, response);

            String json = getJsonOutput();

            // L'ordine in un JSONArray creato da un HashMap non è garantito,
            // quindi verifichiamo solo che le stringhe corrette siano presenti.
            assertTrue(json.contains("\"Cioccolato (2)\""));
            assertTrue(json.contains("\"Vaniglia (1)\""));
        }
    }
}