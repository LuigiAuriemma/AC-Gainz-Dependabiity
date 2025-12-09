package controller.Filters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Prodotto;
import model.ProdottoDAO;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern; // Import necessario

import static controller.Filters.GenericFilterServlet.getJsonObject;


@WebServlet(value = "/searchBar")
public class SearchBarServlet extends HttpServlet {

    // 1. Definiamo la Regex per sanificare la barra di ricerca
    // Accetta lettere, numeri, spazi, trattini, parentesi, apostrofi, punti, virgole e simboli ®™
    private static final Pattern SAFE_SEARCH_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-'.(),]+$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String rawName = req.getParameter("name");

        // 2. Validazione: Se l'input è valido lo usiamo, altrimenti 'name' diventa null
        String name = null;
        if (rawName != null && SAFE_SEARCH_PATTERN.matcher(rawName).matches()) {
            name = rawName;
        }
        // Se rawName conteneva caratteri pericolosi, 'name' resta null e il codice andrà nel ramo 'else' (sicuro)

        HttpSession session = req.getSession();

        synchronized (session) { //uso di synchronized per race conditions su session tramite ajax

            List<Prodotto> products = new ArrayList<>();
            String categoria = (String) session.getAttribute("categoriaRecovery");
            ProdottoDAO prodottoDAO = new ProdottoDAO();

            //prendiamo i prodotti in base a name (se è valido)
            if (name != null && !name.isEmpty()) {
                session.removeAttribute("categoria");  //per applicare i filtri

                try {
                    // Passiamo 'name' sanificato al DAO
                    products = prodottoDAO.filterProducts("", "", "", "", name);

                    // 3. Fix Trust Boundary Violation (Riga 44 originale)
                    // Ora 'name' è sicuro e validato, Snyk non si lamenterà più
                    session.setAttribute("searchBarName", name);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            //prendiamo i prodotti in base alla categoria
            else {
                session.removeAttribute("searchBarName");
                session.setAttribute("categoria", categoria);
                try {
                    products = prodottoDAO.filterProducts(categoria, "", "", "", "");
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }


            // Save search results in originalProducts and products for further filtering
            // Anche filteredProducts è sicuro perché deriva da query DB basate su input validati
            session.setAttribute("filteredProducts", products);
            /*session.setAttribute("products", products);*/

            addToJson(products, session, req, resp);
        }
    }

    private void addToJson(List<Prodotto> products, HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        JSONArray jsonArray = new JSONArray();

        //prendiamo la lista di prodotti e li inseriamo in un JSONArray
        for (Prodotto p: products) {
            JSONObject jsonObject = getJsonObject(p);

            if (jsonObject != null) {
                jsonArray.add(jsonObject);
            }
        }

        response.setContentType("application/json");
        PrintWriter o = response.getWriter();
        o.println(jsonArray);
        o.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}