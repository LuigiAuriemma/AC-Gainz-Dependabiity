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
import org.mockito.stubbing.Stubber;

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
    private ServletContext servletContext;

    @BeforeEach
    void setup() throws Exception {
        servlet = new AreaPersonaleServlet();

        // Mock ServletConfig e ServletContext
        ServletConfig servletConfig = mock(ServletConfig.class);
        servletContext = mock(ServletContext.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletConfig.getServletName()).thenReturn("AreaPersonaleServlet");

        // Inizializza il servlet
        servlet.init(servletConfig);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        // Stub di base
        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher("WEB-INF/AreaUtente.jsp")).thenReturn(dispatcher);
        // FIX: Mock protocol per evitare NPE in super.doGet
        when(request.getProtocol()).thenReturn("HTTP/1.1");
    }

    // --- Test Utente non loggato ---

    @Test
    @DisplayName("doPost con utente nullo non deve fare nulla")
    void doPost_noUser_doesNothing() throws ServletException, IOException {
        when(session.getAttribute("Utente")).thenReturn(null);

        servlet.doPost(request, response);

        verify(request, never()).getRequestDispatcher(anyString());
        verify(dispatcher, never()).forward(any(), any());
        verify(request, never()).setAttribute(anyString(), any());
    }

    // --- Test Utente loggato, nessun ordine ---

    @Test
    @DisplayName("doPost con utente loggato ma 0 ordini, inoltra con liste vuote")
    void doPost_userWithNoOrders_forwardsEmptyLists() throws ServletException, IOException {
        Utente utente = new Utente();
        utente.setEmail("user@example.com");
        when(session.getAttribute("Utente")).thenReturn(utente);

        List<Ordine> emptyOrders = new ArrayList<>();

        try (MockedConstruction<OrdineDao> mockedOrdineDao = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            doReturn(emptyOrders).when(mock).doRetrieveByEmail("user@example.com");
        });
                MockedConstruction<DettaglioOrdineDAO> mockedDettaglioDao = mockConstruction(
                        DettaglioOrdineDAO.class)) {

            servlet.doPost(request, response);

            verify(mockedOrdineDao.constructed().get(0)).doRetrieveByEmail("user@example.com");
            verify(mockedDettaglioDao.constructed().get(0), never()).doRetrieveById(anyInt());

            ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<HashMap> mapCaptor = ArgumentCaptor.forClass(HashMap.class);

            verify(request).setAttribute(eq("ordini"), listCaptor.capture());
            assertTrue(listCaptor.getValue().isEmpty());

            verify(request).setAttribute(eq("dettaglioOrdini"), mapCaptor.capture());
            assertTrue(mapCaptor.getValue().isEmpty());

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
                MockedConstruction<DettaglioOrdineDAO> mockedDettaglioDao = mockConstruction(
                        DettaglioOrdineDAO.class)) {

            servlet.doPost(request, response);

            verify(mockedDettaglioDao.constructed().get(0), never()).doRetrieveById(anyInt());

            ArgumentCaptor<HashMap> mapCaptor = ArgumentCaptor.forClass(HashMap.class);
            verify(request).setAttribute(eq("dettaglioOrdini"), mapCaptor.capture());
            HashMap<Integer, List<DettaglioOrdine>> capturedMap = mapCaptor.getValue();

            assertTrue(capturedMap.containsKey(101));
            List<DettaglioOrdine> dettagli = capturedMap.get(101);
            assertEquals(1, dettagli.size());

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
        testChiamataDAO(null);
    }

    @Test
    @DisplayName("doPost usa il DAO se la descrizione è vuota")
    void doPost_userWithOrder_usesDAO_whenDescrizioneIsEmpty() throws ServletException, IOException {
        testChiamataDAO("");
    }

    private void testChiamataDAO(String descrizione) throws ServletException, IOException {
        Utente utente = new Utente();
        utente.setEmail("user@example.com");
        when(session.getAttribute("Utente")).thenReturn(utente);

        Ordine ordine1 = new Ordine();
        ordine1.setIdOrdine(102);
        ordine1.setDescrizione(descrizione);
        List<Ordine> ordini = List.of(ordine1);

        DettaglioOrdine dettaglioDalDB = new DettaglioOrdine();
        dettaglioDalDB.setNomeProdotto("Prodotto da DB");
        List<DettaglioOrdine> dettagliFromDB = List.of(dettaglioDalDB);

        try (MockedConstruction<OrdineDao> mockedOrdineDao = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            when(mock.doRetrieveByEmail("user@example.com")).thenReturn(ordini);
        });
                MockedConstruction<DettaglioOrdineDAO> mockedDettaglioDao = mockConstruction(DettaglioOrdineDAO.class,
                        (mock, ctx) -> {
                            when(mock.doRetrieveById(102)).thenReturn(dettagliFromDB);
                        })) {

            servlet.doPost(request, response);

            verify(mockedDettaglioDao.constructed().get(0)).doRetrieveById(102);

            ArgumentCaptor<HashMap> mapCaptor = ArgumentCaptor.forClass(HashMap.class);
            verify(request).setAttribute(eq("dettaglioOrdini"), mapCaptor.capture());
            HashMap<Integer, List<DettaglioOrdine>> capturedMap = mapCaptor.getValue();

            assertEquals(dettagliFromDB, capturedMap.get(102));
            assertEquals("Prodotto da DB", capturedMap.get(102).get(0).getNomeProdotto());

            verify(dispatcher).forward(request, response);
        }
    }

    @Test
    @DisplayName("doPost salta i prodotti malformati e continua l'elaborazione")
    void doPost_parseDescrizione_skipsMalformedProducts() throws Exception {
        Utente utente = new Utente();
        utente.setEmail("user@example.com");
        when(session.getAttribute("Utente")).thenReturn(utente);

        Ordine ordine1 = new Ordine();
        ordine1.setIdOrdine(103);
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
                MockedConstruction<DettaglioOrdineDAO> mockedDettaglioDao = mockConstruction(
                        DettaglioOrdineDAO.class)) {

            servlet.doPost(request, response);

            ArgumentCaptor<HashMap> mapCaptor = ArgumentCaptor.forClass(HashMap.class);
            verify(request).setAttribute(eq("dettaglioOrdini"), mapCaptor.capture());
            HashMap<Integer, List<DettaglioOrdine>> capturedMap = mapCaptor.getValue();

            assertTrue(capturedMap.containsKey(103));
            List<DettaglioOrdine> dettagli = capturedMap.get(103);

            assertEquals(1, dettagli.size());
            assertEquals("Creatina", dettagli.get(0).getNomeProdotto());
            assertEquals("Neutro", dettagli.get(0).getGusto());
            assertEquals(500, dettagli.get(0).getPesoConfezione());
            assertEquals(1, dettagli.get(0).getQuantita());
            assertEquals(25.00f, dettagli.get(0).getPrezzo());

            verify(dispatcher).forward(request, response);
        }
    }

    @Test
    @DisplayName("doGet invia errore 405 (Method Not Allowed)")
    void doGet_sendsMethodNotAllowed() throws ServletException, IOException {
        servlet.doGet(request, response);
        // Poiché abbiamo mockato getProtocol()=HTTP/1.1, deve chiamare sendError(405,
        // ...)
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), anyString());
    }

    @Test
    @DisplayName("doGet lancia eccezione -> logga errore e invia 500")
    void doGet_exception_sendsError500() throws ServletException, IOException {
        // Simuliamo eccezione lanciata dalla response per entrare nel catch di doGet
        // Questo funziona perché super.doGet chiamerà sendError(405).
        doThrow(new IOException("Simulated IO")).when(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED),
                anyString());

        servlet.doGet(request, response);

        // Verifica chiamate
        verify(servletContext).log(eq("AreaPersonaleServlet: Errore in AreaPersonaleServlet doGet"),
                any(Exception.class));
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
    }

    @Test
    @DisplayName("doPost lancia eccezione -> logga errore e invia 500")
    void doPost_exception_sendsError500() throws ServletException, IOException {
        // Simuliamo eccezione nel recupero sessione
        when(request.getSession()).thenThrow(new RuntimeException("Session Error"));

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Errore durante il recupero dell'area personale.");
        verify(servletContext).log(eq("AreaPersonaleServlet: Errore in AreaPersonaleServlet doPost"),
                any(Exception.class));
    }
}