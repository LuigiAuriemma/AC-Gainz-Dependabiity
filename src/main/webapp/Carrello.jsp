<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

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

        <%--
             2. Usiamo c:if per controllare se il carrello esiste ed è pieno.
             Sostituisce if (cartItems != null)
        --%>
        <c:choose>
            <c:when test="${not empty sessionScope.cart}">

                <%-- 3. Inizializziamo il totale a 0 --%>
                <c:set var="total" value="0" />

                <%-- 4. c:forEach sostituisce il ciclo for Java --%>
                <c:forEach items="${sessionScope.cart}" var="cartItem">

                    <%-- Aggiorniamo il totale --%>
                    <c:set var="total" value="${total + cartItem.prezzo}" />

                    <div class="product">
                            <%--
                                 5. c:out PROTEGGE DA XSS.
                                 escapeXml="true" è il default, ma lo specifico per chiarezza.
                            --%>
                        <img src="<c:out value='${cartItem.immagineProdotto}'/>"
                             alt="<c:out value='${cartItem.nomeProdotto}'/>">

                        <div class="product-info">
                            <h3><c:out value="${cartItem.nomeProdotto}"/></h3>
                            <p><c:out value="${cartItem.gusto}"/></p>
                            <p><c:out value="${cartItem.pesoConfezione}"/> grammi</p>

                                <%-- Formattazione prezzo sicura --%>
                            <p>
                                <fmt:formatNumber value="${cartItem.prezzo}" type="number" minFractionDigits="2" maxFractionDigits="2"/>
                            </p>
                        </div>

                        <div class="quantity-div">
                            <input class="inputQuantity" type="number" value="<c:out value='${cartItem.quantita}'/>">

                                <%--
                                     ATTENZIONE QUI: Gli attributi data- contengono stringhe.
                                     c:out gestisce l'escape delle virgolette impedendo che l'HTML si rompa.
                                --%>
                            <button data-product-id="<c:out value='${cartItem.idProdotto}'/>"
                                    data-product-taste="<c:out value='${cartItem.gusto}'/>"
                                    data-product-weight="<c:out value='${cartItem.pesoConfezione}'/>"
                                    class="modifyQuantity">Modifica</button>
                        </div>
                        <div class="rmv-div">
                            <button class="rmvButton"
                                    data-product-id="<c:out value='${cartItem.idProdotto}'/>"
                                    data-product-taste="<c:out value='${cartItem.gusto}'/>"
                                    data-product-weight="<c:out value='${cartItem.pesoConfezione}'/>"
                                    onclick="removeItem()">Rimuovi Elemento</button>
                        </div>
                    </div>
                </c:forEach>
            </c:when>

            <%-- Opzionale: Se il carrello è vuoto --%>
            <c:otherwise>
                <p>Il tuo carrello è vuoto.</p>
                <c:set var="total" value="0" />
            </c:otherwise>
        </c:choose>
    </div>


    <div class="summary">
        <h2>Somma Totale</h2>
        <%-- Formattazione del totale calcolato nel ciclo --%>
        <p id="subtotal">Subtotale: <span> <fmt:formatNumber value="${total}" type="number" minFractionDigits="2" maxFractionDigits="2"/> </span></p>
        <p id="totalOrder">Totale ordine: <span> <fmt:formatNumber value="${total}" type="number" minFractionDigits="2" maxFractionDigits="2"/> </span></p>
        <div class="buy">
            <button id="buyCart">Procedi all'acquisto</button>
        </div>
    </div>
</div>

<%@ include file="WEB-INF/Footer.jsp" %>
</body>
</html>