exports.NFCManager = {
    test: function() {
        var message = JSON.stringify({
            "action": "test"
        });

        return extension.internal.sendSyncMessage(message);
    }
}
