package controller.utente;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Classe di test per AreaPersonaleServlet.
 */
public class AreaPersonaleServletTest {

    private AreaPersonaleServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;

    @BeforeEach
    void setup() throws Exception {
        servlet = new AreaPersonaleServlet();
        
        // Mock ServletConfig e ServletContext per permettere il logging
        ServletConfig servletConfig = mock(ServletConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        
        // Inizializza il servlet con il config mockato
        servlet.init(servletConfig);
        
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        // Stub di base
        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher("WEB-INF/AreaUtente.jsp")).thenReturn(dispatcher);
    }


    // --- Test Utente non loggato ---

    @Test
    @DisplayName("doPost con utente nullo non deve fare nulla")
    void doPost_noUser_doesNothing() throws ServletException, IOException {
        when(session.getAttribute("Utente")).thenReturn(null);

        servlet.doPost(request, response);

        // Nessun forward, nessun setAttribute, nessuna chiamata al dispatcher
        verify(request, never()).getRequestDispatcher(anyString());
        verify(dispatcher, never()).forward(any(), any());
        verify(request, never()).setAttribute(anyString(), any());
    }

    // --- Test Utente loggato, nessun ordine ---

    @Test
    @DisplayName("doPost con utente loggato ma 0 ordini, inoltra con liste vuote")
    void doPost_userWithNoOrders_forwardsEmptyLists() throws ServletException, IOException {
        // Prepara utente
        Utente utente = new Utente();
        utente.setEmail("user@example.com");
        when(session.getAttribute("Utente")).thenReturn(utente);

        // Lista ordini vuota
        List<Ordine> emptyOrders = new ArrayList<>();

        // Mock dei DAO
        try (MockedConstruction<OrdineDao> mockedOrdineDao = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            // Stub: doRetrieveByEmail restituisce una lista vuota
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(emptyOrders);
        });
             MockedConstruction<DettaglioOrdineDAO> mockedDettaglioDao = mockConstruction(DettaglioOrdineDAO.class)) {

            servlet.doPost(request, response);

            // Verifica che il DAO degli ordini sia stato chiamato
            verify(mockedOrdineDao.constructed().get(0)).doRetrieveByEmail("user@example.com");
            // Verifica che il DAO dei dettagli NON sia stato chiamato (perché il loop era vuoto)
            verify(mockedDettaglioDao.constructed().get(0), never()).doRetrieveById(anyInt());

            // Cattura gli attributi impostati sulla request
            ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<HashMap> mapCaptor = ArgumentCaptor.forClass(HashMap.class);

            // Verifica che "ordini" sia la lista vuota
            verify(request).setAttribute(eq("ordini"), listCaptor.capture());
            assertTrue(listCaptor.getValue().isEmpty());

            // Verifica che "dettaglioOrdini" sia una mappa vuota
            verify(request).setAttribute(eq("dettaglioOrdini"), mapCaptor.capture());
            assertTrue(mapCaptor.getValue().isEmpty());

            // Verifica l'inoltro
            verify(dispatcher).forward(request, response);
        }
    }

    // --- Test Ramo Parsing Descrizione ---

    @Test
    @DisplayName("doPost usa il parsing della descrizione se disponibile")
    void doPost_userWithOrder_usesDescrizioneParsing() throws ServletException, IOException {
        Utente utente = new Utente();
        utente.setEmail("user@example.com");
        when(session.getAttribute("Utente")).thenReturn(utente);

        // Prepara un ordine con descrizione
        Ordine ordine1 = new Ordine();
        ordine1.setIdOrdine(101);
        String descrizione = "Prodotto: Proteine Whey\n" +
                "Gusto: Cioccolato\n" +
                "Confezione: 900 grammi\n" +
                "Quantità: 2\n" +
                "Prezzo: 45.50 €\n";
        ordine1.setDescrizione(descrizione);
        List<Ordine> ordini = List.of(ordine1);

        try (MockedConstruction<OrdineDao> mockedOrdineDao = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(ordini);
        });
             MockedConstruction<DettaglioOrdineDAO> mockedDettaglioDao = mockConstruction(DettaglioOrdineDAO.class)) {

            servlet.doPost(request, response);

            // VERIFICA CHIAVE: Il DAO dei dettagli NON deve essere chiamato
            verify(mockedDettaglioDao.constructed().get(0), never()).doRetrieveById(anyInt());

            // Cattura la mappa "dettaglioOrdini"
            ArgumentCaptor<HashMap> mapCaptor = ArgumentCaptor.forClass(HashMap.class);
            verify(request).setAttribute(eq("dettaglioOrdini"), mapCaptor.capture());
            HashMap<Integer, List<DettaglioOrdine>> capturedMap = mapCaptor.getValue();

            // Verifica il contenuto della mappa
            assertTrue(capturedMap.containsKey(101));
            List<DettaglioOrdine> dettagli = capturedMap.get(101);
            assertEquals(1, dettagli.size());

            // Verifica che il parsing sia avvenuto correttamente
            DettaglioOrdine d = dettagli.get(0);
            assertEquals("Proteine Whey", d.getNomeProdotto());
            assertEquals("Cioccolato", d.getGusto());
            assertEquals(900, d.getPesoConfezione());
            assertEquals(2, d.getQuantita());
            assertEquals(45.50f, d.getPrezzo());

            verify(dispatcher).forward(request, response);
        }
    }

    // --- Test Ramo Chiamata DAO ---

    @Test
    @DisplayName("doPost usa il DAO se la descrizione è null")
    void doPost_userWithOrder_usesDAO_whenDescrizioneIsNull() throws ServletException, IOException {
        testChiamataDAO(null); // Chiama il metodo helper con descrizione null
    }

    @Test
    @DisplayName("doPost usa il DAO se la descrizione è vuota")
    void doPost_userWithOrder_usesDAO_whenDescrizioneIsEmpty() throws ServletException, IOException {
        testChiamataDAO(""); // Chiama il metodo helper con descrizione vuota
    }

    // Metodo helper per raggruppare i test 5 e 6
    private void testChiamataDAO(String descrizione) throws ServletException, IOException {
        Utente utente = new Utente();
        utente.setEmail("user@example.com");
        when(session.getAttribute("Utente")).thenReturn(utente);

        // Prepara ordine (descrizione null o vuota)
        Ordine ordine1 = new Ordine();
        ordine1.setIdOrdine(102);
        ordine1.setDescrizione(descrizione);
        List<Ordine> ordini = List.of(ordine1);

        // Prepara i dettagli che il DAO "restituirà"
        DettaglioOrdine dettaglioDalDB = new DettaglioOrdine();
        dettaglioDalDB.setNomeProdotto("Prodotto da DB");
        List<DettaglioOrdine> dettagliFromDB = List.of(dettaglioDalDB);

        try (MockedConstruction<OrdineDao> mockedOrdineDao = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(ordini);
        });
             MockedConstruction<DettaglioOrdineDAO> mockedDettaglioDao = mockConstruction(DettaglioOrdineDAO.class, (mock, ctx) -> {
                 // Stub: Il DAO restituisce i dettagli preimpostati
                 when(mock.doRetrieveById(102)).thenReturn(dettagliFromDB);
             })) {

            servlet.doPost(request, response);

            // VERIFICA CHIAVE: Il DAO dei dettagli DEVE essere chiamato con l'ID corretto
            verify(mockedDettaglioDao.constructed().get(0)).doRetrieveById(102);

            // Verifica che la mappa contenga i dati restituiti dal DAO
            ArgumentCaptor<HashMap> mapCaptor = ArgumentCaptor.forClass(HashMap.class);
            verify(request).setAttribute(eq("dettaglioOrdini"), mapCaptor.capture());
            HashMap<Integer, List<DettaglioOrdine>> capturedMap = mapCaptor.getValue();

            assertEquals(dettagliFromDB, capturedMap.get(102));
            assertEquals("Prodotto da DB", capturedMap.get(102).get(0).getNomeProdotto());

            verify(dispatcher).forward(request, response);
        }
    }

    // --- Test Fallimento Parsing (Dependability) ---

    @Test
    @DisplayName("doPost salta i prodotti malformati e continua l'elaborazione")
    void doPost_parseDescrizione_skipsMalformedProducts() throws Exception {
        Utente utente = new Utente();
        utente.setEmail("user@example.com");
        when(session.getAttribute("Utente")).thenReturn(utente);

        // Prepara un ordine con una descrizione contenente un prodotto malformato e uno valido
        Ordine ordine1 = new Ordine();
        ordine1.setIdOrdine(103);
        // Il primo prodotto ha una quantità malformata ("due" invece di un numero)
        // Il secondo prodotto è valido
        String mixedDesc = "Prodotto: Proteine Whey\n" +
                "Gusto: Cioccolato\n" +
                "Confezione: 900 grammi\n" +
                "Quantità: due\n" +
                "Prezzo: 45.50 €\n" +
                ";" +
                "Prodotto: Creatina\n" +
                "Gusto: Neutro\n" +
                "Confezione: 500 grammi\n" +
                "Quantità: 1\n" +
                "Prezzo: 25.00 €\n";
        ordine1.setDescrizione(mixedDesc);
        List<Ordine> ordini = List.of(ordine1);

        try (MockedConstruction<OrdineDao> mockedOrdineDao = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(ordini);
        });
             MockedConstruction<DettaglioOrdineDAO> mockedDettaglioDao = mockConstruction(DettaglioOrdineDAO.class)) {

            servlet.doPost(request, response);

            // VERIFICA CHIAVE: La servlet gestisce l'errore gracefully e continua
            // Cattura la mappa "dettaglioOrdini"
            ArgumentCaptor<HashMap> mapCaptor = ArgumentCaptor.forClass(HashMap.class);
            verify(request).setAttribute(eq("dettaglioOrdini"), mapCaptor.capture());
            HashMap<Integer, List<DettaglioOrdine>> capturedMap = mapCaptor.getValue();

            // La mappa contiene l'ordine
            assertTrue(capturedMap.containsKey(103));
            List<DettaglioOrdine> dettagli = capturedMap.get(103);
            
            // VERIFICA: Solo il prodotto valido (Creatina) è stato aggiunto
            // Il prodotto malformato (Proteine Whey con quantità "due") è stato saltato
            assertEquals(1, dettagli.size());
            assertEquals("Creatina", dettagli.get(0).getNomeProdotto());
            assertEquals("Neutro", dettagli.get(0).getGusto());
            assertEquals(500, dettagli.get(0).getPesoConfezione());
            assertEquals(1, dettagli.get(0).getQuantita());
            assertEquals(25.00f, dettagli.get(0).getPrezzo());

            // L'inoltro deve avvenire normalmente
            verify(dispatcher).forward(request, response);
            // Il DAO dei dettagli non deve essere chiamato (usiamo la descrizione)
            verify(mockedDettaglioDao.constructed().get(0), never()).doRetrieveById(anyInt());
        }
    }
}