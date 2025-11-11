document.addEventListener("DOMContentLoaded", function () {
    let selectTastes = document.getElementById("tastes");
    let selectWeights = document.getElementById("weights");
    let idProdotto = selectTastes.getAttribute("data-product-id");

    selectTastes.onchange = function (){
        updateOptionsProduct(idProdotto, this.value)
            .then(() =>{
                updatePriceProduct(idProdotto, selectTastes.value, selectWeights.value);
            })
            .catch(error => {
                console.error(error);
            });
    }

    let cartAdd = document.querySelector(".cartAddProduct");
    cartAdd.onclick = function (){
        let quantityInput = document.getElementById("quantitynumber").value;
        /*console.log(quantityInput);*/
        addCartVariant(idProdotto, quantityInput, selectTastes.value, selectWeights.value);
    }

    selectWeights.onchange = function (){
        let flavourValue = selectTastes.value;
        updatePriceProduct(idProdotto, flavourValue, this.value);
    }

    // Codice per gestire l'accordion della descrizione prodotto
    let acc = document.getElementsByClassName("product-description-accordion");
    for (let i = 0; i < acc.length; i++) {
        acc[i].addEventListener("click", function () {
            this.classList.toggle("active");
            let panel = this.nextElementSibling;
            if (panel.style.display === "block") {
                panel.style.display = "none";
            } else {
                panel.style.display = "block";
            }
        });
    }
});



function updatePriceProduct(idProdotto, flavour, weight){
    /*console.log(idProdotto);
    console.log(flavour);
    console.log(weight);*/

    let urlSearch = new URLSearchParams();
    urlSearch.append("idProdotto", idProdotto);
    urlSearch.append("flavour", flavour);
    urlSearch.append("weight", weight);
    urlSearch.append("action", "updatePrice");


    fetch("showOptions?"+urlSearch.toString())
        .then(response =>{
            if(!response.ok) throw new Error(`Network Error: ${response.status} - ${response.statusText}`);

            return response.text();
        })
        .then(responseText =>{
            updatePriceProductView(responseText);
        })
        .catch(error =>{
            console.error(error);
        })
}

function updatePriceProductView(response) {
    let price = JSON.parse(response);

    let productInfo = document.querySelector(".product-info");
    if (!productInfo) return;

    if (price.sconto > 0) {
        let productRisparmio = productInfo.querySelector(".product-info-risparmio");
        if (!productRisparmio) {
            productRisparmio = document.createElement("div");
            productRisparmio.className = "product-info-risparmio";
        }

        productRisparmio.innerText = `Risparmia ${(price.prezzo * price.sconto / 100).toFixed(2)} €`

        let productCostoAttuale = productInfo.querySelector(".product-info-costoattuale");
        if (!productCostoAttuale) {
            productCostoAttuale = document.createElement("span");
            productCostoAttuale.className = "product-info-costoattuale";
        }

        productCostoAttuale.innerText = `${(price.prezzo * (1 - price.sconto / 100)).toFixed(2)}€`;

        let productCostoOriginale = productInfo.querySelector(".product-info-costooriginale");
        if (!productCostoOriginale) {
            productCostoOriginale = document.createElement("span");
            productCostoOriginale.className = "product-info-costooriginale";

            productInfo.prepend(productRisparmio, productCostoAttuale, productCostoOriginale)
        }

        productCostoOriginale.innerText = ` Era ${price.prezzo} €`;
    } else {
        let productCostoAttuale = productInfo.querySelector(".product-info-costoattuale");
        if (!productCostoAttuale) {
            productCostoAttuale = document.createElement("span");
            productCostoAttuale.className = "product-info-costoattuale";
            productInfo.prepend(productCostoAttuale);
        }

        productCostoAttuale.innerText = `${(price.prezzo * (1 - price.sconto / 100)).toFixed(2)} €`;

        let productRisparmio = productInfo.querySelector(".product-info-risparmio");
        let productCostoOriginale = productInfo.querySelector(".product-info-costooriginale");
        if (productRisparmio) productRisparmio.remove();
        if (productCostoOriginale) productCostoOriginale.remove();

    }

}



function updateOptionsProduct(idProdotto, value) {
    let urlSearch = new URLSearchParams();
    urlSearch.append("action", "updateOptions");
    urlSearch.append("flavour", value);
    urlSearch.append("idProdotto", idProdotto);
    return fetch("showOptions?" + urlSearch.toString())
        .then(response => {
            if (!response.ok) throw new Error(`Network error: ${response.status} - ${response.statusText}`);
            return response.text();
        })
        .then(responseText => {
            updateWeightOptionsProduct(responseText);
        });
}
function updateWeightOptionsProduct(response){
    let weights = JSON.parse(response);

    let productInfo = document.querySelector(".product-info-peso");
    let selectWeights = productInfo.querySelector("#weights");
    selectWeights.innerHTML = "";

    weights.forEach(weight =>{
        const weightOption = document.createElement("option");
        weightOption.innerText = `${weight.peso}`;
        selectWeights.appendChild(weightOption);
    })
}

