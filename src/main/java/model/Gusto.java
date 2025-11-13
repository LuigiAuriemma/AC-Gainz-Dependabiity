package model;


public class Gusto {
    /*@
    @   public invariant
    @   idGusto >= 0;
    @*/
    //@ spec_public
    private int idGusto;
    //@ spec_public nullable
    private String nomeGusto;


    //@ pure
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


    //@ pure nullable
    public String getNomeGusto() {
        return nomeGusto;
    }
    /*@
    @   requires nomeGusto != null;
    @   assignable this.nomeGusto;
    @   ensures this.nomeGusto.equals(nomeGusto);
    @*/
    public void setNome(String nomeGusto) {
        this.nomeGusto = nomeGusto;
    }
}
