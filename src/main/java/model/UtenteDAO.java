package model;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UtenteDAO {

    public Utente doRetrieveByEmailAndPassword(String email, String password) throws SQLException {
        Utente u = null; // Imposta l'utente a null inizialmente
        try (Connection con = ConPool.getConnection()){
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM utente WHERE email=? AND password = SHA2(?, 512)");
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, password);


            System.out.println("Email: " + email);
            System.out.println("Password: " + password);


            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()){ // Usa if invece di while perché ti aspetti al massimo un utente
                u = new Utente();
                u.setEmail(resultSet.getString("email"));
                u.setPassword(resultSet.getString("password"));
                u.setNome(resultSet.getString("nome"));
                u.setCognome(resultSet.getString("cognome"));
                u.setCodiceFiscale(resultSet.getString("codice_fiscale"));
                u.setDataNascita(resultSet.getDate("data_di_nascita"));
                u.setIndirizzo(resultSet.getString("indirizzo"));
                u.setTelefono(resultSet.getString("numero_di_cellulare"));
                u.setPoteri(resultSet.getBoolean("poteri"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return u;
    }


    public Utente doRetrieveByEmail(String email) {
        Utente u = null;
        try(Connection con= ConPool.getConnection()) {
            PreparedStatement preparedStatement=con.prepareStatement("SELECT email,password,nome,cognome,codice_fiscale,data_di_nascita,indirizzo,numero_di_cellulare,poteri FROM utente WHERE email=?");
            preparedStatement.setString(1,email);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                u = new Utente();
                u.setEmail(resultSet.getString("email"));
                u.setPassword(resultSet.getString("password"));
                u.setNome(resultSet.getString("nome"));
                u.setCognome(resultSet.getString("cognome"));
                u.setCodiceFiscale(resultSet.getString("codice_fiscale"));
                u.setDataNascita(resultSet.getDate("data_di_nascita"));
                u.setIndirizzo(resultSet.getString("indirizzo"));
                u.setTelefono(resultSet.getString("numero_di_cellulare"));
                u.setPoteri(resultSet.getBoolean("poteri"));
            }
        }
        catch (SQLException sqlException)
        {
            throw new RuntimeException(sqlException);
        }

        return u;
    }

    public void doSave(Utente utente) {
        try (Connection con = ConPool.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO utente (email,password,nome,cognome,codice_fiscale,data_di_nascita,indirizzo,numero_di_cellulare) VALUES(?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, utente.getEmail());
            ps.setString(2, utente.getPassword());
            ps.setString(3, utente.getNome());
            ps.setString(4, utente.getCognome());
            ps.setString(5, utente.getCodiceFiscale());
            Date utilDate = utente.getDataNascita();
            java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());

            ps.setDate(6, sqlDate);
            ps.setString(7, utente.getIndirizzo());
            ps.setString(8, utente.getTelefono());
            if (ps.executeUpdate() != 1) {
                throw new RuntimeException("INSERT error.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Utente> doRetrieveAll(){

        ArrayList<Utente> utenti = new ArrayList<>();

        Statement st;

        ResultSet rs;

        Utente p;

        try (Connection con = ConPool.getConnection()) {

            st = con.createStatement();

            rs = st.executeQuery("SELECT * FROM utente");



            while(rs.next()) {

                p = new Utente();


                p.setEmail(rs.getString(1));

                p.setPassword(rs.getString(2));

                p.setNome(rs.getString(3));

                p.setCognome(rs.getString(4));
                p.setCodiceFiscale(rs.getString(5));

                p.setDataNascita(rs.getDate(6));

                p.setIndirizzo(rs.getString(7));

                p.setTelefono(rs.getString(8));
                p.setPoteri(rs.getBoolean(9));

                utenti.add(p);
            }

            con.close();

            return utenti;
        }

        catch (SQLException e) {

            throw new RuntimeException(e);
        }
    }

    public void doUpdateCustomer(Utente u, String email) {
        String query = "UPDATE utente SET email=?, nome=?, cognome=?, codice_fiscale=?, data_di_nascita=?, indirizzo=?, numero_di_cellulare=?, poteri=? WHERE email=?";
        try (Connection con = ConPool.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, u.getEmail());
            ps.setString(2, u.getNome());
            ps.setString(3, u.getCognome());
            ps.setString(4, u.getCodiceFiscale());
            ps.setDate(5, new java.sql.Date(u.getDataNascita().getTime()));
            ps.setString(6, u.getIndirizzo());
            ps.setString(7, u.getTelefono());
            ps.setBoolean(8, u.getPoteri());

            ps.setString(9, email);

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



    public void doUpdateCustomerGeneric(Utente u, String attributeToUpdate, String value){

       String query = "UPDATE Utente SET " + attributeToUpdate + " = ? WHERE email = ?";


        try(Connection connection = ConPool.getConnection()){
            PreparedStatement preparedStatement = connection.prepareStatement(query);

            switch (attributeToUpdate){
                case "dataDiNascita" ->{
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date d = simpleDateFormat.parse(value);
                    preparedStatement.setDate(1, new java.sql.Date(d.getTime()));
                }
                case "poteri" ->{
                    if(value.equals("true")){
                        boolean b = true;
                        preparedStatement.setBoolean(1, b);
                    }else {
                        boolean x = false;
                        preparedStatement.setBoolean(1, x);
                    }

                }
                default -> preparedStatement.setString(1, value);
            }


            preparedStatement.setString(2, u.getEmail());

            preparedStatement.executeUpdate();

        }catch (SQLException | ParseException e){
            throw new RuntimeException(e);
        }
    }


    public void doRemoveUserByEmail(String email){
        try (Connection connection = ConPool.getConnection()){
            PreparedStatement preparedStatement = connection.prepareStatement("delete from utente where email = ?");
            preparedStatement.setString(1, email);

            int rowsDeleted = preparedStatement.executeUpdate();

            if (rowsDeleted <= 0){
                System.out.println("No user was deleted from db");
            }else {
                System.out.println("User with email:" + email + " was deleted from db");
            }

        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }
}
