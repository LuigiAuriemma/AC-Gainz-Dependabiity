function showTastes() {
    fetch("showTastes")
        .then(response => {
            if (!response.ok)
                throw new Error();
            return response.text();
        })
        .then(responseText => {
            updateSelectView(responseText);
        })
        .catch(error => {
            console.error(error);
        });
}



function updateSelectView(response) {
    const select = document.querySelector("#tastes");
    const currentSelection = select.value;

    const gusti = JSON.parse(response);

    select.innerHTML = "<option value=''>Seleziona un gusto</option>"; // Default option

    gusti.forEach(gusto => {
        const option = document.createElement("option");
        option.value = gusto;
        option.innerHTML = gusto;
        select.appendChild(option);
    });

    // Ripristina il valore selezionato, se esiste
    if (currentSelection) {
        select.value = currentSelection;
    }

    // Chiama genericFilter quando cambia la selezione
    select.addEventListener('change', function() {
        genericFilter(select.value);
    });
}

document.addEventListener("DOMContentLoaded", function() {
    showTastes();
});
