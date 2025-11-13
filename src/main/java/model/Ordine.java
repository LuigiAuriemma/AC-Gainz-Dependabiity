package model;

import java.util.Date;

//@ nullable_by_default
public class Ordine {
    /*@
    @   public invariant
    @   idOrdine >= 0;
    @   totale >= 0;
    @*/
    //@ spec_public
    private String emailUtente;
    //@ spec_public
    private int idOrdine;
    //@ spec_public
    private String stato;
    //@ spec_public
    private float totale;
    //@ spec_public
    private Date dataOrdine;
    //@ spec_public
    private String descrizione;


    //@ pure
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


    //@ pure
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


    //@ pure
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


    //@ pure
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


    //@ pure
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


    //@ pure
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
