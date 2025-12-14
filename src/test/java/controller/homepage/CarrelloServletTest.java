package controller.homepage;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Carrello;
import model.Prodotto;
import model.ProdottoDAO;
import model.Variante;
import model.VarianteDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Classe di test completa per CarrelloServlet.
 * Testa tutte le azioni (show, add, remove, quantity) e le loro faglie.
 */
public class CarrelloServletTest {

    private CarrelloServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private ServletConfig servletConfig;
    private ServletContext servletContext;

    // Per catturare l'output JSON
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    // Lista carrello fittizia per i test
    private List<Carrello> mockCart;

    @BeforeEach
    void setup() throws IOException, ServletException {
        servlet = new CarrelloServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        servletConfig = mock(ServletConfig.class);
        servletContext = mock(ServletContext.class);

        // Prepariamo un writer in memoria per catturare l'output JSON
        stringWriter = new StringWriter();
        printWriter = spy(new PrintWriter(stringWriter)); // Spy per verificare flush/close

        // Colleghiamo i mock
        when(request.getSession()).thenReturn(session);
        when(response.getWriter()).thenReturn(printWriter);

        // Setup ServletContext per log
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        servlet.init(servletConfig);

        // Prepariamo un carrello "reale" (ma fittizio) per i test
        mockCart = new ArrayList<>();
    }

    private String getJsonOutput() {
        return stringWriter.toString().trim();
    }

    // --- Test 1: Generali ---

    @Nested
    @DisplayName("Test Generali")
    class GeneralTests {

        @Test
        @DisplayName("doPost deve delegare a doGet")
        void doPost_delegatesToDoGet() throws ServletException, IOException {
            CarrelloServlet spyServlet = spy(new CarrelloServlet());
            doNothing().when(spyServlet).doGet(any(HttpServletRequest.class), any(HttpServletResponse.class));
            spyServlet.doPost(request, response);
            verify(spyServlet).doGet(request, response);
        }

        @Test
        @DisplayName("Action non valida -> Restituisce []")
        void invalidAction_returnsEmptyJson() throws ServletException, IOException {
            when(request.getParameter("action")).thenReturn("azione-sbagliata");
            servlet.doGet(request, response);
            verify(response).setContentType("application/json");
            verify(response).setCharacterEncoding("UTF-8"); // Kill VoidMethodCall
            assertEquals("[]", getJsonOutput());
            verify(printWriter, atLeastOnce()).flush(); // Kill flush
        }

        @Test
        @DisplayName("Action null -> Restituisce []")
        void doGet_nullAction_returnsEmptyJson() throws ServletException, IOException {
            when(request.getParameter("action")).thenReturn(null);
            servlet.doGet(request, response);
            assertEquals("[]", getJsonOutput());
            verify(response).setCharacterEncoding("UTF-8"); // Kill VoidMethodCall
            verify(printWriter, atLeastOnce()).flush(); // Kill flush
        }

        @Test
        @DisplayName("Eccezione in doGet -> Logga errore e invia 500")
        void doGet_exception_logsAndSendsError() throws ServletException, IOException {
            when(request.getParameter("action")).thenThrow(new RuntimeException("Test Exception"));
            servlet.doGet(request, response);
            verify(servletContext).log(eq("null: Errore in CarrelloServlet doGet"), any(RuntimeException.class));
            verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
        }
    }

    // --- Test 2: Show ---

    @Nested
    @DisplayName("Azione: 'show'")
    class ShowTests {
        @Test
        @DisplayName("Mostra carrello nullo -> Restituisce []")
        void show_nullCart_returnsEmptyArray() throws ServletException, IOException {
            when(request.getParameter("action")).thenReturn("show");
            when(session.getAttribute("cart")).thenReturn(null);
            servlet.doGet(request, response);
            assertEquals("[]", getJsonOutput());
        }

        @Test
        @DisplayName("Mostra carrello vuoto -> Restituisce []")
        void show_emptyCart_returnsEmptyArray() throws ServletException, IOException {
            when(request.getParameter("action")).thenReturn("show");
            when(session.getAttribute("cart")).thenReturn(mockCart);
            servlet.doGet(request, response);
            assertEquals("[]", getJsonOutput());
        }

