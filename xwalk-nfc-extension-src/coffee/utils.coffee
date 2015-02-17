((n) ->
  n.nfc._internal ?= {}
  n.nfc._internal.utils =
    tnfCode: (s) ->
      n.nfc.TNF.indexOf s

    messageToJson: (id, content, args = null, persistent = false) ->
      obj =
        id: id.toString()
        content: content
        args: JSON.stringify args
        persistent: persistent

      JSON.stringify obj

    sendMessage: (content, args, callback) ->
      if callback?
        n.nfc._internal.callbacks.add callback
      message = n.nfc._internal.utils.messageToJson(
        n.nfc._internal.callbacks.nextId, content, args)
      JSON.parse(extension.internal.sendSyncMessage(message))

    uuid: ->
      s4 = ->
        Math.floor((1 + Math.random()) * 0x10000)
          .toString(16)
          .substring(1)

      (
        s4() + s4() + '-' +
        s4() + '-' +
        s4() + '-' +
        s4() + '-' +
        s4() + s4() + s4()
      )
)(navigator)
