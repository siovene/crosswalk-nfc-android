/*jslint todo: true, nomen: true */

(function (win) {
    'use strict';

    /*global nfc */
    var manager = new nfc.NFCManager(),
        currentTag = null;

    // Event handlers

    function onpoweron() {
        /*global document */
        document.getElementById("power-status").textContent = "ON";
    }

    function onpoweroff() {
        document.getElementById("power-status").textContent = "OFF";
    }

    function ontagfound(tag) {
        currentTag = tag;
        tag.readNDEF().then(function (message) {
            var record = message.records[0];

            document.getElementById('tag').className = 'show';

            document.getElementById("tnf").textContent = nfc.TNF[record.tnf];
            document.getElementById("type").textContent = record.type;

            switch (record.tnf) {
            case 0:
                /*global console */
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
                document.getElementById("payload").textContent = payload;
            });
        });

    }

    document.addEventListener('onpoweron', function () { onpoweron(); });
    document.addEventListener('onpoweroff', function () { onpoweroff(); });
    document.addEventListener('ontagfound', function (e) { ontagfound(e.detail); });


    if (manager.powered) {
        onpoweron();
    } else {
        onpoweroff();
    }

    manager.startPoll().catch(function (e) {
        console.error(e);
    });


    function reset() {
        document.getElementById('tag').className = 'hide';

        document.getElementById("tnf").textContent = '...';
        document.getElementById("type").textContent = '...';
        document.getElementById("payload").textContent = '...';

        document.getElementById('type-text').className = 'hide';
        document.getElementById('text').textContent = '...';
        document.getElementById('language-code').textContent = '...';
        document.getElementById('encoding').textContent = '...';

        document.getElementById('type-uri').className = 'hide';
        document.getElementById('uri').textContent = '...';
    }

    function writeFormSubmit() {
        var type = document.getElementsByName('write-form-type')[0].value,
            content = document.getElementsByName('write-form-content')[0].value,
            record,
            message;

        if (type === "Text") {
            record = new nfc.NDEFRecordText(content, "en-US", "UTF-8");
        } else if (type === "URI") {
            record = new nfc.NDEFRecordURI(content);
        }

        message = new nfc.NDEFMessage([record], currentTag._uuid);
        currentTag.writeNDEF(message).then(function () {
            reset();
        });

        return false;
    }

    win.writeFormSubmit = writeFormSubmit;
    /*global window*/
}(window));
