package controller.Filters;

import jakarta.servlet.RequestDispatcher;
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
import static org.mockito.Mockito.*;

/**
 * Classe di test per GenericFilterServlet.
 * Testa le due modalità (nameForm/JSP e Filtro/JSON)
 * e verifica che il codice corretto gestisca prodotti senza varianti.
 */
public class GenericFilterServletTest {

    private GenericFilterServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;

    // Per catturare l'output JSON
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setup() throws IOException {
        servlet = new GenericFilterServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        // Prepariamo un writer in memoria per catturare l'output JSON
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);

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
        GenericFilterServlet spyServlet = spy(new GenericFilterServlet());
        // Disattiviamo il doGet reale per testare solo la delega
        doNothing().when(spyServlet).doGet(any(HttpServletRequest.class), any(HttpServletResponse.class));

        spyServlet.doPost(request, response);

        verify(spyServlet).doGet(request, response);
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
            v.setSconto(20); // Set discount to match assertion
            p.setNome("Proteine");
            p.setVarianti(List.of(v)); // Ha una variante

            JSONObject json = GenericFilterServlet.getJsonObject(p);

            assertNotNull(json);
            assertEquals("Proteine", json.get("nome"));
            assertEquals(20, json.get("sconto"));
        }

        @Test
        @DisplayName("Prodotto con Varianti null (Mocked) -> Restituisce null")
        void getJsonObject_mockedNullVariants_returnsNull() {
            // Poiché Prodotto.getVarianti() inizializza la lista se null,
            // dobbiamo usare un mock per forzare il ritorno di null.
            Prodotto p = mock(Prodotto.class);
            when(p.getVarianti()).thenReturn(null);

            JSONObject json = GenericFilterServlet.getJsonObject(p);

            assertNull(json);
        }

        @Test
        @DisplayName("Verifica tutti i campi del JSON")
        void getJsonObject_verifyAllFields() {
            Prodotto p = new Prodotto();
            p.setIdProdotto("1"); // Fixed: String instead of int
            p.setNome("Prodotto Test");
            p.setCategoria("Integratori");
            p.setCalorie(100);
            p.setImmagine("img.jpg");

            Variante v = new Variante();
            v.setIdVariante(10);
            v.setPrezzo(50.0f);
            v.setGusto("Vaniglia");
            v.setPesoConfezione(1000); // Fixed: int instead of String (assuming 1000g for 1kg)
            v.setSconto(0);

            p.setVarianti(List.of(v));

            JSONObject json = GenericFilterServlet.getJsonObject(p);

            assertNotNull(json);
            assertEquals("1", json.get("id")); // Fixed: String
            assertEquals("Prodotto Test", json.get("nome"));
            assertEquals("Integratori", json.get("categoria"));
            assertEquals(100, json.get("calorie"));
            assertEquals("img.jpg", json.get("immagine"));
            assertEquals(10, json.get("idVariante"));
            assertEquals(50.0f, json.get("prezzo"));
            assertEquals("Vaniglia", json.get("gusto"));
            assertEquals(1000, json.get("peso")); // Fixed: int
            assertNull(json.get("sconto")); // Sconto 0 non viene aggiunto
        }

        @Test
        @DisplayName("(CORRETTO) Prodotto senza Varianti -> Restituisce null")
        void getJsonObject_noVariant_returnsNull() {
            Prodotto p = new Prodotto();
            p.setNome("Prodotto Fallato");
            p.setVarianti(new ArrayList<>()); // Lista varianti vuota

            // Questo è il NUOVO test: verifica che il metodo restituisca null
            JSONObject json = GenericFilterServlet.getJsonObject(p);

            assertNull(json);
        }
    }

    // --- Test 3: Modalità nameForm (JSP) ---

    @Nested
    @DisplayName("Modalità: nameForm (JSP)")
    class NameFormTests {

        @Test
        @DisplayName("nameForm con valore -> Chiama filterProducts e fa forward")
        void nameForm_withValue_forwardsToJSP() throws ServletException, IOException, SQLException {
            when(request.getParameter("nameForm")).thenReturn("Proteine");
            List<Prodotto> listA = List.of(new Prodotto());

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.filterProducts("", "", "", "", "Proteine")).thenReturn(listA);
            })) {

                servlet.doGet(request, response);

                // Verifica chiamata DAO
                verify(dao.constructed().get(0)).filterProducts("", "", "", "", "Proteine");
                verify(dao.constructed().get(0), never()).doRetrieveAll();

                // Verifica attributi
                verify(request).setAttribute("originalProducts", listA);
                verify(session).setAttribute("searchBarName", "Proteine");

                // Verifica forward (e non JSON)
                verify(dispatcher).forward(request, response);
                assertTrue(getJsonOutput().isEmpty());
            }
        }

        @Test
        @DisplayName("nameForm vuoto -> Chiama doRetrieveAll e fa forward")
        void nameForm_blank_callsDoRetrieveAll() throws ServletException, IOException, SQLException {
            when(request.getParameter("nameForm")).thenReturn(""); // Blank
            List<Prodotto> listB = List.of(new Prodotto());

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveAll()).thenReturn(listB);
            })) {

                servlet.doGet(request, response);

                // Verifica chiamata DAO
                verify(dao.constructed().get(0)).doRetrieveAll();
                verify(dao.constructed().get(0), never()).filterProducts(any(), any(), any(), any(), any());

                // Verifica pulizia sessione
                verify(session).removeAttribute("categoria");

                // Verifica forward
                verify(dispatcher).forward(request, response);
            }
        }

        @Test
        @DisplayName("nameForm lancia SQLException -> Rilancia RuntimeException")
        void nameForm_sqlException_throwsRuntime() throws SQLException {
            when(request.getParameter("nameForm")).thenReturn("Proteine");

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                // Simula il crash del DAO
                when(mock.filterProducts(any(), any(), any(), any(), any())).thenThrow(new SQLException("DB Error"));
            })) {

                // Verifica che la servlet catturi l'eccezione e la rilanci
                RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                    servlet.doGet(request, response);
                });

                // Verifica che la causa sia l'eccezione SQL
                assertTrue(ex.getCause() instanceof SQLException);
            }
        }
    }

    // --- Test 4: Modalità Filtro AJAX (JSON) ---

    @Nested
    @DisplayName("Modalità: Filtro AJAX (JSON)")
    class AjaxFilterTests {

        @Test
        @DisplayName("(CORRETTO) Filtro AJAX ignora prodotti senza varianti")
        void ajaxFilter_skipsNullVariantProducts() throws ServletException, IOException, SQLException {
            when(request.getParameter("nameForm")).thenReturn(null); // Attiva modalità AJAX

            // 1. Prepara i dati (un prodotto OK, uno fallato)
            Prodotto pBuono = new Prodotto();
            pBuono.setNome("Prodotto Buono");
            pBuono.setVarianti(List.of(new Variante())); // Ha varianti

            Prodotto pFallato = new Prodotto();
            pFallato.setNome("Prodotto Fallato");
            pFallato.setVarianti(new ArrayList<>()); // NON ha varianti

            List<Prodotto> listFromDB = List.of(pBuono, pFallato);

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                // Il DAO restituisce entrambi i prodotti
                when(mock.filterProducts(any(), any(), any(), any(), any())).thenReturn(listFromDB);
            })) {

                servlet.doGet(request, response);

                // Verifica che non ci sia stato forward
                verify(dispatcher, never()).forward(request, response);

                // Verifica l'output JSON
                String json = getJsonOutput();

                // VERIFICA CHIAVE:
                // Il JSON deve contenere il prodotto buono
                assertTrue(json.contains("\"nome\":\"Prodotto Buono\""));
                // Il JSON NON deve contenere il prodotto fallato (perché è stato skippato)
                assertFalse(json.contains("\"nome\":\"Prodotto Fallato\""));
            }
        }

        @Test
        @DisplayName("Filtro AJAX lancia SQLException -> Rilancia RuntimeException")
        void ajaxFilter_sqlException_throwsRuntime() throws SQLException {
            when(request.getParameter("nameForm")).thenReturn(null); // Attiva modalità AJAX

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                // Simula il crash del DAO
                when(mock.filterProducts(any(), any(), any(), any(), any())).thenThrow(new SQLException("DB Error"));
            })) {

                RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                    servlet.doGet(request, response);
                });

                assertTrue(ex.getCause() instanceof SQLException);
            }
        }

        @Test
        @DisplayName("Filtro AJAX usa parametri sessione e request (con valori in Whitelist)")
        void ajaxFilter_usesSessionAndRequestParameters() throws ServletException, IOException, SQLException {
            when(request.getParameter("nameForm")).thenReturn(null);

            // Session attributes
            when(session.getAttribute("categoria")).thenReturn("Integratori");
            when(session.getAttribute("searchBarName")).thenReturn("Whey");

            // Request parameters
            when(request.getParameter("weight")).thenReturn("1kg");
            when(request.getParameter("taste")).thenReturn("Cioccolato");

            // CORREZIONE: Uso "PriceAsc" (come nella tua lista) invece di "price_asc"
            when(request.getParameter("sorting")).thenReturn("PriceAsc");

            List<Prodotto> emptyList = new ArrayList<>();

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.filterProducts(anyString(), anyString(), anyString(), anyString(), anyString()))
                        .thenReturn(emptyList);
            })) {

                servlet.doGet(request, response);

                // Verifica che i parametri siano passati correttamente al DAO
                verify(dao.constructed().get(0)).filterProducts(
                        eq("Integratori"),
                        eq("PriceAsc"), // <-- Deve corrispondere esattamente a quello sopra
                        eq("1kg"),
                        eq("Cioccolato"),
                        eq("Whey")
                );

                // Verifica aggiornamento sessione
                verify(session).setAttribute("filteredProducts", emptyList);
            }
        }
    }
}