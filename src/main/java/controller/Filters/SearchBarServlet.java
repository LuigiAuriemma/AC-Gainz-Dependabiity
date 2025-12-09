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
import java.util.regex.Pattern;

import static controller.Filters.GenericFilterServlet.getJsonObject;


@WebServlet(value = "/searchBar")
public class SearchBarServlet extends HttpServlet {

    private static final Pattern SAFE_SEARCH_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-'.(),]+$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String rawName = req.getParameter("name");

            // 2. Validazione
            String name = null;
            if (rawName != null && SAFE_SEARCH_PATTERN.matcher(rawName).matches()) {
                name = rawName;
            }

            HttpSession session = req.getSession();

            synchronized (session) {
                List<Prodotto> products = new ArrayList<>();
                String categoria = (String) session.getAttribute("categoriaRecovery");
                ProdottoDAO prodottoDAO = new ProdottoDAO();

                //prendiamo i prodotti in base a name (se è valido)
                if (name != null && !name.isEmpty()) {
                    session.removeAttribute("categoria");

                    try {
                        products = prodottoDAO.filterProducts("", "", "", "", name);
                        session.setAttribute("searchBarName", name);
                    } catch (SQLException e) {
                        log("Errore SQL ricerca per nome", e);
                        if (!resp.isCommitted()) resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore database.");
                        return;
                    }
                }
                //prendiamo i prodotti in base alla categoria
                else {
                    session.removeAttribute("searchBarName");
                    session.setAttribute("categoria", categoria);
                    try {
                        products = prodottoDAO.filterProducts(categoria, "", "", "", "");
                    } catch (SQLException e) {
                        log("Errore SQL ricerca per categoria", e);
                        if (!resp.isCommitted()) resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore database.");
                        return;
                    }
                }

                session.setAttribute("filteredProducts", products);

                addToJson(products, session, req, resp);
            }
        } catch (Exception e) {
            log("Errore in SearchBarServlet doGet", e);
            if (!resp.isCommitted()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno durante la ricerca.");
            }
        }
    }

    private void addToJson(List<Prodotto> products, HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        JSONArray jsonArray = new JSONArray();

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
        try {
            doGet(req, resp);
        } catch (ServletException | IOException e) {
            log("Errore in SearchBarServlet doPost", e);
            if (!resp.isCommitted()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
            }
        }
    }
}