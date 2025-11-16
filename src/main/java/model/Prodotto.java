package model;


import java.util.ArrayList;
import java.util.List;


public class Prodotto {

    
    private String idProdotto;
    
    private String nome;
    
    private String descrizione;
    
    private String categoria;
    
    private String immagine;
    
    private int calorie;
    
    private int carboidrati;
    
    private int proteine;
    
    private int grassi;

    private List<Variante> varianti;


    public List<Variante> getVarianti() {
        if (this.varianti == null) this.varianti = new ArrayList<>();

        return varianti;
    }

    public void setVarianti(List<Variante> varianti) {
        this.varianti = varianti;
    }


    
    public String getIdProdotto() {
        return idProdotto;
    }

    public void setIdProdotto(String idProdotto) {
        this.idProdotto = idProdotto;
    }


    
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }


    
    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }


    
    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }



    public String getImmagine() {
        return immagine;
    }

    public void setImmagine(String immagine) {
        this.immagine = immagine;
    }


    
    public int getCalorie() {
        return calorie;
    }

    public void setCalorie(int calorie) {
        this.calorie = calorie;
    }


    
    public int getCarboidrati() {
        return carboidrati;
    }

    public void setCarboidrati(int carboidrati) {
        this.carboidrati = carboidrati;
    }


    
    public int getProteine() {
        return proteine;
    }

    public void setProteine(int proteine) {
        this.proteine = proteine;
    }


    
    public int getGrassi() {
        return grassi;
    }

    public void setGrassi(int grassi) {
        this.grassi = grassi;
    }
}










