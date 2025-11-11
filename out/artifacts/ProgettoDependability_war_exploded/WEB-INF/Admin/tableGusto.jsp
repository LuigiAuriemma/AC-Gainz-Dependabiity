<%@ page import="model.Gusto" %>
<%@ page import="java.util.List" %><%--
  Created by IntelliJ IDEA.
  User: raffa
  Date: 13/07/2024
  Time: 21:47
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Gusti</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
    <link rel="stylesheet" href="CSS/Tables.css">
</head>

<body>
<script src="JS/Tables.js"></script>
<a class="backbutton" href="admin">Torna indietro</a>
<%  @SuppressWarnings("unchecked")
    List<Gusto> gusti = (List<Gusto>) request.getAttribute("tableGusto");
    if (gusti != null){
%>

<div class="nav-list-tables">
    <a href="showTable?tableName=prodotto">Prodotto</a>
    <a href="showTable?tableName=variante">Variante</a>
    <a href="showTable?tableName=confezione">Confezione</a>
    <a href="showTable?tableName=utente">Utente</a>
    <a href="showTable?tableName=ordine">Ordine</a>
    <a href="showTable?tableName=dettaglioOrdine">Dettaglio Ordini</a>
</div>

<div class="tableContainer">
    <table class="tableDB">
        <tr>
            <th>Id Gusto</th>
            <th>Nome Gusto</th>
            <th>Azione</th>
        </tr>
        <% for (Gusto g : gusti) {
        %>
        <tr>
            <td><%= g.getIdGusto() %></td>
            <td><%= g.getNomeGusto() %></td>
            <td class="center">
                <button class="button" onclick="editTableRow('gusto', '<%=g.getIdGusto()%>')">Modifica</button>
                <button class="button" onclick="deleteTableRow('gusto', '<%=g.getIdGusto()%>')">Elimina</button>
            </td>
        </tr>
        <% } %>
        <tr>
            <td colspan="3" class="center">
                <button class="add-button" onclick="addRow('gusto')">+</button>
            </td>
        </tr>
    </table>
</div>
<% } %>

</body>
</html>
