package controller.homepage;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Prodotto;
import model.ProdottoDAO;
import model.Variante;
import model.VarianteDAO;
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
 * Classe di test per ProductServlet.
 * Testa i percorsi di fallimento (null key, prodotto non trovato, varianti vuote)
 * e il percorso di successo (happy path).
 */
public class ProductServletTest {

    private ProductServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private RequestDispatcher dispatcher;

    @BeforeEach
    void setup() {
        servlet = new ProductServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        dispatcher = mock(RequestDispatcher.class);

        // Stub di base per il forward
        when(request.getRequestDispatcher("Product.jsp")).thenReturn(dispatcher);
    }

    // --- Test 1: doPost deve delegare a doGet ---

    @Test
    @DisplayName("doPost deve delegare a doGet")
    void doPost_delegatesToDoGet() throws ServletException, IOException {
        // Usiamo uno 'spy' per verificare la chiamata a un altro metodo della stessa classe
        ProductServlet spyServlet = spy(new ProductServlet());
        spyServlet.doPost(request, response);
        verify(spyServlet).doGet(request, response);
    }

    // --- Test 2: primaryKey è null ---

    @Test
    @DisplayName("doGet con primaryKey null non fa nulla")
    void doGet_nullPrimaryKey_doesNothing() throws ServletException, IOException {
        when(request.getParameter("primaryKey")).thenReturn(null);

        // Usiamo MockedConstruction per assicurarci che NESSUN DAO venga creato
        try (MockedConstruction<ProdottoDAO> mockedProdDao = mockConstruction(ProdottoDAO.class)) {

            servlet.doGet(request, response);

            // Verifica che non sia stato creato nessun DAO
            assertEquals(0, mockedProdDao.constructed().size());
            // Verifica che non ci sia stato nessun forward
            verify(dispatcher, never()).forward(request, response);
            // Verifica che non sia stato impostato nessun attributo
            verify(request, never()).setAttribute(anyString(), any());
        }
    }

    // --- Test 3: Prodotto non trovato ---

