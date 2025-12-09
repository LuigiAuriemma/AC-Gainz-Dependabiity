package controller.Filters;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Prodotto;
import model.ProdottoDAO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@WebServlet(value = "/categories")
public class CategoriesServlet extends HttpServlet {

    // 1. Define an allowlist of valid categories
    private static final List<String> ALLOWED_CATEGORIES = Arrays.asList(
            "tutto", "proteine", "barrette", "creatina", "creme", "post-workout", "pre-workout", "salse"
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Prendo la categoria dalla request
            String rawFilter = req.getParameter("category");

            // 2. Sanitize and Validate: Default to "tutto" if invalid or null
            String filter = "tutto";
            if (rawFilter != null && ALLOWED_CATEGORIES.contains(rawFilter)) {
                filter = rawFilter;
            }

            ProdottoDAO prodottoDAO = new ProdottoDAO();
            HttpSession session = req.getSession();
            List<Prodotto> productsByCriteria = new ArrayList<>();

            // In base alla categoria prendo, tramite metodo DAO, tutte le tuple che soddisfano tale categoria
            if ("tutto".equals(filter)) {
                productsByCriteria = prodottoDAO.doRetrieveAll();
            } else {
                // Now 'filter' is safe to use in SQL generation (DAO) and Session
                productsByCriteria = prodottoDAO.doRetrieveByCriteria("categoria", filter);
            }

            // rimuovo per mantenere coerenza con i gusti
            session.removeAttribute("products");
            session.removeAttribute("searchBarName");

            req.setAttribute("originalProducts", productsByCriteria);

            // 3. Safe to set attributes because 'filter' is now strictly controlled
            session.setAttribute("categoria", filter);
            session.setAttribute("categoriaRecovery", filter);

            // setto nella session per vedere i gusti disponibili tramite ajax in showTasteServlet
            session.setAttribute("filteredProducts", productsByCriteria);

            RequestDispatcher requestDispatcher = req.getRequestDispatcher("FilterProducts.jsp");
            requestDispatcher.forward(req, resp);
        } catch (ServletException | IOException e) {
            log("Errore in CategoriesServlet doGet", e);
            if (!resp.isCommitted()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno durante il recupero delle categorie.");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doGet(req, resp);
        } catch (ServletException | IOException e) {
            log("Errore in CategoriesServlet doPost", e);
            if (!resp.isCommitted()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
            }
        }
    }
}