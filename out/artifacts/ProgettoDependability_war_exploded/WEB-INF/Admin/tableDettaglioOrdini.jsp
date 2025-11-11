<%@ page import="model.DettaglioOrdine" %>
<%@ page import="java.util.List" %><%--
  Created by IntelliJ IDEA.
  User: raffa
  Date: 13/07/2024
  Time: 13:42
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Dettaglio Ordini</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
    <link rel="stylesheet" href="CSS/Tables.css">
</head>
<body>
<script src="JS/Tables.js"></script>
<a class="backbutton" href="admin">Torna indietro</a>
<%   @SuppressWarnings("unchecked")
     List<DettaglioOrdine> dettaglioOrdini = (List<DettaglioOrdine>) request.getAttribute("tableDettaglioOrdini");
    if (dettaglioOrdini != null){
%>

<div class="nav-list-tables">
    <a href="showTable?tableName=ordine">Ordine</a>
    <a href="showTable?tableName=variante">Variante</a>
    <a href="showTable?tableName=prodotto">Prodotto</a>
    <a href="showTable?tableName=gusto">Gusto</a>
    <a href="showTable?tableName=confezione">Confezione</a>
    <a href="showTable?tableName=utente">Utente</a>
</div>
<div class="tableContainer">
    <table class="tableDB">
        <tr>
            <th><a href="showTable?tableName=ordine">Id Ordine</a></th>
            <th><a href="showTable?tableName=prodotto">Id Prodotto</a></th>
            <th><a href="showTable?tableName=variante">Id Variante</a></th>
            <th>Quantità</th>
            <th>Prezzo</th>
            <th>Azione</th>
        </tr>
        <% for (DettaglioOrdine d : dettaglioOrdini) {
        %>
        <tr>
            <td><%= d.getIdOrdine() %></td>
            <td><%= d.getIdProdotto() %></td>
            <td><%= d.getIdVariante() %></td>
            <td><%= d.getQuantita() %></td>
            <td><%= d.getPrezzo() %></td>
            <td class="center">
                <button class="button" onclick="editTableRow('dettaglioOrdine', '<%= d.getIdOrdine() %>, <%=d.getIdProdotto()%>, <%=d.getIdVariante()%>')">Modifica</button>
                <button class="button" onclick="deleteTableRow('dettaglioOrdine', '<%= d.getIdOrdine() %>, <%=d.getIdProdotto()%>, <%=d.getIdVariante()%>')">Elimina</button>
            </td>
        </tr>
        <% } %>
        <tr>
            <td colspan="6" class="center">
                <button class="add-button" onclick="addRow('dettaglioOrdine')">+</button>
            </td>
        </tr>
    </table>
</div>
<% } %>

</body>
</html>
