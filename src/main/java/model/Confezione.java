package model;

public class Confezione {
    /*@
    @   public invariant
    @   idConfezione >= 0 &&
    @   peso >= 0;
    @*/
    private int idConfezione;
    private int peso;

    //@ public pure;
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


    //@ public pure;
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
