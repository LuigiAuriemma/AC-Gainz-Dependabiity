package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ProdottoDAO {

    public Prodotto doRetrieveById(String id) {
        try (Connection con = ConPool.getConnection()) {
            PreparedStatement preparedStatement = con
                    .prepareStatement("SELECT * FROM prodotto WHERE prodotto.id_prodotto = ?");
            preparedStatement.setString(1, id);

            ResultSet resultSet = preparedStatement.executeQuery();
            List<Variante> varianti = new ArrayList<>();
            if (resultSet.next()) {
                Prodotto p = new Prodotto();

                p.setIdProdotto(resultSet.getString("id_prodotto"));
                p.setNome(resultSet.getString("nome"));
                p.setDescrizione(resultSet.getString("descrizione"));
                p.setCategoria(resultSet.getString("categoria"));
                p.setImmagine(resultSet.getString("immagine"));
                p.setCalorie(resultSet.getInt("calorie"));
                p.setCarboidrati(resultSet.getInt("carboidrati"));
                p.setProteine(resultSet.getInt("proteine"));
                p.setGrassi(resultSet.getInt("grassi"));

                VarianteDAO varianteDAO = new VarianteDAO();
                varianti = varianteDAO.doRetrieveVariantiByIdProdotto(p.getIdProdotto());

                p.setVarianti(varianti);

                return p;
            }
            return null;

        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public List<Prodotto> filterProducts(String category, String sortingFilter, String weightFilter, String tasteFilter,
            String nameFilter) throws SQLException {
        List<Prodotto> filteredProducts = new ArrayList<>();
        VarianteDAO varianteDAO = new VarianteDAO();
        boolean filterOnEvidence;

        System.out.println("nameFilterDAO: " + nameFilter);

        try (Connection connection = ConPool.getConnection()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT p.* FROM prodotto p ");

            // Gestione delle condizioni di filtraggio
            List<String> conditions = new ArrayList<>();
            List<Object> params = new ArrayList<>();

            if (category != null && !category.equals("tutto") && !category.isBlank()) {
                conditions.add("p.categoria = ?");
                params.add(category);
            }

            if (nameFilter != null && !nameFilter.isBlank()) {
                conditions.add("p.nome LIKE ?");
                params.add("%" + nameFilter + "%");
            }

            if (!conditions.isEmpty()) {
                sql.append("WHERE ");
                sql.append(String.join(" AND ", conditions));
            }

            System.out.println(sql.toString());

            PreparedStatement preparedStatement = connection.prepareStatement(sql.toString());

            // Imposta i parametri per il PreparedStatement
            for (int i = 0; i < params.size(); i++) {
                preparedStatement.setObject(i + 1, params.get(i));
            }

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Prodotto p = extractProductFromResultSet(resultSet);
                filteredProducts.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (sortingFilter != null) {
            filterOnEvidence = sortingFilter.equals("evidence");
        } else {
            filterOnEvidence = false;
        }

        // Fetch and set the cheapest variant for the products
        filteredProducts.removeIf(prodotto -> {
            Variante cheapestVariante = null;
            try {
                cheapestVariante = varianteDAO.doRetrieveCheapestFilteredVarianteByIdProdotto(
                        prodotto.getIdProdotto(), weightFilter, tasteFilter, filterOnEvidence);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (cheapestVariante != null) {
                List<Variante> varianti = new ArrayList<>();
                varianti.add(cheapestVariante);
                prodotto.setVarianti(varianti);
                return false; // Keep the product
            }
            return true; // Remove the product
        });

        // Handle sorting
        if (sortingFilter != null && !sortingFilter.isBlank()) {
            filteredProducts.sort(new Comparator<Prodotto>() {
                public int compare(Prodotto p1, Prodotto p2) {
                    return switch (sortingFilter) {
                        case "PriceDesc" -> Float.compare(getLowestPrice(p2), getLowestPrice(p1));
                        case "PriceAsc" -> Float.compare(getLowestPrice(p1), getLowestPrice(p2));
                        case "CaloriesDesc" -> Integer.compare(p2.getCalorie(), p1.getCalorie());
                        case "CaloriesAsc" -> Integer.compare(p1.getCalorie(), p2.getCalorie());
                        default -> 0;
                    };
                }
            });
        }

        return filteredProducts;
    }

    private float getLowestPrice(Prodotto prodotto) {
        List<Variante> varianti = prodotto.getVarianti();
        if (varianti == null || varianti.isEmpty()) {
            return Float.MAX_VALUE;
        }
        Variante variante = varianti.get(0);
        return variante.getPrezzo() * (1 - variante.getSconto() / 100.0f);
    }

    private Prodotto extractProductFromResultSet(ResultSet resultSet) throws SQLException {
        Prodotto p = new Prodotto();
        p.setIdProdotto(resultSet.getString("id_prodotto"));
        p.setNome(resultSet.getString("nome"));
        p.setCategoria(resultSet.getString("categoria"));
        p.setCalorie(resultSet.getInt("calorie"));
        p.setImmagine(resultSet.getString("immagine"));
        return p;
    }

    public void doSave(Prodotto prodotto) {
        try (Connection con = ConPool.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO prodotto (id_prodotto, nome, descrizione, categoria, immagine, calorie, carboidrati, proteine, grassi) VALUES(?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, prodotto.getIdProdotto());
            ps.setString(2, prodotto.getNome());
            ps.setString(3, prodotto.getDescrizione());
            ps.setString(4, prodotto.getCategoria());
            ps.setString(5, prodotto.getImmagine());
            ps.setInt(6, prodotto.getCalorie());
            ps.setInt(7, prodotto.getCarboidrati());
            ps.setInt(8, prodotto.getProteine());
            ps.setInt(9, prodotto.getGrassi());

            if (ps.executeUpdate() != 1) {
                throw new RuntimeException("INSERT error.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Prodotto> doRetrieveByCriteria(String attribute, String value) {
        if (Objects.equals(value, "Tutto"))
            return doRetrieveAll();

        ArrayList<Prodotto> prodotti = new ArrayList<>();

        PreparedStatement preparedStatement;
        ResultSet resultSet;

        Prodotto p;

        try (Connection connection = ConPool.getConnection()) {

            preparedStatement = connection.prepareStatement("SELECT * FROM prodotto where " + attribute + " = ?");
            preparedStatement.setString(1, value);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                p = new Prodotto();
                p.setIdProdotto(resultSet.getString("id_prodotto"));
                p.setNome(resultSet.getString("nome"));
                p.setDescrizione(resultSet.getString("descrizione"));
                p.setCategoria(resultSet.getString("categoria"));
                p.setImmagine(resultSet.getString("immagine"));
                p.setCalorie(resultSet.getInt("calorie"));
                p.setCarboidrati(resultSet.getInt("carboidrati"));
                p.setProteine(resultSet.getInt("proteine"));
                p.setGrassi(resultSet.getInt("grassi"));

                /*
                 * VarianteDAO varianteDAO = new VarianteDAO();
                 * List<Variante> varianti =
                 * varianteDAO.doRetrieveVariantiByIdProdotto(p.getIdProdotto());
                 * 
                 * p.setVarianti(varianti);
                 */
                VarianteDAO varianteDAO = new VarianteDAO();
                Variante cheapest = varianteDAO.doRetrieveCheapestVariant(p.getIdProdotto());
                List<Variante> varianti = new ArrayList<>();
                varianti.add(cheapest);

                p.setVarianti(varianti);

                prodotti.add(p);

            }
            connection.close();
            return prodotti;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Prodotto> doRetrieveAll() {

        ArrayList<Prodotto> prodotti = new ArrayList<>();
        Statement st;
        ResultSet resultSet;
        Prodotto p;

        try (Connection con = ConPool.getConnection()) {

            st = con.createStatement();

            String query = "SELECT * FROM prodotto";

            resultSet = st.executeQuery(query);

            while (resultSet.next()) {
                p = new Prodotto();
                p.setIdProdotto(resultSet.getString("id_prodotto"));
                p.setNome(resultSet.getString("nome"));
                p.setDescrizione(resultSet.getString("descrizione"));
                p.setCategoria(resultSet.getString("categoria"));
                p.setImmagine(resultSet.getString("immagine"));
                p.setCalorie(resultSet.getInt("calorie"));
                p.setCarboidrati(resultSet.getInt("carboidrati"));
                p.setProteine(resultSet.getInt("proteine"));
                p.setGrassi(resultSet.getInt("grassi"));

                VarianteDAO varianteDAO = new VarianteDAO();
                List<Variante> varianti = new ArrayList<>();
                Variante variante = varianteDAO.doRetrieveCheapestVariant(p.getIdProdotto());
                varianti.add(variante);

                p.setVarianti(varianti);

                prodotti.add(p);
            }

            return prodotti;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateProduct(Prodotto p, String idProdotto) {
        try (Connection connection = ConPool.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "update prodotto set id_prodotto = ?, nome = ?, descrizione = ?, categoria = ?, immagine = ?, calorie = ?, carboidrati = ?, proteine = ?, grassi = ? where id_prodotto = ?");
            preparedStatement.setString(1, p.getIdProdotto());
            preparedStatement.setString(2, p.getNome());
            preparedStatement.setString(3, p.getDescrizione());
            preparedStatement.setString(4, p.getCategoria());
            preparedStatement.setString(5, p.getImmagine());
            preparedStatement.setInt(6, p.getCalorie());
            preparedStatement.setInt(7, p.getCarboidrati());
            preparedStatement.setInt(8, p.getProteine());
            preparedStatement.setInt(9, p.getGrassi());
            preparedStatement.setString(10, idProdotto);

            int rowsUpdated = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeProductFromIdProdotto(String idProdotto) {
        try (Connection connection = ConPool.getConnection()) {
            PreparedStatement preparedStatement = connection
                    .prepareStatement("delete from prodotto where id_prodotto = ?");
            preparedStatement.setString(1, idProdotto);
            int rowsUpdated = preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
