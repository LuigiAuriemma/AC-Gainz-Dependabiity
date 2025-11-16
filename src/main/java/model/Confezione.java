package model;

public class Confezione {

    //@ spec_public
    private int idConfezione;
    //@ spec_public
    private int peso;

    // --- AGGIUNGI QUESTO BLOCCO ---
    /*@
        assignable \nothing;
        ensures idConfezione == 0;
        ensures peso == 0;
     @*/
    public Confezione() {}

    /*@
    @ ensures \result == this.idConfezione;
    @ pure
    @*/
    public int getIdConfezione() {
        return idConfezione;
    }
    /*@
    @   requires idConfezione > 0;
    @   assignable this.idConfezione;
    @   ensures this.idConfezione == idConfezione;
    @*/
    public void setIdConfezione(int idConfezione) {
        this.idConfezione = idConfezione;
    }


    /*@
    @ ensures \result == this.peso;
    @ pure
    @*/
    public int getPeso() {
        return peso;
    }
    /*@
    @   requires peso > 0;
    @   assignable this.peso;
    @   ensures this.peso == peso;
    @*/
    public void setPeso(int peso) {
        this.peso = peso;
    }
}
