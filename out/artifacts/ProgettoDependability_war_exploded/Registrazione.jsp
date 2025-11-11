<%@ page import="model.Prodotto" %>
<%@ page import="java.util.List" %>
<%--
  Created by IntelliJ IDEA.
  User: luigiauriemma
  Date: 17/05/24
  Time: 10:11
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href='https://unpkg.com/boxicons@2.1.4/css/boxicons.min.css' rel='stylesheet'>
    <title>Registration Form</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Inter:ital,wght@0,100;0,300;0,400;0,500;0,700;0,900;1,100;1,300;1,400;1,500;1,700;1,900&display=swap');
        *{
            font-family:"Inter";
        }
        body{
            background-image: url("./Immagini/sfondoregister2.jpeg");
            height: fit-content;
            background-position: center;
            background-repeat: no-repeat;
            background-size: cover;
        }

        .register-contenitore{
            display: grid;
            justify-content: center;
            height: fit-content;
            width: fit-content;
            border: 2px solid black;
            backdrop-filter: blur(40px);
            box-shadow: 0 0 5px black;
            border-radius: 20px;
            padding: 30px 40px;
            margin: 30px auto;

        }

        .register-contenitore h1{
            font-size: 38px;
            text-align: center;
            color: orangered;
        }
        .register-contenitore .submit-button{
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
        .register-contenitore .submit-button:hover{
            color: black;
        }
        .password-requirements {
            display: none;
            position: absolute;
            top: 100%;
            left: 0;
            width: 100%;
            background: white;
            color: black;
            border: 1px solid black;
            border-radius: 10px;
            padding: 10px;
            font-size: 14px;
            z-index: 10;
        }

        .register-contenitore .input-box{
            position: relative;
            height: 50px;
            background: white;
            margin: 20px 0;
            border-radius: 40px;
        }

        .input-box input{
            width: 100%;
            height: 100%;
            background: transparent;
            outline: none;
            border: 1px solid black;
            border-radius: 40px;
            padding: 20px 45px 20px 20px;
        }
        .input-box input::placeholder{
            color: black;
        }

        .input-box img{
            position: absolute;
            top: 50%;
            left: 80%;
            transform: translateY(-50%);
            cursor: pointer;
            width: 30px;
        }

        form{
            display: inline-grid;
            width: fit-content;
        }

        .register-first-row, .register-second-row{
            width: 100%;
            display: inline-grid;
        }

        .tab{
            display: inline-flex;
            gap: 30px;
        }

        .register-contenitore p{
            font-size: 25px;
            color: orangered;
            margin: -8px 0 0 0;
        }
        .register-contenitore a{
            font-size: 20px;
            color: orangered;
            text-decoration: none;
            cursor: pointer;
        }

    </style>
<body>

<script defer src="JS/validateForm.js"></script>
<script defer src="JS/togglePassword.js"></script>

<div class="register-contenitore">
    <h1>Registrazione</h1>
    <form id="registration-form" action="register" method="post" onsubmit="return validateForm()">
        <div class="tab">
            <div class="register-first-row">
                <div class="input-box">
                    <label for="email"></label><input type="email" name="email" id="email" placeholder="Email" required autocomplete="">
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

                <div class="input-box">
                    <label for="nome"></label><input type="text" name="nome" id="nome" placeholder="Nome" required>
                </div>
                <div class="input-box">
                    <label for="cognome"></label><input type="text" name="cognome" id="cognome" placeholder="Cognome" required>
                </div>
            </div>

            <div class="register-second-row">
                <div class="input-box">
                    <label for="codiceFiscale"></label><input type="text" name="codiceFiscale" id="codiceFiscale" placeholder="Codice Fiscale" required>
                </div>
                <div class="input-box">
                    <label for="dataDiNascita"></label><input type="date" name="dataDiNascita" id="dataDiNascita" placeholder="Data di nascita" required min="1900-01-01">
                </div>
                <div class="input-box">
                    <label for="indirizzo"></label><input type="text" name="indirizzo" id="indirizzo" placeholder="Indirizzo" required>
                </div>
                <div class="input-box">
                    <label for="numCellulare"></label><input type="text" name="numCellulare" id="numCellulare" placeholder="numCellulare" required >
                </div>
            </div>
        </div>
        <button type="submit" class="submit-button">Invia</button>
    </form>

    <p>Sei già registrato?<a href="Login.jsp"> Effettua il Login</a></p>

</div>

</body>
</html>