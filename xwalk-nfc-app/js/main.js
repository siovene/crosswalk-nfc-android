// Event handlers
var manager = new nfc.NFCManager();

nfc.NFCManager.prototype.onpoweron = function() {
    document.getElementById("power-status").textContent = "ON";
}

nfc.NFCManager.prototype.onpoweroff = function() {
    document.getElementById("power-status").textContent = "OFF";
}

if (manager.powered) {
    manager.onpoweron();
} else {
    manager.onpoweroff();
}
