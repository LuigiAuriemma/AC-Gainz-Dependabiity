function validateForm(){
    let email = document.getElementById("email").value;// Ottenere il valore dell'input email
    console.log(email);

    let password = document.getElementById("password").value;

    if(document.getElementById("codiceFiscale")){
        let cf = document.getElementById("codiceFiscale").value;
    }

    if(document.getElementById("numCellulare")) {
        let numCellulare = document.getElementById("numCellulare").value;
    }



    let emailPattern =/^[\w.%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,8}$/;


    let passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,}$/;



    let cfPattern = /^[A-Z]{6}\d{2}[A-Z]\d{2}[A-Z]\d{3}[A-Z]$/;


    let cellularPattern = /^3\d{8,9}$/;



    if(!emailPattern.test(email)){

        alert("Inserisci un'email valida");
        return false;
    }
    if(!passwordPattern.test(password)){
        alert("Inserisci una password valida")
        return false;
    }

    if(cf) {
        if (!cfPattern.test(cf)) {
            alert("Inserisci un codice fiscale valido");
            return false;
        }
    }


    if(numCellulare) {
        if (!cellularPattern.test(numCellulare)) {
            alert("Inserisci un numero di cellulare valido: 3xxxxxxx o 3xxxxxxxx");
            return false;
        }
    }
    return true;
}