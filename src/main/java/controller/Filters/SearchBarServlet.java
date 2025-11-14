package controller.Filters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Prodotto;
import model.ProdottoDAO;
import model.VarianteDAO;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static controller.Filters.GenericFilterServlet.getJsonObject;


@WebServlet(value = "/searchBar")
public class SearchBarServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        HttpSession session = req.getSession();

        synchronized (session) { //uso di synchronized per race conditions su session tramite ajax

            List<Prodotto> products = new ArrayList<>();
            String categoria = (String) session.getAttribute("categoriaRecovery");
            ProdottoDAO prodottoDAO = new ProdottoDAO();


            //prendiamo i prodotti in base a name
            if (name != null && !name.isEmpty()) {
                session.removeAttribute("categoria");  //per applicare i filtri

                try {
                    products = prodottoDAO.filterProducts("", "", "", "", name);
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
