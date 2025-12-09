function editTableRow(nameTable, primaryKey) {
    let table = document.querySelector(".tableDB");
    table.style.display = "none";
    let urlSearch = new URLSearchParams();
    urlSearch.append("tableName", nameTable);
    urlSearch.append("primaryKey", primaryKey);

    fetch("showRowForm?" + urlSearch.toString())
        .then(response => {
            if (!response.ok) throw new Error("Network error");
            return response.json();
        })
        .then(responseData => {
            showUpdateForm(responseData[0], nameTable, primaryKey);
        })
        .catch(error => {
            console.error(error);
        });
}

function showUpdateForm(dataTable, nameTable, primaryKey) {
    const divForm = document.createElement("div");
    divForm.className = "divForm";

    // Crea il pulsante "Go Back"
    const goBackButton = document.createElement("button");
    goBackButton.innerText = "Go Back";
    goBackButton.onclick = function () {
        window.location.href = "showTable?tableName=" + nameTable;
    };
    divForm.appendChild(goBackButton);

    // Crea il form di aggiornamento
    const form = document.createElement("form");
    form.className = "formUpdate";
    form.action = "editRow";
    form.method = "POST";
    form.enctype = "multipart/form-data";

    // Aggiungi campi nascosti per nome tabella e chiave primaria
    const hiddenInput = document.createElement("input");
    hiddenInput.type = "hidden";
    hiddenInput.name = "tableName";
    hiddenInput.value = nameTable;
    form.appendChild(hiddenInput);

    const hiddenInput1 = document.createElement("input");
    hiddenInput1.type = "hidden";
    hiddenInput1.name = "primaryKey";
    hiddenInput1.value = primaryKey;
    form.appendChild(hiddenInput1);

    // Recupera i campi della tabella
    const fields = getFields(nameTable);

    // Aggiungi campi al form
    fields.forEach(field => {
        if (field.type !== "textarea" && field.type !== "password") {
            let label = document.createElement("label");
            label.htmlFor = field.id;
            label.innerText = field.label;
            form.appendChild(label);

            // Crea un elemento input per altri tipi
            let input = document.createElement("input");
            if (field.type === "file") input.type = "text";
            input.name = field.id;

            input.onfocus = function () {
                input.value = dataTable[field.id];
            };

            form.appendChild(input);
        }
    });

    // Crea il pulsante di submit
    const button = document.createElement("button");
    button.innerText = "Modifica";
    button.type = "submit";
    form.append(button);

    divForm.append(form);
    document.body.append(divForm);
}


function deleteTableRow(nameTable, primaryKey) {
    let urlSearch = new URLSearchParams();
    urlSearch.append("tableName", nameTable);
    urlSearch.append("primaryKey", primaryKey);
    if (confirm("Vuoi davvero eliminare questa riga?")) {
        fetch("deleteRow?" + urlSearch.toString())
            .then(response => {
                // --- SECURITY FIX START ---
                if (response.redirected) {
                    console.log("Response redirected");
                    
                    // Parse the new URL
                    const potentialRedirect = new URL(response.url);
                    
                    // Check if the redirect matches your current origin (Domain, Protocol, Port)
                    if (potentialRedirect.origin === window.location.origin) {
                        window.location.href = response.url;
                    } else {
                        console.error("Blocked unsafe redirect to: ", response.url);
                        // Optional: Show an error message to the user
                        alert("Security Warning: Attempted redirect to an external site blocked.");
                    }
                    return; 
                }
                // --- SECURITY FIX END ---

                if (!response.ok) throw new Error(`Network error: ${response.status} - ${response.statusText}`);
                return response.json();
            })
            .then(responseData => {
                // Check if responseData exists before using it to prevent errors if the redirect block triggered
                if(responseData) {
                    console.log(responseData);
                    updateTableView(nameTable, responseData);
                }
            })
            .catch(error => {
                console.error(error);
            });
    }
}

function updateTableView(tableName, data) {
    let table = document.querySelector(".tableDB");
    if (table.style.display === "none") table.style.display = "block";
    let rows = table.getElementsByTagName("tr");

    // Cancella tutte le righe tranne l'intestazione
    for (let i = rows.length - 1; i > 0; i--) {
        table.deleteRow(i);
    }

    if (tableName === "utente") {
        updateUserView(table, data);
    } else if (tableName === "prodotto") {
        updateProductView(table, data);
    } else if (tableName === "variante") {
        updateVarianteView(table, data);
    } else if (tableName === "ordine"){
        updateOrdiniView(table, data);
    } else if (tableName === "dettaglioOrdine"){
        updateDettaglioOrdiniView(table, data);
    } else if (tableName === "gusto"){
        updateGustoView(table, data);
    } else if (tableName === "confezione"){
        updateConfezioneView(table, data);
    }
}


