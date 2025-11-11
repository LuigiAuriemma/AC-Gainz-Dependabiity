// Script per le tab laterali
function opentab(button, pageName) {
    // Nascondere tutte le tab e rimuovere la classe active dai bottoni delle tab
    document.querySelectorAll('.tabcontent').forEach(tab => tab.style.display = 'none');
    document.querySelectorAll('.tablinks').forEach(tab => tab.classList.remove('active'));

    // Mostrare la tab selezionata
    document.getElementById(pageName).style.display = 'block';

    // Aggiungere la classe active al bottone della tab selezionata
    button.classList.add('active');
}


function displayAll(idx){
    let x = document.getElementById(idx);

    if(x.style.display === "none") x.style.display = "block";
    else x.style.display = "none";
}


function toggleDetails(orderId) {
    let detailsDiv = document.getElementById('dettagli-' + orderId);
    if (detailsDiv.style.display === 'none') {
        detailsDiv.style.display = 'block';
    } else {
        detailsDiv.style.display = 'none';
    }
}
