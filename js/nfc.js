// ============================================================================
// Object definition
// ============================================================================

function NFCManager() {
    var _eventHandlers = {
        onpoweron: function() {},
        onpoweroff: function() {}
    };

    // ========================================================================
    // Events
    // ========================================================================

    Object.defineProperty(this, "onpoweron", {
        value: function() {},
        set: function(f) {
            _eventHandlers.onpoweron = f;
            extension.internal.sendSyncMessage(
                JSON.stringify({
                    "action": "subscribe",
                    "event": "onpoweron",
                    "eventHandler": "onpoweron"
                })
            );
        }
    });

    Object.defineProperty(this, "onpoweroff", {
        value: function() {}
    });
};

// ============================================================================
// Functions
// ============================================================================

NFCManager.powerOn = function() {
    return new Promise(function(resolve, reject) {
        var message = JSON.stringify({
            "action": "powerOn"
        });
        resolve(extension.internal.sendSyncMessage(message));
    });
};

// ============================================================================
// Properties
// ============================================================================

Object.defineProperty(NFCManager, "powered", {
    get: function() {
        var message = JSON.stringify({"action": "powerStatus"});
        var poweredString = extension.internal.sendSyncMessage(message);
        if (poweredString == 'ON') {
            return true;
        }

        return false;
    }
});

// ============================================================================
// Scope assignment
// ============================================================================

exports.NFCManager = NFCManager;
