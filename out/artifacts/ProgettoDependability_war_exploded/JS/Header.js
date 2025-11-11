window.onload = function() {
    if (Logged === true){
        document.getElementById("utente").innerHTML = `
                    <form action="areaUtenteServlet" method="post">
                        <button type="submit" class="header-custom_button">Area Personale</button>
                    </form>
                `;
    }
};



/* quando viene cliccato il bottone, si apre e si toglie il contenuto del dropdown */
function myFunction() {
    document.getElementById("myDropdown").classList.toggle("header-show"); //header-show ha come style display:block
}
// chiudi il dropdown se l'utente clicca all'esterno del riquadro
window.onclick = function(event) {
    if (!event.target.matches('.header-dropbutton')) {
        let dropdowns = document.getElementsByClassName("header-dropdown-content");
        for (let i = 0; i < dropdowns.length; i++) {
            let openDropdown = dropdowns[i];
            if (openDropdown.classList.contains('header-show')) {
                openDropdown.classList.remove('header-show');
            }
        }
    }
}





function toggleMenu() {
    let menu = document.getElementById("lista");
    menu.classList.toggle("header-showlista"); //display:block
}


