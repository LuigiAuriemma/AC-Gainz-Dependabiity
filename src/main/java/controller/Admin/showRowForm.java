package controller.Admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import controller.Security.ServletUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

@WebServlet(value = "/showRowForm")
public class showRowForm extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Recupera il nome della tabella e la chiave primaria dai parametri della richiesta
            String tableName = req.getParameter("tableName");
            String primaryKey = req.getParameter("primaryKey");

            // Crea un JSONArray per raccogliere le righe della tabella in formato JSON
            JSONArray jsonArray = new JSONArray();
            resp.setContentType("application/json");


            // Controlla se i parametri tableName e primaryKey sono validi
            if (tableName != null && !tableName.isBlank() && primaryKey != null && !primaryKey.isBlank()) {
                // Determina quale tabella mostrare
                switch (tableName) {
                    case "utente" -> showUtenteRowTable(primaryKey, jsonArray);
                    case "prodotto" -> showProdottoRowTable(primaryKey, jsonArray);
                    case "variante" -> showVarianteRowTable(primaryKey, jsonArray);
                    case "ordine" -> showOrdineRowTable(primaryKey, jsonArray);
                    case "dettaglioOrdine" -> showDettaglioOrdineRowTable(primaryKey, jsonArray);
                    case "gusto" -> showGustoRowTable(primaryKey, jsonArray);
                    case "confezione" -> showConfezioneRowTable(primaryKey, jsonArray);
                    default -> {
                        ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_BAD_REQUEST, "Tabella non valida.");
                        return;
                    }
                }
            }

            // Invia il JSON di risposta
            try (PrintWriter out = resp.getWriter()) {
                out.println(jsonArray.toJSONString());
                out.flush();
            }
        } catch (Exception e) {
            log("Errore in showRowForm doGet", e);
            if (!resp.isCommitted()) {
                ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno durante il recupero dei dati.");
            }
        }
    }


    //Prende la Confezione dal DB tramite la primaryKey e la aggiunge ad un JSONARRAY
    private void showConfezioneRowTable(String primaryKey, JSONArray jsonArray) {
        try {
            ConfezioneDAO confezioneDAO = new ConfezioneDAO();
            int idConfezione = Integer.parseInt(primaryKey);
            Confezione confezione = confezioneDAO.doRetrieveById(idConfezione);
            if (confezione != null) {
                jsonArray.add(confezioneHelper(confezione));
            }
        } catch (NumberFormatException e) {
            // Se la primaryKey non è un numero (es. "abc"), cattura l'errore.
            log("Errore parsing primaryKey Confezione", e);
        }
    }

    //Prende il gusto dal DB tramite la primaryKey e la aggiunge ad un JSONARRAY
    private void showGustoRowTable(String primaryKey, JSONArray jsonArray) {
        try {
            GustoDAO gustoDAO = new GustoDAO();
            int idGusto = Integer.parseInt(primaryKey);
            Gusto gusto = gustoDAO.doRetrieveById(idGusto);
            if (gusto != null) {
                jsonArray.add(gustoHelper(gusto));
            }
        } catch (NumberFormatException e) {
            // Se la primaryKey non è un numero (es. "abc"), cattura l'errore.
            log("Errore parsing primaryKey Gusto", e);
        }
    }


    //Prende il dettaglio ordine dal DB tramite la primaryKey e la aggiunge ad un JSONARRAY
    private void showDettaglioOrdineRowTable(String primaryKey, JSONArray jsonArray) {
        DettaglioOrdineDAO dettaglioOrdineDAO = new DettaglioOrdineDAO();
        String[] keys = primaryKey.split(", ");
        if (keys.length == 3) {
            try {
                int idOrdine = Integer.parseInt(keys[0].trim()); // Aggiunto .trim() per sicurezza
                int idVariante = Integer.parseInt(keys[2].trim());
                DettaglioOrdine dettaglioOrdine = dettaglioOrdineDAO.doRetrieveByIdOrderAndIdVariant(idOrdine, idVariante);
                if (dettaglioOrdine != null) {
                    jsonArray.add(dettaglioOrdineHelper(dettaglioOrdine));
                }
            } catch (NumberFormatException e) {
                // Se le chiavi non sono numeri (es. "a, b, c"), cattura l'errore.
                log("Errore parsing primaryKey DettaglioOrdine", e);
            }
        }
    }


    //Prende l'ordine dal DB tramite la primaryKey e la aggiunge ad un JSONARRAY

    private void showOrdineRowTable(String primaryKey, JSONArray jsonArray) {
        try {
            OrdineDao ordineDao = new OrdineDao();
            Ordine ordine = ordineDao.doRetrieveById(Integer.parseInt(primaryKey));
            if (ordine != null) {
                log(ordine.getIdOrdine() + " OOOOKKK");
                jsonArray.add(jsonOrdineHelper(ordine));
            }
        } catch (NumberFormatException e) {
            // Se la primaryKey non è un numero, cattura l'errore.
            log("Errore parsing primaryKey Ordine", e);
        }
    }


    //Prende la variante dal DB tramite la primaryKey e la aggiunge ad un JSONARRAY
    private void showVarianteRowTable(String primaryKey, JSONArray jsonArray) {
        try {
            VarianteDAO varianteDAO = new VarianteDAO();
            Variante variante = varianteDAO.doRetrieveVarianteByIdVariante(Integer.parseInt(primaryKey));
            if (variante != null) {
                jsonArray.add(jsonVarianteHelper(variante));
            }
        } catch (NumberFormatException e) {
            // Se la primaryKey non è un numero, cattura l'errore.
            log("Errore parsing primaryKey Variante", e);
        }
    }


    //Prende il prodotto dal DB tramite la primaryKey e la aggiunge ad un JSONARRAY

    private void showProdottoRowTable(String primaryKey, JSONArray jsonArray) {
        ProdottoDAO prodottoDAO = new ProdottoDAO();
        Prodotto prodotto = prodottoDAO.doRetrieveById(primaryKey);
        if (prodotto != null) {
            jsonArray.add(jsonProductHelper(prodotto));
        }
    }


    //Prende l'utente dal DB tramite la primaryKey e la aggiunge ad un JSONARRAY
    private void showUtenteRowTable(String primaryKey, JSONArray jsonArray) {
        UtenteDAO utenteDAO = new UtenteDAO();
        Utente utente = utenteDAO.doRetrieveByEmail(primaryKey);
        if (utente != null) {
            jsonArray.add(jsonUtenteHelper(utente));
        }
    }


    //Crea un oggetto JSON
    protected static JSONObject confezioneHelper(Confezione confezione) {
        JSONObject confezioneObject = new JSONObject();
        confezioneObject.put("idConfezione", confezione.getIdConfezione());
        confezioneObject.put("pesoConfezione", confezione.getPeso());
        return confezioneObject;
    }

    //Crea un oggetto JSON
    protected static JSONObject gustoHelper(Gusto gusto) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("idGusto", gusto.getIdGusto());
        jsonObject.put("nomeGusto", gusto.getNomeGusto());
        return jsonObject;
    }

    //Crea un oggetto JSON
    protected static JSONObject dettaglioOrdineHelper(DettaglioOrdine dettaglioOrdine) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("idOrdine", dettaglioOrdine.getIdOrdine());
        jsonObject.put("idProdotto", dettaglioOrdine.getIdProdotto());
        jsonObject.put("idVariante", dettaglioOrdine.getIdVariante());
        jsonObject.put("quantity", dettaglioOrdine.getQuantita());
        jsonObject.put("prezzo", dettaglioOrdine.getPrezzo());
        return jsonObject;
    }

    //Crea un oggetto JSON
    protected static JSONObject jsonOrdineHelper(Ordine ordine) {
        JSONObject ordineObject = new JSONObject();
        ordineObject.put("idOrdine", ordine.getIdOrdine());
        ordineObject.put("emailUtente", ordine.getEmailUtente());
        ordineObject.put("stato", ordine.getStato());
        ordineObject.put("data", ordine.getDataOrdine() != null ? new SimpleDateFormat("yyyy-MM-dd").format(ordine.getDataOrdine()) : "");
        ordineObject.put("totale", ordine.getTotale());
        ordineObject.put("descrizione", ordine.getDescrizione());
        return ordineObject;
    }

    //Crea un oggetto JSON
    protected static JSONObject jsonVarianteHelper(Variante variante) {
        JSONObject varianteObject = new JSONObject();
        varianteObject.put("idVariante", variante.getIdVariante());
        varianteObject.put("idProdottoVariante", variante.getIdProdotto());
        varianteObject.put("idGusto", variante.getIdGusto());
        varianteObject.put("idConfezione", variante.getIdConfezione());
        varianteObject.put("prezzo", variante.getPrezzo());
        varianteObject.put("quantity", variante.getQuantita());
        varianteObject.put("sconto", variante.getSconto());
        varianteObject.put("evidenza", variante.isEvidenza() ? 1 : 0);
        return varianteObject;
    }

    //Crea un oggetto JSON
    protected static JSONObject jsonProductHelper(Prodotto prodotto) {
        JSONObject productObject = new JSONObject();
        productObject.put("idProdotto", prodotto.getIdProdotto());
        productObject.put("nome", prodotto.getNome());
        productObject.put("descrizione", prodotto.getDescrizione());
        productObject.put("categoria", prodotto.getCategoria());
        productObject.put("immagine", prodotto.getImmagine());
        productObject.put("calorie", prodotto.getCalorie());
        productObject.put("carboidrati", prodotto.getCarboidrati());
        productObject.put("proteine", prodotto.getProteine());
        productObject.put("grassi", prodotto.getGrassi());
        return productObject;
    }

    //Crea un oggetto JSON
    protected static JSONObject jsonUtenteHelper(Utente utente) {
        JSONObject userObject = new JSONObject();
        userObject.put("email", utente.getEmail());
        userObject.put("password", utente.getPassword());
        userObject.put("nome", utente.getNome());
        userObject.put("cognome", utente.getCognome());
        userObject.put("codiceFiscale", utente.getCodiceFiscale());
        userObject.put("dataDiNascita", utente.getDataNascita() != null ? new SimpleDateFormat("yyyy-MM-dd").format(utente.getDataNascita()) : "");
        userObject.put("indirizzo", utente.getIndirizzo());
        userObject.put("telefono", utente.getTelefono());
        return userObject;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doGet(req, resp);
        } catch (ServletException | IOException e) {
            log("Errore in showRowForm doPost", e);
            if (!resp.isCommitted()) {
                ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno.");
            }
        }
    }
}