package model;


public class DettaglioOrdine {
    
    //@ spec_public
    private int idOrdine;
    
    //@ spec_public nullable
    private String idProdotto;
    
    //@ spec_public
    private int idVariante;
    
    //@ spec_public
    private int quantita;
    
    //@ spec_public
    private float prezzo;
    
    //@ spec_public nullable
    private String gusto;
    
    //@ spec_public
    private int pesoConfezione;
    
    //@ spec_public nullable
    private String immagineProdotto;
    
    //@ spec_public nullable
    private String nomeProdotto;


    
    /*@ 
    @ ensures \result == immagineProdotto;
    @ pure 
    @*/
    public /*@ nullable @*/ String getImmagineProdotto() {
        return immagineProdotto;
    }

    /*@ 
    @ ensures this.immagineProdotto == immagineProdotto;
    @ assignable this.immagineProdotto; 
    @*/
    public void setImmagineProdotto(/*@ nullable @*/ String immagineProdotto) {
        this.immagineProdotto = immagineProdotto;
    }


    
    /*@ 
    @ ensures \result == nomeProdotto;
    @ pure 
    @*/
    public /*@ nullable @*/ String getNomeProdotto() {
        return nomeProdotto;
    }

    /*@ 
    @ ensures this.nomeProdotto == nomeProdotto;
    @ assignable this.nomeProdotto; 
    @*/
    public void setNomeProdotto(/*@ nullable @*/ String nomeProdotto) {
        this.nomeProdotto = nomeProdotto;
    }


    
    /*@ 
    @ ensures \result == gusto;
    @ pure 
    @*/
    public /*@ nullable @*/ String getGusto() {
        return gusto;
    }

    /*@ 
    @ ensures this.gusto == gusto;
    @ assignable this.gusto; 
    @*/
    public void setGusto(/*@ nullable @*/ String gusto) {
        this.gusto = gusto;
    }


    
    /*@ 
    @ ensures \result == pesoConfezione;
    @ pure 
    @*/
    public int getPesoConfezione() {
        return pesoConfezione;
    }

    /*@ 
    @ requires pesoConfezione > 0;
    @ ensures this.pesoConfezione == pesoConfezione;
    @ assignable this.pesoConfezione; 
    @*/
    public void setPesoConfezione(int pesoConfezione) {
        this.pesoConfezione = pesoConfezione;
    }


    
    /*@ 
    @ ensures \result == idVariante;
    @ pure 
    @*/
    public int getIdVariante() {
        return idVariante;
    }

    /*@ 
    @ requires idVariante > 0;
    @ ensures this.idVariante == idVariante;
    @ assignable this.idVariante; 
    @*/
    public void setIdVariante(int idVariante) {
        this.idVariante = idVariante;
    }


    
    /*@ 
    @ ensures \result == idOrdine;
    @ pure 
    @*/
    public int getIdOrdine() {
        return idOrdine;
    }

    /*@ 
    @ requires idOrdine > 0;
    @ ensures this.idOrdine == idOrdine;
    @ assignable this.idOrdine; 
    @*/
    public void setIdOrdine(int idOrdine) {
        this.idOrdine = idOrdine;
    }


    
    /*@ 
    @ ensures \result == idProdotto;
    @ pure 
    @*/
    public /*@ nullable @*/ String getIdProdotto() {
        return idProdotto;
    }

    /*@ 
    @ ensures this.idProdotto == idProdotto;
    @ assignable this.idProdotto; 
    @*/
    public void setIdProdotto(/*@ nullable @*/ String idProdotto) {
        this.idProdotto = idProdotto;
    }


    
    /*@ 
    @ ensures \result == quantita;
    @ pure 
    @*/
    public int getQuantita() {
        return quantita;
    }

    /*@ 
    @ requires quantita > 0;
    @ ensures this.quantita == quantita;
    @ assignable this.quantita; 
    @*/
    public void setQuantita(int quantita) {
        this.quantita = quantita;
    }


    
    /*@ 
    @ ensures \result == prezzo;
    @ pure 
    @*/
    public float getPrezzo() {
        return prezzo;
    }

    /*@ 
    @ requires prezzo >= 0;
    @ ensures this.prezzo == prezzo;
    @ assignable this.prezzo; 
    @*/
    public void setPrezzo(float prezzo) {
        this.prezzo = prezzo;
    }
}
