package model;

//@nullable_by_default
public class DettaglioOrdine {
    /*@
    @   public invariant
    @   idOrdine >= 0 &&
    @   idVariante >= 0 &&
    @   quantita >=0 &&
    @   prezzo >= 0 &&
    @   pesoConfezione >= 0;
    @ */
    //@ spec_public
    private int idOrdine;
    //@ spec_public
    private String idProdotto;
    //@ spec_public
    private int idVariante;
    //@ spec_public
    private int quantita;
    //@ spec_public
    private float prezzo;
    //@ spec_public
    private String gusto;
    //@ spec_public
    private int pesoConfezione;
    //@ spec_public
    private String immagineProdotto;
    //@ spec_public
    private String nomeProdotto;


    //@ pure
    public String getImmagineProdotto() {
        return immagineProdotto;
    }
    /*@
    @   requires immagineProdotto != null;
    @   assignable this.immagineProdotto;
    @   ensures this.immagineProdotto.equals(immagineProdotto);
    @*/
    public void setImmagineProdotto(String immagineProdotto) {
        this.immagineProdotto = immagineProdotto;
    }


    //@ pure
    public String getNomeProdotto() {
        return nomeProdotto;
    }
    /*@
    @   requires nomeProdotto != null;
    @   assignable this.nomeProdotto;
    @   ensures this.nomeProdotto.equals(nomeProdotto);
    @*/
    public void setNomeProdotto(String nomeProdotto) {
        this.nomeProdotto = nomeProdotto;
    }


    //@ pure
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


    //@ pure
    public int getPesoConfezione() {
        return pesoConfezione;
    }
    /*@
    @   requires pesoConfezione > 0;
    @   assignable this.pesoConfezione;
    @   ensures this.pesoConfezione;
    @*/
    public void setPesoConfezione(int pesoConfezione) {
        this.pesoConfezione = pesoConfezione;
    }


    //@ pure
    public int getIdVariante() {
        return idVariante;
    }
    /*@
    @   requires idVariante;
    @   assignable this.idVariante;
    @   ensures this.idVariante.equals(idVariante);
    @*/
    public void setIdVariante(int idVariante) {
        this.idVariante = idVariante;
    }


    //@ pure
    public int getIdOrdine() {
        return idOrdine;
    }
    /*@
    @   requires idOrdine > 0;
    @   assignable this.idOrdine;
    @   ensures this.idOrdine;
    @*/
    public void setIdOrdine(int idOrdine) {
        this.idOrdine = idOrdine;
    }


    //@ pure
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


    //@ pure
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


    //@ pure
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
}
