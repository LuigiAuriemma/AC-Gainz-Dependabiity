package controller.homepage;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Classe di test completa per CarrelloServlet.
 * Testa tutte le azioni (show, add, remove, quantity) e le loro faglie (NPE, IOOBE, NFE).
 */
public class CarrelloServletTest {

    private CarrelloServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    // Per catturare l'output JSON
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    // Lista carrello fittizia per i test
    private List<Carrello> mockCart;

    @BeforeEach
    void setup() throws IOException {
        servlet = new CarrelloServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);

        // Prepariamo un writer in memoria per catturare l'output JSON
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);

        // Colleghiamo i mock
        when(request.getSession()).thenReturn(session);
        when(response.getWriter()).thenReturn(printWriter);

        // Prepariamo un carrello "reale" (ma fittizio) per i test
        // Usiamo una ArrayList reale perché deve essere modificabile (es. .removeIf)
        mockCart = new ArrayList<>();
    }

    /**
     * Helper per ottenere l'output JSON catturato.
     */
    private String getJsonOutput() {
        printWriter.flush();
        return stringWriter.toString().trim();
    }

    /**
     * Helper per preparare i mock DAO di base
     */
    private void stubProdottoEVariante(Prodotto p, Variante v, String pId, String gusto, int peso) {
        // Stub ProdottoDAO
        when(pDaoMock.doRetrieveById(pId)).thenReturn(p);

        // Stub VarianteDAO
        // Usiamo anyString(), anyString(), anyInt() perché i parametri potrebbero essere null
        when(vDaoMock.doRetrieveVariantByFlavourAndWeight(anyString(), anyString(), anyInt()))
                .thenReturn(new ArrayList<>()); // Ritorna vuoto di default
        when(vDaoMock.doRetrieveVariantByFlavourAndWeight(pId, gusto, peso))
                .thenReturn(List.of(v)); // Ritorna la variante corretta per i parametri giusti
    }

    // Mock globali per i DAO (per non annidare troppi try-with-resources)
    // Questi verranno inizializzati nei @Nested setup
    private ProdottoDAO pDaoMock;
    private VarianteDAO vDaoMock;

    // --- Test 1: Generali ---

    @Test
    @DisplayName("doPost deve delegare a doGet")
    void doPost_delegatesToDoGet() throws ServletException, IOException {
        // 1. Crea lo spy
        CarrelloServlet spyServlet = spy(new CarrelloServlet());

        // 2. IMPORTANTE: Disattiva l'esecuzione del VERO metodo doGet
        // Vogliamo solo verificare che venga chiamato, non rieseguire tutta la sua logica.
        doNothing().when(spyServlet).doGet(any(HttpServletRequest.class), any(HttpServletResponse.class));

        // 3. Esegui doPost (che ora chiamerà il doGet "finto")
        spyServlet.doPost(request, response);

        // 4. Verifica che la chiamata sia avvenuta
        verify(spyServlet).doGet(request, response);
    }

    @Test
    @DisplayName("Action non valida -> Restituisce []")
    void invalidAction_returnsEmptyJson() throws ServletException, IOException {
        when(request.getParameter("action")).thenReturn("azione-sbagliata");

        servlet.doGet(request, response);

        assertEquals("[]", getJsonOutput());
    }
    // --- Test 2: action="show" ---

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
            when(session.getAttribute("cart")).thenReturn(mockCart); // Lista vuota

            servlet.doGet(request, response);

            assertEquals("[]", getJsonOutput());
        }

        @Test
        @DisplayName("Mostra carrello pieno -> Restituisce JSON")
        void show_fullCart_returnsJson() throws ServletException, IOException {
            when(request.getParameter("action")).thenReturn("show");

            // Prepariamo carrello e prodotto
            Prodotto p = new Prodotto(); p.setIdProdotto("P1"); p.setNome("Proteine"); p.setImmagine("img.png");
            Carrello c = new Carrello();
            c.setIdProdotto("P1"); c.setPrezzo(50.0f);

            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);

            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            })) {

                servlet.doGet(request, response);
                String json = getJsonOutput();

                assertTrue(json.contains("\"nomeProdotto\":\"Proteine\""));
                assertTrue(json.contains("\"totalPrice\":50.0"));
            }
        }
    }

    // --- Test 3: action="addVariant" ---

    @Nested
    @DisplayName("Azione: 'addVariant'")
    class AddVariantTests {

        @BeforeEach
        void setupAdd() {
            // Setup parametri comuni per add
            when(request.getParameter("action")).thenReturn("addVariant");
            when(request.getParameter("id")).thenReturn("P1");
            when(request.getParameter("gusto")).thenReturn("Cioccolato");
            when(request.getParameter("pesoConfezione")).thenReturn("900");
            when(request.getParameter("quantity")).thenReturn("1");
        }

        @Test
        @DisplayName("(Happy Path) Aggiunge a carrello nullo")
        void add_toNullCart_createsCartAndAdds() throws ServletException, IOException {
            // --- 1. Setup Parametri Request ---
            when(request.getParameter("action")).thenReturn("addVariant");
            when(request.getParameter("id")).thenReturn("P1");
            when(request.getParameter("gusto")).thenReturn("Cioccolato");
            when(request.getParameter("pesoConfezione")).thenReturn("900");
            when(request.getParameter("quantity")).thenReturn("1");

            when(session.getAttribute("cart")).thenReturn(null); // Carrello nullo

            // --- 2. Setup Dati DAO ---
            Prodotto p = new Prodotto(); p.setIdProdotto("P1"); p.setNome("Proteine"); p.setImmagine("img.png");
            Variante v = new Variante(); v.setIdVariante(10); v.setPrezzo(100f); v.setSconto(10); v.setQuantita(50); // Stock 50

            // --- 3. Definisci il comportamento dei DAO ---
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                // Definisci il comportamento del ProdottoDAO
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                 MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                     // Definisci il comportamento del VarianteDAO
                     when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                             .thenReturn(List.of(v));
                 })) {

                // --- 4. Esegui la servlet ---
                servlet.doGet(request, response);

                // --- 5. Verifica ---
                // Cattura il carrello salvato in sessione
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());

                List<Carrello> savedCart = (List<Carrello>) captor.getValue();
                assertEquals(1, savedCart.size());
                assertEquals(1, savedCart.get(0).getQuantita());
                assertEquals(90.0f, savedCart.get(0).getPrezzo()); // Prezzo 100 con 10% sconto
            }
        }

        @Test
        @DisplayName("(Happy Path) Aggiunge (merge) a carrello esistente")
        void add_toExistingCart_mergesQuantity() throws ServletException, IOException {
            // --- 1. Setup Parametri Request ---
            // Questi sono necessari perché siamo nel @Nested "AddVariantTests"
            when(request.getParameter("action")).thenReturn("addVariant");
            when(request.getParameter("id")).thenReturn("P1");
            when(request.getParameter("gusto")).thenReturn("Cioccolato");
            when(request.getParameter("pesoConfezione")).thenReturn("900");
            when(request.getParameter("quantity")).thenReturn("1"); // Aggiunge 1

            // --- 2. Setup Dati Sessione ---
            // Carrello esistente con 1 item (quantità 2)
            Carrello c = new Carrello();
            c.setIdVariante(10); c.setQuantita(2); c.setPrezzo(180.0f); // 2 * 90
            c.setIdProdotto("P1"); // Importante per writeCartItemsToResponse
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);

            // --- 3. Setup Dati DAO ---
            Prodotto p = new Prodotto(); p.setIdProdotto("P1"); p.setNome("Proteine"); p.setImmagine("img.png");
            Variante v = new Variante(); v.setIdVariante(10); v.setPrezzo(100f); v.setSconto(10); v.setQuantita(50); // Stock 50

            // --- 4. Definisci il comportamento dei DAO ---
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                // Definisci il comportamento del ProdottoDAO
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                 MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                     // Definisci il comportamento del VarianteDAO
                     when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                             .thenReturn(List.of(v));
                 })) {

                // --- 5. Esegui la servlet ---
                servlet.doGet(request, response);

                // --- 6. Verifica ---
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());

                List<Carrello> savedCart = (List<Carrello>) captor.getValue();
                assertEquals(1, savedCart.size()); // Ancora 1 item
                assertEquals(3, savedCart.get(0).getQuantita()); // Quantità (2 + 1 = 3)
                assertEquals(270.0f, savedCart.get(0).getPrezzo()); // Prezzo (180 + 90)
            }
        }

        @Test
        @DisplayName("(FAGLIA 💥) Prodotto non trovato -> Non scrive JSON")
        void add_productNotFound_doesNothing() throws ServletException, IOException {
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(null); // Prodotto non trovato
            })) {
                servlet.doGet(request, response);
                assertTrue(getJsonOutput().isEmpty());
            }
        }

        @Test
        @DisplayName("(CORRETTO) Variante non trovata -> Viene gestita e non fa nulla")
        void add_variantNotFound_isHandledSafely() throws ServletException, IOException {
            when(session.getAttribute("cart")).thenReturn(null);

            // N.B. I parametri (action, id, gusto, peso) sono già
            // impostati nel @BeforeEach della classe @Nested "AddVariantTests"

            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(new Prodotto()); // Prodotto OK
            });
                 MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                     // RESTITUISCE LISTA VUOTA (questo scatena il return;)
                     when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                             .thenReturn(new ArrayList<>());
                 })) {

                // --- 1. Esegui e Verifica che NON crashi ---
                assertDoesNotThrow(() -> {
                    servlet.doGet(request, response);
                });

                // --- 2. Verifica il nuovo comportamento (silent return) ---
                // Nessun JSON deve essere scritto
                assertTrue(getJsonOutput().isEmpty());
                // Il carrello in sessione non deve essere modificato
                verify(session, never()).setAttribute(anyString(), any());
            }
        }

        @Test
        @DisplayName("(Regola) Aggiunge più dello stock (nuovo) -> Non aggiunge")
        void add_newExceedsStock_doesNotAdd() throws ServletException, IOException {
            // --- 1. Setup Parametri Request ---
            when(request.getParameter("action")).thenReturn("addVariant");
            when(request.getParameter("id")).thenReturn("P1");
            when(request.getParameter("gusto")).thenReturn("Cioccolato");
            when(request.getParameter("pesoConfezione")).thenReturn("900");
            when(request.getParameter("quantity")).thenReturn("100"); // Quantità > Stock

            when(session.getAttribute("cart")).thenReturn(null); // Carrello vuoto

            // --- 2. Setup Dati DAO ---
            Prodotto p = new Prodotto(); p.setIdProdotto("P1"); p.setNome("Proteine"); p.setImmagine("img.png");
            Variante v = new Variante(); v.setIdVariante(10); v.setQuantita(50); // Stock 50

            // --- 3. Definisci il comportamento dei DAO ---
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                // Definisci il comportamento del ProdottoDAO
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                 MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                     // Definisci il comportamento del VarianteDAO
                     when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                             .thenReturn(List.of(v));
                 })) {

                // --- 4. Esegui la servlet ---
                servlet.doGet(request, response);

                // --- 5. Verifica ---
                // Cattura il carrello salvato in sessione
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());

                List<Carrello> savedCart = (List<Carrello>) captor.getValue();
                // L'if (!itemExists && quantity <= v.getQuantita()) fallisce,
                // quindi il carrello deve rimanere vuoto.
                assertTrue(savedCart.isEmpty());
            }
        }

        @Test
        @DisplayName("(Regola) Aggiunge più dello stock (merge) -> Non aggiorna")
        void add_mergeExceedsStock_doesNotUpdate() throws ServletException, IOException {
            // --- 1. Setup Parametri Request ---
            when(request.getParameter("action")).thenReturn("addVariant");
            when(request.getParameter("id")).thenReturn("P1");
            when(request.getParameter("gusto")).thenReturn("Cioccolato");
            when(request.getParameter("pesoConfezione")).thenReturn("900");
            when(request.getParameter("quantity")).thenReturn("20"); // Aggiunge 20

            // --- 2. Setup Dati Sessione ---
            // Carrello esistente con 40
            Carrello c = new Carrello();
            c.setIdVariante(10); c.setQuantita(40); c.setIdProdotto("P1");
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);

            // --- 3. Setup Dati DAO ---
            Prodotto p = new Prodotto(); p.setIdProdotto("P1"); p.setNome("Proteine"); p.setImmagine("img.png");
            Variante v = new Variante(); v.setIdVariante(10); v.setQuantita(50); // Stock 50

            // --- 4. Definisci il comportamento dei DAO ---
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                // Definisci il comportamento del ProdottoDAO
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                 MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                     // Definisci il comportamento del VarianteDAO
                     when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                             .thenReturn(List.of(v));
                 })) {

                // --- 5. Esegui la servlet ---
                servlet.doGet(request, response);

                // --- 6. Verifica ---
                // Cattura carrello
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());

                List<Carrello> savedCart = (List<Carrello>) captor.getValue();
                // L'if (newQuantity <= v.getQuantita()) fallisce (60 non è <= 50)
                assertEquals(1, savedCart.size());
                assertEquals(40, savedCart.get(0).getQuantita()); // Quantità invariata
            }
        }
    }

    // --- Test 4: action="removeVariant" ---

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
        @DisplayName("(Happy Path) Rimuove item dal carrello")
        void remove_happyPath_removesItem() throws ServletException, IOException {
            // --- 1. Setup Parametri Request ---
            when(request.getParameter("action")).thenReturn("removeVariant");
            when(request.getParameter("id")).thenReturn("P1"); // ID della variante da rimuovere
            when(request.getParameter("gusto")).thenReturn("Cioccolato");
            when(request.getParameter("pesoConfezione")).thenReturn("900");

            // --- 2. Setup Dati Sessione ---
            // Carrello con 2 items
            Carrello c1 = new Carrello(); c1.setIdVariante(10); c1.setIdProdotto("P1"); // Da rimuovere
            Carrello c2 = new Carrello(); c2.setIdVariante(20); c2.setIdProdotto("P2"); // Da tenere
            mockCart.addAll(List.of(c1, c2));
            when(session.getAttribute("cart")).thenReturn(mockCart);

            // --- 3. Setup Dati DAO ---
            Prodotto p = new Prodotto(); p.setIdProdotto("P1"); // Prodotto da rimuovere
            Prodotto p2 = new Prodotto(); p2.setIdProdotto("P2"); p2.setNome("Altro"); p2.setImmagine("img2.png"); // Prodotto che resta
            Variante v = new Variante(); v.setIdVariante(10); // Variante da rimuovere

            // --- 4. Definisci il comportamento dei DAO ---
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                // Definisci il comportamento del ProdottoDAO
                when(mock.doRetrieveById("P1")).thenReturn(p); // Chiamato da handleRemove...
                when(mock.doRetrieveById("P2")).thenReturn(p2); // Chiamato da writeCartItemsToResponse
            });
                 MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                     // Definisci il comportamento del VarianteDAO
                     when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                             .thenReturn(List.of(v));
                 })) {

                // --- 5. Esegui la servlet ---
                servlet.doGet(request, response);

                // --- 6. Verifica ---
                // Cattura il carrello salvato in sessione
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());

                List<Carrello> savedCart = (List<Carrello>) captor.getValue();
                // Verifica che il carrello ora abbia solo 1 item
                assertEquals(1, savedCart.size());
                // Verifica che l'item rimasto sia quello corretto
                assertEquals(20, savedCart.get(0).getIdVariante());
            }
        }

        @Test
        @DisplayName("(CORRETTO) Variante non trovata -> Viene gestita e non fa nulla")
        void remove_variantNotFound_isHandledSafely() throws ServletException, IOException {
            // --- 1. Setup Parametri Request ---
            // Dati necessari per eseguire il metodo
            when(request.getParameter("action")).thenReturn("removeVariant");
            when(request.getParameter("id")).thenReturn("P1");
            when(request.getParameter("gusto")).thenReturn("Cioccolato");
            when(request.getParameter("pesoConfezione")).thenReturn("900");
            when(session.getAttribute("cart")).thenReturn(mockCart);

            // --- 2. Setup Dati DAO ---
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(new Prodotto()); // Prodotto OK
            });
                 MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                     // RESTITUISCE LISTA VUOTA (questo scatena il return;)
                     when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                             .thenReturn(new ArrayList<>());
                 })) {

                // --- 3. Esegui e Verifica che NON crashi ---
                assertDoesNotThrow(() -> {
                    servlet.doGet(request, response);
                });

                // --- 4. Verifica il nuovo comportamento (silent return) ---
                // Nessun JSON deve essere scritto
                assertTrue(getJsonOutput().isEmpty());
                // Il carrello in sessione non deve essere modificato
                verify(session, never()).setAttribute(anyString(), any());
            }
        }
    }

    // --- Test 5: action="quantityVariant" ---

    @Nested
    @DisplayName("Azione: 'quantityVariant'")
    class QuantityVariantTests {

        @BeforeEach
        void setupQuantity() {
            when(request.getParameter("action")).thenReturn("quantityVariant");
            when(request.getParameter("id")).thenReturn("P1");
            when(request.getParameter("gusto")).thenReturn("Cioccolato");
            when(request.getParameter("pesoConfezione")).thenReturn("900");

            // Carrello esistente con 1 item
            Carrello c = new Carrello();
            c.setIdVariante(10); c.setQuantita(2); c.setPrezzo(20.0f);
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);
        }

        @Test
        @DisplayName("(Happy Path) Aggiorna quantità e prezzo")
        void quantity_happyPath_updatesItem() throws ServletException, IOException {
            when(request.getParameter("quantity")).thenReturn("5");

            // --- 1. Definisci i dati da restituire ---
            Prodotto p = new Prodotto(); p.setIdProdotto("P1"); p.setNome("Proteine"); p.setImmagine("img.png");
            Variante v = new Variante(); v.setIdVariante(10); v.setPrezzo(10f); v.setSconto(0); v.setQuantita(50); // Stock 50

            // --- 2. Definisci il comportamento dei DAO quando vengono creati ---
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                // Definisci il comportamento del ProdottoDAO qui
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                 MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                     // Definisci il comportamento del VarianteDAO qui
                     when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                             .thenReturn(List.of(v));
                 })) {

                // --- 3. Esegui la servlet ---
                servlet.doGet(request, response);

                // --- 4. Verifica ---
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                verify(session).setAttribute(eq("cart"), captor.capture());

                List<Carrello> savedCart = (List<Carrello>) captor.getValue();
                assertEquals(1, savedCart.size());
                assertEquals(5, savedCart.get(0).getQuantita()); // Quantità aggiornata
                assertEquals(50.0f, savedCart.get(0).getPrezzo()); // Prezzo aggiornato (5 * 10)
            }
        }

        @Test
        @DisplayName("(Regola) Quantità negativa -> Rimuove item")
        void quantity_negative_removesItem() throws ServletException, IOException {
            // --- 1. Setup Parametri Request ---
            when(request.getParameter("action")).thenReturn("quantityVariant");
            when(request.getParameter("id")).thenReturn("P1");
            when(request.getParameter("gusto")).thenReturn("Cioccolato");
            when(request.getParameter("pesoConfezione")).thenReturn("900");
            when(request.getParameter("quantity")).thenReturn("-1"); // Questo fa scattare la logica di rimozione

            // --- 2. Setup Dati Sessione ---
            // Carrello con 1 item (quello che verrà rimosso)
            Carrello c = new Carrello();
            c.setIdVariante(10); c.setQuantita(2); c.setPrezzo(20.0f);
            mockCart.add(c);
            when(session.getAttribute("cart")).thenReturn(mockCart);

            // --- 3. Setup Dati DAO ---
            Prodotto p = new Prodotto(); p.setIdProdotto("P1"); p.setNome("Proteine"); p.setImmagine("img.png");
            Variante v = new Variante(); v.setIdVariante(10); v.setQuantita(50); // Stock 50

            // --- 4. Definisci il comportamento dei DAO ---
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                // Questo mock gestirà le chiamate sia da handleQuantity... che da handleRemove...
                // e anche da writeCartItemsToResponse (che ora restituirà un carrello vuoto)
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                 MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                     // Questo mock gestirà le chiamate da handleQuantity... e handleRemove...
                     when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                             .thenReturn(List.of(v));
                 })) {

                // --- 5. Esegui la servlet ---
                servlet.doGet(request, response);

                // --- 6. Verifica ---
                ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
                // Verifichiamo che il carrello sia stato salvato in sessione
                // (verrà chiamato in handleRemoveVariantAction)
                verify(session).setAttribute(eq("cart"), captor.capture());

                List<Carrello> savedCart = (List<Carrello>) captor.getValue();
                // Verifichiamo che il carrello salvato sia ora vuoto
                assertTrue(savedCart.isEmpty()); // L'item è stato rimosso
            }
        }

        @Test
        @DisplayName("(FAGLIA 💥) Quantità > stock -> Non aggiorna")
        void quantity_exceedsStock_doesNotUpdate() throws ServletException, IOException {
            when(request.getParameter("quantity")).thenReturn("100");

            // 1. Definiamo i dati che i DAO "restituiranno"
            Prodotto p = new Prodotto(); p.setIdProdotto("P1");
            Variante v = new Variante(); v.setIdVariante(10); v.setQuantita(50); // Stock 50

            // 2. Definiamo il comportamento dei DAO QUANDO vengono costruiti
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                // QUANDO il ProdottoDAO viene creato E viene chiamato doRetrieveById("P1"),
                // ALLORA restituisci 'p'.
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                 MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                     // QUANDO il VarianteDAO viene creato E viene chiamato...
                     when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                             .thenReturn(List.of(v)); // ALLORA restituisci 'v'
                 })) {

                // 3. Ora chiamiamo la servlet.
                // Dentro questo metodo, verranno creati i new ProdottoDAO() e new VarianteDAO(),
                // e i mock che abbiamo appena definito verranno usati al loro posto.
                servlet.doGet(request, response);

                // 4. Verifiche
                // L'if fallisce (100 non è <= 50), quindi non viene scritto nessun JSON
                assertTrue(getJsonOutput().isEmpty());
                // La sessione non viene ri-settata (il carrello rimane com'era)
                verify(session, never()).setAttribute(anyString(), any());
            }
        }

        @Test
        @DisplayName("(CORRETTO) Quantità non numerica -> Viene gestita e non fa nulla")
        void quantity_invalidParam_isHandled() throws ServletException, IOException {
            // --- 1. Setup Parametri Request ---
            when(request.getParameter("quantity")).thenReturn("abc"); // Causa il return

            // Altri parametri
            when(request.getParameter("action")).thenReturn("quantityVariant");
            when(request.getParameter("id")).thenReturn("P1");
            when(request.getParameter("gusto")).thenReturn("Cioccolato");
            when(request.getParameter("pesoConfezione")).thenReturn("900");

            // --- 2. Setup Dati DAO ---
            Prodotto p = new Prodotto(); p.setIdProdotto("P1");
            Variante v = new Variante(); v.setIdVariante(10); v.setQuantita(50);

            // --- 3. Definisci il comportamento dei DAO ---
            try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                when(mock.doRetrieveById("P1")).thenReturn(p);
            });
                 MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                     when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Cioccolato", 900))
                             .thenReturn(List.of(v));
                 })) {

                // --- 4. Esegui e Verifica che NON crashi ---
                // Verifichiamo che l'esecuzione termini senza eccezioni
                assertDoesNotThrow(() -> {
                    servlet.doGet(request, response);
                });

                // --- 5. Verifica il nuovo comportamento (silent return) ---
                // L'azione è stata interrotta, quindi nessun JSON deve essere scritto
                assertTrue(getJsonOutput().isEmpty());
                // E la sessione non deve essere aggiornata
                verify(session, never()).setAttribute(anyString(), any());
            }
        }



    }
}