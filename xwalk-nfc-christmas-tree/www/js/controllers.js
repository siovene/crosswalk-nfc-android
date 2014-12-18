angular.module('xwalk-nfc-christmas-tree')

.controller('AppController', function($scope, NfcService) {
  $scope.nfc =  NfcService.nfc;

  navigator.nfc.NFC.requestTagRegistration().then(function(registration) {
    NfcService.nfc.tagRegistration = registration
    registration.onmessage = function(ev) {
      NfcService.nfc.lastMessageEvent = ev;
    }
  });
});
