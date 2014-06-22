// Event handlers
nfc.NFCManager.onpoweron = function() {
    document.getElementById("power-status").textContent = "ON";
}
nfc.NFCManager.onpoweroff = function() {
    document.getElementById("power-status").textContent = "OFF";
}

if (nfc.NFCManager.powered) {
    nfc.NFCManager.onpoweron();
} else {
    nfc.NFCManager.onpoweroff();
}
