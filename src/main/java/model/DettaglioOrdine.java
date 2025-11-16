package model;


public class DettaglioOrdine {
    
    private int idOrdine;
    
    private String idProdotto;
    
    private int idVariante;
    
    private int quantita;
    
    private float prezzo;
    
    private String gusto;
    
    private int pesoConfezione;
    
    private String immagineProdotto;
    
    private String nomeProdotto;


    
    public String getImmagineProdotto() {
        return immagineProdotto;
    }

    public void setImmagineProdotto(String immagineProdotto) {
        this.immagineProdotto = immagineProdotto;
    }


    
    public String getNomeProdotto() {
        return nomeProdotto;
    }

    public void setNomeProdotto(String nomeProdotto) {
        this.nomeProdotto = nomeProdotto;
    }


    
    public String getGusto() {
        return gusto;
    }

    public void setGusto(String gusto) {
        this.gusto = gusto;
    }


    
    public int getPesoConfezione() {
        return pesoConfezione;
    }

    public void setPesoConfezione(int pesoConfezione) {
        this.pesoConfezione = pesoConfezione;
    }


    
    public int getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(int idVariante) {
        this.idVariante = idVariante;
    }


    
    public int getIdOrdine() {
        return idOrdine;
    }

    public void setIdOrdine(int idOrdine) {
        this.idOrdine = idOrdine;
    }


    
    public String getIdProdotto() {
        return idProdotto;
    }

    public void setIdProdotto(String idProdotto) {
        this.idProdotto = idProdotto;
    }


    
    public int getQuantita() {
        return quantita;
    }

    public void setQuantita(int quantita) {
        this.quantita = quantita;
    }


    
    public float getPrezzo() {
        return prezzo;
    }

    public void setPrezzo(float prezzo) {
        this.prezzo = prezzo;
    }
}
