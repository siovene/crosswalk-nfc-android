// Event handlers
var manager = new nfc.NFCManager();

nfc.NFCManager.prototype.onpoweron = function() {
    document.getElementById("power-status").textContent = "ON";
}

nfc.NFCManager.prototype.onpoweroff = function() {
    document.getElementById("power-status").textContent = "OFF";
}

nfc.NFCManager.prototype.ontagfound = function(e) {
    var tag = e.tag;
    tag.readNDEF().then(function(record) {
        record.getPayload().then(function(payload) {
            alert("Tag content: " + String.fromCharCode.apply(null, payload));
        });
    });
}

if (manager.powered) {
    manager.onpoweron();
} else {
    manager.onpoweroff();
}

manager.startPoll().catch(function(e) {
    console.error(e);
});
