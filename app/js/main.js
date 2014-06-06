(function() {
    var sayIt = function() {
        value = document.forms[0]["text"].value

        alert(echo.echo(value));
    }

    window.sayIt = sayIt;
})();
