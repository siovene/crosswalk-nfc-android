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

  $scope.showModal = function() {
    return $scope.modal.show();
  };

  $scope.closeModal = function() {
    return $scope.modal.hide();
  };

  $scope.showReadEventModal = function(uuid) {
    $scope.readEvent = NfcService.findReadEventByUuid(uuid);

    $ionicModal.fromTemplateUrl('templates/readevent.html', {
      scope: $scope,
      animation: 'slide-in-up'
    }).then(function(modal) {
      $scope.modal = modal;
      $scope.showModal();
    });
  };

  $scope.$on('$destroy', function() {
    $scope.modal.remove();
  });
})

.controller('WriteController', function($scope, $ionicModal, NfcService) {
  $scope.recordBeingAdded = {
    type: "text",
    content: ""
  };
  $scope.records = [];

  $scope.showModal = function() {
    return $scope.modal.show();
  };

  $scope.closeModal = function() {
    return $scope.modal.hide();
  };

  $scope.showAddRecordModal = function() {
    $ionicModal.fromTemplateUrl('templates/write-add-record.html', {
      scope: $scope,
      animation: 'slide-in-up'
    }).then(function(modal) {
      $scope.modal = modal;
      $scope.showModal();
    });
  };

  $scope.addRecord = function() {
    $scope.recordBeingAdded.uuid = navigator.nfc._internal.utils.uuid();
    $scope.records.push($scope.recordBeingAdded);
    $scope.closeModal().then(function() {
      $scope.recordBeingAdded = {type: "text", content: ""};
    });
  };

  $scope.removeRecord = function(record) {
    $scope.records = $scope.records.filter(function(r) {
      return record.uuid !== r.uuid;
    });
  };

  $scope.writeTag = function(records) {
    NfcService.write(records);
  };

  $scope.$on('$destroy', function() {
    $scope.modal.remove();
  });
});
