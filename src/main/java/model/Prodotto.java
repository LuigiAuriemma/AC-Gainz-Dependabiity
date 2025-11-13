package model;


import java.util.ArrayList;
import java.util.List;

//@ nullable_by_default
public class Prodotto {
    /*@
    @   public invariant
    @   calorie >= 0 &&
    @   carboidrati >= 0 &&
    @   proteine >= 0 &&
    @   grassi >= 0;
    @*/
    //@ spec_public
    private String idProdotto;
    //@ spec_public
    private String nome;
    //@ spec_public
    private String descrizione;
    //@ spec_public
    private String categoria;
    //@ spec_public
    private String immagine;
    //@ spec_public
    private int calorie;
    //@ spec_public
    private int carboidrati;
    //@ spec_public
    private int proteine;
    //@ spec_public
    private int grassi;

    private List<Variante> varianti;

    /*@
    @ // --- Comportamento 1: La lista esiste già ---
    @ behavior
    @   requires this.varianti != null;
    @   assignable \nothing;
    @   ensures \result == this.varianti;
    @
    @ also
    @
    @ // --- Comportamento 2: La lista non esiste
    @ behavior
    @   requires this.varianti == null;
    @   assignable this.varianti;
    @   ensures this.varianti != null && this.varianti.isEmpty();
    @   ensures \result == this.varianti;
    @*/
    public List<Variante> getVarianti() {
        if (this.varianti == null) this.varianti = new ArrayList<>();

        return varianti;
    }
    /*@
    @   requires varianti != null;
    @   assignable this.varianti;
    @   ensures this.varianti == varianti;
    @*/
    public void setVarianti(List<Variante> varianti) {
        this.varianti = varianti;
    }


    //@ pure
    public String getIdProdotto() {
        return idProdotto;
    }
    /*@
    @   requires idPordotto != null;
    @   assignable this.idProdotto;
    @   ensures this.idProdotto.equals(idProdotto);
    @*/
    public void setIdProdotto(String idProdotto) {
        this.idProdotto = idProdotto;
    }


    //@ pure
    public String getNome() {
        return nome;
    }
    /*@
    @   requires nome != null;
    @   assignable this.nome;
    @   ensures this.nome.equals(nome);
    @*/
    public void setNome(String nome) {
        this.nome = nome;
    }


    //@ pure
    public String getDescrizione() {
        return descrizione;
    }
    /*@
    @   requires descizione != null;
    @   assignable this.descrizione;
    @   ensures this.descrizione.equlas(descrizione);
    @*/
    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }


    //@ pure
    public String getCategoria() {
        return categoria;
    }
    /*@
    @   requires categoria != null;
    @   assignable this.categoria;
    @   ensures this.categoria.equlas(categoria);
    @*/
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }


    //@ pure
    public String getImmagine() {
        return immagine;
    }
    /*@
    @   requires immagine != null;
    @   assignable this.immagine;
    @   ensures this.immagine.equlas(immagine);
    @*/
    public void setImmagine(String immagine) {
        this.immagine = immagine;
    }


    //@ pure
    public int getCalorie() {
        return calorie;
    }
    /*@
    @   requires  calorie > 0;
    @   assignable this.calorie;
    @   ensures this.calorie.equals(calorie);
    @*/
    public void setCalorie(int calorie) {
        this.calorie = calorie;
    }


    //@ pure
    public int getCarboidrati() {
        return carboidrati;
    }
    /*@
    @   requires carboidrati > 0;
    @   assignable this.carboidrati;
    @   ensures this.carboidrati == carboidrati;
    @*/
    public void setCarboidrati(int carboidrati) {
        this.carboidrati = carboidrati;
    }


    //@ pure
    public int getProteine() {
        return proteine;
    }
    /*@
    @   requires proteine > 0;
    @   assignable this.proteine;
    @   ensures this.proteine == proteine;
    @*/
    public void setProteine(int proteine) {
        this.proteine = proteine;
    }


    //@ pure
    public int getGrassi() {
        return grassi;
    }
    /*@
    @   requires grassi > 0;
    @   assignable this.grassi;
    @   ensures this.grassi == grassi;
    @*/
    public void setGrassi(int grassi) {
        this.grassi = grassi;
    }
}










