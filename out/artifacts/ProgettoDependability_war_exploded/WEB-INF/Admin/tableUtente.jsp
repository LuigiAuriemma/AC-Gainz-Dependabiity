<%@ page import="model.Utente" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %><%--
  Created by IntelliJ IDEA.
  User: raffa
  Date: 10/07/2024
  Time: 17:26
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
  <title>Utenti</title>
  <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
  <link rel="stylesheet" href="CSS/Tables.css">
</head>
<body>
<script src="JS/Tables.js"></script>
<a class="backbutton" href="admin">Torna indietro</a>
<%
  List<Utente> utenti = (List<Utente>) request.getAttribute("tableUtente");
  if (utenti != null){
%>

<div class="nav-list-tables">
  <a href="showTable?tableName=prodotto">Prodotto</a>
  <a href="showTable?tableName=gusto">Gusto</a>
  <a href="showTable?tableName=confezione">Confezione</a>
  <a href="showTable?tableName=variante">Variante</a>
  <a href="showTable?tableName=ordine">Ordine</a>
  <a href="showTable?tableName=dettaglioOrdine">Dettaglio Ordini</a>
</div>

<div class="tableContainer">
<table class="tableDB">
  <tr>
    <th>Email</th>
    <th>Nome</th>
    <th>Cognome</th>
    <th>Codice Fiscale</th>
    <th>Data di Nascita</th>
    <th>Indirizzo</th>
    <th>Numero di cellulare</th>
    <th>Azione</th>
  </tr>
  <% for (Utente u : utenti) { %>
  <tr>
    <td><%= u.getEmail() %></td>
    <td><%= u.getNome() %></td>
    <td><%= u.getCognome() %></td>
    <td><%= u.getCodiceFiscale() %></td>
    <td><%= u.getDataNascita() %></td>
    <td><%= u.getIndirizzo() %></td>
    <td><%= u.getTelefono() %></td>
    <td class="center">
      <button class="button" onclick="editTableRow('utente', '<%=u.getEmail()%>')">Modifica</button>
      <button class="button" onclick="deleteTableRow('utente', '<%=u.getEmail()%>')">Elimina</button>
    </td>

  </tr>
  <% } %>
  <tr>
    <td colspan="8" class="center">
      <button class="add-button" onclick="addRow('utente')">+</button>
    </td>
  </tr>
</table>
<% } %>
</div>


</body>
</html>
