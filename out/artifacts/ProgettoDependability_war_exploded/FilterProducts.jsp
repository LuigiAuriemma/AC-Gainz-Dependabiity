<%@ page import="java.util.List" %>
<%@ page import="model.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Prodotti</title>
    <link rel="stylesheet" href="CSS/ProductCard.css">
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
    <style>
        .pageContainer {
            display: flex;
            justify-content: center;
            align-items: center;
        }
        .filtersContainer {
            padding: 20px;
            width: 80%;
            display: flex;
            justify-content: space-between;
            flex-wrap: wrap;
            row-gap: 20px;
            background: #f6f6f6;
            border-radius: 10px;
            margin: 10px auto 50px;
        }
        .filter {
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .filter label {
            margin-bottom: 5px;
        }
        .filter select, #reset-button {
            appearance: none;
            min-width: 150px;
            padding: 12px 15px;
            border: 1px solid #d6d6d6;
            border-radius: 15px;
            transition: 0.3s;
        }
        #reset-button:hover {
            background-color: orangered;
            color: black;
        }
        .content-group {
            width: 100%;
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            justify-content: center;
        }
    </style>
</head>
<body>
<%@ include file="WEB-INF/Header.jsp" %>

<div class="pageContainer">
    <div class="filtersContainer" id="filtersContainer">
        <div class="filter">
            <select id="sorting" name="sort" onchange="genericFilter()">
                <option value="">Sort by:</option>
                <option value="PriceDesc">Prezzo: da alto a basso</option>
                <option value="PriceAsc">Prezzo: da basso ad alto</option>
                <option value="CaloriesDesc">Calorie: da alto a basso</option>
                <option value="CaloriesAsc">Calorie: da basso ad alto</option>
                <option value="evidence">In evidenza</option>
            </select>
        </div>

        <div class="filter">
            <select id="weights" name="weights" onchange="genericFilter()">
                <option value="">Seleziona un peso</option>
                <option value="100">100 grammi</option>
                <option value="250">250 grammi</option>
                <option value="500">500 grammi</option>
                <option value="1000">1 kg</option>
                <option value="1500">1,5 kg</option>
                <option value="2000">2 kg</option>
            </select>
        </div>


        <div class="filter">
            <select id="tastes" name="taste" onclick="showTastes()" onchange="genericFilter()">
                <option value="">Seleziona un gusto</option>
            </select>
        </div>


        <button id="reset-button" onclick="resetProducts()">Reset</button>
    </div>
</div>

<script defer src="JS/genericFilter.js"></script>
<script defer src="JS/showTastes.js"></script>
<script src="JS/productOptions.js"></script>



<div id="gr" class="content-group">
    <%
        List<Prodotto> products = null;
        if (request.getAttribute("originalProducts") != null) {
            products = (List<Prodotto>) request.getAttribute("originalProducts");
        } else {
            products = (List<Prodotto>) application.getAttribute("Products");
        }

        if (products != null) {
            for (Prodotto p : products) {
                Variante variante = p.getVarianti().get(0);
    %>
    <div class="product-card">
        <div class="product-image" tabindex="0" onkeydown="">
            <% if (variante.getSconto() > 0) { %>
            <span class="product-sconto"><%= variante.getSconto() %>% di Sconto</span>
            <% } %>
            <form id="<%=p.getIdProdotto()%>" action="ProductInfo" method="post">
                <input type="hidden" name="primaryKey" value="<%=p.getIdProdotto()%>">
            </form>
            <img src="<%= p.getImmagine() %>" alt="<%= p.getNome() %>" tabindex="0" onkeydown="" onclick="document.getElementById('<%=p.getIdProdotto()%>').submit();">
        </div>
            <div class="product-info">
            <h2 class="product-info-name"><%= p.getNome() %></h2>
            <% if (variante.getSconto() > 0) {
                float prezzoscontato = (variante.getPrezzo() - ((variante.getPrezzo() * variante.getSconto()) / 100));
                prezzoscontato = Math.round(prezzoscontato * 100.0f) / 100.0f;
            %>
            <span class="product-info-flavour"><%= variante.getGusto() %></span>
            <span class="product-info-price-off"><%= prezzoscontato %> €</span>
            <span class="product-info-price"><%= variante.getPrezzo() %> €</span>
            <% } else { %>
            <span class="product-info-flavour"><%=variante.getGusto()%></span>
            <span class="product-info-price-off"><%= variante.getPrezzo() %> €</span>
            <% } %>
        </div>
        <button class="cartAdd"  onclick="optionsVarianti('<%=variante.getIdVariante()%>')">Aggiungi al Carrello</button>
    </div>
    <%
            }
        }
    %>
</div>

<div class="centered-div">
    <span>
        <span class="nome-div-options"></span>
    </span>
    <span class="price-span">
        <span class="current-price"></span>
    </span>

    <div class="flavour-container">
        <label>Flavour</label>
        <select class="flavour-select"></select>
    </div>
    <div class="weight-container">
        <label>Size</label>
        <select class="weight-select"></select>
    </div>
    <select class="quantity-select">
        <% for(int i = 1; i <= 10; i++){ %>
        <option value="<%=i%>"><%=i%></option>
        <%
            }
        %>
    </select>

    <button class="add-to-cart-button">Aggiungi al carrello</button>
</div>



<%@ include file="WEB-INF/Footer.jsp" %>
</body>
</html>