    @Test
    @DisplayName("doGet con Prodotto non trovato non fa nulla")
    void doGet_productNotFound_doesNothing() throws ServletException, IOException {
        String primaryKey = "999";
        when(request.getParameter("primaryKey")).thenReturn(primaryKey);

        // Mockiamo ProdottoDAO e VarianteDAO
        try (MockedConstruction<ProdottoDAO> mockedProdDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            // Stub: doRetrieveById restituisce null
            when(mock.doRetrieveById(primaryKey)).thenReturn(null);
        });
             MockedConstruction<VarianteDAO> mockedVarDao = mockConstruction(VarianteDAO.class)) {

            servlet.doGet(request, response);

            // Verifica che il primo ProdottoDAO sia stato chiamato
            verify(mockedProdDao.constructed().get(0)).doRetrieveById(primaryKey);

            // Verifica che NESSUN altro DAO sia stato creato
            assertEquals(0, mockedVarDao.constructed().size());
            // Il secondo ProdottoDAO (suggeriti) non deve essere creato
            assertEquals(1, mockedProdDao.constructed().size());

            // Verifica che non ci sia stato nessun forward
            verify(dispatcher, never()).forward(request, response);
        }
    }

    // --- Test 4: Vulnerabilità (Varianti vuote) ---

    @Test
    @DisplayName("doGet con Varianti vuote lancia IndexOutOfBoundsException")
    void doGet_emptyVarianti_throwsException() throws ServletException, IOException {
        String primaryKey = "123";
        when(request.getParameter("primaryKey")).thenReturn(primaryKey);

        // Creiamo un Prodotto mockato
        Prodotto mockProdotto = mock(Prodotto.class);
        // Stub: getVarianti() restituisce una LISTA VUOTA
        when(mockProdotto.getVarianti()).thenReturn(new ArrayList<>());
        when(mockProdotto.getIdProdotto()).thenReturn("S123");

        try (MockedConstruction<ProdottoDAO> mockedProdDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            when(mock.doRetrieveById(primaryKey)).thenReturn(mockProdotto);
        });
             MockedConstruction<VarianteDAO> mockedVarDao = mockConstruction(VarianteDAO.class)) {

            // VERIFICA CHIAVE: ci aspettiamo un crash
            // Questo accade perché il codice chiama varianti.get(0).getGusto()
            assertThrows(IndexOutOfBoundsException.class, () -> {
                servlet.doGet(request, response);
            });

            // Verifichiamo che il forward non sia avvenuto
            verify(dispatcher, never()).forward(request, response);
        }
    }

    // --- Test 5: Happy Path (Successo) ---

    @Test
    @DisplayName("doGet con Prodotto valido popola attributi e fa il forward")
    void doGet_happyPath_populatesAttributesAndForwards() throws ServletException, IOException {
        String primaryKey = "123";
        when(request.getParameter("primaryKey")).thenReturn(primaryKey);

        // --- 1. Preparazione Dati Mock ---

        // Prodotto
        Prodotto mockProdotto = mock(Prodotto.class);
        when(mockProdotto.getIdProdotto()).thenReturn("S123");
        when(mockProdotto.getCategoria()).thenReturn("Proteine");

        // Varianti (con duplicati di gusto e peso)
        Variante v1 = new Variante(); v1.setGusto("Cioccolato"); v1.setPesoConfezione(900);
        Variante v2 = new Variante(); v2.setGusto("Vaniglia"); v2.setPesoConfezione(1000);
        Variante v3 = new Variante(); v3.setGusto("Cioccolato"); v3.setPesoConfezione(500);
        List<Variante> varianti = List.of(v1, v2, v3);
        when(mockProdotto.getVarianti()).thenReturn(varianti);

        // Il codice prende il gusto di varianti.get(0) ("Cioccolato")
        // Lista fittizia di ritorno per i pesi al "Cioccolato"
        List<Variante> variantiCriteria = List.of(v1, v3); // Pesi 900 e 500

        // Lista fittizia di ritorno per i suggeriti
        List<Prodotto> suggeriti = List.of(mock(Prodotto.class), mock(Prodotto.class));

        // --- 2. Mock dei DAO ---
        try (MockedConstruction<ProdottoDAO> mockedProdDao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
            // Questo mock intercetta ENTRAMBE le creazioni di ProdottoDAO
            when(mock.doRetrieveById(primaryKey)).thenReturn(mockProdotto);
            when(mock.doRetrieveByCriteria("categoria", "Proteine")).thenReturn(suggeriti);
        });
             MockedConstruction<VarianteDAO> mockedVarDao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
                 when(mock.doRetrieveVariantByCriteria("S123", "flavour", "Cioccolato")).thenReturn(variantiCriteria);
             })) {

            // --- 3. Esecuzione ---
            servlet.doGet(request, response);

            // --- 4. Verifica ---

            // Verifica Chiamate DAO
            ProdottoDAO daoProd1 = mockedProdDao.constructed().get(0); // Il primo DAO
            ProdottoDAO daoProd2 = mockedProdDao.constructed().get(1); // Il 'suggeritiDAO'
            VarianteDAO daoVar = mockedVarDao.constructed().get(0);

            verify(daoProd1).doRetrieveById(primaryKey);
            verify(daoVar).doRetrieveVariantByCriteria("S123", "flavour", "Cioccolato");
            verify(daoProd2).doRetrieveByCriteria("categoria", "Proteine");

            // Verifica Attributi (con cattura)
            ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);

            // Verifica Gusti (devono essere 2, non 3, a causa della de-duplicazione)
            verify(request).setAttribute(eq("allTastes"), listCaptor.capture());
            List<String> gusti = listCaptor.getValue();
            assertEquals(2, gusti.size());
            assertTrue(gusti.contains("Cioccolato") && gusti.contains("Vaniglia"));

            // Verifica Pesi (devono essere 2)
            verify(request).setAttribute(eq("firstWeights"), listCaptor.capture());
            List<Integer> pesi = listCaptor.getValue();
            assertEquals(2, pesi.size());
            assertTrue(pesi.contains(900) && pesi.contains(500));

            // Verifica altri attributi
            verify(request).setAttribute("suggeriti", suggeriti);
            verify(request).setAttribute("prodotto", mockProdotto);

            // Verifica Forward
            verify(dispatcher).forward(request, response);
        }
    }
}