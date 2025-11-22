package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConfezioneDAO {

    /*@
    @   requires id > 0;
    @   ensures \result == null || \result != null;
    @   signals (RuntimeException e) true;
    @ also
    @   requires id <= 0;
    @   signals (IllegalArgumentException) true;
    @*/
    public /*@ nullable @*/Confezione doRetrieveById(int id) {

        if (id <= 0) {
            throw new IllegalArgumentException("ID confezione non valido: deve essere > 0");
        }

        try (Connection connection = ConPool.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * from confezione where id_confezione = ?");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Confezione confezione = new Confezione();
                int result = resultSet.getInt("id_confezione");
                if (result == 0) {
                    throw new RuntimeException("ID confezione non può essere 0");
                }
                confezione.setIdConfezione(result);
                confezione.setPeso(resultSet.getInt("peso"));
                return confezione;
            } else {
                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /*@
    @   ensures \result != null;
    @   signals (RuntimeException e) true;
    @*/
    public List<Confezione> doRetrieveAll() {
        List<Confezione> confezioni = new ArrayList<>();
        try (Connection connection = ConPool.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("select * from confezione");
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Confezione confezione = new Confezione();
                confezione.setIdConfezione(resultSet.getInt("id_confezione"));
                confezione.setPeso(resultSet.getInt("peso"));
                confezioni.add(confezione);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return confezioni;
    }

    /*@
    @   requires confezione != null;
    @   signals (RuntimeException e) true;
    @*/
    public void doSaveConfezione(Confezione confezione) {

        if (confezione == null) {
            throw new NullPointerException("Confezione non può essere nulla");
        }

        try (Connection connection = ConPool.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO confezione(id_confezione, peso) VALUES (?, ?)");

            preparedStatement.setInt(1, confezione.getIdConfezione());
            preparedStatement.setInt(2, confezione.getPeso());

            preparedStatement.executeQuery();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /*@
    @   requires confezione != null && idConfezione > 0;
    @   signals (RuntimeException e) true;
    @*/
    public void doUpdateConfezione(Confezione confezione, int idConfezione){

        if (confezione == null) {
            throw new NullPointerException("La confezione non può essere nulla");
        }

        try (Connection connection = ConPool.getConnection()){
            PreparedStatement preparedStatement = connection.prepareStatement("update confezione set id_confezione = ?, peso = ? where id_confezione = ?");
            preparedStatement.setInt(1, confezione.getIdConfezione());
            preparedStatement.setInt(2, confezione.getPeso());
            preparedStatement.setInt(3, idConfezione);

            preparedStatement.executeUpdate();

        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    /*@
    @   requires idConfezione > 0;
    @   signals (RuntimeException e) true;
    @ also
    @   requires idConfezione <= 0;
    @   signals (IllegalArgumentException) true;
    @*/
    public void doRemoveConfezione(int idConfezione){

        if (idConfezione <= 0) {
            throw new IllegalArgumentException("ID confezione non valido: deve essere > 0");
        }

        try (Connection connection = ConPool.getConnection()){
            PreparedStatement preparedStatement = connection.prepareStatement("delete from confezione where id_confezione = ?");
            preparedStatement.setInt(1, idConfezione);

            preparedStatement.executeUpdate();

        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }
}

