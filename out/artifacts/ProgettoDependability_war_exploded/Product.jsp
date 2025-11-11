<%@ page import="model.Prodotto" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Random" %>
<%@ page import="model.Variante" %>
<%@ page import="java.util.ArrayList" %><%--
  Created by IntelliJ IDEA.
  User: luigiauriemma
  Date: 27/06/24
  Time: 15:13
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
<!DOCTYPE html>
<html>
<head>
    <title>Product Page</title>
    <style>
        .container-flex {
            display: flex;
            justify-content: center;
            flex-wrap: wrap;
        }

        .info-left, .info-right {
            margin: 20px;
        }

        .info-right {
            flex: 1;
        }

        .info-right-name {
            text-transform: capitalize;
            font-weight: 500;
            font-size: 30px;
            color: black;
            margin-top: 10px;
        }

        .product-info {
            background-color: #f5f5f5;
            margin-top: 10px;
            border-radius: 20px;
            padding: 15px 20px;
            width: 100%;
        }

        .product-info-risparmio {
            color: orangered;
            font-size: 15px;
            text-transform: capitalize;
        }

        .product-info-costoattuale {
            font-size: 20px;
            color: black;
            font-weight: bold;
        }

        .product-info-costooriginale {
            color: rgb(128, 128, 128);
            font-size: 17px;
        }

        .product-info-gusto, .product-info-peso, .product-info-quantita:not(:last-child) {
            margin-top: 20px;
            margin-right: 5px;
            color: black;
            font-size: 20px;
            font-weight: 400;
        }

        .product-info-gusto #tastes, .product-info-peso #weights, .product-info-quantita #quantitynumber {
            border-radius: 5px;
            background-color: white;
            color: black;
            border: 1px solid lightgray;
            padding: 6px;
        }

        .cartAddProduct {
            margin-top: 50px;
            padding: 10px;
            color: white;
            background-color: orangered;
            font-size: 20px;
            font-weight: 350;
            transition: .3s;
            border: none;
            border-radius: 15px;
        }

        .cartAddProduct:hover {
            color: black;
            transform: translate(0, -1px);
            cursor: pointer;
        }

        .product-suggests {
            background-color: #f5f5f5;
            width: 100%;
            border-radius: 20px;
            margin-top: 5px;
        }

        .suggests-product-container {
            display: flex;
            flex-wrap: wrap;
            justify-content: space-around;
        }

        .suggests-product-card {
            margin: 10px 15px;
            border-radius: 20px;
            transition: .3s;
        }

        .suggests-product-image {
            position: relative;
            text-align: center;
        }

        .suggests-product-image img {
            max-width: 100px;
            max-height: 150px;
            border-top-left-radius: 20px;
            border-top-right-radius: 20px;
        }

        .suggests-product-sconto {
            position: absolute;
            font-size: 13px;
            background-color: #ffffff;
            padding: 5px;
            border-radius: 5px;
            color: orangered;
            right: 10px;
            top: 10px;
            text-transform: capitalize;
        }

        .suggests-cartAdd {
            position: absolute;
            bottom: 10px;
            left: 50%;
            transform: translateX(-50%);
            padding: 8px;
            width: 80%;
            text-transform: capitalize;
            border: none;
            outline: none;
            background-color: orangered;
            border-radius: 10px;
            transition: 0.4s;
            cursor: pointer;
            opacity: 0;
            color: white;
        }

        .suggests-product-card:hover .suggests-cartAdd {
            opacity: 1;
        }

        .suggests-cartAdd:hover {
            color: black;
        }

        .suggests-product-info {
            width: 100%;
            padding-top: 5px;
        }

        .suggests-product-info-name-redirect {
            text-transform: capitalize;
            margin-bottom: 4px;
            background-color: #f5f5f5;
            font-weight: 450;
            font-size: 17px;
            border: none;
            outline: none;
            cursor: pointer;
            color: black;
        }

        .suggests-product-info-costoattuale {
            color: black;
            font-size: 18px;
            font-weight: 400;
        }

        .suggests-product-info-costooriginale {
            color: orangered;
            text-decoration: line-through;
            font-size: 16px;
            font-weight: 400;
        }

        .suggests-product-info-flavour, .suggests-product-info-weight {
            font-size: 16px;
            font-weight: 400;
            text-transform: capitalize;
        }

        .info-left {
            width: 40%;
        }

        .info-left img {
            width: 100%;
            height: auto;
            border-radius: 20px;
        }

        .product-description {
            margin-top: 20px;
            margin-left: 20px;
        }

        .product-description-accordion {
            background-color: #f0f0f0;
            color: #333;
            cursor: pointer;
            padding: 15px;
            width: 100%;
            border-radius: 10px;
            margin-bottom: 5px;
            border: 1px solid #ccc;
            text-align: left;
            outline: none;
            font-size: 16px;
            transition: background-color 0.4s, border-color 0.4s;
        }

        .product-description-accordion:hover, .product-description-accordion.active {
            background-color: #ddd;
            border-color: #999;
        }

        .product-description-panel {
            padding: 15px;
            margin: 5px 0;
            display: none;
            background-color: #fff;
            overflow: hidden;
            border: 1px solid #ccc;
            border-radius: 10px;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
            max-width: 100%;
        }

        .product-description-panel p {
            margin: 0;
            line-height: 1.6;
            font-size: 14px;
            color: #555;
        }

        table {
            width: 100%;
            border: 1px solid lightgray;
        }

        td {
            text-align: left;
            padding: 16px;
        }

        tr:nth-child(even) {
            background-color: #f5f5f5;
        }

        /* Media Queries */
        @media (max-width: 768px) {
            .info-left, .info-right {
                width: 100%;
            }

            .product-info-quantita #quantitynumber{
                width: 30%;
                margin-top: 5px;
            }

            .product-info {
                width: 100%;
            }

            .cartAddProduct {
                width: 100%;
            }
        }

        @media (max-width: 480px) {
            .product-info-gusto, .product-info-peso, .product-info-quantita {
                font-size: 16px;
            }

            .product-info-costoattuale, .product-info-costooriginale {
                font-size: 18px;
            }

            .cartAddProduct {
                font-size: 18px;
                margin-top: 30px;
            }

            .product-description-accordion {
                font-size: 14px;
            }

            .product-description-panel p {
                font-size: 12px;
            }
        }
    </style>
