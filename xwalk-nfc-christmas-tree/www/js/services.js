angular.module('xwalk-nfc-christmas-tree')

.factory('NfcService', function() {
  var _nfc = {
    tagRegistration: null,
    messageEvent: null
  };

  return {
    nfc: _nfc
  };
});
