package model;

public class Variante {
    /*@
    @   public invariant
    @   idVariante >= 0 &&
    @   idGusto >= 0 &&
    @   idConfezione >= 0 &&
    @   pesoConfezione >= 0 &&
    @   quantita >= 0 &&
    @   prezzo >= 0 &&
    @   sconto >= 0;
    @*/
    private int idVariante;
    private String idProdotto;
    private int idGusto;
    private String gusto;
    private int idConfezione;
    private int pesoConfezione;
    private int quantita; //Quantità disponibile
    private float prezzo;
    private int sconto;
    private boolean evidenza;


    //@public pure;
    public String getGusto() {
        return gusto;
    }
    /*@
    @   requires gusto != null;
    @   assignable this.gusto;
    @   ensures this.gusto.equals(gusto);
    @*/
    public void setGusto(String gusto) {
        this.gusto = gusto;
    }


    //@public pure;
    public int getPesoConfezione() {
        return pesoConfezione;
    }
    /*@
    @   requires pesoConfezione > 0;
    @   assignable this.pesoConfezione;
    @   ensures this.pesoConfezione == pesoConfezione;
    @*/
    public void setPesoConfezione(int pesoConfezione) {
        this.pesoConfezione = pesoConfezione;
    }


    //@public pure;
    public int getIdVariante() {
        return idVariante;
    }
    /*@
    @   requires idVariante > 0;
    @   assignable this.idVariante;
    @   ensures this.idVariante == idVariante;
    @*/
    public void setIdVariante(int idVariante) {
        this.idVariante = idVariante;
    }


    //@public pure;
    public String getIdProdotto() {
        return idProdotto;
    }
    /*@
    @   requires idProdotto != null;
    @   assignable this.idProdotto;
    @   ensures this.idProdotto.equals(idProdotto);
    @*/
    public void setIdProdotto(String idProdotto) {
        this.idProdotto = idProdotto;
    }


    //@public pure;
    public int getIdGusto() {
        return idGusto;
    }
    /*@
    @   requires idGusto > 0;
    @   assignable this.idGusto;
    @   ensures this.idGusto == idGusto;
    @*/
    public void setIdGusto(int idGusto) {
        this.idGusto = idGusto;
    }


    //@public pure;
    public int getIdConfezione() {
        return idConfezione;
    }
    /*@
    @   requires idConfezione > 0;
    @   assignable this.idConfezione;
    @   ensures this.idConfezione.equals(idConfezione)
    @*/
    public void setIdConfezione(int idConfezione) {
        this.idConfezione = idConfezione;
    }


    //@public pure;
    public int getQuantita() {
        return quantita;
    }
    /*@
    @   requires quantita > 0;
    @   assignable this.quantita;
    @   ensures this.quantita == quantita;
    @*/
    public void setQuantita(int quantita) {
        this.quantita = quantita;
    }


    //@public pure;
    public float getPrezzo() {
        return prezzo;
    }
    /*@
    @   requires prezzo > 0;
    @   assignable this.prezzo;
    @   ensures this.prezzo == prezzo;
    @*/
    public void setPrezzo(float prezzo) {
        this.prezzo = prezzo;
    }


    //@public pure;
    public int getSconto() {
        return sconto;
    }
    /*@
    @   requires sconto >= 0;
    @   assignable this.sconto;
    @   ensures this.sconto == sconto;
    @*/
    public void setSconto(int sconto) {
        this.sconto = sconto;
    }


    //@public pure;
    public boolean isEvidenza() {
        return evidenza;
    }
    /*@
    @   assignable  this.evidenza;
    @   ensures this.evidenza == evidenza
    @*/
    public void setEvidenza(boolean evidenza) {
        this.evidenza = evidenza;
    }
}
