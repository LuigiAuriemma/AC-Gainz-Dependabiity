package controller.Filters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Prodotto;
import model.ProdottoDAO;
import model.Variante;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@WebServlet("/genericFilter")
public class GenericFilterServlet extends HttpServlet {

    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-%]+$");

    private static final List<String> ALLOWED_SORTING = Arrays.asList(
            "PriceDesc", "PriceAsc", "CaloriesDesc", "CaloriesAsc", "evidence", "default", ""
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        HttpSession session = req.getSession();
        synchronized (session) {

            String rawNameForm = req.getParameter("nameForm");

            if (rawNameForm != null) {
                String safeNameForm = isValidInput(rawNameForm) ? rawNameForm : "";

                try {
                    handleNameForm(safeNameForm, req, resp, session);
                } catch (SQLException | ServletException | IOException e) {
                    log("Errore in handleNameForm", e);
                    if (!resp.isCommitted()) {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno durante la ricerca.");
                    }
                }
                return;
            }

            String category = (String) session.getAttribute("categoria");
            String nameFilter = "";
            if (session.getAttribute("searchBarName") != null)
                nameFilter = (String) session.getAttribute("searchBarName");

            String rawWeight = req.getParameter("weight");
            String weightFilter = isValidInput(rawWeight) ? rawWeight : null;

            String rawTaste = req.getParameter("taste");
            String tasteFilter = isValidInput(rawTaste) ? rawTaste : null;

            String rawSorting = req.getParameter("sorting");
            String sortingFilter = "default";
            if (rawSorting != null && ALLOWED_SORTING.contains(rawSorting)) {
                sortingFilter = rawSorting;
            }

            List<Prodotto> filteredProducts = new ArrayList<>();
            ProdottoDAO prodottoDAO = new ProdottoDAO();

            try {
                filteredProducts = prodottoDAO.filterProducts(category, sortingFilter, weightFilter, tasteFilter, nameFilter);
            } catch (SQLException e) {
                log("Errore in filterProducts", e);
                if (!resp.isCommitted()) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno durante il filtraggio.");
                }
                return;
            }

            session.setAttribute("filteredProducts", filteredProducts);

            sendJsonResponse(resp, filteredProducts);
        }
    }

    private void handleNameForm(String nameForm, HttpServletRequest request, HttpServletResponse response, HttpSession session) throws ServletException, IOException, SQLException {
        List<Prodotto> products = new ArrayList<>();
        ProdottoDAO prodottoDAO = new ProdottoDAO();

        if (nameForm.isBlank()){
            products = prodottoDAO.doRetrieveAll();
            session.removeAttribute("categoria");
        } else {
            products = prodottoDAO.filterProducts("", "", "", "", nameForm);
        }

        request.setAttribute("originalProducts", products);

        session.setAttribute("searchBarName", nameForm);

        session.setAttribute("filteredProducts", products);

        request.getRequestDispatcher("FilterProducts.jsp").forward(request, response);
    }

    private boolean isValidInput(String input) {
        if (input == null || input.isBlank()) return true;
        return SAFE_TEXT_PATTERN.matcher(input).matches();
    }

    private void sendJsonResponse(HttpServletResponse resp, List<Prodotto> resultProducts) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        JSONArray jsonArray = new JSONArray();

        for (Prodotto p : resultProducts) {
            JSONObject jsonObject = getJsonObject(p);
            if (jsonObject != null) {
                jsonArray.add(jsonObject);
            }
        }
        out.println(jsonArray);
        out.flush();
    }

    public static JSONObject getJsonObject(Prodotto p) {
        List<Variante> varianti = p.getVarianti();
        if (varianti == null || varianti.isEmpty()) {
            return null;
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", p.getIdProdotto());
        jsonObject.put("nome", p.getNome());
        jsonObject.put("categoria", p.getCategoria());
        jsonObject.put("calorie", p.getCalorie());
        jsonObject.put("immagine", p.getImmagine());

        Variante variante = varianti.get(0);
        jsonObject.put("idVariante", variante.getIdVariante());
        if (variante.getSconto() > 0){
            jsonObject.put("sconto", variante.getSconto());
        }
        jsonObject.put("prezzo", variante.getPrezzo());
        jsonObject.put("gusto", variante.getGusto());
        jsonObject.put("peso", variante.getPesoConfezione());

        return jsonObject;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doGet(req, resp);
        } catch (ServletException | IOException e) {
            log("Errore in GenericFilterServlet doPost", e);
            if (!resp.isCommitted()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
            }
        }
    }
}