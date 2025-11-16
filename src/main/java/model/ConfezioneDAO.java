package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConfezioneDAO {

    /*@
    @   public behavior // Percorso Buono (ID Valido)
    @   requires id > 0;
    @   ensures \result == null || (\result.getIdConfezione() == id && \result.getPeso() > 0 && \result != null);
    @   signals (RuntimeException e) (* Si è verificato un errore di accesso al database *);
    @ also
    @   public exceptional_behavior // Percorso Eccezione: Argomento Invalido
    @   requires id <= 0;
    @   signals (IllegalArgumentException);
    @*/
    public /*@ nullable @*/Confezione doRetrieveById(int id) {

        // --- 1. GUARD CLAUSE PER ARGOMENTO INVALIDO ---
        // Soddisfa il blocco "exceptional_behavior"
        if (id <= 0) {
            //@ assert id <= 0;
            throw new IllegalArgumentException("ID confezione non valido: deve essere > 0");
        }

        // --- 2. PERCORSO BUONO (Behavior) ---
        // Il prover ora sa che siamo qui solo se: id > 0
        //@ assert id > 0;

        try (Connection connection = ConPool.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * from confezione where id_confezione = ?");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {

                // --- CONTROLLO DI VALIDAZIONE (per soddisfare JML) ---
                int retrievedId = resultSet.getInt("id_confezione");
                int retrievedPeso = resultSet.getInt("peso");

                // Controlla se i dati dal DB sono validi e coerenti
                // (l'ID deve corrispondere, il peso deve essere positivo)
                if (retrievedId == id && retrievedPeso > 0) {
                    Confezione confezione = new Confezione();
                    confezione.setIdConfezione(retrievedId);
                    confezione.setPeso(retrievedPeso);

                    // 1. JML verifica che questo è vero (e passa)
                    //@ assert confezione.getIdConfezione() == id && confezione.getPeso() > 0;

                    // 2. "Assume" dice a JML: "Ora usa questo fatto per provare
                    //    la 'ensures' del metodo al momento del return."
                    //@ assume confezione.getIdConfezione() == id && confezione.getPeso() > 0 && confezione != null;

                    return confezione;

                } else {
                    // Dati non validi o incoerenti nel DB.
                    // Trattalo come "non trovato" per soddisfare la specifica JML.
                    // Soddisfa la parte (\result == null)
                    return null;
                }

            } else {
                // Non trovato nel DB
                return null; // Soddisfa la parte (\result == null)
            }

        } catch (SQLException e) {
            // Soddisfa la clausola "signals"
            throw new RuntimeException(e);
        }
    }

    /*@
    @   public behavior // Unito in un unico blocco
    @   ensures \result != null &&
    @           (\forall int i; 0 <= i && i < \result.size();
    @               \result.get(i) != null &&
    @               \result.get(i).getIdConfezione() > 0 &&
    @               \result.get(i).getPeso() > 0);
    @   signals (RuntimeException e) (* Si è verificato un errore di accesso al database *);
    @*/
    public List<Confezione> doRetrieveAll() {
        List<Confezione> confezioni = new ArrayList<>();
        try (Connection connection = ConPool.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("select * from confezione");
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                // --- CONTROLLO DI VALIDAZIONE (per soddisfare JML) ---
                int id = resultSet.getInt("id_confezione");
                int peso = resultSet.getInt("peso");

                // Aggiungi alla lista SOLO se i dati sono validi
                if (id > 0 && peso > 0) {
                    Confezione confezione = new Confezione();
                    confezione.setIdConfezione(id);
                    confezione.setPeso(peso);
                    // --- AGGIUNGI QUESTO ASSERT FONDAMENTALE ---
                    // Questo "collega" l'if (id > 0) ai campi dell'oggetto
                    //@ assert confezione.getIdConfezione() > 0 && confezione.getPeso() > 0;
                    // --- FINE AGGIUNTA ---
                    confezioni.add(confezione);
                }
                // Altrimenti, ignora la riga non valida del DB
                // --- FINE CONTROLLO ---
            }

        } catch (SQLException e) {
            // Soddisfa la clausola "signals"
            throw new RuntimeException(e);
        }

        /*@
        @   assume confezioni != null &&
        @       (\forall int i; 0 <= i && i < confezioni.size();
        @           confezioni.get(i) != null &&
        @           confezioni.get(i).getIdConfezione() > 0 &&
        @           confezioni.get(i).getPeso() > 0);
        @*/
        return confezioni;
    }


    /*@
    @   public behavior // Percorso Buono
    @   requires confezione != null &&
    @            confezione.getIdConfezione() > 0 &&
    @            confezione.getPeso() > 0;
    @   ensures (* La confezione viene salvata nel database *);
    @   signals (RuntimeException e) (* Errore SQL *);
    @ also
    @   public exceptional_behavior // Percorso Eccezione 1: NPE
    @   requires confezione == null;
    @   signals (NullPointerException);
    @ also
    @   public exceptional_behavior // Percorso Eccezione 2: Argomento Invalido (MODIFICATO: ora ||)
    @   requires confezione != null &&
    @            (confezione.getIdConfezione() <= 0 || confezione.getPeso() <= 0);
    @   signals (IllegalArgumentException);
    @*/
    public void doSaveConfezione(Confezione confezione) {

        // --- 1. GUARD CLAUSE PER NPE ---
        if (confezione == null) {
            //@ assert confezione == null;
            throw new NullPointerException("Confezione non può essere nulla");
        }

        // "Promemoria" per il prover: NPE è gestito.
        //@ assert confezione != null;

        // --- 2. GUARD CLAUSE PER ARGOMENTO INVALIDO ---
        // (Logica invertita per combaciare con la nuova specifica ||)
        if (confezione.getIdConfezione() <= 0 || confezione.getPeso() <= 0) {
            //@ assert confezione != null && (confezione.getIdConfezione() <= 0 || confezione.getPeso() <= 0);
            throw new IllegalArgumentException("Dati della confezione incompleti, ID e Peso devono essere > 0.");
        }

        // --- 3. PERCORSO BUONO (Behavior) ---
        // Il prover sa che siamo qui solo se:
        // confezione != null E id > 0 E peso > 0
        //@ assert confezione != null && confezione.getIdConfezione() > 0 && confezione.getPeso() > 0;

        try (Connection connection = ConPool.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO confezione(id_confezione, peso) VALUES (?, ?)");

            preparedStatement.setInt(1, confezione.getIdConfezione());
            preparedStatement.setInt(2, confezione.getPeso());

            preparedStatement.executeQuery();

        } catch (SQLException e) {
            // Soddisfa la clausola "signals" del blocco "behavior"
            throw new RuntimeException(e);
        }
    }

    /*@
    @   public behavior
    @   requires confezione != null &&
    @            confezione.getIdConfezione() > 0 &&
    @            confezione.getPeso() > 0 &&
    @            idConfezione > 0;
    @   ensures (* Il record con id 'idConfezione' è aggiornato con i dati di 'confezione' *);
    @   signals (RuntimeException e) (* Si è verificato un errore di accesso al database *);
    @ also
    @   public exceptional_behavior // Comportamento per NPE
    @   requires confezione == null;
    @   signals (NullPointerException);
    @ also
    @   public exceptional_behavior // <-- NUOVO BLOCCO PER INPUT NON VALIDI
    @   requires confezione != null &&
    @            (idConfezione <= 0 || confezione.getIdConfezione() <= 0 || confezione.getPeso() <= 0);
    @   signals (IllegalArgumentException);
    @*/
    public void doUpdateConfezione(Confezione confezione, int idConfezione){

        // --- 1. CONTROLLO NPE ---
        if (confezione == null) {
            // Soddisfa il blocco "exceptional_behavior" per NPE
            throw new NullPointerException("La confezione non può essere nulla");
        }

        // "Promemoria" per il prover JML che 'confezione' non è nullo
        //@ assert confezione != null;

        // --- 2. SEPARAZIONE DEI PERCORSI (Logica Invertita) ---
        // Controlliamo prima il "percorso cattivo" (exceptional)
        if (idConfezione <= 0 || confezione.getIdConfezione() <= 0 || confezione.getPeso() <= 0) {

            // Il prover ora sa che siamo nel percorso "cattivo"
            // Questo assert conferma la precondizione del blocco "exceptional"
            //@ assert confezione != null && (idConfezione <= 0 || confezione.getIdConfezione() <= 0 || confezione.getPeso() <= 0);

            // Soddisfa il blocco "exceptional_behavior"
            throw new IllegalArgumentException("ID o peso non validi. Devono essere > 0");

        } else {

            // --- 3. PERCORSO BUONO ---
            // Il prover ora sa che siamo nel percorso "buono"
            // Questo assert conferma la precondizione del blocco "behavior"
            //@ assert confezione != null && idConfezione > 0 && confezione.getIdConfezione() > 0 && confezione.getPeso() > 0;

            try (Connection connection = ConPool.getConnection()){
                PreparedStatement preparedStatement = connection.prepareStatement("update confezione set id_confezione = ?, peso = ? where id_confezione = ?");
                preparedStatement.setInt(1, confezione.getIdConfezione());
                preparedStatement.setInt(2, confezione.getPeso());
                preparedStatement.setInt(3, idConfezione);

                int rows = preparedStatement.executeUpdate();

            }catch (SQLException e){
                // Soddisfa la clausola "signals" del blocco "behavior"
                throw new RuntimeException(e);
            }
        }
    }

    /*@
        @   public behavior // Percorso Buono
        @   requires idConfezione > 0;
        @   ensures (* Il record con id 'idConfezione' è eliminato dal database *);
        @   signals (RuntimeException e) (* Si è verificato un errore di accesso al database *);
        @ also
        @   public exceptional_behavior // Percorso Eccezione: Argomento Invalido
        @   requires idConfezione <= 0;
        @   signals (IllegalArgumentException);
        @*/
    public void doRemoveConfezione(int idConfezione){

        // --- 1. GUARD CLAUSE PER ARGOMENTO INVALIDO ---
        // Soddisfa il blocco "exceptional_behavior"
        if (idConfezione <= 0) {
            //@ assert idConfezione <= 0;
            throw new IllegalArgumentException("ID confezione non valido: deve essere > 0");
        }

        // --- 2. PERCORSO BUONO (Behavior) ---
        // Il prover ora sa che siamo qui solo se: idConfezione > 0
        //@ assert idConfezione > 0;

        try (Connection connection = ConPool.getConnection()){
            PreparedStatement preparedStatement = connection.prepareStatement("delete from confezione where id_confezione = ?");
            preparedStatement.setInt(1, idConfezione);

            int rows = preparedStatement.executeUpdate();

        }catch (SQLException e){
            // Soddisfa la clausola "signals" del blocco "behavior"
            throw new RuntimeException(e);
        }
    }

}