function updateConfezioneView(table, data){
    data.forEach(confezione =>{
        let row = table.insertRow();
        let idConfezioneCell = row.insertCell();
        idConfezioneCell.innerText = confezione.idConfezione ?? 'undefined';
        let pesoConfezioneCell = row.insertCell();
        pesoConfezioneCell.innerText = confezione.pesoConfezione ?? 'undefined';

        let actionCell = row.insertCell();
        
        let editBtn = document.createElement("button");
        editBtn.className = "button";
        editBtn.innerText = "Modifica";
        editBtn.onclick = function() { editTableRow('confezione', confezione.idConfezione); };
        
        let deleteBtn = document.createElement("button");
        deleteBtn.className = "button";
        deleteBtn.innerText = "Elimina";
        deleteBtn.onclick = function() { deleteTableRow('confezione', confezione.idConfezione); };
        
        actionCell.appendChild(editBtn);
        actionCell.appendChild(deleteBtn);
    });
    // Aggiungi la riga del pulsante "add"
    let addRow1 = table.insertRow();
    let addCell = addRow1.insertCell();
    addCell.colSpan = 8;
    addCell.className = "center";
    let addButton = document.createElement("button");
    addButton.className = "add-button";
    addButton.innerText = "+";
    addButton.addEventListener("click", function () {
        addRow('confezione');
    });
    addCell.appendChild(addButton);
}

function updateGustoView(table, data){
    data.forEach(gusto =>{
        let row = table.insertRow();
        let idGustoCell = row.insertCell();
        idGustoCell.innerText = gusto.idGusto ?? 'undefined';
        let nomeGustoCell = row.insertCell();
        nomeGustoCell.innerText = gusto.nomeGusto  ?? 'undefined';

        let actionCell = row.insertCell();
        
        let editBtn = document.createElement("button");
        editBtn.className = "button";
        editBtn.innerText = "Modifica";
        editBtn.onclick = function() { editTableRow('gusto', gusto.idGusto); };
        
        let deleteBtn = document.createElement("button");
        deleteBtn.className = "button";
        deleteBtn.innerText = "Elimina";
        deleteBtn.onclick = function() { deleteTableRow('gusto', gusto.idGusto); };
        
        actionCell.appendChild(editBtn);
        actionCell.appendChild(deleteBtn);
    });

    // Aggiungi la riga del pulsante "add"
    let addRow1 = table.insertRow();
    let addCell = addRow1.insertCell();
    addCell.colSpan = 8;
    addCell.className = "center";
    let addButton = document.createElement("button");
    addButton.className = "add-button";
    addButton.innerText = "+";
    addButton.addEventListener("click", function () {
        addRow('gusto');
    });
    addCell.appendChild(addButton);
}

function updateDettaglioOrdiniView(table, data){
    data.forEach(dettaglioOrdine =>{
        let row = table.insertRow();
        let idOrdineCell = row.insertCell();
        idOrdineCell.innerText = dettaglioOrdine.idOrdine ?? 'undefined';
        let idProdottoCell = row.insertCell();
        idProdottoCell.innerText = dettaglioOrdine.idProdotto ?? 'undefined';
        let idVarianteCell = row.insertCell();
        idVarianteCell.innerText = dettaglioOrdine.idVariante ?? 'undefined';
        let quantityCell = row.insertCell();
        quantityCell.innerText = dettaglioOrdine.quantity ?? 'undefined';
        let prezzoCell = row.insertCell();
        prezzoCell.innerText = dettaglioOrdine.prezzo ?? 'undefined';

        let actionCell = row.insertCell();
        let primaryKey = dettaglioOrdine.idOrdine + ", " + dettaglioOrdine.idProdotto + ", " + dettaglioOrdine.idVariante;
        
        let editBtn = document.createElement("button");
        editBtn.className = "button";
        editBtn.innerText = "Modifica";
        editBtn.onclick = function() { editTableRow('dettaglioOrdine', primaryKey); };
        
        let deleteBtn = document.createElement("button");
        deleteBtn.className = "button";
        deleteBtn.innerText = "Elimina";
        deleteBtn.onclick = function() { deleteTableRow('dettaglioOrdine', primaryKey); };
        
        actionCell.appendChild(editBtn);
        actionCell.appendChild(deleteBtn);
    });

    // Aggiungi la riga del pulsante "add"
    let addRow1 = table.insertRow();
    let addCell = addRow1.insertCell();
    addCell.colSpan = 8;
    addCell.className = "center";
    let addButton = document.createElement("button");
    addButton.className = "add-button";
    addButton.innerText = "+";
    addButton.addEventListener("click", function () {
        addRow('dettaglioOrdine');
    });
    addCell.appendChild(addButton);
}



