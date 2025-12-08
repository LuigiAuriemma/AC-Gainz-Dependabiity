package controller.Admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import model.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@WebServlet("/insertRow")
@MultipartConfig
public class insertRowServlet extends HttpServlet {

    // Lista dei nomi di tabella validi
    private static final List<String> VALID_TABLE_NAMES = List.of(
            "utente", "prodotto", "variante", "ordine",
            "dettaglioOrdine", "gusto", "confezione"
    );

    private static final String CARTELLA_UPLOAD = "Immagini";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String nameTable = req.getParameter("nameTable");
        System.out.println(nameTable);

        if (nameTable == null || nameTable.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parametro 'nameTable' mancante.");
            return; // Interrompe l'esecuzione del metodo
        }

        // Validazione whitelist
        if (nameTable == null || !VALID_TABLE_NAMES.contains(nameTable)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid table name.");
            return;
        }

        boolean success = false;

        // in base a quale tabella viene scelta viene chiamato un metodo
        // se il nome della tabella è errato manda un errore
        switch (nameTable) {
            case "utente" ->
                success = insertUtente(req);
            case "prodotto" ->
                success = insertProdotto(req);
            case "variante" ->
                success = insertVariante(req);
            case "ordine" ->
                success = insertOrdine(req);
            case "dettagliOrdine" ->
                success = insertDettaglioOrdine(req);
            case "gusto" ->
                success = insertGusto(req);
            case "confezione" ->
                success = insertConfezione(req);
        }

