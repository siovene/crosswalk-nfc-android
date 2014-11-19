/*jslint todo: true, nomen: true, plusplus: true */
/*global console */

(function () {
    'use strict';

    var _callbacks = {},
        _next_response_id = 0,
        _messageToJson = function (id, content, args) {
            var obj = {
                id: id.toString(),
                content: content,
                args: args
            };

            return JSON.stringify(obj);
        },

        UUID = function () {
            function s4() {
                return Math.floor((1 + Math.random()) * 0x10000)
                    .toString(16)
                    .substring(1);
            }

            return function () {
                return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
                       s4() + '-' + s4() + s4() + s4();
            };
        },

    // =============================================================================
    // Object definitions
    // =============================================================================

        TNF = [
            "Empty",
            "Well-known",
            "Media-type",
            "AbsoluteURI",
            "External",
            "Unknown",
            "Unchanged",
            "Reserved"
        ],

        utils = {
            tnfCode: function (s) {
                var i;

                for (i = 0; i < TNF.length; i++) {
                    if (TNF[i] === s) {
                        return i;
                    }
                }

                return -1;
            }
        };


    function NDEFRecord(tnf, type, id, _uuid) {
        this.tnf = tnf;
        this.type = type;
        this.id = id;
        this._uuid = _uuid;

        if (this._uuid === undefined || this._uuid === null) {
            this.__uuid = UUID();
        }

        this.getPayload = function () {
            _next_response_id += 1;
            var message = _messageToJson(_next_response_id, "nfc_ndefrecord_get_payload", this._uuid);

            /*global Promise */
            return new Promise(function (resolve, reject) {
                try {
                    /*global extension */
                    var response = extension.internal.sendSyncMessage(message),
                        responseJson = JSON.parse(response),
                        payload = JSON.parse(responseJson.args);

                    resolve(payload);
                } catch (e) {
                    console.error(e);
                    reject(e);
                }
            });
        };
    }


    function NDEFRecordEmpty(id, _uuid) {
        NDEFRecord.call(this, utils.tnfCode("Empty"), [], id, _uuid);

        this.getPayload = function () {
            return new Promise(function (resolve) {
                resolve(null);
            });
        };
    }


    function NDEFRecordText(text, languageCode, encoding, _uuid) {
        NDEFRecord.call(this, utils.tnfCode("Well-known"), 'T', null, _uuid);

        this.text = text;
        this.languageCode = languageCode;
        this.encoding = encoding;
    }


    function NDEFRecordURI(uri, _uuid) {
        NDEFRecord.call(this, utils.tnfCode("Well-known"), 'U', null, _uuid);

        this.uri = uri;
    }


    function NDEFRecordMedia(type, content, _uuid) {
        NDEFRecord.call(this, utils.tnfCode("Media-type"), type, null, _uuid);

        this.content = content;
    }

    function NDEFRecordExternal(type, payload, _uuid) {
        NDEFRecord.call(this, utils.tnfCode("External"), type, null, _uuid);

        this.payload = payload;
    }


    function NDEFMessage(records, _uuid) {
        this.records = records;
        this._uuid = _uuid;

        this.getBytes = function () {
            // TODO
            return;
        };
    }


    function NDEFMessageEvent() {
        this.message = null;
    }


    function NFCTag(_uuid) {
        var tag = this;
        this._uuid = _uuid;

        this.readNDEF = function () {
            _next_response_id += 1;
            var message = _messageToJson(_next_response_id, "nfc_tag_read_ndef", this._uuid);

            return new Promise(function (resolve, reject) {
                try {
                    var response = extension.internal.sendSyncMessage(message),
                        responseJson = JSON.parse(response),
                        argsJson = JSON.parse(responseJson.args),
                        recordsJson = argsJson.records,
                        recordJson,
                        record = null,
                        records = [],
                        i;

                    for (i = 0; i < recordsJson.length; i++) {
                        recordJson = recordsJson[i];

                        switch (recordJson.tnf) {
                        case utils.tnfCode("Empty"):
                            record = new NDEFRecordEmpty(recordJson.id, tag._uuid);
                            break;

                        case utils.tnfCode("Well-known"):
                            if (recordJson.type.toLowerCase() === 't') {
                                record = new NDEFRecordText(
                                    recordJson.text,
                                    recordJson.languageCode,
                                    recordJson.encoding,
                                    tag._uuid
                                );
                            } else if (recordJson.type.toLowerCase() === 'u') {
                                record = new NDEFRecordURI(
                                    recordJson.uri,
                                    tag._uuid
                                );
                            }
                            break;

                        case utils.tnfCode("Media-type"):
                            record = new NDEFRecordMedia(recordJson.type, recordJson.content, tag._uuid);
                            break;

                        case utils.tnfCode("External"):
                            record = new NDEFRecordExternal(recordJson.type, recordJson.payload, tag._uuid);
                            break;
                        }

                        records.push(record);
                    }

                    resolve(new NDEFMessage(records, tag._uuid));
                } catch (e) {
                    console.error(e);
                    reject(e);
                }
            });
        };

        this.writeNDEF = function (ndefMessage) {
            _next_response_id += 1;
            var message = _messageToJson(
                _next_response_id,
                "nfc_tag_write_ndef",
                JSON.stringify(ndefMessage)
            );

            return new Promise(function (resolve, reject) {
                try {
                    extension.internal.sendSyncMessage(message);
                    resolve();
                } catch (e) {
                    console.error(e);
                    reject(e);
                }
            });
        };
    }


    function NFCTagEvent(tag) {
        this.tag = tag;
    }


    function NFCManager() {
        var _subscribed_power_state_changed = false,
            _subscribed_tag_discovered = false,

            _subscribe_power_state_changed = function () {
                if (_subscribed_power_state_changed) {
                    return;
                }

                _next_response_id += 1;
                _callbacks[_next_response_id] = function (response) {
                    if (response.content === "nfc_state_on") {
                        /*global document, CustomEvent */
                        document.dispatchEvent(new CustomEvent('onpoweron'));
                    } else if (response.content === "nfc_state_off") {
                        document.dispatchEvent(new CustomEvent('onpoweroff'));
                    }
                };
                extension.internal.sendSyncMessage(
                    _messageToJson(_next_response_id, "nfc_subscribe_power_state_changed")
                );

                _subscribed_power_state_changed = true;
            };

        // message listener for ALL messages; this invokes the correct
        // callback depending on the ID in the message
        extension.setMessageListener(function (message) {
            var data = JSON.parse(message),
                cb = _callbacks[data.id];

            if (cb !== undefined) {
                cb.call(NFCManager, data);

                if (!data.persistent) {
                    delete _callbacks[data.id];
                }
            } else {
                console.error("Callback not found");
            }
        });

        // =========================================================================
        // Properties
        // =========================================================================

        Object.defineProperty(this, "powered", {
            get: function () {
                var message = JSON.stringify({"content": "nfc_get_power_state"}),
                    result = extension.internal.sendSyncMessage(message),
                    resultJson = JSON.parse(result);

                _subscribe_power_state_changed();

                if (resultJson.content === 'on') {
                    return true;
                }

                return false;
            }
        });

        // =============================================================================
        // Functions
        // =============================================================================

        this.powerOn = function () {
            _next_response_id += 1;
            var message = _messageToJson(_next_response_id, "nfc_set_power_on");

            return new Promise(function (resolve, reject) {
                _callbacks[_next_response_id] = resolve;

                try {
                    extension.internal.sendSyncMessage(message);
                } catch (e) {
                    reject(e);
                }
            });
        };


        this.powerOff = function () {
            _next_response_id += 1;
            var message = _messageToJson(_next_response_id, "nfc_set_power_off");

            return new Promise(function (resolve, reject) {
                _callbacks[_next_response_id] = resolve;

                try {
                    extension.internal.sendSyncMessage(message);
                } catch (e) {
                    reject(e);
                }
            });
        };


        this.startPoll = function () {
            _next_response_id += 1;
            var message = _messageToJson(_next_response_id, "nfc_subscribe_tag_discovered");

            return new Promise(function (resolve, reject) {
                _callbacks[_next_response_id] = function (response) {
                    if (_subscribed_tag_discovered) {
                        var tag = new NFCTag(response.args),
                            evt = new CustomEvent('ontagfound', {'detail': tag});

                        document.dispatchEvent(evt);
                    }
                };

                try {
                    extension.internal.sendSyncMessage(message);
                    _subscribed_tag_discovered = true;
                    resolve();
                } catch (e) {
                    console.error(e);
                    reject(e);
                }
            });
        };
    }


    // =============================================================================
    // Scope assignment
    // =============================================================================

    /*global navigator */
    navigator.nfc = new NFCManager();

    navigator.nfc.TNF = TNF;
    navigator.nfc.NDEFRecord = NDEFRecord;
    navigator.nfc.NDEFRecordText = NDEFRecordText;
    navigator.nfc.NDEFRecordURI = NDEFRecordURI;
    navigator.nfc.NDEFRecordMedia = NDEFRecordMedia;
    navigator.nfc.NDEFRecordExternal = NDEFRecordExternal;
    navigator.nfc.NDEFMessage = NDEFMessage;
    navigator.nfc.NDEFMessageEvent = NDEFMessageEvent;
    navigator.nfc.NFCTag = NFCTag;
    navigator.nfc.NFCTagEvent = NFCTagEvent;
}());
