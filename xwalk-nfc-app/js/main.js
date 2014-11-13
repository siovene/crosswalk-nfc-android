/*jslint todo: true, nomen: true */

(function (win) {
    'use strict';

    // Trigger loading of extension, because the navigator.nfc entry point
    // is currently not possible.
    // See: https://crosswalk-project.org/jira/browse/XWALK-2840
    /*global console, nfc */
    console.log(nfc.NFCManager);

    /*global navigator */
    var manager = navigator.nfc,
        currentTag = null;

    function isImage(type) {
        var t = type.toLowerCase();
        return (
            t === 'image/jpg' ||
            t === 'image/jpeg' ||
            t === 'image/png' ||
            t === 'image/gif'
        );
    }

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

            document.getElementById("tnf").textContent = navigator.nfc.TNF[record.tnf];
            document.getElementById("type").textContent = record.type;

            switch (record.tnf) {
            // Empty
            case 0:
                /*global console */
                console.log("Empty record");
                break;

            // Well-known
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

            // Media-type
            case 2:
                if (isImage(record.type)) {
                    document.getElementById('type-image').className = 'show';
                }
                break;
            }

            record.getPayload().then(function (payload) {
                var encoded;

                document.getElementById("payload").textContent = payload;
                encoded = win.btoa(payload);

                if (isImage(record.type)) {
                    document.getElementById('image').src =
                        'data:' + record.type + ';base64,' + encoded;
                }
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

        document.getElementById('type-image').className = 'hide';
        document.getElementById('image').src = '';
    }

    function writeTag(record) {
        var message = new navigator.nfc.NDEFMessage([record], currentTag._uuid);
        currentTag.writeNDEF(message).then(function () {
            reset();
        });
    }

    function writeFormSubmit() {
        var type = document.getElementsByName('write-form-type')[0].value,
            content = document.getElementsByName('write-form-content')[0].value,
            media = document.getElementsByName('write-form-content-file')[0].files[0],
            reader;

        if (type === "Text") {
            writeTag(new navigator.nfc.NDEFRecordText(content, "en-US", "UTF-8"));
        } else if (type === "URI") {
            writeTag(new navigator.nfc.NDEFRecordURI(content));
        } else if (type === "File") {
            /*global FileReader */
            reader = new FileReader();
            reader.onload = function (e) {
                /*global Uint8Array */
                var s = String.fromCharCode.apply(null, new Uint8Array(e.target.result));
                writeTag(new navigator.nfc.NDEFRecordMedia(media.type, s));
            };

            reader.readAsArrayBuffer(media);
        }

        return false;
    }

    win.writeFormSubmit = writeFormSubmit;

    /*global window*/
}(window));
