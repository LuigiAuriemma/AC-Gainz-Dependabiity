function optionsVarianti(idVariante){
    let urlSearchPar = new URLSearchParams();
    urlSearchPar.append("action", "showFirst");
    urlSearchPar.append("idVariante", idVariante);
    fetch("showOptions?"+urlSearchPar.toString())
        .then(response =>{
            if(!response.ok) throw new Error(`Network error: ${response.status} - ${response.statusText}`);

            return response.text();
        })
        .then(responseText =>{
            updateDivOptions(responseText);
            document.querySelector(".centered-div").style.display = 'flex';
        })
        .catch(error =>{
            console.error(error);
        })
}

function updateDivOptions(response){
    let varianti;
    try {
        varianti = JSON.parse(response);
    }catch (e){
        console.error(e);
        return;
    }

    let firstOne = varianti[0];

    const divOption = document.querySelector(".centered-div");
    const nome = divOption.querySelector(".nome-div-options");
    nome.innerText = `${firstOne.nomeProdotto}`;

    const prezzoSpan = divOption.querySelector(".price-span");
    prezzoSpan.innerHTML = '';
    const prezzo = document.createElement("h3");
    prezzo.className = "current-price";
    prezzo.innerText = `${(firstOne.cheapestPrice * (1-firstOne.cheapestDiscount/100)).toFixed(2)}€`;
    prezzoSpan.appendChild(prezzo);

    if(firstOne.cheapestDiscount > 0){
        const prezzoOriginale = document.createElement("span");
        prezzoOriginale.className = "original-price";
        prezzoOriginale.innerText = `${firstOne.cheapestPrice}€`;
        prezzoSpan.appendChild(prezzoOriginale);
    }

    const selectFlavours = divOption.querySelector(".flavour-select");
    selectFlavours.innerHTML = '';
    const firstOption = document.createElement("option");
    firstOption.selected = true;
    firstOption.innerText = firstOne.cheapestFlavour;
    selectFlavours.append(firstOption);

    const selectWeights = divOption.querySelector(".weight-select");
    selectWeights.innerHTML = '';
    const firstWeightOption = document.createElement("option");
    firstWeightOption.selected = true;
    firstWeightOption.innerText = firstOne.cheapestWeight;
    selectWeights.append(firstWeightOption);

    for (let i = 1;  i < varianti.length; i++){
        let variante = varianti[i];

        if (variante.cheapestWeightOptions !== undefined){
            const weightOption = document.createElement("option");
            weightOption.innerText = variante.cheapestWeightOptions;
            selectWeights.append(weightOption);
        }

        if (variante.gusto !== undefined){
            const flavourOption = document.createElement("option");
            flavourOption.innerText = variante.gusto;
            selectFlavours.append(flavourOption);
        }
    }

    selectFlavours.onchange = function () {
        updateOptions(firstOne.idProdotto, this.value)
            .then(() => {
                let weightValue = selectWeights.value;
                updatePrice(firstOne.idProdotto, this.value, weightValue);
            })
            .catch(error => {
                console.error(error);
            });
    };



    selectWeights.onchange = function (){
        let flavourValue = selectFlavours.value;
        updatePrice(firstOne.idProdotto, flavourValue, this.value);
    }


    let quantitySelect = divOption.querySelector(".quantity-select");
    let buttonCart = divOption.querySelector(".add-to-cart-button");
    buttonCart.onclick = function (){
        addCartVariant(firstOne.idProdotto, quantitySelect.value, selectFlavours.value, selectWeights.value);
    }


    // Check if form already exists
    let form = divOption.querySelector(".formInfoProduct");
    if (!form) {
        form = document.createElement("form");
        form.className = "formInfoProduct";
        form.method = "POST";
        form.action = "ProductInfo";

        const inputElement = document.createElement("input");
        inputElement.type = "hidden";
        inputElement.name = "primaryKey";
        inputElement.value = `${firstOne.idProdotto}`;
        form.append(inputElement);

        const buttonSend = document.createElement("button");
        buttonSend.type = "submit";
        buttonSend.innerText = "Visualizza il prodotto completo";
        buttonSend.className = "product-info-name-redirect";

        form.append(buttonSend);
        divOption.append(form);
    } else {
        form.querySelector("input[name='primaryKey']").value = firstOne.idProdotto;
    }


    // Add the close button if it doesn't exist
    let closeButton = divOption.querySelector(".close-button");
    if (!closeButton) {
        closeButton = document.createElement("span");
        closeButton.className = "close-button";
        let imgClose = document.createElement("img");
        imgClose.src = "Immagini/close.png";
        imgClose.alt = "close";
        imgClose.style.width = "50px";
        imgClose.style.cursor = "pointer";
        closeButton.append(imgClose);
        imgClose.onclick = function () {
            divOption.style.display = 'none';
        };
        divOption.appendChild(closeButton);
    }

    console.log(firstOne.idProdotto);


}

function updatePrice(idProdotto, flavour, weight){
    console.log(idProdotto);
    console.log(flavour);
    console.log(weight);
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
            updatePriceView(responseText);
        })
        .catch(error =>{
            console.error(error);
        })
}

function updatePriceView(response){
    let price = JSON.parse(response);
    const divCent = document.querySelector(".centered-div");

    const prezzoSpan = divCent.querySelector(".price-span");
    prezzoSpan.innerHTML = '';
    const prezzo = document.createElement("h3");
    prezzo.className = "current-price";
    prezzo.innerText = `${(price.prezzo * (1-price.sconto/100)).toFixed(2)}€`;
    prezzoSpan.appendChild(prezzo);

    if(price.sconto > 0){
        const prezzoOriginale = document.createElement("span");
        prezzoOriginale.className = "original-price";
        prezzoOriginale.innerText = `${price.prezzo}€`;
        prezzoSpan.appendChild(prezzoOriginale);
    }


}

function updateOptions(idProdotto, value) {
    let urlSearch = new URLSearchParams();
    urlSearch.append("action", "updateOptions");
    urlSearch.append("flavour", value);
    urlSearch.append("idProdotto", idProdotto);
    return fetch("showOptions?" + urlSearch.toString()) // Ensure return of promise
        .then(response => {
            if (!response.ok) throw new Error(`Network error: ${response.status} - ${response.statusText}`);
            return response.text();
        })
        .then(responseText => {
            updateWeightOptions(responseText);
        });
}
function updateWeightOptions(response){
    let weights = JSON.parse(response);

    let divCent = document.querySelector(".centered-div");
    let weightSelect = divCent.querySelector(".weight-select");
    weightSelect.innerHTML = '';

    weights.forEach(weight =>{
        const weightOption = document.createElement("option");
        weightOption.innerText = `${weight.peso}`;
        weightSelect.appendChild(weightOption);
    })
}
