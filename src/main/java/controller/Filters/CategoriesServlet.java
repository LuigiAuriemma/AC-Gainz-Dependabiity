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
import java.util.List;

@WebServlet(value = "/categories")
public class CategoriesServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //Prendo la categoria dalla request e prendo la sessione
        String filter = req.getParameter("category");
        ProdottoDAO prodottoDAO = new ProdottoDAO();
        HttpSession session = req.getSession();
        List<Prodotto> productsByCriteria = new ArrayList<>();

        //In base alla categoria prendo,tramite metodo DAO,tutte le tuple che soddisfano tale categoria
        if("tutto".equals(filter)){
            productsByCriteria = prodottoDAO.doRetrieveAll();
        }else {
            productsByCriteria = prodottoDAO.doRetrieveByCriteria("categoria", filter);
        }


        //rimuovo per mantenere coerenza con i gusti
        session.removeAttribute("products");
        session.removeAttribute("searchBarName");

        req.setAttribute("originalProducts", productsByCriteria);


        //metto nella sessione la categoria scelta
        session.setAttribute("categoria", filter);
        session.setAttribute("categoriaRecovery", filter);

        //setto nella session per vedere i gusti disponibili tramite ajax in showTasteServlet
        session.setAttribute("filteredProducts", productsByCriteria);


        RequestDispatcher requestDispatcher = req.getRequestDispatcher("FilterProducts.jsp");
        requestDispatcher.forward(req, resp);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }}
