var _callbacks = {}
var _next_response_id = 0;
var _messageToJson = function(id, content, args) {
    var obj = {
        id: '' + id,
        content: content,
        args: args
    };

    return JSON.stringify(obj);
};


// =============================================================================
// Object definitions
// =============================================================================

var TNF = {
    "Empty"      : 0,
    "Well-known" : 1,
    "Media-type" : 2,
    "AbsoluteURI": 3,
    "External"   : 4,
    "Unknown"    : 5,
    "Unchanged"  : 6,
    "Reserved"   : 7
};


function NDEFRecord(tnf, type, id) {
    this.tnf = tnf;
    this.type = type;
    this.id = id;

    this.getPayload = function() {
        _next_response_id += 1;
        var message = _messageToJson(_next_response_id, "nfc_ndefrecord_getpayload");
        // TODO
    };
};


function NDEFMessage() {
    this.records = [];

    this.getBytes = function() {
        // TODO
    };
};


function NDEFMessageEvent() {
    this.message = null;
};


function NFCTag(uuid) {
    this._uuid = uuid;

    this.readNDEF = function() {
        _next_response_id += 1;
        var message = _messageToJson(_next_response_id, "nfc_tag_read_ndef", this._uuid);

        return new Promise(function(resolve, reject) {
            try {
                var response = extension.internal.sendSyncMessage(message);
                var responseJson = JSON.parse(response);
                var argsJson = JSON.parse(responseJson.args);
                var record = new NDEFRecord(
                    argsJson.tnf,
                    argsJson.type,
                    argsJson.id);

                resolve(record);
            } catch (e) {
                console.err(e);
                reject(e);
            }
        });
    };

    this.writeNDEF = function(message) {
        // TODO
    };
};


function NFCTagEvent(tag) {
    this.tag = tag;
};


function NFCManager() {
    var _subscribed_power_state_changed = false;
    var _subscribed_tag_discovered = false;

    var _subscribe_power_state_changed = function() {
        if (_subscribed_power_state_changed)
            return;

        _next_response_id += 1;
        _callbacks[_next_response_id] = function(response) {
            if(response.content == "nfc_state_on" && this.prototype.onpoweron !== undefined)
                this.prototype.onpoweron();
            else if (response.content == "nfc_state_off" && this.prototype.onpoweroff !== undefined)
                this.prototype.onpoweroff();
        };
        extension.internal.sendSyncMessage(_messageToJson(
            _next_response_id, "nfc_subscribe_power_state_changed"));

        _subscribed_power_state_changed = true;
    };

    // message listener for ALL messages; this invokes the correct
    // callback depending on the ID in the message
    extension.setMessageListener(function (message) {
        var data = JSON.parse(message);
        var cb = _callbacks[data.id];

        if (cb !== undefined) {
            cb.call(NFCManager, data);

            if (!data.persistent)
                delete _callbacks[data.id];
        } else {
            console.err("Callback not found");
        }
    });

    // =========================================================================
    // Properties
    // =========================================================================

    Object.defineProperty(this, "powered", {
        get: function() {
            var message = JSON.stringify({"content": "nfc_get_power_state"});
            var result = extension.internal.sendSyncMessage(message);
            var resultJson = JSON.parse(result);

            _subscribe_power_state_changed();

            if (resultJson.content == 'on')
                return true;

            return false;
        }
    });

    // =============================================================================
    // Functions
    // =============================================================================

    this.powerOn = function() {
        _next_response_id += 1;
        var message = _messageToJson(_next_response_id, "nfc_set_power_on");
        console.log("powerOn called on manager");

        return new Promise(function(resolve, reject) {
            _callbacks[_next_response_id] = resolve;

            try {
                extension.internal.sendSyncMessage(message);
            } catch (e) {
                reject(e);
            }
        });
    };


    this.powerOff = function() {
        _next_response_id += 1;
        var message = _messageToJson(_next_response_id, "nfc_set_power_off");

        return new Promise(function(resolve, reject) {
            _callbacks[_next_response_id] = resolve;

            try {
                extension.internal.sendSyncMessage(message);
            } catch (e) {
                reject(e);
            }
        });
    };


    this.startPoll = function() {
        _next_response_id += 1;
        var message = _messageToJson(_next_response_id, "nfc_subscribe_tag_discovered");

        return new Promise(function(resolve, reject) {
            _callbacks[_next_response_id] = function(response) {
                if (_subscribed_tag_discovered) {
                    var tag = new NFCTag(response.args);
                    var evt = new NFCTagEvent(tag);
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
};


// =============================================================================
// Scope assignment
// =============================================================================

exports.TNF = TNF;
exports.NDEFRecord = NDEFRecord;
exports.NDEFMessage = NDEFMessage;
exports.NDEFMessageEvent = NDEFMessageEvent;
exports.NFCTag = NFCTag;
exports.NFCTagEvent = NFCTagEvent;
exports.NFCManager = NFCManager;
