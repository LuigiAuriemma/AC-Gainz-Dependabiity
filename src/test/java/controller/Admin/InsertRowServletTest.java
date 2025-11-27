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

import model.Variante;
import model.Ordine;
import model.OrdineDao;
import model.DettaglioOrdine;
import model.DettaglioOrdineDAO;
import model.Gusto;
import model.GustoDAO;
import model.Confezione;
import model.ConfezioneDAO;

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

    @Test
    @DisplayName("Invalid table name -> Sends 400")
    void doPost_invalidTableName_sendsError() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("invalid_table");
        servlet.doPost(request, response);
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid table name.");
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
            when(servletContext.getRealPath("Immagini/test-image.jpg"))
                    .thenReturn("/fake/path/Immagini/test-image.jpg");

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

    // --- Test 6: Variante ---

    @Test
    @DisplayName("Inserimento 'variante' (Happy Path)")
    void doPost_insertVariante_happyPath() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("variante");
        when(request.getParameter("idProdottoVariante")).thenReturn("P1");
        when(request.getParameter("idGusto")).thenReturn("1");
        when(request.getParameter("idConfezione")).thenReturn("1");
        when(request.getParameter("prezzo")).thenReturn("10.5");
        when(request.getParameter("quantity")).thenReturn("100");
        when(request.getParameter("sconto")).thenReturn("20");
        when(request.getParameter("evidenza")).thenReturn("1");

        when(request.getRequestDispatcher("showTable?tableName=variante")).thenReturn(dispatcher);

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class, (mock, ctx) -> {
            doNothing().when(mock).doSaveVariante(any(Variante.class));
        })) {
            servlet.doPost(request, response);

            verify(dispatcher).forward(request, response);
            VarianteDAO mockDao = dao.constructed().get(0);
            verify(mockDao).doSaveVariante(any(Variante.class));
        }
    }

    @Test
    @DisplayName("Inserimento 'variante' (Sad Path: Logical Validation)")
    void doPost_insertVariante_logicalError() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("variante");
        when(request.getParameter("idProdottoVariante")).thenReturn("P1");
        when(request.getParameter("idGusto")).thenReturn("1");
        when(request.getParameter("idConfezione")).thenReturn("1");
        when(request.getParameter("prezzo")).thenReturn("-10.5"); // Invalid price
        when(request.getParameter("quantity")).thenReturn("100");
        when(request.getParameter("sconto")).thenReturn("20");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    // --- Test 7: Ordine ---

    @Test
    @DisplayName("Inserimento 'ordine' (Happy Path)")
    void doPost_insertOrdine_happyPath() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("ordine");
        when(request.getParameter("emailUtente")).thenReturn("user@example.com");
        when(request.getParameter("stato")).thenReturn("Consegnato");
        when(request.getParameter("totale")).thenReturn("50.0");
        when(request.getParameter("data")).thenReturn("2023-01-01");

        when(request.getRequestDispatcher("showTable?tableName=ordine")).thenReturn(dispatcher);

        try (MockedConstruction<OrdineDao> dao = mockConstruction(OrdineDao.class, (mock, ctx) -> {
            doNothing().when(mock).doSave(any(Ordine.class));
        })) {
            servlet.doPost(request, response);

            verify(dispatcher).forward(request, response);
            OrdineDao mockDao = dao.constructed().get(0);
            verify(mockDao).doSave(any(Ordine.class));
        }
    }

    @Test
    @DisplayName("Inserimento 'ordine' (Sad Path: Invalid Date)")
    void doPost_insertOrdine_invalidDate() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("ordine");
        when(request.getParameter("emailUtente")).thenReturn("user@example.com");
        when(request.getParameter("stato")).thenReturn("Consegnato");
        when(request.getParameter("totale")).thenReturn("50.0");
        when(request.getParameter("data")).thenReturn("invalid-date");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    // --- Test 8: DettaglioOrdine ---

    @Test
    @DisplayName("Inserimento 'dettagliOrdine' (Happy Path)")
    void doPost_insertDettaglioOrdine_happyPath() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("dettagliOrdine");
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("idVariante")).thenReturn("1");
        when(request.getParameter("quantity")).thenReturn("5");

        when(request.getRequestDispatcher("showTable?tableName=dettagliOrdine")).thenReturn(dispatcher);

        try (MockedConstruction<DettaglioOrdineDAO> dao = mockConstruction(DettaglioOrdineDAO.class, (mock, ctx) -> {
            doNothing().when(mock).doSave(any(DettaglioOrdine.class));
        })) {
            servlet.doPost(request, response);

            verify(dispatcher).forward(request, response);
            DettaglioOrdineDAO mockDao = dao.constructed().get(0);
            verify(mockDao).doSave(any(DettaglioOrdine.class));
        }
    }

    @Test
    @DisplayName("Inserimento 'dettagliOrdine' (Sad Path: Quantity <= 0)")
    void doPost_insertDettaglioOrdine_invalidQuantity() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("dettagliOrdine");
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("idVariante")).thenReturn("1");
        when(request.getParameter("quantity")).thenReturn("0");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    @Test
    @DisplayName("Inserimento 'dettagliOrdine' (Sad Path: NumberFormatException)")
    void doPost_insertDettaglioOrdine_numberFormatError() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("dettagliOrdine");
        when(request.getParameter("idOrdine")).thenReturn("1");
        when(request.getParameter("idProdotto")).thenReturn("P1");
        when(request.getParameter("idVariante")).thenReturn("1");
        when(request.getParameter("quantity")).thenReturn("abc");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    // --- Test 9: Gusto ---

    @Test
    @DisplayName("Inserimento 'gusto' (Happy Path)")
    void doPost_insertGusto_happyPath() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("gusto");
        when(request.getParameter("nomeGusto")).thenReturn("Cioccolato");

        when(request.getRequestDispatcher("showTable?tableName=gusto")).thenReturn(dispatcher);

        try (MockedConstruction<GustoDAO> dao = mockConstruction(GustoDAO.class, (mock, ctx) -> {
            doNothing().when(mock).doSaveGusto(any(Gusto.class));
        })) {
            servlet.doPost(request, response);

            verify(dispatcher).forward(request, response);
            GustoDAO mockDao = dao.constructed().get(0);
            verify(mockDao).doSaveGusto(any(Gusto.class));
        }
    }

    @Test
    @DisplayName("Inserimento 'gusto' (Sad Path: Empty Name)")
    void doPost_insertGusto_emptyName() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("gusto");
        when(request.getParameter("nomeGusto")).thenReturn("");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    // --- Test 10: Confezione ---

    @Test
    @DisplayName("Inserimento 'confezione' (Happy Path)")
    void doPost_insertConfezione_happyPath() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("confezione");
        when(request.getParameter("pesoConfezione")).thenReturn("500");

        when(request.getRequestDispatcher("showTable?tableName=confezione")).thenReturn(dispatcher);

        try (MockedConstruction<ConfezioneDAO> dao = mockConstruction(ConfezioneDAO.class, (mock, ctx) -> {
            doNothing().when(mock).doSaveConfezione(any(Confezione.class));
        })) {
            servlet.doPost(request, response);

            verify(dispatcher).forward(request, response);
            ConfezioneDAO mockDao = dao.constructed().get(0);
            verify(mockDao).doSaveConfezione(any(Confezione.class));
        }
    }

    @Test
    @DisplayName("Inserimento 'confezione' (Sad Path: Invalid Weight)")
    void doPost_insertConfezione_invalidWeight() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("confezione");
        when(request.getParameter("pesoConfezione")).thenReturn("0");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    @Test
    @DisplayName("Inserimento 'confezione' (Sad Path: NumberFormatException)")
    void doPost_insertConfezione_numberFormatError() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("confezione");
        when(request.getParameter("pesoConfezione")).thenReturn("abc");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    // --- Test 11: Utente ParseException ---

    @Test
    @DisplayName("Inserimento 'utente' (Sad Path: ParseException) -> Invia 500")
    void doPost_insertUtente_parseException() throws ServletException, IOException {
        setupValidUtenteParams();
        when(request.getParameter("nameTable")).thenReturn("utente");
        when(request.getParameter("dataDiNascita")).thenReturn("invalid-date"); // Causes ParseException

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    // --- Test 12: Prodotto File Collision & Edge Cases ---

    @Test
    @DisplayName("Inserimento 'prodotto' (File Collision) -> Rinomina file")
    void doPost_insertProdotto_fileCollision() throws ServletException, IOException {
        setupValidProdottoParams();
        when(request.getParameter("nameTable")).thenReturn("prodotto");
        when(request.getRequestDispatcher("showTable?tableName=prodotto")).thenReturn(dispatcher);

        ServletContext servletContext = mock(ServletContext.class);
        ServletConfig servletConfig = mock(ServletConfig.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        servlet.init(servletConfig);

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class);
                MockedStatic<Files> filesMock = mockStatic(Files.class)) {

            Path mockPath = mock(Path.class);
            Path mockParentPath = mock(Path.class);

            pathsMock.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getFileName()).thenReturn(mockPath);
            when(mockPath.toString()).thenReturn("test-image.jpg");

            // Mock collision: first exists, second doesn't
            when(servletContext.getRealPath(anyString())).thenReturn("/fake/path");
            filesMock.when(() -> Files.exists(any(Path.class)))
                    .thenReturn(true) // First check: exists (collision)
                    .thenReturn(false); // Second check: free

            when(mockPath.getParent()).thenReturn(mockParentPath);
            filesMock.when(() -> Files.createDirectories(any(Path.class))).thenReturn(mockParentPath);
            filesMock.when(() -> Files.copy(any(InputStream.class), any(Path.class))).thenReturn(0L);

            try (MockedConstruction<ProdottoDAO> dao = mockConstruction(ProdottoDAO.class, (mock, ctx) -> {
                doNothing().when(mock).doSave(any(Prodotto.class));
            })) {
                servlet.doPost(request, response);

                verify(dispatcher).forward(request, response);
                // Verify that the loop ran (Files.exists called at least twice)
                filesMock.verify(() -> Files.exists(any(Path.class)), atLeast(2));
            }
        }
    }

    @Test
    @DisplayName("Inserimento 'prodotto' (Sad Path: File size 0) -> Invia 500")
    void doPost_insertProdotto_emptyFile() throws ServletException, IOException {
        setupValidProdottoParams();
        when(request.getParameter("nameTable")).thenReturn("prodotto");

        Part filePart = request.getPart("immagine");
        when(filePart.getSize()).thenReturn(0L);

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    @Test
    @DisplayName("Inserimento 'prodotto' (Sad Path: Missing Fields) -> Invia 500")
    void doPost_insertProdotto_missingFields() throws ServletException, IOException {
        setupValidProdottoParams();
        when(request.getParameter("nameTable")).thenReturn("prodotto");
        when(request.getParameter("nome")).thenReturn(""); // Missing required field

        // Initialize servlet to avoid IllegalStateException on getServletContext()
        ServletContext servletContext = mock(ServletContext.class);
        ServletConfig servletConfig = mock(ServletConfig.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        servlet.init(servletConfig);

        // Mock getRealPath to avoid NPE if called
        when(servletContext.getRealPath(anyString())).thenReturn("/fake/path");

        // Mock Files/Paths to avoid actual file operations if reached
        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class);
                MockedStatic<Files> filesMock = mockStatic(Files.class)) {

            Path mockPath = mock(Path.class);
            pathsMock.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getFileName()).thenReturn(mockPath);
            when(mockPath.toString()).thenReturn("test-image.jpg");
            when(mockPath.getParent()).thenReturn(mock(Path.class));

            servlet.doPost(request, response);
        }

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    // --- Test 13: Variante Extra Branches ---

    @Test
    @DisplayName("Inserimento 'variante' (Evidenza true)")
    void doPost_insertVariante_evidenzaTrue() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("variante");
        when(request.getParameter("idProdottoVariante")).thenReturn("P1");
        when(request.getParameter("idGusto")).thenReturn("1");
        when(request.getParameter("idConfezione")).thenReturn("1");
        when(request.getParameter("prezzo")).thenReturn("10.5");
        when(request.getParameter("quantity")).thenReturn("100");
        when(request.getParameter("sconto")).thenReturn("20");
        when(request.getParameter("evidenza")).thenReturn("1"); // "1" -> true

        when(request.getRequestDispatcher("showTable?tableName=variante")).thenReturn(dispatcher);

        try (MockedConstruction<VarianteDAO> dao = mockConstruction(VarianteDAO.class)) {
            servlet.doPost(request, response);

            VarianteDAO mockDao = dao.constructed().get(0);
            ArgumentCaptor<Variante> captor = ArgumentCaptor.forClass(Variante.class);
            verify(mockDao).doSaveVariante(captor.capture());
            assertTrue(captor.getValue().isEvidenza());
        }
    }

    @Test
    @DisplayName("Inserimento 'variante' (Sad Path: Missing Fields)")
    void doPost_insertVariante_missingFields() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("variante");
        when(request.getParameter("idProdottoVariante")).thenReturn(""); // Missing

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    // --- Test 14: Ordine Extra Branches ---

    @Test
    @DisplayName("Inserimento 'ordine' (Sad Path: Missing Email)")
    void doPost_insertOrdine_missingEmail() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("ordine");
        when(request.getParameter("emailUtente")).thenReturn("");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    @Test
    @DisplayName("Inserimento 'ordine' (Happy Path: Missing Optional Fields)")
    void doPost_insertOrdine_missingOptional() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("ordine");
        when(request.getParameter("emailUtente")).thenReturn("user@example.com");
        when(request.getParameter("stato")).thenReturn("Consegnato");
        when(request.getParameter("totale")).thenReturn(""); // Optional/Skipped
        when(request.getParameter("data")).thenReturn(""); // Optional/Skipped

        when(request.getRequestDispatcher("showTable?tableName=ordine")).thenReturn(dispatcher);

        try (MockedConstruction<OrdineDao> dao = mockConstruction(OrdineDao.class)) {
            servlet.doPost(request, response);
            verify(dispatcher).forward(request, response);
        }
    }

    // --- Test 15: DettaglioOrdine Extra Branches ---

    @Test
    @DisplayName("Inserimento 'dettagliOrdine' (Sad Path: Missing Fields)")
    void doPost_insertDettaglioOrdine_missingFields() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("dettagliOrdine");
        when(request.getParameter("idOrdine")).thenReturn(""); // Missing

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    // --- Test 16: Prodotto Extra Branches (New Coverage) ---

    @Test
    @DisplayName("Inserimento 'prodotto' (Sad Path: Invalid File Name) -> Invia 500")
    void doPost_insertProdotto_invalidFileName_sendsError() throws ServletException, IOException {
        setupValidProdottoParams();
        when(request.getParameter("nameTable")).thenReturn("prodotto");

        Part filePart = request.getPart("immagine");
        when(filePart.getSubmittedFileName()).thenReturn(""); // Blank file name

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    @Test
    @DisplayName("Inserimento 'prodotto' (Sad Path: NumberFormatException) -> Invia 500")
    void doPost_insertProdotto_numberFormatError() throws ServletException, IOException {
        setupValidProdottoParams();
        when(request.getParameter("nameTable")).thenReturn("prodotto");
        when(request.getParameter("calorie")).thenReturn("abc"); // Invalid number

        // Initialize servlet to avoid IllegalStateException on getServletContext()
        ServletContext servletContext = mock(ServletContext.class);
        ServletConfig servletConfig = mock(ServletConfig.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        servlet.init(servletConfig);

        // Mock getRealPath to avoid NPE if called
        when(servletContext.getRealPath(anyString())).thenReturn("/fake/path");

        // Mock Files/Paths to avoid actual file operations
        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class);
                MockedStatic<Files> filesMock = mockStatic(Files.class)) {

            Path mockPath = mock(Path.class);
            pathsMock.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            when(mockPath.getFileName()).thenReturn(mockPath);
            when(mockPath.toString()).thenReturn("test-image.jpg");
            when(mockPath.getParent()).thenReturn(mock(Path.class));

            servlet.doPost(request, response);
        }

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    // --- Test 17: Ordine Extra Branches (New Coverage) ---

    @Test
    @DisplayName("Inserimento 'ordine' (Sad Path: Invalid Totale) -> Invia 500")
    void doPost_insertOrdine_invalidTotale_sendsError() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("ordine");
        when(request.getParameter("emailUtente")).thenReturn("user@example.com");
        when(request.getParameter("stato")).thenReturn("Consegnato");
        when(request.getParameter("totale")).thenReturn("abc"); // Invalid number

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }

    // --- Test 18: Variante Extra Branches (New Coverage) ---

    @Test
    @DisplayName("Inserimento 'variante' (Sad Path: Invalid Discount > 100) -> Invia 500")
    void doPost_insertVariante_invalidDiscount_sendsError() throws ServletException, IOException {
        when(request.getParameter("nameTable")).thenReturn("variante");
        when(request.getParameter("idProdottoVariante")).thenReturn("P1");
        when(request.getParameter("idGusto")).thenReturn("1");
        when(request.getParameter("idConfezione")).thenReturn("1");
        when(request.getParameter("prezzo")).thenReturn("10.5");
        when(request.getParameter("quantity")).thenReturn("100");
        when(request.getParameter("sconto")).thenReturn("101"); // Invalid discount

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
    }
}
