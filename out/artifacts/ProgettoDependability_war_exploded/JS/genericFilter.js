function genericFilter() {
    console.log("Calling genericFilter");

    const taste = document.getElementById("tastes").value;
    const sorting = document.getElementById("sorting").value;
    const weight = document.getElementById("weights").value;


    const params = new URLSearchParams();


    if (taste) params.append("taste", taste);
    if (sorting) params.append("sorting", sorting);
    if (weight) params.append("weight", weight);

    const urlServlet = `genericFilter?${params.toString()}`;


    fetch(urlServlet)
        .then(response => {
            if (!response.ok) throw new Error();

            return response.text();
        })
        .then(responseText => {
            updateView(responseText);
        })
        .catch(error => {
            console.log(error);
        })

    /*console.log("Calling genericFilter with URL: " + urlServlet);*/
}



 function updateView(response) {
    const group = document.querySelector("#gr");
    group.innerHTML = "";

    let products;

     try {
         products = JSON.parse(response);
     } catch (e) {
         console.error("Error parsing JSON response:", e);
         return;
     }


    products.forEach(prodottoFiltrato => {
        const divProductCard = document.createElement("div");
        divProductCard.className = "product-card";

        const divProductImage = document.createElement("div");
        divProductImage.className = "product-image";

        if (prodottoFiltrato.sconto > 0) {
            const spanSconto = document.createElement("span");
            spanSconto.className = "product-sconto";
            spanSconto.innerText = `${prodottoFiltrato.sconto}% di Sconto`;
            divProductImage.appendChild(spanSconto);
        }

        const form = document.createElement("form");
        form.action = "ProductInfo";
        form.method = "POST";
        form.id=`${prodottoFiltrato.id}`;   //${} template strings
        const input1 = document.createElement("input");
        input1.type = "hidden";
        input1.name = "primaryKey";
        input1.value = `${prodottoFiltrato.id}`;
        form.appendChild(input1);
        divProductImage.appendChild(form);
        const img = document.createElement("img");
        img.src = prodottoFiltrato.immagine;
        img.alt = prodottoFiltrato.nome;
        img.onclick = function (){
            form.submit();
        };
        divProductImage.appendChild(img);
        divProductCard.appendChild(divProductImage);


        const divProductInfo = document.createElement("div");
        divProductInfo.className = "product-info";

        const h2ProductName = document.createElement("h2");
        h2ProductName.className = "product-info-name";
        h2ProductName.innerText= `${prodottoFiltrato.nome}`
        divProductInfo.appendChild(h2ProductName);

        if (prodottoFiltrato.sconto > 0) {
            const spanPriceOff = document.createElement("span");
            spanPriceOff.className = "product-info-price-off";
            const prezzoScontato = prodottoFiltrato.prezzo * (1 - (prodottoFiltrato.sconto / 100));
            spanPriceOff.innerText = `${prezzoScontato.toFixed(2)}€`;
            divProductInfo.appendChild(spanPriceOff);

            const spanPrice = document.createElement("span");
            spanPrice.className = "product-info-price";
            spanPrice.innerText = `${prodottoFiltrato.prezzo.toFixed(2)}€`;
            divProductInfo.appendChild(spanPrice);
        } else {
            const spanPriceOff = document.createElement("span");
            spanPriceOff.className = "product-info-price-off";
            spanPriceOff.innerText = `${prodottoFiltrato.prezzo.toFixed(2)}€`;
            divProductInfo.appendChild(spanPriceOff);
        }

        const spanFlavour = document.createElement("span");
        spanFlavour.className = "product-info-flavour";
        spanFlavour.innerText = prodottoFiltrato.gusto;
        divProductInfo.appendChild(spanFlavour);

        divProductCard.appendChild(divProductInfo);

        const button = document.createElement("button");
        button.innerHTML = "Aggiungi al carrello";
        button.className = "cartAdd";

        button.onclick = function (){
            optionsVarianti(`${prodottoFiltrato.idVariante}`);
        }

        divProductCard.appendChild(button);

        group.appendChild(divProductCard);
    });
}



function resetProducts() {
    /*console.log("Resetting products...");*/
    const selects = document.querySelectorAll('select');
    selects.forEach(select => {
        select.value = '';
    });
    genericFilter();
}
















