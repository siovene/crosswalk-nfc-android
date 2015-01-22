angular.module('xwalk-nfc-christmas-tree')

.controller('AppController', function($scope, $ionicPopup, $timeout, NfcService) {
  $scope.nfc =  NfcService.nfc;

  $scope.showAddTagRegistrationPopup = function() {
    $scope.data = {};

    function requestTagRegistration(scope) {
      navigator.nfc.NFC
        .requestTagRegistration({scope: scope})
        .then(function(registration) {
          NfcService.nfc.tagRegistrations.push(registration);
          registration.onmessage = function(ev) {
            $scope.$apply(function() {
              // Monkey-patch registration.
              if (registration.messageEventCount !== undefined) {
                registration.messageEventCount += 1;
              } else {
                registration.messageEventCount = 0;
              }

              registration.lastMessageEvent = ev;
            });
          }
        });
    }

    $ionicPopup.show({
      template: '<input type="text" ng-model="data.scope"/>',
      title: 'Enter registration scope',
      scope: $scope,
      buttons: [
        { text: 'Cancel' },
        {
          text: '<strong>Register</strong>',
          type: 'button-positive',
          onTap: function(e) {
            requestTagRegistration($scope.data.scope);
          }
        }
      ]
    });
  };

  $scope.tagRegistrationClicked = function(registration) {
    registration.lastMessageEvent = undefined;
  };
});
