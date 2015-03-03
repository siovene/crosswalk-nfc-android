angular.module('xwalk-nfc-christmas-tree')

.controller('AppController', function(
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

  $ionicModal.fromTemplateUrl('templates/readevent.html', {
    scope: $scope,
    animation: 'slide-in-up'
  }).then(function(modal) {
    $scope.modal = modal;
  });

  $scope.showModal = function() {
    $scope.modal.show();
  };

  $scope.closeModal = function() {
    $scope.modal.hide();
  };

  $scope.showReadEventModal = function(uuid) {
    $scope.readEvent = NfcService.findReadEventByUuid(uuid);
    $scope.showModal();
  };

  $scope.$on('$destroy', function() {
    $scope.readEventModal.remove();
  });
})
