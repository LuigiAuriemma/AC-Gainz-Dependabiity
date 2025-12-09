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
import java.util.List;

import static controller.Admin.showRowForm.*;

@WebServlet(value = "/deleteRow")
public class deleteRowServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // prendiamo i dati dalla request
            String tableName = req.getParameter("tableName");
            String primaryKey = req.getParameter("primaryKey");
            Utente utente = (Utente) req.getSession().getAttribute("Utente");

            if (tableName == null || tableName.isBlank()) {
                ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_BAD_REQUEST, "Parametro tableName mancante.");
                return; // Interrompe l'esecuzione
            }

            // usato per controllare se admin cancella il suo stesso profilo
            boolean isTheSame = false;

            JSONArray jsonArray = null;
            boolean success = switch (tableName) {
                case "utente" -> handleRemoveRowFromUtente(primaryKey);
                case "prodotto" -> handleRemoveRowFromProdotto(primaryKey);
                case "variante" -> handleRemoveRowFromVariante(primaryKey);
                case "ordine" -> handleRemoveRowFromOrdine(primaryKey);
                case "dettaglioOrdine" -> handleRemoveRowFromDettaglioOrdine(primaryKey);
                case "gusto" -> handleRemoveRowFromGusto(primaryKey);
                case "confezione" -> handleRemoveRowFromConfezione(primaryKey);
                default -> false;
            };

            if (success && tableName.equals("utente")) {
                isTheSame = checkIfAdminDeletingSelf(primaryKey, utente);
            }

            if (isTheSame) {
                req.getSession(false).invalidate();
                resp.sendRedirect("index.jsp");
                return; // Interrompe l'esecuzione
            } else {
                if (success) {
                    jsonArray = getJsonArrayForTable(tableName);
                }

                if (jsonArray != null) {
                    sendJsonResponse(jsonArray, resp);
                } else {
                    ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid table name or primary key.");
                }
            }
        } catch (Exception e) {
            log("Errore in doGet deleteRowServlet", e);
            if (!resp.isCommitted()) {
                ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno durante l'operazione.");
            }
        }
    }

    private boolean checkIfAdminDeletingSelf(String primaryKey, Utente utente) {
        System.out.println("Checking if admin is deleting self"); // Log per debug

        // Controlla prima che 'utente' non sia nullo
        if (utente == null || primaryKey == null || primaryKey.isBlank()) {
            return false;
        }

        return utente.getEmail().equals(primaryKey);
    }

    private boolean handleRemoveRowFromConfezione(String primaryKey) {
        ConfezioneDAO confezioneDAO = new ConfezioneDAO();
        if (isValidPrimaryKey(primaryKey)) {
            int idConfezione = Integer.parseInt(primaryKey);
            confezioneDAO.doRemoveConfezione(idConfezione);
            return true;
        }
        return false;
    }

    private boolean handleRemoveRowFromGusto(String primaryKey) {
        if (isValidPrimaryKey(primaryKey)) {
            int idGusto = Integer.parseInt(primaryKey);
            GustoDAO gustoDAO = new GustoDAO();
            gustoDAO.doRemoveGusto(idGusto);
            return true;
        }
        return false;
    }

    // metodo che rimuove la tupla dalla tabella dettaglio ordine(presenta 2 chiavi
    // primarie)
    private boolean handleRemoveRowFromDettaglioOrdine(String primaryKey) {
        if (primaryKey == null || primaryKey.isBlank()) {
            return false;
        }

        String[] primaryKeys = primaryKey.split(", ");

        if (primaryKeys.length >= 3) {
            try {
                // Prendi gli ID (convertendoli in modo sicuro)
                int idOrdine = Integer.parseInt(primaryKeys[0]);
                int idVariante = Integer.parseInt(primaryKeys[2]);

                DettaglioOrdineDAO dettaglioOrdineDAO = new DettaglioOrdineDAO();
                dettaglioOrdineDAO.doRemoveDettaglioOrdine(idOrdine, idVariante);
                return true;
            } catch (NumberFormatException e) {
                // Uno degli ID non era un numero
                return false;
            }
        }
        return false; // La PK non era formattata correttamente
    }

    private boolean handleRemoveRowFromOrdine(String primaryKey) {
        if (isValidPrimaryKey(primaryKey)) {
            OrdineDao ordineDao = new OrdineDao();
            ordineDao.doDeleteOrder(Integer.parseInt(primaryKey));
            return true;
        }
        return false;
    }

    private boolean handleRemoveRowFromVariante(String primaryKey) {
        if (isValidPrimaryKey(primaryKey)) {
            VarianteDAO varianteDAO = new VarianteDAO();
            Variante v = varianteDAO.doRetrieveVarianteByIdVariante(Integer.parseInt(primaryKey));
            if (v != null) {
                varianteDAO.doRemoveVariante(Integer.parseInt(primaryKey));
                return true;
            }
        }
        return false;
    }

    private boolean handleRemoveRowFromProdotto(String primaryKey) {
        if (primaryKey == null || primaryKey.isBlank()) {
            return false;
        }

        ProdottoDAO prodottoDAO = new ProdottoDAO();
        Prodotto p = prodottoDAO.doRetrieveById(primaryKey);

        if (p != null) {
            prodottoDAO.removeProductFromIdProdotto(primaryKey);
            return true; // Successo
        }

        return false; // Fallimento (prodotto non trovato)
    }

    private boolean handleRemoveRowFromUtente(String primaryKey) {
        if (primaryKey != null && !primaryKey.isBlank()) {
            UtenteDAO utenteDAO = new UtenteDAO();
            Utente u = utenteDAO.doRetrieveByEmail(primaryKey);
            if (u != null) {
                utenteDAO.doRemoveUserByEmail(u.getEmail());
                return true;
            }
        }
        return false;
    }

    // metodo che in base a tableName prende tutte le tuple e le inserisce in un
    // JSONArray
    private JSONArray getJsonArrayForTable(String tableName) {
        return switch (tableName) {
            case "utente" -> getAllUtentiJsonArray(new UtenteDAO());
            case "prodotto" -> getAllProdottiJsonArray(new ProdottoDAO());
            case "variante" -> getAllVariantiJsonArray(new VarianteDAO());
            case "ordine" -> getAllOrdiniJsonArray(new OrdineDao());
            case "dettaglioOrdine" -> getAllDettagliOrdiniJsonArray(new DettaglioOrdineDAO());
            case "gusto" -> getAllGustiJsonArray(new GustoDAO());
            case "confezione" -> getAllConfezioniJsonArray(new ConfezioneDAO());
            default -> null;
        };
    }

    private static JSONArray getAllUtentiJsonArray(UtenteDAO utenteDAO) {
        List<Utente> utenti = utenteDAO.doRetrieveAll();
        JSONArray jsonArray = new JSONArray();
        for (Utente utente : utenti) {
            jsonArray.add(jsonHelperHere(utente));
        }
        return jsonArray;
    }

    private static JSONArray getAllProdottiJsonArray(ProdottoDAO prodottoDAO) {
        List<Prodotto> prodotti = prodottoDAO.doRetrieveAll();
        JSONArray jsonArray = new JSONArray();
        for (Prodotto prodotto : prodotti) {
            jsonArray.add(jsonProductHelper(prodotto));
        }
        return jsonArray;
    }

    private static JSONArray getAllVariantiJsonArray(VarianteDAO varianteDAO) {
        List<Variante> varianti = varianteDAO.doRetrieveAll();
        JSONArray jsonArray = new JSONArray();
        for (Variante variante : varianti) {
            jsonArray.add(jsonVarianteHelper(variante));
        }
        return jsonArray;
    }

    private static JSONArray getAllOrdiniJsonArray(OrdineDao ordineDao) {
        List<Ordine> ordini = ordineDao.doRetrieveAll();
        JSONArray jsonArray = new JSONArray();
        for (Ordine ordine : ordini) {
            jsonArray.add(jsonOrdineHelper(ordine));
        }
        return jsonArray;
    }

    private static JSONArray getAllDettagliOrdiniJsonArray(DettaglioOrdineDAO dettaglioOrdineDAO) {
        List<DettaglioOrdine> dettagliOrdini = dettaglioOrdineDAO.doRetrieveAll();
        JSONArray jsonArray = new JSONArray();
        for (DettaglioOrdine dettaglioOrdine : dettagliOrdini) {
            jsonArray.add(dettaglioOrdineHelper(dettaglioOrdine));
        }
        return jsonArray;
    }

    private static JSONArray getAllGustiJsonArray(GustoDAO gustoDAO) {
        List<Gusto> gusti = gustoDAO.doRetrieveAll();
        JSONArray jsonArray = new JSONArray();
        for (Gusto gusto : gusti) {
            jsonArray.add(gustoHelper(gusto));
        }
        return jsonArray;
    }

    private static JSONArray getAllConfezioniJsonArray(ConfezioneDAO confezioneDAO) {
        List<Confezione> confezioni = confezioneDAO.doRetrieveAll();
        JSONArray jsonArray = new JSONArray();
        for (Confezione confezione : confezioni) {
            jsonArray.add(confezioneHelper(confezione));
        }
        return jsonArray;
    }

    private static void sendJsonResponse(JSONArray jsonArray, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter o = response.getWriter();
        o.println(jsonArray);
        o.flush();
    }

    private boolean isValidPrimaryKey(String primaryKey) {
        if (primaryKey == null || primaryKey.isBlank()) {
            return false;
        }
        try {
            return Integer.parseInt(primaryKey) > 0;
        } catch (NumberFormatException e) {
            // Se non è un numero, non è una PK valida
            return false;
        }
    }

    // metodo che crea un oggetto JSON dell'utente
    protected static JSONObject jsonHelperHere(Utente x) {
        JSONObject userObject = new JSONObject();
        userObject.put("email", x.getEmail());
        userObject.put("nome", x.getNome());
        userObject.put("cognome", x.getCognome());
        userObject.put("codiceFiscale", x.getCodiceFiscale());
        if (x.getDataNascita() != null) {
            userObject.put("dataDiNascita", new SimpleDateFormat("yyyy-MM-dd").format(x.getDataNascita()));
        } else {
            userObject.put("dataDiNascita", "");
        }
        userObject.put("indirizzo", x.getIndirizzo());
        userObject.put("telefono", x.getTelefono());
        return userObject;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doGet(req, resp);
        } catch (ServletException | IOException e) {
            log("Errore in doPost deleteRowServlet", e);
            if (!resp.isCommitted()) {
                ServletUtils.sendErrorSafe(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno durante l'operazione.");
            }
        }
    }
}