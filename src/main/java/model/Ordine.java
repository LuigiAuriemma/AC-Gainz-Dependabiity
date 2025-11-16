package model;

import java.util.Date;


public class Ordine {

    private String emailUtente;
    
    private int idOrdine;
    
    private String stato;
    
    private float totale;
    
    private Date dataOrdine;
    
    private String descrizione;


    
    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }


    
    public String getEmailUtente() {
        return emailUtente;
    }

    public void setEmailUtente(String emailUtente) {
        this.emailUtente = emailUtente;
    }


    
    public int getIdOrdine() {
        return idOrdine;
    }

    public void setIdOrdine(int idOrdine) {
        this.idOrdine = idOrdine;
    }


    
    public String getStato() {
        return stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }


    
    public float getTotale() {
        return totale;
    }

    public void setTotale(float totale) {
        this.totale = totale;
    }


    
    public Date getDataOrdine() {
        return dataOrdine;
    }

    public void setDataOrdine(Date dataOrdine) {
        this.dataOrdine = dataOrdine;
    }
}
