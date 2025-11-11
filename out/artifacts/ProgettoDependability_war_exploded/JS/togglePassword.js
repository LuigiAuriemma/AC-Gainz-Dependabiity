function togglePassword(idInput, idImg){
        let passwordField = document.getElementById(idInput);
        let imgPass = document.getElementById(idImg);
        if(passwordField.type === "password"){
            passwordField.type = "text";
            imgPass.src = "Immagini/visible.png"
        }else {
            passwordField.type = "password";
            imgPass.src = "Immagini/hide.png";
        }
}

function showPasswordRequirements(show) {
    const requirementsBox = document.getElementById('password-requirements');
    if (show) {
        requirementsBox.style.display = 'block';
    } else {
        requirementsBox.style.display = 'none';
    }
}