document.addEventListener("DOMContentLoaded", function () {
    const input = document.getElementById("search-input");

    input.addEventListener("input", function () {
        const inputValue = this.value;
        if (window.location.pathname.includes("categories"))
            searchBar(inputValue);
    });


    //non ajax
    input.addEventListener("keypress", function (e) {
        if (e.key === 'Enter') {
            const inputValue = this.value;
            const form = document.createElement("form");

            form.action = "genericFilter";
            form.method = "GET";
            const hiddenField = document.createElement("input");
            hiddenField.type = "hidden";
            hiddenField.name = "nameForm";
            hiddenField.value = inputValue;
            form.appendChild(hiddenField);
            document.body.appendChild(form); // Append the form to the body
            form.submit();
        }
    });
});

function searchBar(inputValue) {
    const urlParams = new URLSearchParams();
    urlParams.append("name", inputValue);
    console.log("searching with ajax by searchBar input");

    fetch("searchBar?" + urlParams.toString())
        .then(response => {
            if (!response.ok)
                throw new Error();

            return response.text();
        })
        .then(responseText => {
            updateView(responseText); //chiamata a funzione in genericFilter.js
        })
        .catch(error => {
            console.error(error);
        });
}


