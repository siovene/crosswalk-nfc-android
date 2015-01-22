angular.module('xwalk-nfc-christmas-tree')

.factory('NfcService', function() {
  var _nfc = {
    tagRegistrations: [],
    messageEvent: null
  };

  return {
    nfc: _nfc
  };
});
