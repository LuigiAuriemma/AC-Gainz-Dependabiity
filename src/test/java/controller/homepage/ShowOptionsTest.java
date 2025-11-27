package controller.homepage;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Prodotto;
import model.ProdottoDAO;
import model.Variante;
import model.VarianteDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Classe di test per la servlet ShowOptions.
 * Testa le 3 azioni (showFirst, updateOptions, updatePrice) e i percorsi di
 * fallimento.
 * Cattura l'output JSON mockando il PrintWriter.
 */
public class ShowOptionsTest {

    private ShowOptions servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;

    // Per catturare l'output JSON
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setup() throws IOException {
        servlet = new ShowOptions();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        // Prepariamo un writer in memoria per catturare l'output JSON
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);

        // Quando la servlet chiede il writer, le diamo il nostro
        when(response.getWriter()).thenReturn(printWriter);
    }

    /**
     * Helper per ottenere l'output JSON catturato.
     */
    private String getJsonOutput() {
        printWriter.flush(); // Assicura che tutto sia stato scritto
        return stringWriter.toString().trim(); // .trim() rimuove newline
    }

    // --- Test 1: Generali ---

    @Test
    @DisplayName("doPost deve delegare a doGet")
    void doPost_delegatesToDoGet() throws ServletException, IOException {
        ShowOptions spyServlet = spy(new ShowOptions());
        spyServlet.doPost(request, response);
        verify(spyServlet).doGet(request, response);
    }

    @Test
    @DisplayName("Nessuna azione -> Nessun output")
    void noAction_doesNothing() throws ServletException, IOException {
        // Nessun parametro "action"
        when(request.getParameter("action")).thenReturn(null);

        servlet.doGet(request, response);

        assertTrue(getJsonOutput().isEmpty());
    }

    @Test
    @DisplayName("Azione sconosciuta con idVariante -> Nessun output")
    void unknownAction_with_idVariante_doesNothing() throws ServletException, IOException {
        when(request.getParameter("action")).thenReturn("unknownAction");
        when(request.getParameter("idVariante")).thenReturn("1");

        servlet.doGet(request, response);

        assertTrue(getJsonOutput().isEmpty());
    }

    @Test
    @DisplayName("Azione sconosciuta con TUTTI i parametri -> Nessun output")
    void unknownAction_with_allParams_doesNothing() throws ServletException, IOException {
        when(request.getParameter("action")).thenReturn("unknownAction");
        when(request.getParameter("idVariante")).thenReturn("1");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("flavour")).thenReturn("Vaniglia");
        when(request.getParameter("weight")).thenReturn("500");

        servlet.doGet(request, response);

        assertTrue(getJsonOutput().isEmpty());
    }

    // --- Test 2: action="showFirst" ---

    @Test
    @DisplayName("showFirst (Happy Path) -> Restituisce JSON complesso")
    void showFirst_happyPath_returnsFullJson() throws ServletException, IOException {
        when(request.getParameter("action")).thenReturn("showFirst");
        when(request.getParameter("idVariante")).thenReturn("1");

        // 1. Prepariamo i mock
        Variante vCheapest = new Variante(); // Cioccolato, 900g (ID 1)
        vCheapest.setIdVariante(1);
        vCheapest.setGusto("Cioccolato");
        vCheapest.setPesoConfezione(900);
        vCheapest.setPrezzo(50.0f);
        vCheapest.setSconto(10);
        vCheapest.setIdProdotto("P1");

        Variante vOtherWeight = new Variante(); // Cioccolato, 500g
        vOtherWeight.setGusto("Cioccolato");
        vOtherWeight.setPesoConfezione(500);

        Variante vSameWeight = new Variante(); // Cioccolato, 900g (Stesso peso di vCheapest, deve essere ignorato)
        vSameWeight.setGusto("Cioccolato");
        vSameWeight.setPesoConfezione(900);

        Variante vDuplicateWeight = new Variante(); // Cioccolato, 500g (Duplicato di vOtherWeight, deve essere
                                                    // ignorato)
        vDuplicateWeight.setGusto("Cioccolato");
        vDuplicateWeight.setPesoConfezione(500);

        Variante vOtherFlavour = new Variante(); // Vaniglia, 900g (ID 2)
        vOtherFlavour.setIdVariante(2);
        vOtherFlavour.setGusto("Vaniglia");

        Variante vSameFlavour = new Variante(); // Cioccolato, 100g (Stesso gusto di vCheapest, deve essere ignorato
                                                // nella lista gusti)
        vSameFlavour.setIdVariante(3);
        vSameFlavour.setGusto("Cioccolato");

        Variante vDuplicateFlavour = new Variante(); // Vaniglia, 200g (Duplicato di vOtherFlavour, deve essere
                                                     // ignorato)
        vDuplicateFlavour.setIdVariante(4);
        vDuplicateFlavour.setGusto("Vaniglia");

        Prodotto p = new Prodotto();
        p.setIdProdotto("P1");
        p.setNome("Proteine");

        try (MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveVarianteByIdVariante(1)).thenReturn(vCheapest);
            // Lista completa per i gusti: include tutti i casi limite
            when(mock.doRetrieveVariantiByIdProdotto("P1"))
                    .thenReturn(List.of(vCheapest, vOtherWeight, vOtherFlavour, vSameFlavour, vDuplicateFlavour));
            // Lista per i pesi (dello stesso gusto): include duplicati e stesso peso
            when(mock.doRetrieveVariantByCriteria("P1", "flavour", "Cioccolato"))
                    .thenReturn(List.of(vCheapest, vOtherWeight, vSameWeight, vDuplicateWeight));
        });
                MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                    when(mock.doRetrieveById("P1")).thenReturn(p);
                })) {

            // 2. Esecuzione
            servlet.doGet(request, response);
            String json = getJsonOutput();

            // 3. Verifica
            // Verifichiamo che i pezzi chiave del JSON siano presenti
            assertTrue(json.contains("\"nomeProdotto\":\"Proteine\""));
            assertTrue(json.contains("\"cheapestFlavour\":\"Cioccolato\""));
            assertTrue(json.contains("\"cheapestWeight\":900"));
            assertTrue(json.contains("\"cheapestPrice\":50.0"));
            assertTrue(json.contains("\"cheapestWeightOptions\":500"));
            assertTrue(json.contains("\"gusto\":\"Vaniglia\""));

            // Verifica de-duplicazione
            assertFalse(json.contains("\"gusto\":\"Cioccolato\""));
            assertFalse(json.contains("\"cheapestWeightOptions\":900"));
        }
    }

    @Test
    @DisplayName("showFirst con Variante non trovata -> Nessun output")
    void showFirst_nullVariante_doesNothing() throws ServletException, IOException {
        when(request.getParameter("action")).thenReturn("showFirst");
        when(request.getParameter("idVariante")).thenReturn("99");

        try (MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveVarianteByIdVariante(99)).thenReturn(null); // Non trovato
        })) {
            servlet.doGet(request, response);
            assertTrue(getJsonOutput().isEmpty());
        }
    }

    @Test
    @DisplayName("showFirst con Prodotto non trovato -> Nessun output")
    void showFirst_nullProdotto_doesNothing() throws ServletException, IOException {
        when(request.getParameter("action")).thenReturn("showFirst");
        when(request.getParameter("idVariante")).thenReturn("1");

        try (MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveVarianteByIdVariante(1)).thenReturn(new Variante()); // Trovato
        });
                MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                    when(mock.doRetrieveById(any())).thenReturn(null); // Non trovato
                })) {
            servlet.doGet(request, response);
            assertTrue(getJsonOutput().isEmpty());
        }
    }

    // --- Test 3: action="updateOptions" ---

    @Test
    @DisplayName("updateOptions (Happy Path) -> Restituisce JSON con pesi ordinati")
    void updateOptions_happyPath_returnsSortedWeights() throws ServletException, IOException {
        when(request.getParameter("action")).thenReturn("updateOptions");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("flavour")).thenReturn("Vaniglia");

        // Prepariamo varianti NON ordinate
        Variante v1 = new Variante();
        v1.setPesoConfezione(1000);
        Variante v2 = new Variante();
        v2.setPesoConfezione(500);

        try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveById("P1")).thenReturn(new Prodotto()); // Serve per non dare NPE
        });
                MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                    when(mock.doRetrieveVariantByCriteria(any(), eq("flavour"), eq("Vaniglia")))
                            .thenReturn(List.of(v1, v2)); // Restituisce lista non ordinata
                })) {

            servlet.doGet(request, response);
            String json = getJsonOutput();

            // Verifichiamo che l'output sia ordinato (500 prima di 1000)
            assertEquals("[{\"peso\":500},{\"peso\":1000}]", json);
        }
    }

    @Test
    @DisplayName("updateOptions con Prodotto null -> NullPointerException")
    void updateOptions_nullProduct_throwsNPE() {
        when(request.getParameter("action")).thenReturn("updateOptions");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("flavour")).thenReturn("Vaniglia");

        try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveById("P1")).thenReturn(null); // Prodotto non trovato
        })) {

            // Il codice della servlet (p.getIdProdotto()) lancerà NPE
            assertThrows(NullPointerException.class, () -> {
                servlet.doGet(request, response);
            });
        }
    }

    // --- Test 4: action="updatePrice" ---

    @Test
    @DisplayName("updatePrice (Happy Path) -> Restituisce JSON con prezzo e sconto")
    void updatePrice_happyPath_returnsPrice() throws ServletException, IOException {
        when(request.getParameter("action")).thenReturn("updatePrice");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("flavour")).thenReturn("Vaniglia");
        when(request.getParameter("weight")).thenReturn("500");

        Variante v = new Variante();
        v.setPrezzo(29.99f);
        v.setSconto(5);

        try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveById("P1")).thenReturn(new Prodotto());
        });
                MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                    when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Vaniglia", 500))
                            .thenReturn(List.of(v));
                })) {

            servlet.doGet(request, response);
            String json = getJsonOutput();

            // Verifica che il JSON sia un oggetto singolo (non un array)
            // e contenga i dati corretti
            assertTrue(json.contains("\"sconto\":5"));
            assertTrue(json.contains("\"prezzo\":29.99"));
        }
    }

    @Test
    @DisplayName("updatePrice con 'weight' non valido -> NumberFormatException")
    void updatePrice_invalidWeight_throwsNFE() {
        when(request.getParameter("action")).thenReturn("updatePrice");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("flavour")).thenReturn("Vaniglia");
        when(request.getParameter("weight")).thenReturn("abc"); // Peso non valido

        // Il codice della servlet (Integer.parseInt) lancerà NPE
        assertThrows(NumberFormatException.class, () -> {
            servlet.doGet(request, response);
        });

        // Verifichiamo che non abbia scritto nulla prima del crash
        assertTrue(getJsonOutput().isEmpty());
    }

    @Test
    @DisplayName("updatePrice con Variante non trovata -> IndexOutOfBoundsException")
    void updatePrice_variantNotFound_throwsIOOBE() {
        when(request.getParameter("action")).thenReturn("updatePrice");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("flavour")).thenReturn("Ananas");
        when(request.getParameter("weight")).thenReturn("500");

        try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveById("P1")).thenReturn(new Prodotto());
        });
                MockedConstruction<VarianteDAO> vDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                    // Restituisce una LISTA VUOTA
                    when(mock.doRetrieveVariantByFlavourAndWeight("P1", "Ananas", 500))
                            .thenReturn(new ArrayList<>());
                })) {

            // Il codice della servlet (...get(0)) lancerà l'eccezione
            assertThrows(IndexOutOfBoundsException.class, () -> {
                servlet.doGet(request, response);
            });
        }
    }

    @Test
    @DisplayName("updatePrice con Prodotto non trovato -> Nessun output")
    void updatePrice_nullProduct_doesNothing() throws ServletException, IOException {
        when(request.getParameter("action")).thenReturn("updatePrice");
        when(request.getParameter("idProdotto")).thenReturn("P_NOT_FOUND");
        when(request.getParameter("flavour")).thenReturn("Vaniglia");
        when(request.getParameter("weight")).thenReturn("500");

        try (MockedConstruction<ProdottoDAO> pDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveById("P_NOT_FOUND")).thenReturn(null);
        })) {
            servlet.doGet(request, response);
            assertTrue(getJsonOutput().isEmpty());
        }
    }

    @Nested
    @DisplayName("Test Parametri Mancanti")
    class MissingParametersTests {

        @Test
        @DisplayName("showFirst senza idVariante -> Nessun output")
        void showFirst_missingIdVariante_doesNothing() throws ServletException, IOException {
            when(request.getParameter("action")).thenReturn("showFirst");
            when(request.getParameter("idVariante")).thenReturn(null);

            servlet.doGet(request, response);
            assertTrue(getJsonOutput().isEmpty());
        }

        @Test
        @DisplayName("updateOptions senza idProdotto -> Nessun output")
        void updateOptions_missingIdProdotto_doesNothing() throws ServletException, IOException {
            when(request.getParameter("action")).thenReturn("updateOptions");
            when(request.getParameter("idProdotto")).thenReturn(null);
            when(request.getParameter("flavour")).thenReturn("Vaniglia");

            servlet.doGet(request, response);
            assertTrue(getJsonOutput().isEmpty());
        }

        @Test
        @DisplayName("updateOptions senza flavour -> Nessun output")
        void updateOptions_missingFlavour_doesNothing() throws ServletException, IOException {
            when(request.getParameter("action")).thenReturn("updateOptions");
            when(request.getParameter("idProdotto")).thenReturn("P1");
            when(request.getParameter("flavour")).thenReturn(null);

            servlet.doGet(request, response);
            assertTrue(getJsonOutput().isEmpty());
        }

        @Test
        @DisplayName("updatePrice senza idProdotto -> Nessun output")
        void updatePrice_missingIdProdotto_doesNothing() throws ServletException, IOException {
            when(request.getParameter("action")).thenReturn("updatePrice");
            when(request.getParameter("idProdotto")).thenReturn(null);
            when(request.getParameter("flavour")).thenReturn("Vaniglia");
            when(request.getParameter("weight")).thenReturn("500");

            servlet.doGet(request, response);
            assertTrue(getJsonOutput().isEmpty());
        }

        @Test
        @DisplayName("updatePrice senza flavour -> Nessun output")
        void updatePrice_missingFlavour_doesNothing() throws ServletException, IOException {
            when(request.getParameter("action")).thenReturn("updatePrice");
            when(request.getParameter("idProdotto")).thenReturn("P1");
            when(request.getParameter("flavour")).thenReturn(null);
            when(request.getParameter("weight")).thenReturn("500");

            servlet.doGet(request, response);
            assertTrue(getJsonOutput().isEmpty());
        }

        @Test
        @DisplayName("updatePrice senza weight -> Nessun output")
        void updatePrice_missingWeight_doesNothing() throws ServletException, IOException {
            when(request.getParameter("action")).thenReturn("updatePrice");
            when(request.getParameter("idProdotto")).thenReturn("P1");
            when(request.getParameter("flavour")).thenReturn("Vaniglia");
            when(request.getParameter("weight")).thenReturn(null);

            servlet.doGet(request, response);
            assertTrue(getJsonOutput().isEmpty());
        }
    }
}