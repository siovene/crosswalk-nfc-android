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
    tag.readNDEF().then(function (record) {
        record.getPayload().then(function (payload) {
            /*global alert */
            alert("Record TNF: " + nfc.TNF[record.tnf] + "\n" +
                  "Record type: " + record.type + "\n" +
                  "Record id: " + record.id + "\n" +
                  "Record payload: " + String.fromCharCode.apply(null, payload));

            switch (record.tnf) {
            case 0:
                console.log("Empty record");
                break;

            case 1:
                if (record.type === 'T') {
                    console.log("Record text: " + record.text);
                    console.log("Record language code: " + record.languageCode);
                    console.log("Record encoding: " + record.enconding);
                } else if (record.type === 'U') {
                    console.log("Record URI: " + record.uri);
                }
                break;
            }
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
