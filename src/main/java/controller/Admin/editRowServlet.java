package controller.Admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@WebServlet(value = "/editRow")
@MultipartConfig
public class editRowServlet extends HttpServlet {

    // Lista dei nomi di tabella validi
    private static final List<String> VALID_TABLE_NAMES = List.of(
            "utente", "prodotto", "variante", "ordine",
            "dettaglioOrdine", "gusto", "confezione"
    );

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // prende il nome della tabella e la primarykey dalla request
        String tableName = req.getParameter("tableName");
        String primaryKey = req.getParameter("primaryKey");

        if (tableName == null || tableName.isBlank() || primaryKey == null || primaryKey.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parametri 'tableName' o 'primaryKey' mancanti.");
            return;
        }

        // Validazione whitelist
        if (tableName == null || !VALID_TABLE_NAMES.contains(tableName)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid table name.");
            return;
        }

        System.out.println("tableName: " + tableName);

        boolean success = false;

        // in base a quale tabella viene scelta viene chiamato un metodo
        // se il nome della tabella è errato manda un errore
        switch (tableName) {
            case "utente" ->
                success = editUtente(req, primaryKey);
            case "prodotto" ->
                success = editProdotto(req, primaryKey);
            case "variante" ->
                success = editVariante(req, primaryKey);
            case "ordine" ->
                success = editOrdine(req, primaryKey);
            case "dettaglioOrdine" ->
                success = editDettaglioOrdine(req, primaryKey);
            case "gusto" ->
                success = editGusto(req, primaryKey);
            case "confezione" ->
                success = editConfezione(req, primaryKey);
        }

        // se ha funzionato tutto correttamente mostra la tabella
        if (success) {
            req.getRequestDispatcher("showTable?tableName=" + tableName).forward(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input data.");
        }
    }

    // Controlla se i parametri sono validi
    private boolean isValid(List<String> params) {
        for (String param : params) {
            if (param == null || param.isBlank()) {
                return false;
            }
        }
        return true;
    }

