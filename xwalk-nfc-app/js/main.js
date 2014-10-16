'use strict';

/*global nfc */
var manager = new nfc.NFCManager(),
    currentTag = null;

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
    currentTag = tag;
    tag.readNDEF().then(function (record) {
        document.getElementById('tag').className = 'show';

        document.getElementById("tnf").textContent = nfc.TNF[record.tnf];
        document.getElementById("type").textContent = record.type;

        switch (record.tnf) {
        case 0:
            console.log("Empty record");
            break;

        case 1:
            if (record.type.toLowerCase() === 't') {
                document.getElementById('type-text').className = 'show';
                document.getElementById('text').textContent = record.text;
                document.getElementById('language-code').textContent = record.languageCode;
                document.getElementById('encoding').textContent = record.encoding;
            } else if (record.type.toLowerCase() === 'u') {
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


function writeFormSubmit() {
    var type = document.getElementsByName('write-form-type')[0].value,
        content = document.getElementsByName('write-form-content')[0].value,
        record, message;

    if (type === "Text") {
        record = new nfc.NDEFRecordText(content, "en-US", "UTF-8");
        message = new nfc.NDEFMessage([record], currentTag._uuid);
        currentTag.writeNDEF(message);
    }

    return false;
}
