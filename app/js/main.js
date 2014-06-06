(function() {
    var sayIt = function() {
        var text = document.getElementById("text").value
        var response = echo.echo(text);

        var $response = document.getElementById("response");
        $response.style.display = "block";

        var $you = $response.getElementsByClassName("you")[0];
        var $crosswalk = $response.getElementsByClassName("crosswalk")[0];

        $you.textContent = text;
        $crosswalk.textContent = response;
    }

    window.sayIt = sayIt;
})();
