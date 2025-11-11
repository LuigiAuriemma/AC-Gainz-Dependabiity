<%@ page import="model.Prodotto" %>
<%@ page import="java.util.List" %><%--
  Created by IntelliJ IDEA.
  User: raffa
  Date: 11/07/2024
  Time: 16:32
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Prodotti</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
    <link rel="stylesheet" href="CSS/Tables.css">
</head>
<body>
<script src="JS/Tables.js"></script>
<a class="backbutton" href="admin">Torna indietro</a>
<%  @SuppressWarnings("unchecked")
    List<Prodotto> prodotti = (List<Prodotto>) request.getAttribute("tableProdotto");
    if (prodotti != null){
%>

<div class="nav-list-tables">
    <a href="showTable?tableName=variante">Variante</a>
    <a href="showTable?tableName=gusto">Gusto</a>
    <a href="showTable?tableName=confezione">Confezione</a>
    <a href="showTable?tableName=utente">Utente</a>
    <a href="showTable?tableName=ordine">Ordine</a>
    <a href="showTable?tableName=dettaglioOrdine">Dettaglio Ordini</a>
</div>
<div class="tableContainer">
    <table class="tableDB">
        <tr>
            <th>Id Prodotto</th>
            <th>Nome</th>
            <th>Categoria</th>
            <th>Immagine</th>
            <th>Calorie</th>
            <th>Carboidrati</th>
            <th>Proteine</th>
            <th>Grassi</th>
            <th>Azione</th>
        </tr>
        <% for (Prodotto p : prodotti) { %>
        <tr>
            <td><%= p.getIdProdotto() %></td>
            <td><%= p.getNome() %></td>
            <td><%= p.getCategoria() %></td>
            <td><%= p.getImmagine() %></td>
            <td><%= p.getCalorie() %></td>
            <td><%= p.getCarboidrati() %></td>
            <td><%= p.getProteine() %></td>
            <td><%= p.getGrassi() %></td>
            <td class="center">
                <button class="button" onclick="editTableRow('prodotto', '<%=p.getIdProdotto()%>')">Modifica</button>
                <button class="button" onclick="deleteTableRow('prodotto', '<%=p.getIdProdotto()%>')">Elimina</button>
            </td>
        </tr>
        <% } %>
        <tr>
            <td colspan="10" class="center">
                <button class="add-button" onclick="addRow('prodotto')">+</button>
            </td>
        </tr>
    </table>
</div>
<% } %>
</body>
</html>
