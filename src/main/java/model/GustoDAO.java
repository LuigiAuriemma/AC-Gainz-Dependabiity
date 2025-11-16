package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GustoDAO {


    public Gusto doRetrieveById(int id){
        Gusto gusto = new Gusto();
        try (Connection connection = ConPool.getConnection()){
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * from gusto where id_gusto = ?");
            preparedStatement.setInt(1, id );
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()){
                gusto.setIdGusto(resultSet.getInt("id_gusto"));
                gusto.setNome(resultSet.getString("nomeGusto"));
            }

        }catch (SQLException e){
            throw new RuntimeException(e);
        }


        return gusto;
    }

    public Gusto doRetrieveByIdVariante(int id) {
        Gusto gusto = null;
        try (Connection connection = ConPool.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT gusto.id_gusto, gusto.nomeGusto FROM gusto JOIN variante ON gusto.id_gusto = variante.id_gusto WHERE variante.id_variante = ?");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                gusto = new Gusto();
                gusto.setIdGusto(resultSet.getInt("id_gusto"));
                gusto.setNome(resultSet.getString("nome"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return gusto;
    }

    public List<Gusto> doRetrieveAll() {
        List<Gusto> gusti = new ArrayList<>();
        try (Connection connection = ConPool.getConnection()){
            PreparedStatement preparedStatement = connection.prepareStatement("select * from gusto");
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()){
                Gusto gusto = new Gusto();
                gusto.setIdGusto(resultSet.getInt("id_gusto"));
                gusto.setNome(resultSet.getString("nomeGusto"));
                gusti.add(gusto);
            }

        }catch (SQLException e){
            throw new RuntimeException(e);
        }



        return gusti;
    }

    public void updateGusto(Gusto g, int idGusto){
        try (Connection connection = ConPool.getConnection()){
            PreparedStatement preparedStatement = connection.prepareStatement("update gusto set id_gusto = ?, nomeGusto = ? where id_gusto = ?");
            preparedStatement.setInt(1, g.getIdGusto());
            preparedStatement.setString(2, g.getNomeGusto());
            preparedStatement.setInt(3, idGusto);


            int rows = preparedStatement.executeUpdate();
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public void doSaveGusto(Gusto g) {
        StringBuilder stringBuilder = new StringBuilder("INSERT INTO gusto (");
        List<Object> parameters = new ArrayList<>();

        boolean first = true;

        if (g.getIdGusto() > 0) {
            stringBuilder.append("id_gusto");
            parameters.add(g.getIdGusto());
            first = false;
        }

        if (g.getNomeGusto() != null) {
            if (!first) {
                stringBuilder.append(", ");
            }
            stringBuilder.append("nomeGusto");
            parameters.add(g.getNomeGusto());
            first = false;
        }

        stringBuilder.append(") VALUES (");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append("?");
        }
        stringBuilder.append(")");

        String sql = stringBuilder.toString();

        try (Connection conn = ConPool.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {
             for (int i = 0; i < parameters.size(); i++) {
                 ps.setObject(i + 1, parameters.get(i));
             }
             ps.executeUpdate();
         } catch (SQLException e) {
             e.printStackTrace();
        }

        System.out.println(sql); // Per debug
        System.out.println(parameters); // Per debug
    }


    public void doRemoveGusto(int idGusto){
        try (Connection connection = ConPool.getConnection()){
            PreparedStatement preparedStatement = connection.prepareStatement("delete from gusto where id_gusto = ?");
            preparedStatement.setInt(1, idGusto);

            int rows = preparedStatement.executeUpdate();


        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }







}