    // prende i parametri dalla request,crea un oggetto Utente avente quei parametri
    // e poi tramite il DAO
    // aggiorna i parametri nel database
    private boolean editUtente(HttpServletRequest req, String primaryKey) {
        String email = req.getParameter("email");
        String nome = req.getParameter("nome");
        String cognome = req.getParameter("cognome");
        String codiceFiscale = req.getParameter("codiceFiscale");
        String dataDiNascita = req.getParameter("dataDiNascita");
        String indirizzo = req.getParameter("indirizzo");
        String telefono = req.getParameter("telefono");

        if (isValid(Arrays.asList(email, nome, cognome, codiceFiscale, dataDiNascita, indirizzo, telefono))) {
            Utente u = new Utente();
            u.setEmail(email);
            u.setNome(nome);
            u.setCognome(cognome);
            u.setCodiceFiscale(codiceFiscale);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date ddn = dateFormat.parse(dataDiNascita);
                u.setDataNascita(ddn);
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }

            u.setIndirizzo(indirizzo);
            u.setTelefono(telefono);

            UtenteDAO utenteDAO = new UtenteDAO();
            utenteDAO.doUpdateCustomer(u, primaryKey);
            return true;
        }
        return false;
    }

    // prende i parametri dalla request,crea un oggetto Prodotto avente quei
    // parametri e poi tramite il DAO
    // aggiorna i parametri nel database
    private boolean editProdotto(HttpServletRequest req, String primaryKey) {
        String idProdotto = req.getParameter("idProdotto");
        String nome = req.getParameter("nome");
        String descrizione = req.getParameter("descrizione");
        String categoria = req.getParameter("categoria");
        String immagine = req.getParameter("immagine");
        String calorie = req.getParameter("calorie");
        String carboidrati = req.getParameter("carboidrati");
        String proteine = req.getParameter("proteine");
        String grassi = req.getParameter("grassi");

        if (isValid(
                Arrays.asList(idProdotto, nome, descrizione, categoria, immagine, calorie, carboidrati, proteine,
                        grassi))) {

            try {
                Prodotto p = new Prodotto();
                p.setIdProdotto(idProdotto);
                p.setNome(nome);
                p.setDescrizione(descrizione);
                p.setCategoria(categoria);
                p.setImmagine(immagine);
                p.setCalorie(Integer.parseInt(calorie));
                p.setCarboidrati(Integer.parseInt(carboidrati));
                p.setProteine(Integer.parseInt(proteine));
                p.setGrassi(Integer.parseInt(grassi));

                ProdottoDAO prodottoDAO = new ProdottoDAO();
                prodottoDAO.updateProduct(p, primaryKey);
                return true;
            } catch (NumberFormatException e) {
                // Se calorie, carboidrati, ecc. non sono numeri, fallisce
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    // prende i parametri dalla request,crea un oggetto Variante avente quei
    // parametri e poi tramite il DAO
    // aggiorna i parametri nel database
    private boolean editVariante(HttpServletRequest req, String primaryKey) {
        String idVariante = req.getParameter("idVariante");
        String idProdottoVariante = req.getParameter("idProdottoVariante");
        String idGusto = req.getParameter("idGusto");
        String idConfezione = req.getParameter("idConfezione");
        String prezzo = req.getParameter("prezzo");
        String quantity = req.getParameter("quantity");
        String sconto = req.getParameter("sconto");
        String evidenza = req.getParameter("evidenza");

        if (isValid(
                Arrays.asList(idVariante, idProdottoVariante, idGusto, idConfezione, prezzo, quantity, sconto,
                        evidenza))) {
            try {
                Variante v = new Variante();
                v.setIdVariante(Integer.parseInt(idVariante));
                v.setIdProdotto(idProdottoVariante);
                v.setIdGusto(Integer.parseInt(idGusto));
                v.setIdConfezione(Integer.parseInt(idConfezione));
                v.setPrezzo(Float.parseFloat(prezzo));
                v.setQuantita(Integer.parseInt(quantity));
                v.setSconto(Integer.parseInt(sconto));
                v.setEvidenza(Integer.parseInt(evidenza) == 1);

                VarianteDAO varianteDAO = new VarianteDAO();
                varianteDAO.updateVariante(v, Integer.parseInt(primaryKey));
                return true;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    // prende i parametri dalla request,crea un oggetto Ordine avente quei parametri
    // e poi tramite il DAO
    // aggiorna i parametri nel database
    private boolean editOrdine(HttpServletRequest req, String primaryKey) {
        String idOrdine = req.getParameter("idOrdine");
        String emailUtente = req.getParameter("emailUtente");
        String dataStr = req.getParameter("data");
        String stato = req.getParameter("stato");
        String totaleStr = req.getParameter("totale");

        // Validate if all parameters are valid
        if (isValid(Arrays.asList(idOrdine, emailUtente, dataStr, stato, totaleStr))) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date data;
            try {
                data = dateFormat.parse(dataStr);
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }

            float totale;
            try {
                totale = Float.parseFloat(totaleStr);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }

            if (totale < 0) {
                return false;
            }

            Ordine ordine = new Ordine();
            try {
                ordine.setIdOrdine(Integer.parseInt(idOrdine));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }

            ordine.setStato(stato);
            ordine.setEmailUtente(emailUtente);
            ordine.setTotale(totale);
            ordine.setDataOrdine(data);

            OrdineDao ordineDao = new OrdineDao();
            try {
                ordineDao.doUpdateOrder(ordine, Integer.parseInt(primaryKey));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    // prende i parametri dalla request,crea un oggetto DettaglioOrdine avente quei
    // parametri e poi tramite il DAO
    // aggiorna i parametri nel database
    private boolean editDettaglioOrdine(HttpServletRequest req, String primaryKey) {
        String idOrdine = req.getParameter("idOrdine");
        String idVariante = req.getParameter("idVariante");
        String idProdotto = req.getParameter("idProdotto");
        String quantity = req.getParameter("quantity");

        if (isValid(Arrays.asList(idOrdine, idVariante, idProdotto, quantity))) {
            try {
                int q = Integer.parseInt(quantity);
                if (q < 0)
                    return false;

                DettaglioOrdine dettaglioOrdine = new DettaglioOrdine();
                dettaglioOrdine.setIdOrdine(Integer.parseInt(idOrdine));
                dettaglioOrdine.setIdProdotto(idProdotto);
                dettaglioOrdine.setIdVariante(Integer.parseInt(idVariante));
                dettaglioOrdine.setQuantita(q);

                String[] primaryKeys = primaryKey.split(", ");
                int firstPK = Integer.parseInt(primaryKeys[0]);
                String secondPK = primaryKeys[1];
                int thirdPK = Integer.parseInt(primaryKeys[2]);

                DettaglioOrdineDAO dettaglioOrdineDAO = new DettaglioOrdineDAO();
                dettaglioOrdineDAO.doUpdateDettaglioOrdine(dettaglioOrdine, firstPK, secondPK, thirdPK);
                return true;
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    // prende i parametri dalla request,crea un oggetto Gusto avente quei parametri
    // e poi tramite il DAO
    // aggiorna i parametri nel database
    private boolean editGusto(HttpServletRequest req, String primaryKey) {
        String idGusto = req.getParameter("idGusto");
        String nomeGusto = req.getParameter("nomeGusto");

        if (isValid(Arrays.asList(idGusto, nomeGusto))) {
            try {
                Gusto gusto = new Gusto();
                gusto.setIdGusto(Integer.parseInt(idGusto));
                gusto.setNome(nomeGusto);

                GustoDAO gustoDAO = new GustoDAO();
                gustoDAO.updateGusto(gusto, Integer.parseInt(primaryKey));
                return true;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    // prende i parametri dalla request,crea un oggetto Confezione avente quei
    // parametri e poi tramite il DAO
    // aggiorna i parametri nel database
    private boolean editConfezione(HttpServletRequest req, String primaryKey) {
        String idConfezione = req.getParameter("idConfezione");
        String pesoConfezione = req.getParameter("pesoConfezione");

        if (isValid(Arrays.asList(idConfezione, pesoConfezione))) {
            try {
                if (Integer.parseInt(pesoConfezione) > 0) {
                    Confezione confezione = new Confezione();
                    confezione.setIdConfezione(Integer.parseInt(idConfezione));
                    confezione.setPeso(Integer.parseInt(pesoConfezione));

                    ConfezioneDAO confezioneDAO = new ConfezioneDAO();
                    confezioneDAO.doUpdateConfezione(confezione, Integer.parseInt(primaryKey));
                    return true;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }
}
