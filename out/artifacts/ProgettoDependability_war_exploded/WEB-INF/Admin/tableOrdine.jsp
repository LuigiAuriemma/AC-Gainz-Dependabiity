<%@ page import="model.Ordine" %>
<%@ page import="java.util.List" %><%--
  Created by IntelliJ IDEA.
  User: raffa
  Date: 11/07/2024
  Time: 16:48
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Ordini</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
    <link rel="stylesheet" href="CSS/Tables.css">
</head>
<body>
<script src="JS/Tables.js"></script>
<a class="backbutton" href="admin">Torna indietro</a>
<%
    List<Ordine> ordini = (List<Ordine>) request.getAttribute("tableOrdine");

%>



<div class="nav-list-tables">
    <a href="showTable?tableName=dettaglioOrdine">Dettaglio Ordini</a>
    <a href="showTable?tableName=prodotto">Prodotto</a>
    <a href="showTable?tableName=variante">Variante</a>
    <a href="showTable?tableName=gusto">Gusto</a>
    <a href="showTable?tableName=confezione">Confezione</a>
    <a href="showTable?tableName=utente">Utente</a>
</div>

<div class="tableContainer">
    <table class="tableDB">
        <tr>
            <th>Id Ordine</th>
            <th><a href="showTable?tableName=utente">Email Utente</a></th>
            <th>Data Ordine</th>
            <th>Stato</th>
            <th>Totale</th>
            <th>Descrizione</th>
            <th>Azione</th>
        </tr>
        <%
            for (Ordine o : ordini) {
                String descrizione = o.getDescrizione();
        %>
        <tr>
            <td><%=o.getIdOrdine()%></td>
            <td><%=o.getEmailUtente()%></td>
            <td><%=o.getDataOrdine()%></td>
            <td><%=o.getStato()%></td>
            <td><%=o.getTotale()%></td>
            <td class="description-scroll">
                <%
                    if (descrizione == null || descrizione.isBlank()) {
                %>
                <a href="showTable?tableName=dettaglioOrdine">Dettaglio ordine</a>
                <%
                } else {
                %>
                 <%=o.getDescrizione()%>
                <%
                    }
                %>
            </td>
            <td class="center">
                <button class="button" onclick="editTableRow('ordine', '<%=o.getIdOrdine()%>')">Modifica</button>
                <button class="button" onclick="deleteTableRow('ordine', '<%=o.getIdOrdine()%>')">Elimina</button>
            </td>
        </tr>
        <%
            }
        %>
        <tr>
            <td colspan="7" class="center">
                <button class="add-button" onclick="addRow('ordine')">+</button>
            </td>
        </tr>
    </table>
</div>

</body>
</html>
