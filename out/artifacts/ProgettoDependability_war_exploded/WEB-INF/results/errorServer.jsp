<%--
  Created by IntelliJ IDEA.
  User: raffa
  Date: 19/07/2024
  Time: 13:32
  To change this template use File | Settings | File Templates.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" isErrorPage="true"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>JSP Error</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
    <style>
        .error-container {
            display: flex;
            flex-direction: column;
            text-align: center;
            align-items: center;
            background-color: #fff;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
            max-width: 800px;
            margin: 50px auto;
        }
        .message {
            margin-right: 20px;
        }
        .message h3 {
            margin: 0;
            font-size: 24px;
            color: #d9534f;
        }
        .message p {
            margin: 5px 0;
        }
        .img-container {
            flex-shrink: 0;
        }
        .img-container img {
            max-width: 300px;
            height: auto;
            border-radius: 8px;
        }
    </style>
</head>
<body>
<div class="error-container">
    <div class="message">
        <h3>Error</h3>
        <% if (exception != null) { %>
        <p>An exception was raised: <%= exception.toString() %></p>
        <p>Exception message is: <%= exception.getMessage() %></p>
        <br>
        <% } %>
    </div>
    <div class="img-container">
        <img src="Immagini/davidLaid.png" alt="Error Image">
    </div>
</div>
</body>
</html>
