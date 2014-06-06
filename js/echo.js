exports.echo = function(s) {
    return extensions.internal.sendSyncMessage(s);
}
