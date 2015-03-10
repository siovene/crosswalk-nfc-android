((n) ->
  n.nfc._internal ?= {}
  n.nfc._internal.RTDMap =
    RTD_TEXT: [0x54], # "T"
    RTD_URI: [0x55], # "U"
)(navigator)
