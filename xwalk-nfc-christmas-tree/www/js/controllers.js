angular.module('xwalk-nfc-christmas-tree')

.controller('AppController', function($scope, $ionicPopup, $timeout, $state, NfcService) {
  $scope.nfc = NfcService.data;

  $scope.showAddWatchPopup = function() {
    $scope.data = {};

    $ionicPopup.show({
      template: '<input type="text" ng-model="data.scope"/>',
      title: 'Enter scope',
      scope: $scope,
      buttons: [
        { text: 'Cancel' },
        {
          text: '<strong>Watch</strong>',
          type: 'button-positive',
          onTap: function(e) {
            NfcService.watch($scope.data.scope);
          }
        }
      ]
    });
  };

  $scope.watchClicked = function(watch) {
  };

  $scope.readEventClicked = function(readEvent) {
  };
});
