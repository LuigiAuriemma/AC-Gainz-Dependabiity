package model;

public class Gusto {
    /*@
    @   public invariant
    @   idGusto >= 0;
    @*/
    private int idGusto;
    private String nomeGusto;


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
    public String getNomeGusto() {
        return nomeGusto;
    }
    /*@
    @   requires this.nomeGusto != null;
    @   assignable this.nomeGusto;
    @   ensures this.nomeGusto.equals(nomeGusto);
    @*/
    public void setNome(String nomeGusto) {
        this.nomeGusto = nomeGusto;
    }
}
