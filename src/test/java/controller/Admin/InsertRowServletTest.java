package controller.Admin;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import model.Prodotto;
import model.ProdottoDAO;
import model.Utente;
import model.VarianteDAO;
import model.UtenteDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Classe di test (aggiornata) per insertRowServlet.
 * Verifica che la servlet gestisca input non validi in modo sicuro
 * e che la logica di inserimento (incluso l'upload di file) funzioni.
 */
public class InsertRowServletTest {
    private insertRowServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private RequestDispatcher dispatcher;

    @BeforeEach
    void setup() {
        servlet = new insertRowServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        dispatcher = mock(RequestDispatcher.class);
    }

    /**
     * Helper per impostare tutti i parametri validi per un test 'utente'.
     */
    private void setupValidUtenteParams() {
        when(request.getParameter("email")).thenReturn("user@example.com");
        when(request.getParameter("password")).thenReturn("Password1!");
        when(request.getParameter("nome")).thenReturn("Mario");
        when(request.getParameter("cognome")).thenReturn("Rossi");
        when(request.getParameter("codiceFiscale")).thenReturn("RSSMRA80A01H501U");
        when(request.getParameter("dataDiNascita")).thenReturn("1980-01-01");
        when(request.getParameter("indirizzo")).thenReturn("Via Roma 1");
        when(request.getParameter("telefono")).thenReturn("3331234567");
    }

    /**
     * Helper per impostare tutti i parametri validi per un test 'prodotto'.
     */
    private void setupValidProdottoParams() throws IOException, ServletException {
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("nome")).thenReturn("Proteine");
        when(request.getParameter("descrizione")).thenReturn("Descrizione test");
        when(request.getParameter("categoria")).thenReturn("Integratori");
        when(request.getParameter("calorie")).thenReturn("100");
        when(request.getParameter("carboidrati")).thenReturn("10");
        when(request.getParameter("proteine")).thenReturn("80");
        when(request.getParameter("grassi")).thenReturn("5");

        // Mock per l'upload del file
        Part filePart = mock(Part.class);
        InputStream inputStream = mock(InputStream.class);
        when(request.getPart("immagine")).thenReturn(filePart);
        when(filePart.getSubmittedFileName()).thenReturn("test-image.jpg");
        when(filePart.getInputStream()).thenReturn(inputStream);
        when(filePart.getSize()).thenReturn(1024L); // 1024 o qualsiasi numero > 0
    }

    // --- Test 1: Generali ---

    @Test
    @DisplayName("doGet deve fallire con Method Not Allowed (405)")
    void doGet_shouldFailWith405() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getProtocol()).thenReturn("HTTP/1.1");
        servlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), anyString());
    }

    // --- Test 2: Verifica Correzione Faglie (Input non validi) ---

    @Test
    @DisplayName("nameTable nullo -> Gestito e invia 400")
    void doPost_nullNameTable_sendsError() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn(null);

        servlet.doPost(request, response);

        // Verifica che la correzione (controllo di guardia) invii un errore
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Parametro 'nameTable' mancante.");
        verify(dispatcher, never()).forward(any(), any());
    }

    @Test
    @DisplayName("Input non numerico (Variante) -> Gestito e invia 500")
    void doPost_nonNumericParam_sendsError() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("variante");
        // Imposta tutti i parametri validi
        when(request.getParameter("idProdottoVariante")).thenReturn("P1");
        when(request.getParameter("idGusto")).thenReturn("1");
        when(request.getParameter("idConfezione")).thenReturn("1");
        when(request.getParameter("quantity")).thenReturn("10");
        when(request.getParameter("sconto")).thenReturn("0");

        // ... tranne questo
        when(request.getParameter("prezzo")).thenReturn("abc");

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class)) {

            servlet.doPost(request, response);

            // 'insertVariante' (corretto) cattura NFE, restituisce false.
            // La servlet invia l'errore "Invalid input data."
            verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
            verify(dispatcher, never()).forward(any(), any());
            assertEquals(0, dao.constructed().size()); // DAO non creato
        }
    }

    @Test
    @DisplayName("(File 'Part' nullo (Prodotto) -> Gestito e invia 500")
    void doPost_nullFilePart_sendsError() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("prodotto");
        setupValidProdottoParams(); // Imposta tutti i parametri...
        when(request.getPart("immagine")).thenReturn(null); // ... tranne il file

        servlet.doPost(request, response);

        // 'insertProdotto' (corretto) rileva il file nullo, restituisce false.
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
        verify(dispatcher, never()).forward(any(), any());
    }

    // --- Test 3: Happy Path (Inserimento Utente) ---

    @Test
    @DisplayName("Inserimento 'utente' (Happy Path) -> Chiama DAO e fa forward")
    void doPost_insertUtente_happyPath_forwards() throws ServletException, IOException, ParseException {
        setupValidUtenteParams(); // Imposta tutti i parametri validi
        when(request.getParameter("nameTable")).thenReturn("utente");

        // Stub del dispatcher per il forward
        when(request.getRequestDispatcher("showTable?tableName=utente")).thenReturn(dispatcher);

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class, (mock, ctx) -> {
            doNothing().when(mock).doSave(any(Utente.class));
        })) {

            servlet.doPost(request, response);

            // Verifica che il DAO sia stato creato e chiamato
            UtenteDAO mockDao = dao.constructed().get(0);

            // Cattura l'oggetto Utente passato al DAO
            ArgumentCaptor<Utente> utenteCaptor = ArgumentCaptor.forClass(Utente.class);
            verify(mockDao).doSave(utenteCaptor.capture());

            // Verifica che l'oggetto Utente catturato abbia i dati corretti
            Utente savedUser = utenteCaptor.getValue();
            assertEquals("Mario", savedUser.getNome());
            assertEquals("user@example.com", savedUser.getEmail());
            // Verifica che la password sia stata hashata (non è più quella in chiaro)
            assertNotEquals("Password1!", savedUser.getPassword());

            // Verifica che il forward sia avvenuto
            verify(dispatcher).forward(request, response);
            verify(response, never()).sendError(anyInt(), anyString());
        }
    }

    // --- Test 4: Happy Path (Inserimento Prodotto con Upload) ---

    @Test
    @DisplayName("Inserimento 'prodotto' (Happy Path) -> Salva file, chiama DAO e fa forward")
    void doPost_insertProdotto_happyPath_forwards() throws ServletException, IOException {
        // 1. Setup Request
        setupValidProdottoParams();
        when(request.getParameter("nameTable")).thenReturn("prodotto");

        // 2. Mock degli oggetti per il forward
        when(request.getRequestDispatcher("showTable?tableName=prodotto")).thenReturn(dispatcher);

        // --- INIZIO CORREZIONE TEST ---
        // Dobbiamo mockare ServletConfig e ServletContext
        ServletContext servletContext = mock(ServletContext.class);
        ServletConfig servletConfig = mock(ServletConfig.class);

        // Collega i mock
        when(servletConfig.getServletContext()).thenReturn(servletContext);

        // Inizializza manualmente la servlet
        servlet.init(servletConfig);
        // --- FINE CORREZIONE TEST ---

        // 3. Mock avanzato per il sistema di file (Paths e Files)
        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class);
             MockedStatic<Files> filesMock = mockStatic(Files.class)) {

            // Prepara i mock per i Path
            Path mockPath = mock(Path.class);
            Path mockParentPath = mock(Path.class);

            pathsMock.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getFileName()).thenReturn(mockPath);
            when(mockPath.toString()).thenReturn("test-image.jpg");

            // Simula la logica di 'getServletContext().getRealPath(...)'
            // (Ora usa servletContext, non request.getServletContext())
            when(servletContext.getRealPath("Immagini/test-image.jpg")).thenReturn("/fake/path/Immagini/test-image.jpg");

            // (Il resto dei mock di Files e DAO rimane identico...)
            filesMock.when(() -> Files.exists(any(Path.class))).thenReturn(false);
            when(mockPath.getParent()).thenReturn(mockParentPath);
            filesMock.when(() -> Files.createDirectories(any(Path.class))).thenReturn(mockParentPath);
            filesMock.when(() -> Files.copy(any(InputStream.class), any(Path.class))).thenReturn(0L);

            // 4. Mock del DAO (ProdottoDAO)
            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                doNothing().when(mock).doSave(any(Prodotto.class));
            })) {

                // 5. Esegui la servlet
                servlet.doPost(request, response);

                // 6. Verifica (rimane identica)
                filesMock.verify(() -> Files.copy(any(InputStream.class), any(Path.class)));
                ProdottoDAO mockDao = dao.constructed().get(0);
                ArgumentCaptor<Prodotto> pCaptor = ArgumentCaptor.forClass(Prodotto.class);
                verify(mockDao).doSave(pCaptor.capture());
                assertEquals("Immagini/test-image.jpg", pCaptor.getValue().getImmagine());
                verify(dispatcher).forward(request, response);
            }
        }
    }

    // --- Test 5: Sad Path (Logica di business) ---
    @Test
    @DisplayName("Inserimento 'utente' (Sad Path) -> 'isValid' false -> Invia 500")
    void doPost_insertUtente_invalidParam_sendsError() throws ServletException, IOException {
        setupValidUtenteParams(); // Imposta tutti i parametri validi
        when(request.getParameter("nome")).thenReturn(""); // ... tranne questo (isBlank)
        when(request.getParameter("nameTable")).thenReturn("utente");

        try (MockedConstruction<UtenteDAO> dao = mockConstruction(UtenteDAO.class)) {

            servlet.doPost(request, response);

            // Il metodo 'isValid' restituisce false, 'insertUtente' restituisce false.
            verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
            verify(dispatcher, never()).forward(any(), any());
            // Il DAO non deve essere creato
            assertEquals(0, dao.constructed().size());
        }
    }
}
