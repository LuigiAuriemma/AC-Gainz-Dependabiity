package controller.homepage;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(value = "/orderServlet")
public class OrdineServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // RIGA 20 FIX: Gestione eccezioni per super.doGet
        try {
            super.doGet(req, resp);
        } catch (ServletException | IOException e) {
            log("Errore in OrdineServlet doGet", e);
            if (!resp.isCommitted()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            HttpSession session = req.getSession(false);

            if (session == null) {
                resp.sendRedirect("index.jsp");
                return;
            }

            //prendiamo il carrello dalla sessione
            List<Carrello> cart = (List<Carrello>) session.getAttribute("cart");

            // Aggiunto check 'cart != null' per evitare NullPointerException
            if (cart != null && !cart.isEmpty() && session.getAttribute("Utente") != null){
                Utente x = (Utente) session.getAttribute("Utente");

                //Creiamo un nuovo ordine e lo inseriamo nel DB
                Ordine ordine = new Ordine();
                ordine.setEmailUtente(x.getEmail());
                OrdineDao ordineDao = new OrdineDao();
                ordineDao.doSave(ordine);

                //prendiamo l'id dell'ultimo ordine del DB
                int id_order = ordineDao.getLastInsertedId();

                //creaiamo una lista dei prodotti presenti nell'ordine
                List<DettaglioOrdine> dettaglioOrdine = new ArrayList<>();
                for (Carrello cartItem: cart){
                    DettaglioOrdine dettaglioOrdineItem = new DettaglioOrdine();
                    dettaglioOrdineItem.setIdOrdine(id_order);
                    dettaglioOrdineItem.setIdVariante(cartItem.getIdVariante());
                    dettaglioOrdineItem.setIdProdotto(cartItem.getIdProdotto());
                    dettaglioOrdineItem.setQuantita(cartItem.getQuantita());
                    dettaglioOrdine.add(dettaglioOrdineItem);
                }

                //eliminiamo il carrello visto che abbiamo effettuato l'ordine
                session.removeAttribute("cart");
                CarrelloDAO carrelloDAO = new CarrelloDAO();
                carrelloDAO.doRemoveCartByUser(x.getEmail());

                //salviamo i dettagli dell'ordine all'interno del DB
                DettaglioOrdineDAO dettaglioOrdineDAO = new DettaglioOrdineDAO();
                for (DettaglioOrdine dettaglioOrdineItem : dettaglioOrdine)
                    dettaglioOrdineDAO.doSave(dettaglioOrdineItem);

                //prendiamo l'ordine che abbiamo effettuato e mostriamo il resoconto
                Ordine ordine1 = ordineDao.doRetrieveById(ordineDao.getLastInsertedId());
                List<DettaglioOrdine> dettaglioOrdini1 = new ArrayList<>();
                dettaglioOrdini1 = dettaglioOrdineDAO.doRetrieveById(ordine1.getIdOrdine());
                req.setAttribute("order", ordine1);
                req.setAttribute("orderDetails", dettaglioOrdini1);

                // RIGA 71 FIX: Avvolto nel try-catch generale
                req.getRequestDispatcher("WEB-INF/Ordine.jsp").forward(req, resp);
            }
        } catch (Exception e) {
            log("Errore in OrdineServlet doPost", e);
            if (!resp.isCommitted()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore durante il salvataggio dell'ordine.");
            }
        }
    }
}