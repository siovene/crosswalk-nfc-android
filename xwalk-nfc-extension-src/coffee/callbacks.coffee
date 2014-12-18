((n) ->
  n.nfc._internal ?= {}
  n.nfc._internal.callbacks =
    functions: {}
    nextId: 0
    add: (f) ->
      id = ++n.nfc._internal.callbacks.nextId
      functions = n.nfc._internal.callbacks.functions
      functions[id] = f
)(navigator)
