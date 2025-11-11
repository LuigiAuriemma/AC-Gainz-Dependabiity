<%--
  Created by IntelliJ IDEA.
  User: raffa
  Date: 16/06/2024
  Time: 17:11
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Area Admin</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
    <style>
        #listTables{
            display: flex;
            align-items: center;
            justify-content: center;
            flex-direction: column;
            row-gap: 15px;
        }
        #listTables button{
            padding: 20px 0;
            width: 270px;
            border: none;
            border-radius: 15px;
            font-size: 25px;
            font-weight: 450;
            background-color: orangered;
            color: white;
            transition: 0.3s;

        }
        #listTables button:hover{
            color: black;
            cursor: pointer;
        }

    </style>
</head>
<body>
<%@include file="../Header.jsp"%>

<div id="listTables">
    <form action="showTable" method="get">
        <input type="hidden" name="tableName" value="utente">
        <button type="submit" id="utenteTableButton">Utenti</button>
    </form>

    <form action="showTable" method="get">
        <input type="hidden" name="tableName" value="prodotto">
        <button type="submit" id="prodottoTableButton">Prodotti</button>
    </form>

    <form action="showTable" method="get">
        <input type="hidden" name="tableName" value="variante">
        <button type="submit" id="variantiTableButton">Varianti</button>
    </form>

    <form action="showTable" method="get">
        <input type="hidden" name="tableName" value="ordine">
        <button type="submit" id="ordineTableButton">Ordini</button>
    </form>

    <form action="showTable" method="get">
        <input type="hidden" name="tableName" value="dettaglioOrdine">
        <button type="submit" id="dettaglioOrdineTableButton">Dettaglio Ordine</button>
    </form>

    <form action="showTable" method="get">
        <input type="hidden" name="tableName" value="gusto">
        <button type="submit" id="gustoTableButton">Gusto</button>
    </form>

    <form action="showTable" method="get">
        <input type="hidden" name="tableName" value="confezione">
        <button type="submit" id="confezioneTableButton">Confezione</button>
    </form>
</div>



<%@include file="../Footer.jsp"%>
</body>
</html>
