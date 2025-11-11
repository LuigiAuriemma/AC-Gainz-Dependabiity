<%@ page import="model.Variante" %>
<%@ page import="java.util.List" %><%--
  Created by IntelliJ IDEA.
  User: raffa
  Date: 11/07/2024
  Time: 16:41
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Varianti</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
    <link rel="stylesheet" href="CSS/Tables.css">
</head>
<body>
<script src="JS/Tables.js"></script>
<a class="backbutton" href="admin">Torna indietro</a>
    <%
     List<Variante> varianti = (List<Variante>) request.getAttribute("tableVariante");
    if (varianti != null){
%>


<div class="nav-list-tables">
    <a href="showTable?tableName=prodotto">Prodotto</a>
    <a href="showTable?tableName=gusto">Gusto</a>
    <a href="showTable?tableName=confezione">Confezione</a>
    <a href="showTable?tableName=utente">Utente</a>
    <a href="showTable?tableName=ordine">Ordine</a>
    <a href="showTable?tableName=dettaglioOrdine">Dettaglio Ordini</a>
</div>
<div class="tableContainer">
<table class="tableDB">
    <tr>
        <th>Id Variante</th>
        <th><a href="showTable?tableName=prodotto">Id Prodotto Variante</a></th>
        <th><a href="showTable?tableName=gusto">Id Gusto</a></th>
        <th><a href="showTable?tableName=confezione">Id Confezione</a></th>
        <th>Prezzo</th>
        <th>Quantità</th>
        <th>Sconto</th>
        <th>Evidenza</th>
        <th>Azione</th>
    </tr>
    <% for (Variante v: varianti) {
        int evidence = 0;
        if (v.isEvidenza()) evidence = 1;
    %>
    <tr>
        <td><%= v.getIdVariante() %></td>
        <td><%= v.getIdProdotto() %></td>
        <td><%= v.getIdGusto() %></td>
        <td><%= v.getIdConfezione() %></td>
        <td><%= v.getPrezzo() %></td>
        <td><%= v.getQuantita() %></td>
        <td><%= v.getSconto() %></td>
        <td><%= evidence %></td>
        <td class="center">
            <button class="button" onclick="editTableRow('variante', '<%=v.getIdVariante()%>')">Modifica</button>
            <button class="button" onclick="deleteTableRow('variante', '<%=v.getIdVariante()%>')">Elimina</button>
        </td>

    </tr>
    <% } %>
    <tr>
        <td colspan="9" class="center">
            <button class="add-button" onclick="addRow('variante')">+</button>
        </td>
    </tr>
</table>
    <% } %>
</div>
