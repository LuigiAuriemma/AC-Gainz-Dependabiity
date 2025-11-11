package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class CarrelloDAO {


    public void doSave(Carrello c){
        try (Connection con = ConPool.getConnection()){
            String query = "INSERT INTO carrello (email_utente, id_prodotto, id_variante, quantità, prezzo) " +
                    "VALUES (?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = con.prepareStatement(query,Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, c.getEmailUtente());
            preparedStatement.setString(2, c.getIdProdotto());
            preparedStatement.setInt(3, c.getIdVariante());
            preparedStatement.setInt(4, c.getQuantita());
            preparedStatement.setFloat(5, c.getPrezzo());


            preparedStatement.executeUpdate();
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public void doRemoveCartByUser(String emailUtente){
        try (Connection connection = ConPool.getConnection()){

            PreparedStatement preparedStatement = connection.prepareStatement("Delete from carrello where email_utente = ?");
            preparedStatement.setString(1, emailUtente);

            int rowsDeleted = preparedStatement.executeUpdate();
            if (rowsDeleted > 0)
                System.out.println("Eliminate correttamente: " + rowsDeleted + "righe dal carrello di: " + emailUtente);
            else System.out.println("Nessuna riga eliminata");

        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }


    public List<Carrello> doRetrieveCartItemsByUser(String emailUtente) {
        List<Carrello> carrelli = new ArrayList<>();

        try (Connection connection = ConPool.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM carrello join variante v " +
                    "on carrello.id_variante = v.id_variante join gusto g on v.id_gusto = g.id_gusto join confezione c on v.id_confezione = c.id_confezione join prodotto p on carrello.id_prodotto = p.id_prodotto " +
                    "WHERE email_utente = ?");
            preparedStatement.setString(1, emailUtente);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Carrello carrello = new Carrello();
                carrello.setEmailUtente(resultSet.getString("email_utente"));
                carrello.setIdProdotto(resultSet.getString("id_prodotto"));
                carrello.setIdVariante(resultSet.getInt("id_variante"));
                carrello.setQuantita(resultSet.getInt("quantità"));
                carrello.setPrezzo(resultSet.getFloat("prezzo"));

                carrello.setGusto(resultSet.getString("nomeGusto"));
                carrello.setPesoConfezione(resultSet.getInt("peso"));
                carrello.setNomeProdotto(resultSet.getString("nome"));
                carrello.setImmagineProdotto(resultSet.getString("immagine"));

                carrelli.add(carrello);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return carrelli;
    }


}