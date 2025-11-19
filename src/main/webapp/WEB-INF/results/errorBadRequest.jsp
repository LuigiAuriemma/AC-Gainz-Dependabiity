<%--
  Created by IntelliJ IDEA.
  User: raffa
  Date: 19/07/2024
  Time: 15:32
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Page not found</title>
    <link rel="icon" type="image/x-icon" href="Immagini/favicon.ico">
    <style>
        .error-container {
            display: flex;
            flex-direction: column;
            align-items: center;
            background-color: #fff;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
            max-width: 800px;
            margin: 50px auto;
            text-align: center;
        }
        .error-container h1, .error-container h3 {
            margin: 10px 0;
        }
        .img-container {
            margin-top: 20px;
        }
        .img-container img {
            max-width: 500px;
            height: auto;
            border-radius: 8px;
        }
    </style>
</head>
<body>
<div class="error-container">
    <h1>Error 400</h1>
    <h3>You probably entered a wrong input, retry</h3>
    <div class="img-container">
        <img src="Immagini/cbumErr.png" alt="Error Image">
    </div>
</div>
</body>
</html>
