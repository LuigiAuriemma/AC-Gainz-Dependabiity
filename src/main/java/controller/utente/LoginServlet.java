package controller.utente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Carrello;
import model.CarrelloDAO;
import model.Utente;
import model.UtenteDAO;
import controller.Security.ServletUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(value = "/login")
public class LoginServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            super.doGet(request, response);
        } catch (ServletException | IOException e) {
            log("Errore in LoginServlet doGet", e);
            if (!response.isCommitted()) {
                ServletUtils.sendErrorSafe(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
            }
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            // Prendo i parametri dal form
            String email = request.getParameter("email");
            String password = request.getParameter("password");

            UtenteDAO utenteDAO = new UtenteDAO();

            //controlliamo che il pattern dell'email inserita sia corretto
            String emailPattern = "^[\\w.%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,8}$";
            Pattern emailRegex = Pattern.compile(emailPattern);
            Matcher emailMatcher = emailRegex.matcher(email);

            //nel caso il pattern dell'email non fosse rispettato
            if (!emailMatcher.matches()) {
                request.setAttribute("patternEmail", "Pattern email non rispettato!");
                request.getRequestDispatcher("Login.jsp").forward(request, response);
                request.removeAttribute("patternEmail");
                return;
            }

            //Controlliamo che il pattern della password inserita sia corretto
            String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$";
            Pattern passwordRegex = Pattern.compile(passwordPattern);
            Matcher passwordMatcher = passwordRegex.matcher(password);

            //nel caso il pattern della password non fosse rispettato
            if (!passwordMatcher.matches()) {
                request.setAttribute("patternPassword", "Pattern password non rispettato!");
                request.getRequestDispatcher("Login.jsp").forward(request, response);
                request.removeAttribute("patternPassword");
                return;
            }

            // Se nel mio db non c'è alcuna corrispondenza con l'email passata dal form, ricarico la pagina del login
            // e faccio uscire un messaggio di errore
            if (utenteDAO.doRetrieveByEmail(email) == null) {
                request.setAttribute("userNotFound", "Utente non registrato!");
                request.getRequestDispatcher("Login.jsp").forward(request, response);
                request.removeAttribute("userNotFound");
                return;
            }

            Utente x = null;
            try {
                x = utenteDAO.doRetrieveByEmailAndPassword(email, password);
            } catch (SQLException e) {
                log("Errore SQL durante il login", e);
                ServletUtils.sendErrorSafe(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore database.");
                return;
            }

            // In questo caso, se arrivo qui significa che l'email è presente nel db ma ciò che è sbagliata è la password
            // per cui faccio ciò che ho fatto prima, con un messaggio di errore evidentemente diverso
            if (x == null) {
                request.setAttribute("wrongPassword", "Password errata!");
                request.getRequestDispatcher("Login.jsp").forward(request, response);
                request.removeAttribute("wrongPassword");
                return;
            }

            // Se arrivo qui, ho messo i dati giusti nel form

            // Mi prendo la sessione
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(1800); // Set session timeout

            // Inserisco nella sessione l'oggetto contenente l'utente vero e proprio
            session.setAttribute("Utente", x);

            //A questo punto controllo se l'utente ha un carrello nel DB altrimenti ne creo uno nuovo
            CarrelloDAO carrelloDAO = new CarrelloDAO();
            List<Carrello> dbCart = carrelloDAO.doRetrieveCartItemsByUser(x.getEmail());
            if (dbCart == null) dbCart = new ArrayList<>();

            //controllo se l'utente ha un carrello nella sessione attuale
            List<Carrello> sessionCart = (List<Carrello>) session.getAttribute("cart");
            if (sessionCart == null) sessionCart = new ArrayList<>();

            //Se è presente il carrello nel DB e anche nella sessione li unisco(sommo le quantità eventuali prodotti uguali)
            for (Carrello sessionCartEntry : sessionCart) {
                boolean found = false;
                for (Carrello dbCartEntry : dbCart) {
                    if (dbCartEntry.getIdVariante() == sessionCartEntry.getIdVariante()) {
                        dbCartEntry.setQuantita(dbCartEntry.getQuantita() + sessionCartEntry.getQuantita());
                        dbCartEntry.setPrezzo(dbCartEntry.getPrezzo() + sessionCartEntry.getPrezzo());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    dbCart.add(sessionCartEntry);
                }
            }

            session.setAttribute("cart", dbCart);

            // Redirect to the index page
            request.getRequestDispatcher("index.jsp").forward(request, response);

        } catch (Exception e) {
            log("Errore critico in LoginServlet doPost", e);
            if (!response.isCommitted()) {
                ServletUtils.sendErrorSafe(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno durante il login.");
            }
        }
    }
}