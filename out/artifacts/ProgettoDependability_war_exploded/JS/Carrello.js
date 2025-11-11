document.addEventListener("DOMContentLoaded", function () {
    document.getElementById("buyCart").addEventListener("click", function () {

        if (Logged === false) {
            const previousErrors = document.querySelector(".error-message-container");
            if (previousErrors)
                previousErrors.remove();

            const errorMessageContainer = document.createElement("div");
            errorMessageContainer.className = "error-message-container";

            const errorMessage = document.createElement("p");
            errorMessage.className = "error-message";
            errorMessage.innerText = "Per poter confermare l'ordine bisogna essere loggati al sito!";
            errorMessage.style.color = "red";

            const login = document.createElement("a");
            login.href = "Login.jsp";
            login.className = "login-link";
            login.innerText = "Effettua il Login";

            const br = document.createElement("br");


            const register = document.createElement("a");
            register.href = "Registrazione.jsp";
            register.className = "register-link";
            register.innerText = "Registrati";

            errorMessageContainer.appendChild(errorMessage);
            errorMessageContainer.appendChild(login);
            errorMessageContainer.appendChild(br);
            errorMessageContainer.appendChild(register);

            const summary = document.querySelector(".summary");
            summary.appendChild(errorMessageContainer);

        } else {
            const form = document.createElement("form");
            form.action = "orderServlet";
            form.method = "POST";

            document.body.appendChild(form);
            form.submit();
        }
    })
});


function updateQuantity(quantity, idProdotto, taste, weight) {
    const urlSearch = new URLSearchParams();
    urlSearch.append("action", "quantityVariant");
    urlSearch.append("quantity", quantity);
    urlSearch.append("id", idProdotto);
    urlSearch.append("gusto", taste);
    urlSearch.append("pesoConfezione", weight);
    fetch("cartServlet?" + urlSearch.toString())
        .then(response => {
            if (!response.ok) throw new Error(`Network Error: ${response.status} - ${response.statusText}`);

            return response.text();
        })
        .then(responseText => {
            showCartCheckOut();
            showCart();
        })
        .catch(error => {
            console.error(error);
        })
}



function showCartCheckOut() {
    const urlParam = new URLSearchParams();
    urlParam.append("action", "show");

    fetch("cartServlet?" + urlParam.toString())
        .then(response => {
            if (!response.ok) {
                throw new Error(`Network error: ${response.status} - ${response.statusText}`);
            }

            return response.text();
        })
        .then(responseText => {
            updateCartJSP(responseText);
        })
        .catch(error => {
            console.error(error);
        });
}


function updateCartJSP(response) {
    try {
        if (!response) {
            response = '[]';
        }
        const cartItems = JSON.parse(response);
        const checkOutItemContainer = document.getElementById("checkOutItem");

        const subtotalElement = document.querySelector("#subtotal span");
        const totalOrderElement = document.querySelector("#totalOrder span");

        // Clear previous content
        checkOutItemContainer.innerHTML = "";


        if (cartItems.length === 0){
            checkOutItemContainer.innerText = 'Carrello vuoto';
            subtotalElement.innerText = '$0';
            totalOrderElement.innerText = '$0';
            return;
        }

        cartItems.forEach((cartItem, index) => {
            if (index === cartItems.length - 1) return; // Skip the last item (summary)
            const productDiv = document.createElement("div");
            productDiv.className = "product";

            const img = document.createElement("img");
            img.src = cartItem.imgSrc;
            img.alt = cartItem.nomeProdotto;

            const productInfoDiv = document.createElement("div");
            productInfoDiv.className = "product-info";

            const productName = document.createElement("h3");
            productName.innerText = cartItem.nomeProdotto;

            const productFlavour = document.createElement("p");
            productFlavour.innerText =`${cartItem.flavour}`;

            const productWeight = document.createElement("p");
            productWeight.innerText = `${cartItem.weight} grammi`;

            const price = document.createElement("p");
            price.innerText = `${cartItem.prezzo}€`;

            productInfoDiv.appendChild(productName);
            productInfoDiv.appendChild(productFlavour);
            productInfoDiv.appendChild(productWeight);
            productInfoDiv.appendChild(price);


            const quantityDiv = document.createElement("div");
            quantityDiv.className = "quantity-div";
            const quantity = document.createElement("input");
            quantity.type = 'number';
            quantity.value = cartItem.quantity;
            const updateQuantities = document.createElement("button");
            updateQuantities.innerText = 'Modifica';
            updateQuantities.onclick = function () {
                if (quantity.value !== '')
                    updateQuantity(quantity.value, cartItem.idProdotto, cartItem.flavour, cartItem.weight);
            }

            quantityDiv.appendChild(quantity);
            quantityDiv.appendChild(updateQuantities);

            const rmvDiv = document.createElement("div");
            rmvDiv.className = "rmv-div";
            const rmvButton = document.createElement("button");
            rmvButton.className = "rmvButton";
            rmvButton.innerText = "Rimuovi Elemento";
            rmvButton.style.backgroundColor = "orangered"

            rmvButton.onclick = function () {
                removeItemVariant(cartItem.idProdotto, cartItem.flavour, cartItem.weight);
            };
            rmvDiv.appendChild(rmvButton);

            productDiv.appendChild(img);
            productDiv.appendChild(productInfoDiv);
            productDiv.appendChild(quantityDiv);
            productDiv.appendChild(rmvDiv);

            checkOutItemContainer.appendChild(productDiv);
        });


        const total = cartItems[cartItems.length - 1];

        subtotalElement.innerText = `${total.totalPrice}€`;
        totalOrderElement.innerText = `${total.totalPrice}€`;

    } catch (error) {
        console.error("Error parsing JSON response:", error);
        console.log("Response text:", response);
    }
}




