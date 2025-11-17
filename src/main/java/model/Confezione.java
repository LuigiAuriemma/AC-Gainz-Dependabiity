package model;

public class Confezione {

    //@ spec_public
    private int idConfezione;
    //@ spec_public
    private int peso;

    public Confezione() {}

    /*@ 
    @ ensures \result == idConfezione;
    @ pure 
    @*/
    public int getIdConfezione() {
        return idConfezione;
    }
    
    /*@ 
    @ ensures this.idConfezione == idConfezione;
    @ assignable this.idConfezione; 
    @*/
    public void setIdConfezione(int idConfezione) {
        this.idConfezione = idConfezione;
    }


    /*@ 
    @ ensures \result == peso;
    @ pure 
    @*/
    public int getPeso() {
        return peso;
    }
    
    /*@ 
    @ ensures this.peso == peso;
    @ assignable this.peso; 
    @*/
    public void setPeso(int peso) {
        this.peso = peso;
    }
}
