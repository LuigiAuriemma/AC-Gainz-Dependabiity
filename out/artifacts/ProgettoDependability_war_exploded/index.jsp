<%@ page import="java.util.List" %>
<%@ page import="model.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="WEB-INF/Header.jsp"%>
<!DOCTYPE html>
<html>
<head>
    <title>AC-Gainz HomePage</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
    <link rel="stylesheet" href="CSS/ProductCard.css">
    <style>
        .page-container {
            margin: 40px;
        }
        .img-container {
            position: relative;
            width: 90%;
            margin: 0 auto;
            align-items: center;
        }
        .main-img, .secondary-img {
            width: 100%;
            max-width: 100%;
            height: auto;
            border-radius: 20px;
        }
        .text-overlay {
            position: absolute;
            top: 27%;
            left: 27%;
            transform: translate(-50%, -50%);
            color: white;
            font-size: 2.5vw;
            padding: 5px;
            font-weight: 550;
            text-align: left;
            max-width: 50%;
            border-radius: 20px;
            backdrop-filter: blur(40px);
            box-sizing: border-box;
        }
        .prodotti-phrase {
            font-size: 2vw;
            color: black;
            font-weight: 450;
            margin: 10px 0 5px 0;
        }
        .prodotti-list {
            width: 100%;
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            justify-content: center;
            gap: 15px;
        }
        @media (max-width: 1320px) {
            .text-overlay {
                font-size: 2vw;
                top: 25%;
                left: 33%;
                transform: translate(-50%, -50%);
                max-width: 60%;
            }
            .prodotti-phrase {
                font-size: 2.5vw;
            }
        }
        @media (max-width: 768px) {
            .text-overlay {
                font-size: 2.5vw;
                top: 25%;
                left: 35%;
                transform: translate(-50%, -50%);
                max-width: 80%;
            }
            .prodotti-phrase {
                font-size: 3vw;
            }
        }
        @media (max-width: 515px) {
            .text-overlay {
                font-size: 2.7vw;
                top: 27%;
                left: 36%;
                transform: translate(-50%, -50%);
                max-width: 90%;
            }
            .prodotti-phrase {
                font-size: 4vw;
            }
        }
    </style>
</head>

<body>
<script src="JS/productOptions.js"></script>

<div class="page-container">
    <div class="img-container">
        <img class="main-img" src="./Immagini/discount-immagine-homepage.png" alt="immagine sconto">
        <div class="text-overlay">Carburante per L'attività Fisica, dai un occhio ai nostri prodotti scontati</div>
        <div class="prodotti-phrase">In Sconto: </div>
    </div>
    <div class="prodotti-list">
        <%
            List<Prodotto> products = null;
            products = (List<Prodotto>) application.getAttribute("Products");

            if (products != null) {
                for (Prodotto p : products) {
                    Variante variante = p.getVarianti().get(0);
                    if(variante.getSconto() > 0){
        %>
        <div class="product-card">
            <div class="product-image">
                <span class="product-sconto"><%= variante.getSconto() %>% di Sconto</span>

                <form id="<%=p.getIdProdotto()%>" action="ProductInfo" method="post">
                    <input type="hidden" name="primaryKey" value="<%=p.getIdProdotto()%>">
                </form>
                <img src="<%= p.getImmagine() %>" alt="<%= p.getNome() %>" onclick="document.getElementById('<%=p.getIdProdotto()%>').submit();">
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
            }
        %>
    </div>

    <div class="img-container">
        <img class="secondary-img" src="./Immagini/seconda-immagine-homepage.jpg" alt="immagine prodotto">
        <div class="text-overlay">Scopri i Nostri Prodotti In Evidenza</div>
        <div class="prodotti-phrase">In Evidenza: </div>
    </div>
    <div class="prodotti-list">
        <%
            if (products != null) {
                for (Prodotto p : products) {
                    Variante variante = p.getVarianti().get(0);
                    if (variante.isEvidenza()){
        %>
        <div class="product-card">
            <div class="product-image">
                <% if (variante.getSconto() > 0) { %>
                <span class="product-sconto"><%= variante.getSconto() %>% di Sconto</span>
                <% } %>
                <form id="<%=p.getIdProdotto()%>" action="ProductInfo" method="post">
                    <input type="hidden" name="primaryKey" value="<%=p.getIdProdotto()%>">
                </form>
                <img src="<%= p.getImmagine() %>" alt="<%= p.getNome() %>" onclick="document.getElementById('<%=p.getIdProdotto()%>').submit();">
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
            }
        %>
    </div>
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

<%@ include file="WEB-INF/Footer.jsp"%>
</body>
</html>
