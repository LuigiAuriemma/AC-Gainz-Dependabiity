package controller.utente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Utente;
import model.UtenteDAO;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet("/register")
public class RegistrazioneServlet extends HttpServlet {

    //definiamo i pattern da dover rispettare
    private static final String EMAIL_PATTERN = "^[\\w.%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,8}$";
    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$";
    private static final String COD_FISCALE_PATTERN = "^[A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z]$";
    private static final String PHONE_PATTERN = "^3[0-9]{8,9}$";
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            this.doPost(request, response);
        } catch (ServletException | IOException e) {
            log("Errore in RegistrazioneServlet doGet", e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
            }
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            //prendiamo i dati dal form
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String nome = request.getParameter("nome");
            String cognome = request.getParameter("cognome");
            String codiceFiscale = request.getParameter("codiceFiscale");
            String dateString = request.getParameter("dataDiNascita");
            String indirizzo = request.getParameter("indirizzo");
            String numCellulare = request.getParameter("numCellulare");

            // Validifichiamo l'email
            if (!isValidEmail(email)) {
                request.setAttribute("error", "Pattern email non rispettato");
                request.getRequestDispatcher("Registrazione.jsp").forward(request, response);
                return;
            }

            // Controlliamo se l'email non sia gia presente
            UtenteDAO utenteDAO = new UtenteDAO();
            if (utenteDAO.doRetrieveByEmail(email) != null) {
                request.setAttribute("error", "Email già registrata.");
                request.getRequestDispatcher("Registrazione.jsp").forward(request, response);
                return;
            }

            // Validifichiamo la password
            if (!isValidPassword(password)) {
                request.setAttribute("error", "Pattern password non rispettato");
                request.getRequestDispatcher("Registrazione.jsp").forward(request, response);
                return;
            }

            // Validifichiamo il codice fiscale
            if (!isValidCodiceFiscale(codiceFiscale)) {
                request.setAttribute("error", "Pattern codice fiscale non rispettato");
                request.getRequestDispatcher("Registrazione.jsp").forward(request, response);
                return;
            }

            // Parse della data di nascita
            Date dataDiNascita = parseDate(dateString);
            if (dataDiNascita == null) {
                request.setAttribute("error", "Pattern data non rispettato");
                request.getRequestDispatcher("Registrazione.jsp").forward(request, response);
                return;
            }

            // Validifichiamo il numero di telefono
            if (!isValidPhone(numCellulare)) {
                request.setAttribute("error", "Pattern numero di telefono non rispettato");
                request.getRequestDispatcher("Registrazione.jsp").forward(request, response);
                return;
            }

            // Dopo aver passato tutte le validificazioni creiamo l'oggetto utente da salvare nel DB
            Utente utente = new Utente();
            utente.setEmail(email);
            utente.setPassword(password);
            utente.hashPassword();
            utente.setCodiceFiscale(codiceFiscale);
            utente.setNome(nome);
            utente.setCognome(cognome);
            utente.setIndirizzo(indirizzo);
            utente.setTelefono(numCellulare);
            utente.setDataNascita(dataDiNascita);

            utenteDAO.doSave(utente);

            // Store user in session and forward to index.jsp
            HttpSession session = request.getSession();
            session.setAttribute("Utente", utente);
            response.sendRedirect("index.jsp");

        } catch (Exception e) {
            log("Errore in RegistrazioneServlet doPost", e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore durante la registrazione.");
            }
        }
    }

    // Metodo per validificare l'email
    private boolean isValidEmail(String email) {
        Pattern emailRegex = Pattern.compile(EMAIL_PATTERN);
        Matcher emailMatcher = emailRegex.matcher(email);
        return emailMatcher.matches();
    }

    // Metodo per validificare la password
    private boolean isValidPassword(String password) {
        Pattern passwordRegex = Pattern.compile(PASSWORD_PATTERN);
        Matcher passwordMatcher = passwordRegex.matcher(password);
        return passwordMatcher.matches();
    }

    // Metodo per validificare codice fiscale
    private boolean isValidCodiceFiscale(String codiceFiscale) {
        Pattern codFiscaleRegex = Pattern.compile(COD_FISCALE_PATTERN);
        Matcher codFiscaleMatcher = codFiscaleRegex.matcher(codiceFiscale);
        return codFiscaleMatcher.matches();
    }

    // Metodo per il parse della data
    private Date parseDate(String dateString) {
        try {
            return DATE_FORMATTER.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    // Method to validificare il numero di telefono
    private boolean isValidPhone(String numCellulare) {
        Pattern phoneRegex = Pattern.compile(PHONE_PATTERN);
        Matcher phoneMatcher = phoneRegex.matcher(numCellulare);
        return phoneMatcher.matches();
    }
}