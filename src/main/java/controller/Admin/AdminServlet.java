package controller.Admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Utente;
import controller.Security.ServletUtils;

import java.io.IOException;

@WebServlet(value = "/admin")
public class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Logica di controllo permessi
        Utente x = (Utente) req.getSession().getAttribute("Utente");

        if (x != null && x.getPoteri()) {
            try {
                req.getRequestDispatcher("WEB-INF/Admin/AreaAdmin.jsp").forward(req, resp);
            } catch (ServletException | IOException e) {
                log("Errore nel forward AreaAdmin (doGet)", e);
                // FIX: Uso di ServletUtils per inviare l'errore in modo sicuro
                ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore durante il caricamento della pagina Admin.");
            }
        } else {
            // FIX: Uso di ServletUtils anche qui per consistenza e sicurezza
            ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_FORBIDDEN, "Accesso Negato");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doGet(req, resp);
        } catch (ServletException | IOException e) {
            log("Errore nel doPost AdminServlet", e);
            // FIX: Uso di ServletUtils per gestire l'errore senza nested try-catch
            ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno nel processare la richiesta.");
        }
    }
}