package controller.homepage;

import jakarta.servlet.RequestDispatcher;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Classe di test per OrdineServlet.
 * Testa i percorsi di fallimento (NPE), i percorsi di blocco
 * e il percorso di successo (happy path).
 */
public class OrdineServletTest {

    private OrdineServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;

    @BeforeEach
    void setup() {
        servlet = new OrdineServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class); // Mockato ma non ancora "collegato"
        dispatcher = mock(RequestDispatcher.class);

        // Stub di base per il forward
        when(request.getRequestDispatcher("WEB-INF/Ordine.jsp")).thenReturn(dispatcher);
    }

    // --- Test 1: doGet ---

    @Test
    @DisplayName("doGet deve fallire con Method Not Allowed (405)")
    void doGet_shouldFailWith405() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getProtocol()).thenReturn("HTTP/1.1");

        servlet.doGet(request, response);

        // Verifica che venga chiamato super.doGet() che invia un 405
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), anyString());
    }

    // --- Test 2: Sessione null ---

    @Test
    @DisplayName("doPost con sessione nulla fa redirect a index.jsp")
    void sessioneNulla_redirectToIndex() throws ServletException, IOException {
        // La servlet chiama req.getSession(false), che restituisce null
        when(request.getSession(false)).thenReturn(null);

        servlet.doPost(request, response);

        // Verifica che venga fatto il redirect
        verify(response).sendRedirect("index.jsp");

        // Verifichiamo che non abbia provato a fare il forward
        verify(dispatcher, never()).forward(request, response);
    }

    // --- Test 3: Carrello null ---

    @Test
    @DisplayName("doPost con carrello nullo non fa nulla")
    void carrelloNullo_nonFaNulla() throws ServletException, IOException {
        // L'utente ha una sessione
        when(request.getSession(false)).thenReturn(session);
        // Ma il carrello non è mai stato inizializzato
        when(session.getAttribute("cart")).thenReturn(null);

        // Usiamo MockedConstruction per verificare che NESSUN DAO venga creato
        try (MockedConstruction<OrdineDao> mockedOrd = mockConstruction(OrdineDao.class)) {

            servlet.doPost(request, response);

            // L'if (cart != null && ...) fallisce, nessun DAO creato
            assertEquals(0, mockedOrd.constructed().size());
            verify(dispatcher, never()).forward(request, response);
        }
    }

    // --- Test 4: Utente non loggato ---

    @Test
    @DisplayName("doPost con utente non loggato non fa nulla")
    void utenteNonLoggato_nonFaNulla() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(session);
        // Il carrello c'è
        when(session.getAttribute("cart")).thenReturn(List.of(new Carrello()));
        // Ma l'utente non c'è
        when(session.getAttribute("Utente")).thenReturn(null);

        // Usiamo MockedConstruction per verificare che NESSUN DAO venga creato
        try (MockedConstruction<OrdineDao> mockedOrd = mockConstruction(OrdineDao.class);
                MockedConstruction<CarrelloDAO> mockedCart = mockConstruction(CarrelloDAO.class);
                MockedConstruction<DettaglioOrdineDAO> mockedDet = mockConstruction(DettaglioOrdineDAO.class)) {

            servlet.doPost(request, response);

            // L'if fallisce, nessun DAO viene creato
            assertEquals(0, mockedOrd.constructed().size());
            assertEquals(0, mockedCart.constructed().size());
            assertEquals(0, mockedDet.constructed().size());

            // Nessun forward
            verify(dispatcher, never()).forward(request, response);
        }
    }

    // --- Test 5: Carrello vuoto ---

    @Test
    @DisplayName("doPost con carrello vuoto non fa nulla")
    void carrelloVuoto_nonFaNulla() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(session);
        // Il carrello è una lista vuota
        when(session.getAttribute("cart")).thenReturn(new ArrayList<Carrello>());
        // L'utente c'è
        when(session.getAttribute("Utente")).thenReturn(new Utente());

        try (MockedConstruction<OrdineDao> mockedOrd = mockConstruction(OrdineDao.class)) {

            servlet.doPost(request, response);

            // L'if fallisce (a causa di !cart.isEmpty()), nessun DAO creato
            assertEquals(0, mockedOrd.constructed().size());
            verify(dispatcher, never()).forward(request, response);
        }
    }

    // --- Test 6: Happy Path (Checkout) ---

    @Test
    @DisplayName("Checkout con successo -> Salva, Pulisce sessione e fa Forward")
    void checkoutConSuccesso_completaOrdine() throws ServletException, IOException {
        // 1. Preparazione Dati
        Utente utente = new Utente();
        utente.setEmail("user@example.com");

        // Carrello con 2 items
        Carrello item1 = new Carrello();
        item1.setIdProdotto("P1");
        item1.setIdVariante(10);
        item1.setQuantita(2);

        Carrello item2 = new Carrello();
        item2.setIdProdotto("P2");
        item2.setIdVariante(20);
        item2.setQuantita(1);

        List<Carrello> cart = List.of(item1, item2);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("cart")).thenReturn(cart);
        when(session.getAttribute("Utente")).thenReturn(utente);

        // 2. Preparazione Mock DAO
        Ordine mockOrdine = new Ordine();
        mockOrdine.setIdOrdine(123);
        List<DettaglioOrdine> mockDettagli = new ArrayList<>();

        try (MockedConstruction<OrdineDao> mockedOrd = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            when(mock.getLastInsertedId()).thenReturn(123);
            when(mock.doRetrieveById(123)).thenReturn(mockOrdine);
        });
                MockedConstruction<CarrelloDAO> mockedCart = mockConstruction(CarrelloDAO.class);
                MockedConstruction<DettaglioOrdineDAO> mockedDet = mockConstruction(DettaglioOrdineDAO.class,
                        (mock, ctx) -> {
                            when(mock.doRetrieveById(123)).thenReturn(mockDettagli);
                        })) {

            // 3. Esecuzione
            servlet.doPost(request, response);

            // 4. Verifica
            OrdineDao ordineDao = mockedOrd.constructed().get(0);
            CarrelloDAO carrelloDAO = mockedCart.constructed().get(0);
            DettaglioOrdineDAO dettaglioDAO = mockedDet.constructed().get(0);

            // Kill Mutant 1 & Setters: Ordine
            ArgumentCaptor<Ordine> ordineCaptor = ArgumentCaptor.forClass(Ordine.class);
            verify(ordineDao).doSave(ordineCaptor.capture());
            assertEquals("user@example.com", ordineCaptor.getValue().getEmailUtente());

            // Kill Mutant 4 & Setters: DettaglioOrdine
            // We expect 2 saves. We capture all of them.
            ArgumentCaptor<DettaglioOrdine> dettagliCaptor = ArgumentCaptor.forClass(DettaglioOrdine.class);
            verify(dettaglioDAO, times(2)).doSave(dettagliCaptor.capture());

            List<DettaglioOrdine> capturedDetails = dettagliCaptor.getAllValues();
            assertEquals(2, capturedDetails.size());

            // Check Item 1 (P1, Var 10, Qty 2)
            DettaglioOrdine d1 = capturedDetails.stream().filter(d -> d.getIdProdotto().equals("P1")).findFirst()
                    .orElseThrow();
            assertEquals(123, d1.getIdOrdine());
            assertEquals(10, d1.getIdVariante());
            assertEquals(2, d1.getQuantita());

            // Check Item 2 (P2, Var 20, Qty 1)
            DettaglioOrdine d2 = capturedDetails.stream().filter(d -> d.getIdProdotto().equals("P2")).findFirst()
                    .orElseThrow();
            assertEquals(123, d2.getIdOrdine());
            assertEquals(20, d2.getIdVariante());
            assertEquals(1, d2.getQuantita());

            // Kill Mutant 3: session.removeAttribute("cart")
            verify(session).removeAttribute("cart");

            // Additional InOrder verification for sequence strictness
            org.mockito.InOrder inOrder = inOrder(ordineDao, session, carrelloDAO, dettaglioDAO, dispatcher, request);
            inOrder.verify(ordineDao).doSave(any(Ordine.class)); // Already strictly verified above
            inOrder.verify(ordineDao).getLastInsertedId();
            inOrder.verify(session).removeAttribute("cart");
            inOrder.verify(carrelloDAO).doRemoveCartByUser("user@example.com");
            inOrder.verify(dettaglioDAO, times(2)).doSave(any(DettaglioOrdine.class)); // Already verified above
            inOrder.verify(ordineDao).getLastInsertedId();
            inOrder.verify(ordineDao).doRetrieveById(123);
            inOrder.verify(dettaglioDAO).doRetrieveById(123);

            // Verify setAttribute to kill mutants there
            inOrder.verify(request).setAttribute(eq("order"), any(Ordine.class));
            inOrder.verify(request).setAttribute(eq("orderDetails"), anyList());
            inOrder.verify(dispatcher).forward(request, response);
        }
    }
}