function updateOrdiniView(table, data) {
    data.forEach(ordine => {
        let row = table.insertRow();
        let idOrdineCell = row.insertCell();
        idOrdineCell.innerText = ordine.idOrdine ?? 'undefined';
        let emailUtenteCell = row.insertCell();
        emailUtenteCell.innerText = ordine.emailUtente ?? 'undefined';
        let statoCell = row.insertCell();
        statoCell.innerText = ordine.stato ?? 'undefined';
        let dataCell = row.insertCell();
        dataCell.innerText = ordine.data ?? 'undefined';
        let totaleCell = row.insertCell();
        totaleCell.innerText = ordine.totale ?? 'undefined';
        let descrizioneCell = row.insertCell();
        if (ordine.descrizione) {
            descrizioneCell.innerText = ordine.descrizione;
        } else {
            let link = document.createElement("a");
            link.href = "showTable?tableName=dettaglioOrdine";
            link.innerText = "Dettaglio ordine";
            descrizioneCell.appendChild(link);
        }

        let actionCell = row.insertCell();
        
        let editBtn = document.createElement("button");
        editBtn.className = "button";
        editBtn.innerText = "Modifica";
        editBtn.onclick = function() { editTableRow('ordine', ordine.idOrdine); };
        
        let deleteBtn = document.createElement("button");
        deleteBtn.className = "button";
        deleteBtn.innerText = "Elimina";
        deleteBtn.onclick = function() { deleteTableRow('ordine', ordine.idOrdine); };
        
        actionCell.appendChild(editBtn);
        actionCell.appendChild(deleteBtn);
    });

    // Aggiungi la riga del pulsante "add"
    let addRow1 = table.insertRow();
    let addCell = addRow1.insertCell();
    addCell.colSpan = 8;
    addCell.className = "center";
    let addButton = document.createElement("button");
    addButton.className = "add-button";
    addButton.innerText = "+";
    addButton.addEventListener("click", function () {
        addRow('ordine');
    });
    addCell.appendChild(addButton);
}

function updateVarianteView(table, data) {
    data.forEach(variante => {
        let row = table.insertRow();
        let idVarianteCell = row.insertCell();
        idVarianteCell.innerText = variante.idVariante ?? 'undefined';
        let idProdottoVarianteCell = row.insertCell();
        idProdottoVarianteCell.innerText = variante.idProdottoVariante ?? 'undefined';
        let idGustoCell = row.insertCell();
        idGustoCell.innerText = variante.idGusto ?? 'undefined';
        let idConfezioneCell = row.insertCell();
        idConfezioneCell.innerText = variante.idConfezione ?? 'undefined';
        let prezzoCell = row.insertCell();
        prezzoCell.innerText = variante.prezzo ?? 'undefined';
        let quantityCell = row.insertCell();
        quantityCell.innerText = variante.quantity ?? 'undefined';
        let scontoCell = row.insertCell();
        scontoCell.innerText = variante.sconto ?? 'undefined';
        let evidenzaCell = row.insertCell();
        evidenzaCell.innerText = variante.evidenza ?? 'undefined';
        let actionCell = row.insertCell();
        
        let editBtn = document.createElement("button");
        editBtn.className = "button";
        editBtn.innerText = "Modifica";
        editBtn.onclick = function() { editTableRow('variante', variante.idVariante); };
        
        let deleteBtn = document.createElement("button");
        deleteBtn.className = "button";
        deleteBtn.innerText = "Elimina";
        deleteBtn.onclick = function() { deleteTableRow('variante', variante.idVariante); };
        
        actionCell.appendChild(editBtn);
        actionCell.appendChild(deleteBtn);
    });

    // Aggiungi la riga del pulsante "add" indietro
    let addRow1 = table.insertRow();
    let addCell = addRow1.insertCell();
    addCell.colSpan = 8;
    addCell.className = "center";
    let addButton = document.createElement("button");
    addButton.className = "add-button";
    addButton.innerText = "+";
    addButton.addEventListener("click", function () {
        addRow('variante');
    });
    addCell.appendChild(addButton);
}

