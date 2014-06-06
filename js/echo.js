exports.echo = function(s) {
    return extension.internal.sendSyncMessage(s);
}
