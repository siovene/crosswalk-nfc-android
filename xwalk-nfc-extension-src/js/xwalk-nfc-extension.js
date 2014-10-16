/*jslint todo: true, nomen: true */

'use strict';

var _callbacks = {};
var _next_response_id = 0;
var _messageToJson = function (id, content, args) {
    var obj = {
        id: id.toString(),
        content: content,
        args: args
    };

    return JSON.stringify(obj);
};

var UUID = function () {
    function s4() {
        return Math.floor((1 + Math.random()) * 0x10000)
            .toString(16)
            .substring(1);
    }

    return function () {
        return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
               s4() + '-' + s4() + s4() + s4();
    };
};

// =============================================================================
// Object definitions
// =============================================================================

var TNF = {
    0: "Empty",
    1: "Well-known",
    2: "Media-type",
    3: "AbsoluteURI",
    4: "External",
    5: "Unknown",
    6: "Unchanged",
    7: "Reserved"
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
    NDEFRecord.call(this, null, 0, id, _uuid);

    this.getPayload = function () {
        return new Promise(function (resolve) {
            resolve(null);
        });
    };
}

function NDEFRecordText(text, languageCode, encoding, _uuid) {
    NDEFRecord.call(this, 1, 'T', null, _uuid);

    this.text = text;
    this.languageCode = languageCode;
    this.encoding = encoding;
}


function NDEFRecordURI(uri, _uuid) {
    NDEFRecord.call(this, 1, 'U', null, _uuid);

    this.uri = uri;
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
                    record;

                switch (argsJson.tnf) {
                case 0:
                    record = new NDEFRecordEmpty(argsJson.id, tag._uuid);
                    break;

                case 1:
                    if (argsJson.type.toLowerCase() === 't') {
                        record = new NDEFRecordText(
                            argsJson.text,
                            argsJson.languageCode,
                            argsJson.encoding,
                            tag._uuid
                        );
                    } else if (argsJson.type.toLowerCase() === 'u') {
                        record = new NDEFRecordURI(
                            argsJson.uri,
                            tag._uuid
                        );
                    }
                    break;
                }

                resolve(record);
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
                var response = extension.internal.sendSyncMessage(message);
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
                if (response.content === "nfc_state_on" && this.prototype.onpoweron !== undefined) {
                    this.prototype.onpoweron();
                } else if (response.content === "nfc_state_off" && this.prototype.onpoweroff !== undefined) {
                    this.prototype.onpoweroff();
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
                        evt = new NFCTagEvent(tag);

                    this.prototype.ontagfound(evt);
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

exports.TNF = TNF;
exports.NDEFRecord = NDEFRecord;
exports.NDEFRecordText = NDEFRecordText;
exports.NDEFRecordURI = NDEFRecordURI;
exports.NDEFMessage = NDEFMessage;
exports.NDEFMessageEvent = NDEFMessageEvent;
exports.NFCTag = NFCTag;
exports.NFCTagEvent = NFCTagEvent;
exports.NFCManager = NFCManager;
