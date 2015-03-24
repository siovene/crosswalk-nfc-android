angular.module('xwalk-nfc-christmas-tree')

.controller('ReadController', function(
  $scope, $ionicPopup, $timeout, $state, $ionicModal, NfcService)
{
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

  $scope.showModal = function() {
    return $scope.modal.show();
  };

  $scope.closeModal = function() {
    return $scope.modal.hide();
  };

  $scope.$on('$destroy', function() {
    $scope.readEventModal.remove();
  });
})

.controller('WriteController', function($scope, $ionicModal, $ionicPopup, NfcService) {
  var watchId = null;

  $scope.records = [];
  $scope.recordBeingAdded = {
    type: "text",
    content: ""
  };

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
    NfcService.write(records).then(
      function success(result) {
        console.log(result);
        NfcService.clearWatch(watchId);
        watchId = null;
      },
      function error(msg) {
        var popup;

        if (msg == "nfc_response_tag_lost" ||
            msg == "nfc_response_io_error")
        {
          popup = $ionicPopup.show({
            title: "Please scan a tag",
            templateUrl: 'templates/scan-tag-popup.html',
            buttons: [
              {
                text: 'Cancel',
                type: 'button-default'
              }
            ]
          });

          NfcService.clearWatch(watchId);

          // TODO: scope.
          NfcService.watch("", function(readEvent) {
            popup.close();
            NfcService.clearWatch(watchId);
            watchId = null;
            $scope.writeTag(records);
          }).then(function(uuid) {
            watchId = uuid;
          });
        }
      }
    );
  };

  $scope.$on('$destroy', function() {
    $scope.modal.remove();
  });
});
