// =============================================================================
// Object definition
// =============================================================================

function NFCManager() {
    var _callbacks = {}
    var _next_response_id = 0;
    var _subscribed_power_state_changed = false;
    var _subscribed_tag_discovered = false;

    var _messageToJson = function(id, content) {
        var obj = {
            id: '' + id,
            content: content
        };

        return JSON.stringify(obj);
    };

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

        if (cb) {
            cb.call(NFCManager, data);

            if (!data.persistent)
                delete _callbacks[data.id];
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
                    this.prototype.ontagfound(response.content);
                }

                resolve();
            };

            try {
                extension.internal.sendSyncMessage(message);
                _subscribed_tag_discovered = true;
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

exports.NFCManager = NFCManager;
