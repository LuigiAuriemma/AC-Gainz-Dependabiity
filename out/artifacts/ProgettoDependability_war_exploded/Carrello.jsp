<%@ page import="java.util.List" %>
<%@ page import="model.Carrello" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Carrello</title>
    <link rel="stylesheet" href="CSS/Cart.css">
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
</head>
<body>
<%@ include file="WEB-INF/Header.jsp" %>

<script src="JS/Carrello.js"></script>

<div id="checkOutContainer">
    <div id="checkOutItem">
        <%

            List<Carrello> cartItems = (List<Carrello>) session.getAttribute("cart");

            float total = 0;
            if (cartItems != null){
               for(Carrello cartItem: cartItems){
                    total += cartItem.getPrezzo();
        %>
        <div class="product">
            <img src="<%= cartItem.getImmagineProdotto() %>" alt="<%= cartItem.getNomeProdotto() %>">
            <div class="product-info">
                <h3><%= cartItem.getNomeProdotto() %></h3>
                <p><%= cartItem.getGusto() %></p>
                <p><%= cartItem.getPesoConfezione() %> grammi</p>
                <p><%= Math.round(cartItem.getPrezzo() * 100.0f) / 100.0f %></p>

            </div>
            <div class="quantity-div">
                <input class="inputQuantity" type="number" value="<%= cartItem.getQuantita() %>">
                <button data-product-id = '<%=cartItem.getIdProdotto()%>' data-product-taste = '<%=cartItem.getGusto()%>' data-product-weight = '<%=cartItem.getPesoConfezione()%>'
                         class="modifyQuantity">Modifica</button>
            </div>
            <div class="rmv-div">
                <button class="rmvButton" data-product-id = '<%=cartItem.getIdProdotto()%>' data-product-taste = '<%=cartItem.getGusto()%>' data-product-weight = '<%=cartItem.getPesoConfezione()%>'
                        onclick="removeItem()">Rimuovi Elemento</button>
            </div>
        </div>
            <%
                    }
                }
            %>
    </div>


    <div class="summary">
        <h2>Somma Totale</h2>
        <p id="subtotal">Subtotale: <span> <%=total%> </span></p>
        <p id="totalOrder">Totale ordine: <span> <%=total%> </span></p>
        <div class="buy">
            <button id="buyCart">Procedi all'acquisto</button>
        </div>
    </div>
</div>

<%@ include file="WEB-INF/Footer.jsp" %>
</body>
</html>
