<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href='https://unpkg.com/boxicons@2.1.4/css/boxicons.min.css' rel='stylesheet'>
    <title>Login Form</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">

    <style>
        @import url('https://fonts.googleapis.com/css2?family=Inter:ital,wght@0,100;0,300;0,400;0,500;0,700;0,900&display=swap');
        * {
            font-family: "Inter";
        }

        body {
            display: flex;
            justify-content: center;
            align-items: center;
            background-image: url("./Immagini/Drawing.png");
            background-repeat: no-repeat;
            height: 100vh;
            background-size: cover;
            margin: 0;
        }

        .login-contenitore {
            display: flex;
            flex-direction: column;
            width: 40%;
            height: 60%;
            border: 2px solid black;
            backdrop-filter: blur(40px);
            box-shadow: 0 0 5px black;
            color: white;
            border-radius: 18px;
            margin: 10px;
            padding: 30px 40px;
        }

        .login-contenitore h1 {
            font-size: 38px;
            text-align: center;
            color: black;
        }

        .input-box {
            position: relative;
            width: 100%;
            height: 50px;
            background: white;
            margin: 30px 0;
            border-radius: 40px;
        }

        .input-box input {
            width: 100%;
            height: 100%;
            background: transparent;
            outline: none;
            border: 1px solid black;
            border-radius: 40px;
            padding: 0 20px 0 45px;
        }

        .input-box input::placeholder {
            color: black;
        }

        .input-box img {
            position: absolute;
            top: 50%;
            left: 90%;
            transform: translateY(-50%);
            cursor: pointer;
            width: 30px;
        }

        .password-requirements {
            display: none;
            position: absolute;
            top: 60px;
            left: 0;
            width: 100%;
            background: white;
            color: black;
            border: 1px solid black;
            border-radius: 10px;
            padding: 10px;
            font-size: 14px;
        }



        .submit-button {
            margin-top: auto;
            width: 100%;
            height: 45px;
            background: orangered;
            outline: none;
            border-radius: 40px;
            border: 1px solid black;
            cursor: pointer;
            font-size: 20px;
            color: white;
            font-weight: 500;
            transition: 0.2s;
        }

        .submit-button:hover {
            color: black;
        }

        .registrazione {
            font-size: 14px;
            text-align: center;
            margin: 20px 0 15px;
            color: white;
        }

        .registrazione p a {
            color: white;
            text-decoration: none;
            font-weight: 600;
            transition: 0.2s;
        }

        .registrazione p a:hover {
            color: orangered;
        }

        .errorMessage {
            color: black;
            font-size: 30px;
            margin-left: 40px;
        }
    </style>
</head>
<body>

<script defer src="JS/togglePassword.js"></script>
<script src="JS/validateForm.js"></script>

<div class="login-contenitore">
    <form action="login" method="post" autocomplete="on" onsubmit="return validateForm()">
        <h1>Accedi</h1>
        <div class="input-box">
            <label for="email"></label><input type="email" name="email" id="email" placeholder="Email" required>
        </div>
        <div class="input-box">
            <label for="password"></label>
            <input type="password" name="password" id="password" placeholder="Password" required
                   onfocus="showPasswordRequirements(true)" onblur="showPasswordRequirements(false)">
            <img src="Immagini/hide.png" alt="hide png" id="imgPass" onclick="togglePassword('password', 'imgPass')">
            <div class="password-requirements" id="password-requirements">
                <p>La password deve contenere:</p>
                <ul>
                    <li>Almeno 8 caratteri</li>
                    <li>Una lettera maiuscola</li>
                    <li>Una lettera minuscola</li>
                    <li>Un numero</li>
                    <li>Un carattere speciale</li>
                </ul>
            </div>
        </div>
        <button type="submit" class="submit-button">Login</button>
        <div class="registrazione">
            <p>Non hai ancora un account?
                <a href="Registrazione.jsp">Registrati</a>
            </p>
        </div>
    </form>

    <div class="errorMessage" id="errorMessage">
        <% if (request.getAttribute("patternEmail") != null) { %>
        <%= request.getAttribute("patternEmail") %>
        <% } else if (request.getAttribute("patternPassword") != null) { %>
        <%= request.getAttribute("patternPassword") %>
        <% } else if (request.getAttribute("userNotFound") != null) { %>
        <%= request.getAttribute("userNotFound") %>
        <% } else if (request.getAttribute("wrongPassword") != null) { %>
        <%= request.getAttribute("wrongPassword") %>
        <% } %>
    </div>
</div>
</body>
</html>