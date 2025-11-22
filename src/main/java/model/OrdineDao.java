package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrdineDao {
    public Ordine doRetrieveById(int id) {
        Ordine ordine = new Ordine();
        try(Connection con= ConPool.getConnection())
        {
            PreparedStatement preparedStatement=con.prepareStatement("SELECT * FROM ordine WHERE id_ordine=?");
            preparedStatement.setInt(1,id);
            ResultSet resultSet=preparedStatement.executeQuery();
            if(resultSet.next()){
                ordine.setIdOrdine(resultSet.getInt("id_ordine"));
                ordine.setEmailUtente(resultSet.getString("email_utente"));
                ordine.setDataOrdine(resultSet.getDate("data"));
                ordine.setStato(resultSet.getString("stato"));
                ordine.setTotale(resultSet.getFloat("totale"));
                ordine.setDescrizione(resultSet.getString("descrizione"));
            }else {
                return null;
            }

        }
        catch (SQLException sqlException)
        {
            throw new RuntimeException(sqlException);
        }

        return ordine;
    }


    public List<Ordine> doRetrieveByEmail(String email){
        List<Ordine> ordini = new ArrayList<>();
        try (Connection connection = ConPool.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * from ordine where email_utente = ?");
            preparedStatement.setString(1, email);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                Ordine ordine = new Ordine();
                ordine.setIdOrdine(resultSet.getInt("id_ordine"));
                ordine.setEmailUtente(resultSet.getString("email_utente"));
                ordine.setDataOrdine(resultSet.getDate("data"));
                ordine.setStato(resultSet.getString("stato"));
                ordine.setTotale(resultSet.getFloat("totale"));
                ordine.setDescrizione(resultSet.getString("descrizione"));
                ordini.add(ordine);
            }



        }catch (SQLException e){
            throw new RuntimeException(e);
        }

        return ordini;
    }
    public int getLastInsertedId(){
        int id = 0;
        try (Connection connection = ConPool.getConnection()){
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT LAST_INSERT_ID()");
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()){
                id = resultSet.getInt(1);
            }
        }catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return id;
    }

    public void doSave(Ordine ordine) {
        StringBuilder query = new StringBuilder("INSERT INTO ordine (id_ordine");
        List<Object> parameters = new ArrayList<>();

        parameters.add(ordine.getIdOrdine());

        if (ordine.getEmailUtente() != null) {
            query.append(", email_utente");
            parameters.add(ordine.getEmailUtente());
        }

        if (ordine.getDataOrdine() != null) {
            query.append(", data");
            Date utilDate = ordine.getDataOrdine();
            java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
            parameters.add(sqlDate);
        }

        if (ordine.getStato() != null && !ordine.getStato().isBlank()) {
            query.append(", stato");
            parameters.add(ordine.getStato());
        }

        if (ordine.getTotale() > 0) {
            query.append(", totale");
            parameters.add(ordine.getTotale());
        }

        if (ordine.getDescrizione() != null) {
            query.append(", descrizione");
            parameters.add(ordine.getDescrizione());
        }

        query.append(") VALUES (?");
        for (int i = 1; i < parameters.size(); i++) {
            query.append(", ?");
        }
        query.append(")");

        try (Connection con = ConPool.getConnection();
             PreparedStatement ps = con.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 0; i < parameters.size(); i++) {
                ps.setObject(i + 1, parameters.get(i));
            }

            if (ps.executeUpdate() != 1) {
                throw new RuntimeException("INSERT error.");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public List<Ordine> doRetrieveAll(){

        ArrayList<Ordine> ordini = new ArrayList<>();

        Statement st;

        ResultSet rs;

        Ordine o;

        try (Connection con = ConPool.getConnection()) {

            st = con.createStatement();

            rs = st.executeQuery("SELECT * FROM ordine");

            while(rs.next()) {

                o = new Ordine();
                o.setIdOrdine(rs.getInt("id_ordine"));
                o.setEmailUtente(rs.getString("email_utente"));
                o.setDataOrdine(rs.getDate("data"));
                o.setStato(rs.getString("stato"));
                o.setTotale(rs.getFloat("totale"));
                o.setDescrizione(rs.getString("descrizione"));
                ordini.add(o);
            }

            con.close();

            return ordini;
        }

        catch (SQLException e) {

            throw new RuntimeException(e);
        }
    }

    public void doUpdateOrder(Ordine o, int idOrdine){

        try (Connection con = ConPool.getConnection()) {
            PreparedStatement preparedStatement = con.prepareStatement("update ordine set id_ordine = ?, email_utente = ?, stato = ?, data = ?, totale = ?, descrizione = ? where id_ordine = ?");
            preparedStatement.setInt(1, o.getIdOrdine());
            preparedStatement.setString(2, o.getEmailUtente());
            preparedStatement.setString(3, o.getStato());
            preparedStatement.setDate(4, new java.sql.Date(o.getDataOrdine().getTime()));
            preparedStatement.setFloat(5, o.getTotale());
            preparedStatement.setString(6, o.getDescrizione());
            preparedStatement.setInt(7, idOrdine);

            int rows = preparedStatement.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void doDeleteOrder(int idOrdine){
        try (Connection connection = ConPool.getConnection()){
            PreparedStatement preparedStatement = connection.prepareStatement("delete from ordine where id_ordine = ?");
            preparedStatement.setInt(1, idOrdine);

            int rows = preparedStatement.executeUpdate();

        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }
}
