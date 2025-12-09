package controller.Filters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Prodotto;
import model.Variante;
import model.VarianteDAO;
import org.json.simple.JSONArray;
import controller.Security.ServletUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/showTastes")
public class ShowTasteServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<Prodotto> originalProducts = (List<Prodotto>) req.getSession().getAttribute("filteredProducts");

            if (originalProducts == null) {
                originalProducts = new ArrayList<>();
            }

            // Creare una mappa per contare le occorrenze di ciascun gusto
            Map<String, Integer> tasteCounts = new HashMap<>();
            VarianteDAO varianteDAO = new VarianteDAO();

            // Raccogliere tutte le varianti dei prodotti filtrati in una singola query
            List<Variante> varianti = varianteDAO.doRetrieveVariantiByProdotti(originalProducts);

            if (varianti == null) {
                varianti = new ArrayList<>();
            }

            // Contare le occorrenze di ciascun gusto
            for (Variante v : varianti) {
                String gusto = v.getGusto();
                tasteCounts.put(gusto, tasteCounts.getOrDefault(gusto, 0) + 1);
            }

            // Creare il JSONArray per la risposta contenente ogni varainte
            JSONArray jsonArray = new JSONArray();
            for (String key : tasteCounts.keySet()) {
                String tasteWithCount = key + " (" + tasteCounts.get(key) + ")";
                jsonArray.add(tasteWithCount);
            }

            // Impostare il tipo di contenuto e inviare la risposta
            resp.setContentType("application/json");

            try (PrintWriter out = resp.getWriter()) {
                out.println(jsonArray);
                out.flush();
            }
        } catch (Exception e) {
            log("Errore in ShowTasteServlet doGet", e);
            if (!resp.isCommitted()) {
                ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno nel recupero dei gusti.");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doGet(req, resp);
        } catch (ServletException | IOException e) {
            log("Errore in ShowTasteServlet doPost", e);
            if (!resp.isCommitted()) {
                ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
            }
        }
    }
}