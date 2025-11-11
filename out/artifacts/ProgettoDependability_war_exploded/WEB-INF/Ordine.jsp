<%@ page import="java.util.List" %>
<%@ page import="model.*" %><%--
  Created by IntelliJ IDEA.
  User: raffa
  Date: 01/07/2024
  Time: 10:39
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Ordine</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
    <style>
        .container {
            width: 70%;
            margin: 20px auto;
            padding: 10px;
            border: 1px solid lightgray;
            border-radius: 5px;
            background-color: #f5f5f5;
        }

        .container-title {
            font-size: 35px;
            font-weight: 500;
            text-align: center;
            padding: 10px 0;
            color: black;
        }

        .container-data {
            font-size: 16px;
            padding: 10px;
            color: black;
            border-top: 1px solid lightgray;
            border-bottom: 1px solid lightgray;
        }

        #container-phrases {
            font-size: 25px;
            font-weight: 350;
            margin-bottom: 10px;
        }

        .container-data-list {
            margin: 10px 0 0 20px
        }

        .order-item {
            display: flex;
            align-items: center;
            margin-bottom: 15px;
        }

        .order-item:not(:last-child){
            border-bottom: 1px solid lightgray;
            padding-bottom: 10px;
        }

        .product-image {
            width: 100px; /* Imposta la larghezza delle immagini */
            height: auto; /* Altezza automatica proporzionale */
            margin-right: 20px;
            border-radius: 3px;
        }

        .product-details {
            flex: 1; /* Occupa lo spazio rimanente */
        }

        .container-price {
            font-size: 26px;
            font-weight: 400;
            padding: 5px;
            color: black;
            text-align: right;
            border-top: 1px solid lightgray;
        }

    </style>
</head>
<body>
<%@include file="Header.jsp"%>

<div class="container">
    <div class="container-title">
        <p>Ordine confermato</p>
    </div>
    <div class="container-data">
        <p id="container-phrases">Dettagli ordine:</p>
        <div class="container-data-list">
            <%
                Ordine ordine = (Ordine) request.getAttribute("order");
                List<DettaglioOrdine> dettaglioOrdine = (List<DettaglioOrdine>) request.getAttribute("orderDetails");
                if (dettaglioOrdine != null && !dettaglioOrdine.isEmpty()){
                    for (DettaglioOrdine item: dettaglioOrdine){
            %>
            <div class="order-item">
                <img src="<%=item.getImmagineProdotto()%>" class="product-image" alt="<%=item.getNomeProdotto()%>">
                <div class="product-details">
                    <p style="font-size: 21px;font-weight: 450"><%=item.getNomeProdotto()%></p>
                    <p>Gusto: <%=item.getGusto()%></p>
                    <p>Peso: <%=item.getPesoConfezione()%>g</p>
                    <p>Quantità: <%=item.getQuantita()%></p>
                    <p>Prezzo: <%=item.getPrezzo()%>€</p>
                </div>
            </div>
            <%
                    }
                }
            %>
        </div>
    </div>
    <div class="container-price">
        Totale ordine: <%=ordine.getTotale()%>€
    </div>
</div>


<%@include file="Footer.jsp"%>
</body>
</html>
