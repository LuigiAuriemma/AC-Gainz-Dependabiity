package model;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

//@ nullable_by_default
public class Utente {

    //@ spec_public
    private String email;
    //@ spec_public
    private String password;
    //@ spec_public
    private String nome;
    //@ spec_public
    private String cognome;
    //@ spec_public
    private String codiceFiscale;
    //@ spec_public
    private Date dataNascita;
    //@ spec_public
    private String indirizzo;
    //@ spec_public
    private String telefono;
    //@ spec_public
    private boolean poteri;


    //@ pure
    public String getEmail() {
        return email;
    }
    /*@
    @   requires email != null;
    @   assignable this.email;
    @   ensures this.email.equals(email);
    @*/
    public void setEmail(String email) {
        this.email = email;
    }


    //@ pure
    public String getPassword() {
        return password;
    }
    /*@
    @   requires password != null;
    @   assignable this.password;
    @   ensures this.password.equlas(password);
    @*/
    public void setPassword(String password) {
        this.password = password;
    }

    public void hashPassword() {
        try {
            var digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(this.password.getBytes(StandardCharsets.UTF_8));
            this.password = String.format("%040x", new BigInteger(1, digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    //@ pure
    public String getNome() {
        return nome;
    }
    /*@
    @   requires nome != null;
    @   assignable this.nome;
    @   ensures this.nome.equlas(nome);
    @*/
    public void setNome(String nome) {
        this.nome = nome;
    }


    //@ pure
    public String getCognome() {
        return cognome;
    }
    /*@
    @   requires cognome != null;
    @   assignable this.cognome;
    @   ensures this.cognome.equlas(cognome);
    @*/
    public void setCognome(String cognome) {
        this.cognome = cognome;
    }


    //@ pure
    public String getCodiceFiscale() {
        return codiceFiscale;
    }
    /*@
    @   requires codiceFiscale != null;
    @   assignable this.codiceFiscale;
    @   ensures this.codiceFiscale.equlas(codiceFiscale);
    @*/
    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }


    //@ pure
    public Date getDataNascita() {
        return dataNascita;
    }
    /*@
    @   requires dataNascita != null;
    @   assignable this.dataNascita;
    @   ensures this.dataNascita.equals(dataNascita);
    @*/
    public void setDataNascita(Date dataNascita) {
        this.dataNascita = dataNascita;
    }


    //@ pure
    public String getIndirizzo() {
        return indirizzo;
    }
    /*@
    @   requires indirizzo != null;
    @   assignable this.indirizzo;
    @   ensures this.indirizzo.equals(indirizzo);
    @*/
    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }


    //@ pure
    public String getTelefono() {
        return telefono;
    }
    /*@
    @   requires telefono != null;
    @   assignable this.telefono;
    @   ensures this.telefono.equals(telefono)
    @*/
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }


    //@ pure
    public boolean getPoteri() {
        return poteri;
    }
    /*@
    @   assignable this.poteri;
    @   ensures this.poteri == poteri;
    @*/
    public void setPoteri(boolean poteri) {
        this.poteri = poteri;
    }

}