        @Test
        @DisplayName("Mostra carrello pieno -> Restituisce JSON")
        void show_fullCart_returnsJson() throws ServletException, IOException {
            when(request.getParameter("action")).thenReturn("show");

            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            p.setNome("Proteine");
            Carrello c = new Carrello();
            c.setIdProdotto("P1");
            c.setPrezzo(50.12f);
            c.setQuantita(1);
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);

            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            })) {
                servlet.doGet(request, response);
                String json = getJsonOutput();
                assertTrue(json.contains("\"nomeProdotto\":\"Proteine\""));
            }
        }

        @Test
        @DisplayName("Prodotto nel carrello non trovato -> Salta item")
        void show_productInCartNotFound_skipsItem() throws ServletException, IOException {
            when(request.getParameter("action")).thenReturn("show");
            Carrello c = new Carrello();
            c.setIdProdotto("P_DELETED");
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);

            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P_DELETED")).thenReturn(null);
            })) {
                servlet.doGet(request, response);
                String json = getJsonOutput();
                assertTrue(json.contains("\"totalPrice\":0.0"));
                assertFalse(json.contains("P_DELETED"));
            }
        }
    }

    // --- Test 3: AddVariant ---

    @Nested
    @DisplayName("Azione: 'addVariant'")
    class AddVariantTests {

        @BeforeEach
        void setupAdd() {
            when(request.getParameter("action")).thenReturn("addVariant");
            when(request.getParameter("id")).thenReturn("P1");
            when(request.getParameter("gusto")).thenReturn("Cioccolato");
            when(request.getParameter("pesoConfezione")).thenReturn("900");
            when(request.getParameter("quantity")).thenReturn("1");
        }

        @Test
        @DisplayName("(Happy Path) Aggiunge a carrello nullo (q=2)")
        void add_toNullCart_createsCartAndAdds() throws ServletException, IOException {
            when(session.getAttribute("cart")).thenReturn(null);
            when(request.getParameter("quantity")).thenReturn("2");

            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            p.setNome("P");
            p.setImmagine("img.png");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setPrezzo(100f);
            v.setSconto(10);
            v.setQuantita(50);

            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);

                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                List<Carrello> savedCart = (List<Carrello>) captor.getValue();
                assertEquals(1, savedCart.size());
                Carrello c = savedCart.get(0);
                assertEquals(180.0f, c.getPrezzo()); // 90*2
                assertEquals(2, c.getQuantita());
                assertEquals("img.png", c.getImmagineProdotto());
                assertEquals("Cioccolato", c.getGusto());
                assertEquals(900, c.getPesoConfezione());
                assertEquals("P1", c.getIdProdotto());
                assertEquals(10, c.getIdVariante());
                assertEquals("P", c.getNomeProdotto());

                // Kill VoidMethodCallMutator for writeCartItemsToResponse (imgSrc check)
                String json = getJsonOutput();
                assertTrue(json.contains("\"imgSrc\":\"img.png\""));
                assertTrue(json.contains("\"flavour\":\"Cioccolato\""));
                assertTrue(json.contains("\"weight\":900"));
                verify(printWriter, atLeastOnce()).flush(); // Kill flush mutant
            }
        }

        @Test
        @DisplayName("Prodotto trovato ma varianti vuote -> Non aggiunge, no error")
        void add_variantNotFound_doesNothing() throws ServletException, IOException {
            when(session.getAttribute("cart")).thenReturn(null);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");

            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(Collections.emptyList());
                    })) {
                servlet.doGet(request, response);
                verify(session, never()).setAttribute(eq("cart"), any());
                assertTrue(getJsonOutput().isEmpty());
                verify(response, never()).sendError(anyInt(), anyString());
            }
        }

        @Test
        @DisplayName("Boundary: Quantity 0 -> Defaults to 1")
        void add_zeroQuantity_defaultsToOne() throws ServletException, IOException {
            when(request.getParameter("quantity")).thenReturn("0");
            when(session.getAttribute("cart")).thenReturn(null);

            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setPrezzo(100f);
            v.setQuantita(50);

            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                assertEquals(1, ((List<Carrello>) captor.getValue()).get(0).getQuantita());
            }
        }

        @Test
        @DisplayName("Boundary: Sconto 0 -> NESSUN arrotondamento")
        void add_zeroDiscount_noRounding() throws ServletException, IOException {
            when(session.getAttribute("cart")).thenReturn(null);
            when(request.getParameter("quantity")).thenReturn("1");

            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setPrezzo(10.12345f);
            v.setSconto(0);
            v.setQuantita(50);

            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                assertEquals(10.12345f, ((List<Carrello>) captor.getValue()).get(0).getPrezzo(), 0.000001f);
            }
        }

        @Test
        @DisplayName("Math: Arrotondamento")
        void add_roundingTest() throws ServletException, IOException {
            when(session.getAttribute("cart")).thenReturn(null);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setPrezzo(10.129f);
            v.setSconto(0);
            v.setQuantita(50);

            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);

                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                assertEquals(10.129f, ((List<Carrello>) captor.getValue()).get(0).getPrezzo());

                // Kill MathMutator in writeCartItemsToResponse
                String json = getJsonOutput();
                assertTrue(json.contains("\"prezzo\":10.13"));
            }
        }

        @Test
        @DisplayName("(Happy Path) Merge q=2")
        void add_toExistingCart_mergesQuantity() throws ServletException, IOException {
            when(request.getParameter("quantity")).thenReturn("2");
            Carrello c = new Carrello();
            c.setIdVariante(10);
            c.setQuantita(2);
            c.setPrezzo(180.0f);
            c.setIdProdotto("P1");
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);

            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setPrezzo(100f);
            v.setSconto(10);
            v.setQuantita(50);

            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                List<Carrello> savedCart = (List<Carrello>) captor.getValue();
                assertEquals(4, savedCart.get(0).getQuantita());
                assertEquals(360.0f, savedCart.get(0).getPrezzo());

                // Kill VoidMethodCallMutator on writeCartItemsToResponse
                String json = getJsonOutput();
                assertFalse(json.isEmpty());
                assertTrue(json.contains("\"totalPrice\":360.0"));
            }
        }

        @Test
        @DisplayName("Boundary: Exact Stock")
        void add_exactStock_addsItem() throws ServletException, IOException {
            when(request.getParameter("quantity")).thenReturn("50");
            when(session.getAttribute("cart")).thenReturn(null);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setQuantita(50);

            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                assertEquals(50, ((List<Carrello>) captor.getValue()).get(0).getQuantita());
            }
        }

        @Test
        @DisplayName("Boundary: Sconto 0 -> Price Unchanged")
        void add_zeroDiscount_priceUnchanged() throws ServletException, IOException {
            when(session.getAttribute("cart")).thenReturn(null);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setPrezzo(50f);
            v.setSconto(0);
            v.setQuantita(50);
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                assertEquals(50.0f, ((List<Carrello>) captor.getValue()).get(0).getPrezzo());
            }
        }

        @Test
        @DisplayName("Boundary: Sconto 50 -> Price Halved")
        void add_fiftyDiscount_priceHalved() throws ServletException, IOException {
            when(session.getAttribute("cart")).thenReturn(null);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setPrezzo(50f);
            v.setSconto(50);
            v.setQuantita(50);
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                assertEquals(25.0f, ((List<Carrello>) captor.getValue()).get(0).getPrezzo(), 0.01);
            }
        }

        @Test
        @DisplayName("New Exceeds Stock")
        void add_newExceedsStock_doesNotAdd() throws ServletException, IOException {
            when(request.getParameter("quantity")).thenReturn("100");
            when(session.getAttribute("cart")).thenReturn(null);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setQuantita(50);
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                assertTrue(((List<Carrello>) captor.getValue()).isEmpty());
            }
        }

        @Test
        @DisplayName("Product Not Found")
        void add_productNotFound_doesNothing() throws ServletException, IOException {
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(null);
            })) {
                servlet.doGet(request, response);
                assertTrue(getJsonOutput().isEmpty());
            }
        }
    }

    // --- Test 4: Remove ---

    @Nested
    @DisplayName("Azione: 'removeVariant'")
    class RemoveVariantTests {

        @BeforeEach
        void setupRemove() {
            when(request.getParameter("action")).thenReturn("removeVariant");
            when(request.getParameter("id")).thenReturn("P1");
            when(request.getParameter("gusto")).thenReturn("Cioccolato");
            when(request.getParameter("pesoConfezione")).thenReturn("900");
        }

        @Test
        @DisplayName("Remove da carrello vuoto")
        void remove_emptyCart_updatesSessionAndReturnsJson() throws ServletException, IOException {
            when(session.getAttribute("cart")).thenReturn(mockCart);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                verify(session).setAttribute(eq("cart"), anyList());
            }
        }

        @Test
        @DisplayName("Remove Happy Path")
        void remove_happyPath_removesItem() throws ServletException, IOException {
            Carrello c = new Carrello();
            c.setIdProdotto("P1");
            c.setIdVariante(10);
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                assertTrue(((List<Carrello>) captor.getValue()).isEmpty());
            }
        }
    }

    // --- Test 5: Quantity ---

    @Nested
    @DisplayName("Azione: 'quantityVariant'")
    class QuantityVariantTests {

        @BeforeEach
        void setupQ() {
            when(request.getParameter("action")).thenReturn("quantityVariant");
            when(request.getParameter("id")).thenReturn("P1");
            when(request.getParameter("gusto")).thenReturn("Cioccolato");
            when(request.getParameter("pesoConfezione")).thenReturn("900");
        }

        @Test
        @DisplayName("Update con sconto")
        void quantity_withDiscount_updatesPriceCorrectly() throws ServletException, IOException {
            when(request.getParameter("quantity")).thenReturn("3");
            Carrello c = new Carrello();
            c.setIdProdotto("P1");
            c.setIdVariante(10);
            c.setPrezzo(50f);
            c.setQuantita(1);
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setPrezzo(100f);
            v.setSconto(10);
            v.setQuantita(50);
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                assertEquals(270.0f, ((List<Carrello>) captor.getValue()).get(0).getPrezzo());
            }
        }

        @Test
        @DisplayName("Quantity < 0 -> Remove")
        void quantity_lessThanZero_triggersRemove() throws ServletException, IOException {
            when(request.getParameter("quantity")).thenReturn("-1");
            Carrello c = new Carrello();
            c.setIdProdotto("P1");
            c.setIdVariante(10);
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setQuantita(50);
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                assertTrue(((List<Carrello>) captor.getValue()).isEmpty());
            }
        }

        @Test
        @DisplayName("Quantity 0 -> Remove")
        void quantity_zero_removesItem() throws ServletException, IOException {
            when(request.getParameter("quantity")).thenReturn("0");
            Carrello c = new Carrello();
            c.setIdProdotto("P1");
            c.setIdVariante(10);
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setQuantita(50);
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                assertTrue(((List<Carrello>) captor.getValue()).isEmpty());
            }
        }

        @Test
        @DisplayName("Exceeds Stock -> No Update")
        void quantity_exceedsStock_noUpdate() throws ServletException, IOException {
            when(request.getParameter("quantity")).thenReturn("100");
            Carrello c = new Carrello();
            c.setIdVariante(10);
            c.setQuantita(1);
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setQuantita(50);
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                verify(session, never()).setAttribute(eq("cart"), any());
            }
        }

        @Test
        @DisplayName("No Rounding if Discount 0")
        void quantity_zeroDiscount_noRounding() throws ServletException, IOException {
            when(request.getParameter("quantity")).thenReturn("2");
            Carrello c = new Carrello();
            c.setIdProdotto("P1");
            c.setIdVariante(10);
            c.setPrezzo(10.12345f);
            c.setQuantita(1);
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setPrezzo(10.12345f);
            v.setSconto(0);
            v.setQuantita(50);
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                assertEquals(20.2469f, ((List<Carrello>) captor.getValue()).get(0).getPrezzo(), 0.0001f);
            }
        }

        @Test
        @DisplayName("Update Stock Exact")
        void quantity_equalsStock_updates() throws ServletException, IOException {
            when(request.getParameter("quantity")).thenReturn("50");
            Carrello c = new Carrello();
            c.setIdProdotto("P1");
            c.setIdVariante(10);
            c.setPrezzo(50f);
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);
            Prodotto p = new Prodotto();
            p.setIdProdotto("P1");
            Variante v = new Variante();
            v.setIdVariante(10);
            v.setPrezzo(10f);
            v.setQuantita(50);
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                    MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                        when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                                .thenReturn(List.of(v));
                    })) {
                servlet.doGet(request, response);
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());
                assertEquals(50, ((List<Carrello>) captor.getValue()).get(0).getQuantita());
            }
        }
    }
}