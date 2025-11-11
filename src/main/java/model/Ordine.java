package model;

import java.util.Date;

public class Ordine {
    /*@
    @   public invariant
    @   idOrdine >= 0;
    @   totale >= 0;
    @*/
    private String emailUtente;
    private int idOrdine;
    private String stato;
    private float totale;
    private Date dataOrdine;

    private String descrizione;


    //@public pure;
    public String getDescrizione() {
        return descrizione;
    }
    /*@
    @   requires descrizione != null;
    @   assignable this.descrizione != null;
    @   ensures this.descrizione.equals(descrizione);
    @*/
    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }


    //@public pure;
    public String getEmailUtente() {
        return emailUtente;
    }
    /*@
    @   requires emailUtente != null;
    @   assignable this.emailUtente;
    @   ensures this.emailUtente.equals(emailUtente);
    @*/
    public void setEmailUtente(String emailUtente) {
        this.emailUtente = emailUtente;
    }


    //@public pure;
    public int getIdOrdine() {
        return idOrdine;
    }
    /*@
    @   requires idOrdine > 0;
    @   assignable this.idOrdine;
    @   ensures this.idOrdine == idOrdine;
    @*/
    public void setIdOrdine(int idOrdine) {
        this.idOrdine = idOrdine;
    }


    //@public pure;
    public String getStato() {
        return stato;
    }
    /*@
    @   requires stato != null;
    @   assignable this.stato;
    @   ensures this.stato.equals(stato);
    @*/
    public void setStato(String stato) {
        this.stato = stato;
    }


    //@public pure;
    public float getTotale() {
        return totale;
    }
    /*@
    @   requires totale > 0;
    @   assignable this.totale;
    @   ensures this.totale == totale;
    @*/
    public void setTotale(float totale) {
        this.totale = totale;
    }


    //@public pure;
    public Date getDataOrdine() {
        return dataOrdine;
    }
    /*@
    @   requires dataOrdine != null;
    @   assignable this.dataOrdine;
    @   ensures this.dataOrdine.equals(dataOrdine);
    @*/
    public void setDataOrdine(Date dataOrdine) {
        this.dataOrdine = dataOrdine;
    }
}