function updateProductView(table, data) {
    data.forEach(product => {
        let row = table.insertRow();
        let idProductCell = row.insertCell();
        idProductCell.innerText = product.idProdotto ?? 'undefined';
        let nomeCell = row.insertCell();
        nomeCell.innerText = product.nome ?? 'undefined';
        let categoriaCell = row.insertCell();
        categoriaCell.innerText = product.categoria ?? 'undefined';
        let immmagineCell = row.insertCell();
        immmagineCell.innerText = product.immagine ?? 'undefined';
        let calorieCell = row.insertCell();
        calorieCell.innerText = product.calorie ?? 'undefined';
        let carboidratiCell = row.insertCell();
        carboidratiCell.innerText = product.carboidrati ?? 'undefined';
        let proteineCell = row.insertCell();
        proteineCell.innerText = product.proteine ?? 'undefined';
        let grassiCell = row.insertCell();
        grassiCell.innerText = product.grassi ?? 'undefined';
        let actionCell = row.insertCell();
        actionCell.className = "center";
        
        let editBtn = document.createElement("button");
        editBtn.className = "button";
        editBtn.innerText = "Modifica";
        editBtn.onclick = function() { editTableRow('prodotto', product.idProdotto); };
        
        let deleteBtn = document.createElement("button");
        deleteBtn.className = "button";
        deleteBtn.innerText = "Elimina";
        deleteBtn.onclick = function() { deleteTableRow('prodotto', product.idProdotto); };
        
        actionCell.appendChild(editBtn);
        actionCell.appendChild(deleteBtn);
    });

    // Aggiungi la riga del pulsante "add" indietro
    let addRow1 = table.insertRow();
    let addCell = addRow1.insertCell();
    addCell.colSpan = 8;
    addCell.className = "center";
    let addButton = document.createElement("button");
    addButton.className = "add-button";
    addButton.innerText = "+";
    addButton.addEventListener("click", function () {
        addRow('prodotto');
    });
    addCell.appendChild(addButton);
}

function updateUserView(table, data) {
    data.forEach(user => {
        let row = table.insertRow();
        let emailCell = row.insertCell();
        emailCell.innerText = user.email || 'undefined';
        let nomeCell = row.insertCell();
        nomeCell.innerText = user.nome || 'undefined';
        let cognomeCell = row.insertCell();
        cognomeCell.innerText = user.cognome || 'undefined';
        let codiceFiscaleCell = row.insertCell();
        codiceFiscaleCell.innerText = user.codiceFiscale || 'undefined';
        let dataDiNascitaCell = row.insertCell();
        dataDiNascitaCell.innerText = user.dataDiNascita || 'undefined';
        let indirizzoCell = row.insertCell();
        indirizzoCell.innerText = user.indirizzo || 'undefined';
        let telefonoCell = row.insertCell();
        telefonoCell.innerText = user.telefono || 'undefined';
        let actionCell = row.insertCell();
        actionCell.className = "center";
        
        let editBtn = document.createElement("button");
        editBtn.className = "button";
        editBtn.innerText = "Modifica";
        editBtn.onclick = function() { editTableRow('utente', user.email); };
        
        let deleteBtn = document.createElement("button");
        deleteBtn.className = "button";
        deleteBtn.innerText = "Elimina";
        deleteBtn.onclick = function() { deleteTableRow('utente', user.email); };
        
        actionCell.appendChild(editBtn);
        actionCell.appendChild(deleteBtn);
    });

    // Aggiungi la riga del pulsante "add" indietro
    let addRow1 = table.insertRow();
    let addCell = addRow1.insertCell();
    addCell.colSpan = 8;
    addCell.className = "center";
    let addButton = document.createElement("button");
    addButton.className = "add-button";
    addButton.innerText = "+";
    addButton.addEventListener("click", function () {
        addRow('utente');
    });
    addCell.appendChild(addButton);
}

