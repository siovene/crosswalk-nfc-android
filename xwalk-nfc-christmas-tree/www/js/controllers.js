angular.module('xwalk-nfc-christmas-tree')

.controller('AppController', function($scope, $ionicPopup, $timeout, $state, NfcService) {
  $scope.nfc = NfcService.data;
  $scope.readEvents = [];

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
            NfcService.data.adapter.onread = function(readEvent) {
              $scope.$apply(function() {
                // Monkey-patch readEvent.
                readEvent.appPatch = readEvent.appPatch || {};
                if (readEvent.appPatch.count !== undefined) {
                  readEvent.appPatch.count += 1;
                } else {
                  readEvent.appPatch = 1;
                }

                // Monkey-patch adapter.
                NfcService.data.adapter.appPatch = NfcService.data.adapter.appPatch || {};
                NfcService.data.adapter.appPatch.lastReadEvent = readEvent;
              });
            };

            NfcService.watch($scope.data.scope);
          }
        }
      ]
    });
  };

  $scope.watchClicked = function(w) {
    if (w.lastMessageEvent !== undefined) {
      w.lastMessageEvent = undefined;
      $state.transitionTo('app.tag');
    }
  };
});
