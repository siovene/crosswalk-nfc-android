exports.NFCManager = {
    test: function() {
        return extension.internal.sendSyncMessage({
            "action": "test"
        });
    }
}
