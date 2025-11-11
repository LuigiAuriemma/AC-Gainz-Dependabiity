<%@ page import="java.util.Date" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="model.*" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.HashMap" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <script src="JS/AreaUtente.js"></script>
    <link rel="stylesheet" href="CSS/AreaUtente.css">
    <title>Area utente</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">

</head>
<body>

<%@ include file="Header.jsp" %>

<script src="JS/togglePassword.js"></script>

<div class="contenitore">
    <div class="tab">
        <button class="tablinks" onclick="opentab(this, 'areautente')">Area Utente</button>
        <button class="tablinks" onclick="opentab(this, 'areamodifiche')">Area Modifiche</button>
        <button class="tablinks" onclick="opentab(this, 'areaordini')">Area Ordini</button>
    </div>



    <div id="areautente" class="tabcontent">
        <h3>Dettagli Utente</h3>
        <%
            Utente utente = (Utente) session.getAttribute("Utente");
            if (utente != null) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
                String formattedDate = simpleDateFormat.format(utente.getDataNascita());
                boolean powers = utente.getPoteri();
                if (powers) {
        %>
        <div style="margin-top: 4px">
            <a href="admin" id="areaAdmin">Area Admin</a>
        </div>
        <%
            }
        %>
        <div class="logOut">
            <a href="logOut">LogOut</a>
        </div>

        <p id="areautente-tags">Email:</p> ${Utente.email}
       <%-- <br><br><p id="areautente-tags">Password:</p> ${Utente.password} --%>
        <br><br><p id="areautente-tags">Indirizzo:</p> ${Utente.indirizzo}
        <br><br><p id="areautente-tags">Nome:</p> ${Utente.nome}
        <br><br><p id="areautente-tags">Cognome:</p> ${Utente.cognome}
        <br><br><p id="areautente-tags">Codice fiscale:</p> ${Utente.codiceFiscale}
        <br><br><p id="areautente-tags">Data di Nascita:</p><%= formattedDate %>
        <br><br><p id="areautente-tags">Telefono:</p> ${Utente.telefono}
        <%
            }
        %>
    </div>

    <div id="areamodifiche" class="tabcontent" style="display: none;">
        <h3>Area Modifiche</h3>
        <br><br>
        <button onclick="displayAll('passdiv')">Modifica Password</button>
        <div id="passdiv" class="form-container" style="display: none">
            <div id="message-password" class="message"></div>
            <form id="change-password-form" action="editServlet" method="post">
                <input type="hidden" name="field" value="password">
                <label for="current-password">Password Attuale</label>
                <input type="password" id="current-password" name="current-password" required>
                <img src="Immagini/hide.png" alt="hide png" onclick="togglePassword('current-password')">
                <label for="new-password">Nuova Password</label>
                <input type="password" id="new-password" name="new-password" required pattern="^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,}$">
                <img src="Immagini/hide.png" alt="hide png" onclick="togglePassword('new-password')">
                <label for="confirm-password">Conferma Nuova Password</label>
                <input type="password" id="confirm-password" name="confirm-password" required pattern="^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,}$">
                <img src="Immagini/hide.png" alt="hide png" onclick="togglePassword('confirm-password')">
                <input type="submit" class="submit" value="Modifica Password">
            </form>
        </div>

        <br><br>
        <button onclick="displayAll('inddiv')">Modifica Indirizzo</button>
        <div id="inddiv" class="form-container" style="display: none">
            <div id="message-address" class="message"></div>
            <form id="change-address-form" action="editServlet" method="post">
                <input type="hidden" name="field" value="address">
                <label for="street">Via</label>
                <input type="text" id="street" name="street" required>
                <input type="submit" class="submit" value="Modifica Indirizzo">
            </form>
        </div>

        <br><br>
        <button onclick="displayAll('numteldiv')">Modifica Numero di Telefono</button>
        <div id="numteldiv" class="form-container" style="display: none">
            <div id="message-phone" class="message"></div>
            <form id="change-phone-form" action="editServlet" method="post">
                <input type="hidden" name="field" value="phone">
                <label for="new-phone">Nuovo Numero di Telefono</label>
                <input type="text" id="new-phone" name="new-phone" required pattern="^3[0-9]{8,9}$">
                <input type="submit" class="submit" value="Modifica Numero di Telefono">
            </form>
        </div>

        <br><br>
        <button onclick="displayAll('codfisdiv')">Modifica Codice Fiscale</button>
        <div id="codfisdiv" class="form-container" style="display: none">
            <div id="message-codice-fiscale" class="message"></div>
            <form id="change-codice-fiscale-form" action="editServlet" method="post">
                <input type="hidden" name="field" value="codice-fiscale">
                <label for="new-codice-fiscale">Nuovo Codice Fiscale</label>
                <input type="text" id="new-codice-fiscale" name="new-codice-fiscale" required pattern="^[A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z]$">
                <input type="submit" class="submit" value="Modifica Codice Fiscale">
            </form>
        </div>

        <br><br>
        <button onclick="displayAll('ddndiv')">Modifica Data di Nascita</button>
        <div id="ddndiv" class="form-container" style="display: none">
            <div id="data-di-nascita" class="message"></div>
            <form id="change-birthdate-form" action="editServlet" method="post">
                <input type="hidden" name="field" value="data-di-nascita">
                <label for="new-birthdate">Nuova Data di Nascita</label>
                <input type="date" id="new-birthdate" name="new-birthdate" required min="1900-01-01">
                <input type="submit" class="submit" value="Modifica Data di Nascita">
            </form>
        </div>

        <br><br>
        <button onclick="displayAll('nomediv')">Modifica Nome</button>
        <div id="nomediv" class="form-container" style="display: none">
            <div id="message-nome" class="message"></div>
            <form id="change-name-form" action="editServlet" method="post">
                <input type="hidden" name="field" value="nome">
                <label for="new-name">Nuovo Nome</label>
                <input type="text" id="new-name" name="new-name" required>
                <input type="submit" class="submit" value="Modifica Nome">
            </form>
        </div>

        <br><br>
        <button onclick="displayAll('cognomediv')">Modifica Cognome</button>
        <div id="cognomediv" class="form-container" style="display: none">
            <div id="message-cognome" class="message"></div>
            <form id="change-surname-form" action="editServlet" method="post">
                <input type="hidden" name="field" value="cognome">
                <label for="new-surname">Nuovo Cognome</label>
                <input type="text" id="new-surname" name="new-surname" required>
                <input type="submit" class="submit" value="Modifica Cognome">
            </form>
        </div>
    </div>

    <div id="areaordini" class="tabcontent" style="display: none">
        <h3>Ordini Effettuati</h3>
        <% if (utente != null) {
            List<Ordine> ordini = (List<Ordine>) request.getAttribute("ordini");
            HashMap<Integer, List<DettaglioOrdine>> dettaglioOrdini = (HashMap<Integer, List<DettaglioOrdine>>) request.getAttribute("dettaglioOrdini");
            if (ordini != null && !ordini.isEmpty() && dettaglioOrdini != null) { %>
        <div>
            <% for (Ordine ordine : ordini) { %>
            <div class="ordine" onclick="toggleDetails('<%= ordine.getIdOrdine() %>')">
                <p>Id Ordine: <%= ordine.getIdOrdine() %></p>
                <p>Data ordine: <%=ordine.getDataOrdine()%></p>
                <p>Stato: <%= ordine.getStato() %></p>
                <p>Totale: <%= ordine.getTotale() %> €</p>
            </div>
            <div id="dettagli-<%= ordine.getIdOrdine() %>" class="dettagli"> <%--Qui lo script prende l'id del div da mostrare--%>
                <table>
                    <tr>
                        <th>Nome Prodotto</th>
                        <th>Gusto</th>
                        <th>Confezione</th>
                        <th>Quantità</th>
                        <th>Prezzo</th>
                    </tr>
                    <% List<DettaglioOrdine> dettagli = dettaglioOrdini.get(ordine.getIdOrdine());
                        if (dettagli != null) {
                            for (DettaglioOrdine dettaglio : dettagli) { %>
                    <tr>
                        <td><%= dettaglio.getNomeProdotto() %></td>
                        <td><%= dettaglio.getGusto() %></td>
                        <td><%= dettaglio.getPesoConfezione() %> grammi</td>
                        <td><%= dettaglio.getQuantita() %></td>
                        <td><%= dettaglio.getPrezzo() %> €</td>
                    </tr>
                    <% } } %>
                </table>
            </div>
            <% } %>
        </div>
        <% } } %>
    </div>



</div>



<script>
    function handleMessages(type, message, field) {
        let messageDiv = document.getElementById("message-" + field);
        if (messageDiv) {
            messageDiv.innerHTML = message;
            messageDiv.className = "message " + type;
            messageDiv.style.display = "block";
        }
    }

    let message = '<%= request.getAttribute("message") %>';
    let messageType = '<%= request.getAttribute("messageType") %>';
    let field = '<%= request.getAttribute("field") %>';

    console.log(message);
    console.log(messageType);
    console.log(field);

    if (message && messageType && field) {
        handleMessages(messageType, message, field);
    }
</script>





<%@ include file="Footer.jsp" %>
</body>
</html>