function addRow(tableName) {
    let table = document.querySelector(".tableDB");
    if (table.style.display !== "none") table.style.display = "none";

    const divForm = document.createElement("div");
    divForm.className = "divForm";

    let form = document.createElement("form");
    form.className = "formUpdate";
    form.action = "insertRow";
    form.method = "POST";
    form.enctype = "multipart/form-data";


    // Crea il pulsante "Go Back"
    const goBackButton = document.createElement("button");
    goBackButton.innerText = "Go Back";
    goBackButton.onclick = function () {
        window.location.href = "showTable?tableName=" + tableName;
    };
    divForm.appendChild(goBackButton);

    const nameTable = document.createElement("input");
    nameTable.type = "hidden";
    nameTable.name = "nameTable";
    nameTable.value = tableName;
    form.appendChild(nameTable);

    let fields = getFields(tableName);

    fields.forEach(field => {
        if (!field.label.includes("auto")) {
            let label = document.createElement("label");
            label.htmlFor = field.id;
            label.innerText = field.label;

            let input;
            if (field.type !== "textarea"){
                input = document.createElement("input");
                input.type = field.type;
                input.id = field.id;
                input.name = field.id;
                if(tableName === "ordine" && field.id === "emailUtente") input.required = true;

                form.appendChild(label);
                form.appendChild(input);
            }


        }});

    let button = document.createElement("button");
    button.type = "submit";
    button.innerText = "Aggiungi";

    form.appendChild(button);
    divForm.appendChild(form);
    document.body.append(divForm);
}

function getFields(tableName) {
    const fields = {
        utente: [
            { label: "Email (PK) - varchar(100)", type: "email", id: "email" },
            { label: "Password - varchar(63)", type: "password", id: "password" },
            { label: "Nome - varchar(40)", type: "text", id: "nome" },
            { label: "Cognome - varchar(40)", type: "text", id: "cognome" },
            { label: "Codice Fiscale - varchar(16)", type: "text", id: "codiceFiscale" },
            { label: "Data di Nascita - date", type: "date", id: "dataDiNascita" },
            { label: "Indirizzo - varchar(255)", type: "text", id: "indirizzo" },
            { label: "Numero di Cellulare - varchar(20)", type: "tel", id: "telefono" }
        ],
        prodotto: [
            { label: "Id Prodotto (PK) - varchar(30)", type: "text", id: "idProdotto" },
            { label: "Nome - varchar(100)", type: "text", id: "nome" },
            { label: "Descrizione - varchar(3000)", type: "text", id: "descrizione" },
            { label: "Categoria - varchar(50)", type: "text", id: "categoria" },
            { label: "Immagine - varchar(2083)", type: "file", id: "immagine" },
            { label: "Calorie - int", type: "number", id: "calorie" },
            { label: "Carboidrati - int", type: "number", id: "carboidrati" },
            { label: "Proteine - int", type: "number", id: "proteine" },
            { label: "Grassi - int", type: "number", id: "grassi" }
        ],
        variante: [
            { label: "Id Variante (PK) int auto-increment", type: "number", id: "idVariante" },
            { label: "Id Prodotto Variante (FK) - varchar(30)", type: "text", id: "idProdottoVariante" },
            { label: "Id Gusto (FK) - int", type: "number", id: "idGusto" },
            { label: "Id Confezione (FK) - int", type: "number", id: "idConfezione" },
            { label: "Prezzo (float)", type: "text", id: "prezzo" },
            { label: "Quantità (int)", type: "number", id: "quantity" },
            { label: "Sconto (int)", type: "number", id: "sconto" },
            { label: "Evidenza (tinyint(1))", type: "number", id: "evidenza" }
        ],
        ordine: [
            { label: "Id Ordine (PK) int auto-increment", type: "number", id: "idOrdine" },
            { label: "Email Utente (FK) - (varchar 100)", type: "text", id: "emailUtente" },
            { label: "Stato - (varchar 255)", type: "text", id: "stato" },
            { label: "Data - (date)", type: "date", id: "data" },
            { label: "Totale - (float)", type: "text", id: "totale" },
            { label: "Descrizione - (text)", type: "textarea", id: "descrizione"}
        ],
        dettaglioOrdine: [
            { label: "Id Ordine (PK, FK), int", type: "number", id: "idOrdine"},
            { label: "Id prodotto (PK, FK), int", type: "text", id: "idProdotto"},
            { label: "Id Variante (PK, FK), int", type: "number", id: "idVariante"},
            { label: "Quantità, int", type: "number", id:"quantity"},
        ],
        gusto:[
            { label: "Id Gusto (PK) int auto-increment", type: "number", id: "idGusto"},
            { label: "Nome gusto, varchar (50)", type: "text", id: "nomeGusto"}
        ],
        confezione:[
            { label: "Id Confezione (PK) int auto-increment", type: "number", id: "idConfezione"},
            { label: "Peso confezione, int", type: "number", id: "pesoConfezione"}
        ]
    };

    return fields[tableName] || [];
}