        // se ha funzionato tutto correttamente mostra la tabella
        if (success) {
            req.getRequestDispatcher("showTable?tableName=" + nameTable).forward(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input data.");
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

    // prende i parametri dalla request,controlla che siano validi,
    // crea un oggetto Utente avente quei parametri e poi tramite il DAO
    // salva il nuovo Utente nel database tramite metodo DAO
    private boolean insertUtente(HttpServletRequest req) {
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String nome = req.getParameter("nome");
        String cognome = req.getParameter("cognome");
        String codiceFiscale = req.getParameter("codiceFiscale");
        String dataDiNascita = req.getParameter("dataDiNascita");
        String indirizzo = req.getParameter("indirizzo");
        String telefono = req.getParameter("telefono");

        if (isValid(Arrays.asList(email, password, nome, cognome, codiceFiscale, dataDiNascita, indirizzo, telefono))) {
            Utente u = new Utente();
            u.setEmail(email);
            u.setPassword(password);
            u.hashPassword();
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
            utenteDAO.doSave(u);
            return true;
        }
        return false;
    }

    // prende i parametri dalla request,controlla che siano validi,
    // crea un oggetto Prodotto avente quei parametri e poi tramite il DAO
    // salva il nuovo Prodotto nel database tramite metodo DAO
    private boolean insertProdotto(HttpServletRequest req) throws IOException, ServletException {
        String idProdotto = req.getParameter("idProdotto");
        String nome = req.getParameter("nome");
        String descrizione = req.getParameter("descrizione");
        String categoria = req.getParameter("categoria");
        Part filePart = req.getPart("immagine");

        if (filePart == null || filePart.getSize() == 0 || filePart.getSubmittedFileName() == null
                || filePart.getSubmittedFileName().isBlank()) {
            // Se il file non è stato inviato o è vuoto, non possiamo inserire il prodotto
            return false;
        }
        // Per l'immagine
        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        String destinazione = CARTELLA_UPLOAD + "/" + fileName;
        Path pathDestinazione = Paths.get(getServletContext().getRealPath(destinazione));

        for (int i = 2; Files.exists(pathDestinazione); i++) {
            destinazione = CARTELLA_UPLOAD + "/" + i + "_" + fileName;
            pathDestinazione = Paths.get(getServletContext().getRealPath(destinazione));
        }

        InputStream fileInputStream = filePart.getInputStream();
        Files.createDirectories(pathDestinazione.getParent());
        Files.copy(fileInputStream, pathDestinazione);

        String calorie = req.getParameter("calorie");
        String carboidrati = req.getParameter("carboidrati");
        String proteine = req.getParameter("proteine");
        String grassi = req.getParameter("grassi");

        if (isValid(Arrays.asList(idProdotto, nome, descrizione, categoria, calorie, carboidrati, proteine, grassi))) {
            try {
                Prodotto p = new Prodotto();
                p.setIdProdotto(idProdotto);
                p.setNome(nome);
                p.setDescrizione(descrizione);
                p.setCategoria(categoria);
                p.setImmagine(destinazione);
                p.setCalorie(Integer.parseInt(calorie));
                p.setCarboidrati(Integer.parseInt(carboidrati));
                p.setProteine(Integer.parseInt(proteine));
                p.setGrassi(Integer.parseInt(grassi));

                ProdottoDAO prodottoDAO = new ProdottoDAO();
                prodottoDAO.doSave(p);
                return true;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    // prende i parametri dalla request,controlla che siano validi,
    // crea un oggetto Variante avente quei parametri e poi tramite il DAO
    // salva il nuovo Variante nel database tramite metodo DAO
    private boolean insertVariante(HttpServletRequest req) {
        String idProdottoVariante = req.getParameter("idProdottoVariante");
        String idGusto = req.getParameter("idGusto");
        String idConfezione = req.getParameter("idConfezione");
        String prezzo = req.getParameter("prezzo");
        String quantity = req.getParameter("quantity");
        String sconto = req.getParameter("sconto");
        String evidenza = req.getParameter("evidenza");

        if (isValid(Arrays.asList(idProdottoVariante, idGusto, idConfezione, prezzo, quantity, sconto))) {

            try {
                float price = Float.parseFloat(prezzo);
                int q = Integer.parseInt(quantity);
                int discount = Integer.parseInt(sconto);
                boolean evidence = false;
                if (evidenza != null && !evidenza.isEmpty()) {
                    evidence = Integer.parseInt(evidenza) == 1;
                }

                if (price <= 0 || q <= 0 || discount < 0 || discount > 100) {
                    return false; // Dati non validi (es. prezzo negativo)
                }
                Variante v = new Variante();
                v.setIdProdotto(idProdottoVariante);
                v.setIdGusto(Integer.parseInt(idGusto));
                v.setIdConfezione(Integer.parseInt(idConfezione));
                v.setPrezzo(price);
                v.setQuantita(q);
                v.setSconto(discount);
                v.setEvidenza(evidence);

                VarianteDAO varianteDAO = new VarianteDAO();
                varianteDAO.doSaveVariante(v);
                return true;

            } catch (NumberFormatException e) {
                // Se un parametro non è un numero, cattura l'errore
                e.printStackTrace();
                return false; // Restituisce 'false' per far scattare l'errore 500
            }
        }
        return false;
    }

    // prende i parametri dalla request,controlla che siano validi,
    // crea un oggetto Ordine avente quei parametri e poi tramite il DAO
    // salva il nuovo Ordine nel database tramite metodo DAO
    private boolean insertOrdine(HttpServletRequest req) {
        String emailUtente = req.getParameter("emailUtente");
        String stato = req.getParameter("stato");
        String totale = req.getParameter("totale");
        String dataStr = req.getParameter("data");

        if (emailUtente != null && !emailUtente.isBlank()) {
            Ordine ordine = new Ordine();
            ordine.setEmailUtente(emailUtente);
            ordine.setStato(stato);

            try {
                if (totale != null && !totale.isBlank() && Float.parseFloat(totale) >= 0) {
                    ordine.setTotale(Float.parseFloat(totale));
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }

            if (dataStr != null && !dataStr.isBlank()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Date data = dateFormat.parse(dataStr);
                    ordine.setDataOrdine(data);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            OrdineDao ordineDao = new OrdineDao();
            ordineDao.doSave(ordine);
            return true;
        }
        return false;
    }

    // prende i parametri dalla request,controlla che siano validi,
    // crea un oggetto DettaglioOrdine avente quei parametri e poi tramite il DAO
    // salva il nuovo DettaglioOrdine nel database tramite metodo DAO
    private boolean insertDettaglioOrdine(HttpServletRequest req) {
        String idOrdine = req.getParameter("idOrdine");
        String idProdotto = req.getParameter("idProdotto");
        String idVariante = req.getParameter("idVariante");
        String quantity = req.getParameter("quantity");

        if (isValid(Arrays.asList(idOrdine, idProdotto, idVariante, quantity))) {
            try {
                int q = Integer.parseInt(quantity);

                if (q > 0) {
                    DettaglioOrdine dettaglioOrdine = new DettaglioOrdine();
                    dettaglioOrdine.setIdOrdine(Integer.parseInt(idOrdine));
                    dettaglioOrdine.setIdProdotto(idProdotto);
                    dettaglioOrdine.setIdVariante(Integer.parseInt(idVariante));
                    dettaglioOrdine.setQuantita(q);

                    DettaglioOrdineDAO dettaglioOrdineDAO = new DettaglioOrdineDAO();
                    dettaglioOrdineDAO.doSave(dettaglioOrdine);
                    return true;
                }
            } catch (NumberFormatException e) {
                // Se un parametro non è un numero, cattura l'errore
                e.printStackTrace(); // Logga l'errore per il debug
                return false; // Restituisce 'false' per far scattare l'errore 500
            }

        }
        return false;
    }

    // prende i parametri dalla request,controlla che siano validi,
    // crea un oggetto Gusto avente quei parametri e poi tramite il DAO
    // salva il nuovo Gusto nel database tramite metodo DAO
    private boolean insertGusto(HttpServletRequest req) {
        String nomeGusto = req.getParameter("nomeGusto");

        if (nomeGusto != null && !nomeGusto.isBlank()) {
            Gusto gusto = new Gusto();
            gusto.setNome(nomeGusto);

            GustoDAO gustoDAO = new GustoDAO();
            gustoDAO.doSaveGusto(gusto);
            return true;
        }
        return false;
    }

    // prende i parametri dalla request,controlla che siano validi,
    // crea un oggetto Confezione avente quei parametri e poi tramite il DAO
    // salva la nuova Confezione nel database tramite metodo DAO
    private boolean insertConfezione(HttpServletRequest req) {
        try {
            String peso = req.getParameter("pesoConfezione");

            if (peso != null && Integer.parseInt(peso) > 0) {
                Confezione confezione = new Confezione();
                confezione.setPeso(Integer.parseInt(peso));

                ConfezioneDAO confezioneDAO = new ConfezioneDAO();
                confezioneDAO.doSaveConfezione(confezione);
                return true;
            }
        } catch (NumberFormatException e) {
            // Se un parametro non è un numero, cattura l'errore
            e.printStackTrace(); // Logga l'errore per il debug
            return false; // Restituisce 'false' per far scattare l'errore 500
        }

        return false;
    }
}
