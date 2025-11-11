<%@ page import="model.Confezione" %>
<%@ page import="java.util.List" %><%--
  Created by IntelliJ IDEA.
  User: raffa
  Date: 13/07/2024
  Time: 22:29
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Confezioni</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
<link rel="stylesheet" href="CSS/Tables.css">
</head>
<body>
<script src="JS/Tables.js"></script>
<a class="backbutton" href="admin">Torna indietro</a>
<%
    List<Confezione> confezioni = (List<Confezione>) request.getAttribute("tableConfezione");
    if (confezioni != null){
%>
<div class="nav-list-tables">
    <a href="showTable?tableName=variante">Variante</a>
    <a href="showTable?tableName=prodotto">Prodotto</a>
    <a href="showTable?tableName=gusto">Gusto</a>
    <a href="showTable?tableName=utente">Utente</a>
    <a href="showTable?tableName=ordine">Ordine</a>
    <a href="showTable?tableName=dettaglioOrdine">Dettaglio Ordini</a>
</div>
<div class="tableContainer">
    <table class="tableDB">
        <tr>
            <th>Id Confezione</th>
            <th>Peso Confezione</th>
            <th>Azione</th>
        </tr>
        <% for (Confezione c : confezioni) {
        %>
        <tr>
            <td><%= c.getIdConfezione() %></td>
            <td><%= c.getPeso() %></td>
            <td class="center">
                <button class="button" onclick="editTableRow('confezione', '<%=c.getIdConfezione()%>')">Modifica</button>
                <button class="button" onclick="deleteTableRow('confezione', '<%=c.getIdConfezione()%>')">Elimina</button>
            </td>
        </tr>
        <% } %>
        <tr>
            <td colspan="3" class="center">
                <button class="add-button" onclick="addRow('confezione')">+</button>
            </td>
        </tr>
    </table>
</div>
<% } %>

</body>
</html>
