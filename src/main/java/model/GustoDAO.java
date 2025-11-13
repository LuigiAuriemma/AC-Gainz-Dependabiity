package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
//DA CHECKARE
public class GustoDAO {

    /*@
    @   public normal_behavior
    @       requires id >= 0;
    @       assignable \nothing;
    @       // Il tuo codice ritorna un new Gusto() (con id=0) se non trova nulla.
    @       ensures \result != null;
    @       ensures \result.getIdGusto() >= 0;
    @   public exceptional_behavior
    @       requires id >= 0;
    @       assignable \nothing;
    @       // Diciamo a JML che una RuntimeException è un possibile risultato
    @       signals (RuntimeException e) true;
    @*/
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

    /*@
    @   public normal_behavior
    @       requires id >= 0;
    @       assignable \nothing;
    @       // Questo codice ritorna null se non trova, il che è un buon pattern
    @       ensures \result == null || \result.getIdGusto() >= 0;
    @   public exceptional_behavior
    @       requires id >= 0;
    @       assignable \nothing;
    @       signals (RuntimeException e) true;
    @*/
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

    /*@
    @   public normal_behavior
    @       assignable \nothing;
    @       // Il metodo garantisce che la lista restituita non è mai nulla
    @       ensures \result != null;
    @       // Garantisce anche che ogni elemento nella lista è un oggetto Gusto valido
    @       ensures (\forall int i; 0 <= i && i < \result.size();
    @                   \result.get(i) != null && \result.get(i).getIdGusto() >= 0);
    @   public exceptional_behavior
    @       assignable \nothing;
    @       signals (RuntimeException e) true;
    @*/
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


    /*@
    @   public normal_behavior
    @       requires g != null;
    @       requires idGusto >= 0;
    @       // Per un update, è ragionevole richiedere che il nome non sia nullo
    @       requires g.getNomeGusto() != null;
    @       assignable \nothing;
    @   public exceptional_behavior
    @       requires g != null;
    @       requires idGusto >= 0;
    @       assignable \nothing;
    @       signals (RuntimeException e) true;
    @*/
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

    /*@
    @   // Questo metodo ha un comportamento eccezionale diverso!
    @   // Cattura l'eccezione ma NON la rilancia (e.printStackTrace() non conta per JML)
    @   // Di conseguenza, dal punto di vista JML, questo metodo non fallisce mai.
    @   public normal_behavior
    @       requires g != null;
    @       assignable \nothing;
    @*/
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

    /*@
    @   public normal_behavior
    @       requires idGusto >= 0;
    @       assignable \nothing;
    @   public exceptional_behavior
    @       requires idGusto >= 0;
    @       assignable \nothing;
    @       signals (RuntimeException e) true;
    @*/
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
