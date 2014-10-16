'use strict';

/*global nfc */
var manager = new nfc.NFCManager();

// Event handlers
nfc.NFCManager.prototype.onpoweron = function () {
    /*global document */
    document.getElementById("power-status").textContent = "ON";
};

nfc.NFCManager.prototype.onpoweroff = function () {
    document.getElementById("power-status").textContent = "OFF";
};

nfc.NFCManager.prototype.ontagfound = function (e) {
    var tag = e.tag;
    tag.readNDEF().then(function (record) {
        document.getElementById("tnf").textContent = nfc.TNF[record.tnf];
        document.getElementById("type").textContent = record.type;

        switch (record.tnf) {
        case 0:
            console.log("Empty record");
            break;

        case 1:
            if (record.type === 'T') {
                document.getElementById('type-text').className = 'show';
                document.getElementById('text').textContent = record.text;
                document.getElementById('language-code').textContent = record.languageCode;
                document.getElementById('encoding').textContent = record.encoding;
            } else if (record.type === 'U') {
                document.getElementById('type-uri').className = 'show';
                document.getElementById('uri').textContent = record.uri;
            }
            break;
        }

        record.getPayload().then(function (payload) {
            document.getElementById("payload").textContent =
                String.fromCharCode.apply(null, payload);
        });
    });
};

if (manager.powered) {
    manager.onpoweron();
} else {
    manager.onpoweroff();
}

manager.startPoll().catch(function (e) {
    console.error(e);
});
