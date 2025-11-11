<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Header</title>
    <link rel="stylesheet" href="CSS/Header.css">
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
    <script>
        const Logged = <%= (session.getAttribute("Utente") != null) %>;
    </script>
    <script src="JS/Header.js" defer></script>
</head>
<body>
<script src="JS/CartPopUp.js" defer></script>
<script src="JS/SearchBar.js"></script>
<script src="JS/genericFilter.js"></script>

<header>
    <div class="header-container">
        <div class="header-burger-menu" onclick="toggleMenu()">
            <div class="header-line"></div>
            <div class="header-line"></div>
            <div class="header-line"></div>
        </div>
        <div class="header-logo">
            <a href="index.jsp">AC-Gainz</a>
        </div>
        <div class="header-search-bar">
            <label>
                <input type="search" id="search-input" placeholder="Search.." tabindex="0">
            </label>
            <img id="search-img" src="Immagini/search.png" alt="search-img">
        </div>
        <div class="header-carrello">
            <div id="cart" tabindex="0">Carrello (0)</div>
            <div id="listCart" class="hidden">
                <!-- Il contenuto del carrello sarà qui -->
            </div>
        </div>
        <div class="header-utente" id="utente">
            <button onclick="myFunction()" class="header-dropbutton">Utente</button>
            <div id="myDropdown" class="header-dropdown-content">
                <a href="Login.jsp">Login</a>
                <a href="Registrazione.jsp">Register</a>
            </div>
        </div>
    </div>

    <div class="header-lista" id="lista">
        <ul>
            <li>
                <form action="categories" method="get">
                    <input type="hidden" name="category" value="tutto">
                    <button type="submit">Store</button>
                </form>
            </li>
            <li>
                <form action="categories" method="get">
                    <input type="hidden" name="category" value="proteine">
                    <button type="submit">Proteine</button>
                </form>
            </li>
            <li>
                <form action="categories" method="get">
                    <input type="hidden" name="category" value="creatina">
                    <button type="submit">Creatina</button>
                </form>
            </li>
            <li>
                <form action="categories" method="get">
                    <input type="hidden" name="category" value="barrette">
                    <button type="submit">Barrette</button>
                </form>
            </li>
        </ul>
    </div>
</header>
</body>
</html>