package controller.homepage;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@WebServlet(value = "/cartServlet")
public class CarrelloServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");

        // FIX 1: Imposta sempre l'encoding esplicito oltre al content type
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        ProdottoDAO prodottoDAO = new ProdottoDAO();
        HttpSession session = req.getSession();

        synchronized (session) {
            PrintWriter out = resp.getWriter();

            // Controllo null su action per evitare crash
            if (action == null) {
                out.println(new JSONArray());
                out.flush();
                return;
            }

            switch (action) {
                case "show" -> handleShowAction(session, prodottoDAO, out);
                case "addVariant" -> handleAddVariantAction(req, session, prodottoDAO, out);
                case "removeVariant" -> handleRemoveVariantAction(req, session, prodottoDAO, out);
                case "quantityVariant" -> handleQuantityVariantAction(req, session, prodottoDAO, out);
                default -> {
                    // Gestione caso default sicura
                    out.println(new JSONArray());
                    out.flush();
                }
            }
        }
    }

    // --- METODO DI SICUREZZA PER SONARCLOUD (FIX XSS) ---
    /**
     * Pulisce l'input da caratteri pericolosi per prevenire XSS.
     * Sostituisce <, >, &, ", ' con le rispettive entità o stringhe vuote.
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private void handleShowAction(HttpSession session, ProdottoDAO prodottoDAO, PrintWriter out) throws IOException {
        List<Carrello> cartItems = (List<Carrello>) session.getAttribute("cart");

        if (cartItems != null && !cartItems.isEmpty()) {
            writeCartItemsToResponse(cartItems, prodottoDAO, out);
        } else {
            JSONArray jsonArray = new JSONArray();
            out.println(jsonArray);
            out.flush();
            out.close();
        }
    }

    public void handleQuantityVariantAction(HttpServletRequest request,  HttpSession session, ProdottoDAO prodottoDAO, PrintWriter out) throws IOException {
        // FIX 2: Sanitizzazione degli input stringa
        String idProdotto = sanitizeInput(request.getParameter("id"));
        String gusto = sanitizeInput(request.getParameter("gusto"));

        int pesoConfezione;
        try {
            pesoConfezione = Integer.parseInt(request.getParameter("pesoConfezione"));
        } catch (NumberFormatException e) {
            return;
        }

        Prodotto p = prodottoDAO.doRetrieveById(idProdotto);
        if (p != null){
            VarianteDAO varianteDAO = new VarianteDAO();
            List<Variante> varianti = varianteDAO.doRetrieveVariantByFlavourAndWeight(p.getIdProdotto(), gusto, pesoConfezione);

            if (varianti.isEmpty()) return;

            Variante v = varianti.get(0);
            List<Carrello> cartItems = (List<Carrello>) session.getAttribute("cart");

            if (v != null && cartItems != null){
                String quantityStr = request.getParameter("quantity");

                if (quantityStr != null && !quantityStr.isBlank()) {
                    int q;
                    try {
                        q = Integer.parseInt(quantityStr);
                    } catch (NumberFormatException e) {
                        return;
                    }

                    if (q <= v.getQuantita()){
                        if (q <= 0) {
                            handleRemoveVariantAction(request, session, prodottoDAO, out);
                        } else {
                            float price = v.getPrezzo();
                            if (v.getSconto() > 0){
                                price = price * (1 - (float) v.getSconto() / 100);
                                price = Math.round(price * 100.0f) / 100.0f;
                            }

                            price *= q;

                            for (Carrello c: cartItems){
                                if (c.getIdVariante() == v.getIdVariante()){
                                    c.setQuantita(q);
                                    c.setPrezzo(price);
                                    break;
                                }
                            }

                            session.setAttribute("cart", cartItems);
                            writeCartItemsToResponse(cartItems, prodottoDAO, out);
                        }
                    }
                }
            }
        }
    }

    public void handleRemoveVariantAction(HttpServletRequest request, HttpSession session, ProdottoDAO prodottoDAO, PrintWriter out) throws IOException {
        // FIX 2: Sanitizzazione degli input stringa
        String idToRemove = sanitizeInput(request.getParameter("id"));
        String gusto = sanitizeInput(request.getParameter("gusto"));

        int pesoConfezione;
        try {
            pesoConfezione = Integer.parseInt(request.getParameter("pesoConfezione"));
        } catch (NumberFormatException e) {
            return;
        }

        Prodotto p = prodottoDAO.doRetrieveById(idToRemove);

        if (p != null){
            VarianteDAO varianteDAO = new VarianteDAO();
            List<Variante> varianti = varianteDAO.doRetrieveVariantByFlavourAndWeight(p.getIdProdotto(), gusto, pesoConfezione);

            if (varianti.isEmpty()) return;

            Variante v = varianti.get(0);
            if (v != null){
                List<Carrello> cartItems = (List<Carrello>) session.getAttribute("cart");

                if (cartItems != null) {
                    cartItems.removeIf(item -> item.getIdVariante()  == v.getIdVariante());
                    session.setAttribute("cart", cartItems);
                    writeCartItemsToResponse(cartItems, prodottoDAO, out);
                }
            }
        }
    }

    private void handleAddVariantAction(HttpServletRequest request, HttpSession session, ProdottoDAO prodottoDAO, PrintWriter out) throws IOException {
        // FIX 2: Sanitizzazione degli input stringa
        String id = sanitizeInput(request.getParameter("id"));

        Prodotto p = prodottoDAO.doRetrieveById(id);
        if (p != null) {
            int quantity = 1;
            if (request.getParameter("quantity") != null) {
                try {
                    int x = Integer.parseInt(request.getParameter("quantity"));
                    if (x > 0) quantity = x;
                } catch (NumberFormatException e) {
                    // ignore
                }
            }

            // FIX 2: Sanitizzazione degli input stringa
            String gusto = sanitizeInput(request.getParameter("gusto"));
            String pesoConfezioneStr = request.getParameter("pesoConfezione");

            int pesoConfezione;
            try {
                pesoConfezione = Integer.parseInt(pesoConfezioneStr);
            } catch (NumberFormatException e) {
                return;
            }

            VarianteDAO varianteDAO = new VarianteDAO();
            List<Variante> varianti = varianteDAO.doRetrieveVariantByFlavourAndWeight(p.getIdProdotto(), gusto, pesoConfezione);

            if (varianti.isEmpty()) return;

            Variante v = varianti.get(0);
            List<Carrello> cartItems = (List<Carrello>) session.getAttribute("cart");
            if (cartItems == null) cartItems = new ArrayList<>();

            if (v != null) {
                float price = v.getPrezzo();
                if (v.getSconto() > 0) {
                    price = price * (1 - (float) v.getSconto() / 100);
                    price = Math.round(price * 100.0f) / 100.0f;
                }

                boolean itemExists = false;
                if (!cartItems.isEmpty()) {
                    for (Carrello item : cartItems) {
                        if (item.getIdVariante() == v.getIdVariante()) {
                            int newQuantity = item.getQuantita() + quantity;
                            if (newQuantity <= v.getQuantita()) {
                                item.setQuantita(newQuantity);
                                item.setPrezzo(item.getPrezzo() + (price * quantity));
                            }
                            itemExists = true;
                            break;
                        }
                    }
                }

                if (!itemExists && quantity <= v.getQuantita()) {
                    Carrello c = new Carrello();
                    c.setIdProdotto(id);
                    c.setIdVariante(v.getIdVariante());
                    c.setNomeProdotto(p.getNome());
                    c.setQuantita(quantity);
                    c.setPrezzo(price * quantity);
                    c.setGusto(gusto); // Qui usiamo la versione sanitizzata
                    c.setPesoConfezione(pesoConfezione);
                    c.setImmagineProdotto(p.getImmagine());
                    cartItems.add(c);
                }

                session.setAttribute("cart", cartItems);
                writeCartItemsToResponse(cartItems, prodottoDAO, out);
            }
        }
    }

    private void writeCartItemsToResponse(List<Carrello> cartItems, ProdottoDAO prodottoDAO, PrintWriter out) throws IOException{
        JSONArray jsonArray = new JSONArray();
        float totalPrice = 0;

        for (Carrello item: cartItems){
            Prodotto p = prodottoDAO.doRetrieveById(item.getIdProdotto());

            if (p == null) continue;

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("idProdotto", item.getIdProdotto());
            jsonObject.put("idVariante", item.getIdVariante());
            // Anche se vengono dal DB, è buona norma assicurarsi che p.getNome() non sia null
            jsonObject.put("nomeProdotto", p.getNome());
            jsonObject.put("imgSrc", p.getImmagine());
            // Questo valore è stato sanitizzato in fase di input
            jsonObject.put("flavour", item.getGusto());
            jsonObject.put("weight", item.getPesoConfezione());
            jsonObject.put("quantity", item.getQuantita());

            float itemPrice = item.getPrezzo();
            itemPrice = Math.round(itemPrice * 100.0f) / 100.0f;
            jsonObject.put("prezzo", itemPrice);

            totalPrice += item.getPrezzo();

            jsonArray.add(jsonObject);
        }

        totalPrice = Math.round(totalPrice * 100.0f) / 100.0f;

        JSONObject totalPriceObject = new JSONObject();
        totalPriceObject.put("totalPrice", totalPrice);
        jsonArray.add(totalPriceObject);

        out.println(jsonArray);
        out.flush();
        out.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}