</head>
<body>
<%@ include file="WEB-INF/Header.jsp" %>


<script src="JS/Product.js"></script>


<%
    Prodotto p = null;
    Variante v = null;
    List<String> tastes = (List<String>) request.getAttribute("allTastes");
    List<Integer> pesi = (List<Integer>) request.getAttribute("firstWeights");
    p = (Prodotto) request.getAttribute("prodotto");
    if (p != null) {
        v = p.getVarianti().get(0);
    }
%>
<div class="container-flex">
    <div class="info-left">
        <img src="<%=p.getImmagine()%>" alt="<%=p.getNome()%>">
    </div>

    <div class="info-right">
        <div class="info-right-name"><%=p.getNome()%>
        </div>
        <div class="product-info">
            <% if (v.getSconto() > 0) {
                float prezzoscontato = v.getPrezzo() * (1 - (float) v.getSconto() /100);
                prezzoscontato = Math.round(prezzoscontato * 100.0f) / 100.0f;
            %>
            <div class="product-info-risparmio">Risparmia
                <%
                    float costorisparmio = 0;
                    costorisparmio = (v.getPrezzo() * v.getSconto()) / 100;
                    costorisparmio = Math.round(costorisparmio * 100.0f) / 100.0f;
                %>
                <%=costorisparmio%>€
            </div>
            <div>
                <span class="product-info-costoattuale"><%=prezzoscontato%>€</span>
                <span class="product-info-costooriginale">Era <%=v.getPrezzo()%>€</span>
            </div>
            <%} else {%>
            <span class="product-info-costoattuale"><%=v.getPrezzo()%>€</span>
            <% } %>
            <div class="product-info-gusto">
                Gusto:
                <select data-product-id = "<%=p.getIdProdotto()%>" id="tastes" name="taste">
                    <option selected value="<%=v.getGusto()%>"><%=v.getGusto()%></option>
                    <% if (tastes != null && !tastes.isEmpty()){
                            for (String taste: tastes){
                                if (!taste.equals(v.getGusto())){
                    %>
                    <option value="<%=taste%>"><%=taste%></option>
                    <%
                                }
                            }
                        }
                    %>
                </select>
            </div>
            <div class="product-info-peso">
                Peso:
                <select id="weights" name="weight">
                    <option selected value="<%=v.getPesoConfezione()%>"><%=v.getPesoConfezione()%></option>
                    <% if (pesi != null && !pesi.isEmpty()){
                        for (Integer peso: pesi){
                            if (peso != v.getPesoConfezione()){
                    %>
                    <option value="<%=peso%>"><%=peso%> grammi</option>
                    <%
                                }
                            }
                        }
                    %>
                </select>
            </div>
            <div class="product-info-quantita">
                <select id="quantitynumber" name="quantity">
                  <%
                    for (int i = 1; i <= 10; i++){
                  %>
                   <option value="<%=i%>"><%=i%></option>
                  <%
                    }
                  %>
                </select>
                <button class="cartAddProduct" data-product='<%=p.getIdProdotto()%>'>Aggiungi al Carrello</button>
            </div>
        </div>
        <div class="info-right-name">
            Potrebbe Interessarti anche:
        </div>
        <div class="product-suggests">
            <div class="suggests-product-container">
                <%
                    List<Prodotto> suggeriti = null;
                    if (request.getAttribute("suggeriti") != null) {
                        suggeriti = (List<Prodotto>) request.getAttribute("suggeriti");
                    }
                    if (suggeriti != null) {
                        List<Prodotto> alreadySuggested = new ArrayList<>();
                        Random random = new Random();
                        int count = 0;
                        while (count < 2 && alreadySuggested.size() < suggeriti.size()) {
                            int randomIndex = random.nextInt(suggeriti.size());
                            Prodotto s = suggeriti.get(randomIndex);
                            if (!alreadySuggested.contains(s)) {
                                alreadySuggested.add(s);
                                Variante z = s.getVarianti().get(0);
                                count++;
                %>
                <div class="suggests-product-card">
                    <div class="suggests-product-image">
                        <% if (z.getSconto() > 0) { %>
                        <span class="suggests-product-sconto"><%= z.getSconto() %>% di Sconto</span>
                        <% } %>
                        <img src="<%= s.getImmagine() %>" alt="<%= s.getNome() %>">
                        <button class="suggests-cartAdd" onclick="addCartVariant('<%=z.getIdProdotto()%>', '1', '<%=z.getGusto()%>', '<%=z.getPesoConfezione()%>')">Aggiungi al Carrello</button>
                    </div>
                    <div class="suggests-product-info">
                        <form action="ProductInfo" method="post">
                            <input type="hidden" name="primaryKey" value="<%=s.getIdProdotto()%>">
                            <button class="suggests-product-info-name-redirect"><%= s.getNome() %></button>
                        </form>
                        <% if (z.getSconto() > 0) {
                            float prezzoscontato = (z.getPrezzo() - ((z.getPrezzo() * z.getSconto()) / 100));
                            prezzoscontato = Math.round(prezzoscontato * 100.0f) / 100.0f;
                        %>
                        <span class="suggests-product-info-costoattuale"><%=prezzoscontato%>€</span>
                        <span class="suggests-product-info-costooriginale"><%=z.getPrezzo()%>€</span>
                        <% } else { %>
                        <span class="suggests-product-info-costoattuale"><%=z.getPrezzo()%>€</span>
                        <% } %>
                        <div class="suggests-product-info-flavour"><%= z.getGusto() %></div>
                        <div class="suggests-product-info-weight"><%= z.getPesoConfezione() %>gr.</div>
                    </div>
                </div>
                <%
                            }
                        }
                    }
                %>
            </div>
        </div>

    </div>
</div>
<div class="product-description">
    <button class="product-description-accordion">Descrizione</button>
    <div class="product-description-panel">
        <p><%=p.getDescrizione()%>
        </p>
    </div>

    <button class="product-description-accordion">Valori Nutrizionali</button>
    <div class="product-description-panel">
        <table>
            <p style="font-size: 14px; font-weight: 400">Per 100gr.</p>
            <tr>
                <td>Energia kcal</td>
                <td><%=p.getCalorie()%>kcal</td>
            </tr>
            <tr>
                <td>Grassi</td>
                <td><%=p.getGrassi()%>g</td>
            </tr>
            <tr>
                <td>Carboidrati</td>
                <td><%=p.getCarboidrati()%>g</td>
            </tr>
            <tr>
                <td>Proteine</td>
                <td><%=p.getProteine()%>g</td>
            </tr>
        </table>
    </div>
</div>


<%@ include file="WEB-INF/Footer.jsp" %>
</body>
</html>