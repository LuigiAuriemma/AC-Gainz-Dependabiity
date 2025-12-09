package controller.utente;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Carrello;
import model.CarrelloDAO;
import model.Utente;
import controller.Security.ServletUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(value = "/logOut")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            HttpSession session = req.getSession();
            //prendo l'utente corrente
            Utente x = (Utente) session.getAttribute("Utente");

            if (x != null){
                // Save cart into DB before logging out
                CarrelloDAO carrelloDAO = new CarrelloDAO();
                //prendo il carrello corrente
                List<Carrello> cart = (List<Carrello>) session.getAttribute("cart");

                if (cart == null) cart = new ArrayList<>();

                //Rimuovi il carrello precedente nel DB
                carrelloDAO.doRemoveCartByUser(x.getEmail());

                // Salva il carrello della sessione attuale nel DB
                for (Carrello c : cart) {
                    c.setEmailUtente(x.getEmail());
                    carrelloDAO.doSave(c);
                }

                //effettua il logout
                session.removeAttribute("Utente");
                req.getSession().invalidate();

                RequestDispatcher requestDispatcher = req.getRequestDispatcher("index.jsp");
                requestDispatcher.forward(req, resp);
            } else {
                // Se l'utente è già nullo (sessione scaduta), reindirizza comunque alla home
                resp.sendRedirect("index.jsp");
            }
        } catch (Exception e) {
            log("Errore in LogoutServlet doGet", e);
            if (!resp.isCommitted()) {
                ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno durante il logout.");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doGet(req, resp);
        } catch (ServletException | IOException e) {
            log("Errore in LogoutServlet doPost", e);
            if (!resp.isCommitted()) {
                ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
            }
        }
    }
}