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
})

.controller('ReadEventController', function($scope, $state, NfcService) {
  var uuid = $state.params.readEventUuid;
  $scope.readEvent = NfcService.findReadEventByUuid(uuid);

  $scope.asText = function (data) { return data.text(); };
  $scope.asJson = function (data) {
    try {
      return data.json();
    } catch (e) {
      return "No JSON in record.";
    }
  };
  $scope.asUrl = function (data) {
    var url = data.url();
    if (url === undefined) {
      url = "No URL in record."
    }

    return url;
  };
});
