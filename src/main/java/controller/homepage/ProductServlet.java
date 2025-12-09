package controller.homepage;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Prodotto;
import model.ProdottoDAO;
import model.Variante;
import model.VarianteDAO;
import controller.Security.ServletUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(value = "/ProductInfo")
public class ProductServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            //prendiamo dalla request la primarykey del prodotto cliccato
            String primaryKey = req.getParameter("primaryKey");
            System.out.println(primaryKey);
            if(primaryKey != null) {
                //andiamo a prendere tutti i valori del prodotto selezionato
                ProdottoDAO prodottoDAO = new ProdottoDAO();
                Prodotto prodotto = prodottoDAO.doRetrieveById(primaryKey);
                if(prodotto != null) {
                    ProdottoDAO suggeritiDAO = new ProdottoDAO();

                    //prendiamo tutte le sue varianti
                    VarianteDAO varianteDAO = new VarianteDAO();
                    List<Variante> varianti = prodotto.getVarianti();

                    //creiamo una lista di tutti i suoi gusti
                    List<String> gusti = new ArrayList<>();
                    for (Variante v: varianti){
                        if (!gusti.contains(v.getGusto())){
                            gusti.add(v.getGusto());
                        }
                    }

                    //Lista di pesi associati al gusto della variante di costo inferiore
                    List<Integer> pesi = new ArrayList<>();
                    List<Variante> variantiCriteria = varianteDAO.doRetrieveVariantByCriteria(prodotto.getIdProdotto(), "flavour", varianti.get(0).getGusto());

                    for (Variante y: variantiCriteria){
                        if (!pesi.contains(y.getPesoConfezione()))
                            pesi.add(y.getPesoConfezione());
                    }

                    req.setAttribute("allTastes", gusti);
                    req.setAttribute("firstWeights", pesi);

                    //sezione dei suggeriti
                    String category = prodotto.getCategoria();
                    List<Prodotto> suggeriti = suggeritiDAO.doRetrieveByCriteria("categoria",category);
                    req.setAttribute("suggeriti",suggeriti);
                    req.setAttribute("prodotto",prodotto);

                    req.getRequestDispatcher("Product.jsp").forward(req,resp);
                }
            }
        } catch (Exception e) {
            log("Errore in ProductServlet doGet", e);
            if (!resp.isCommitted()) {
                ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno nel recupero del prodotto.");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doGet(req,resp);
        } catch (ServletException | IOException e) {
            log("Errore in ProductServlet doPost", e);
            if (!resp.isCommitted()) {
                ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
            }
        }
    }
}