// Ionic Starter App

// angular.module is a global place for creating, registering and retrieving Angular modules
// 'starter' is the name of this angular module example (also set in a <body> attribute in index.html)
// the 2nd parameter is an array of 'requires'
angular.module('xwalk-nfc-christmas-tree', ['ionic'])

.config(function($stateProvider, $urlRouterProvider) {
  $stateProvider.state('app', {
    url: '/app',
    abstract: true,
    templateUrl: 'templates/menu.html',
    controller: 'AppController'
  })

  .state('app.home', {
    url: '/home',
    views: {
      'menuContent': {
        templateUrl: 'templates/home.html'
      }
    }
  })

  .state('app.home.read', {
    url: '/read',
    views: {
      'homeContent': {
        templateUrl: 'templates/read.html'
      }
    }
  })

  .state('app.home.read.event', {
    url: '/event/:uuid',
    views: {
      'homeContent': {
        templateUrl: 'templates/readevent.html'
      }
    }
  })

  .state('app.home.write', {
    url: '/write',
    views: {
      'homeContent': {
        templateUrl: 'templates/write.html'
      }
    }
  })


  .state('app.about', {
    url: '/about',
    views: {
      'menuContent': {
        templateUrl: 'templates/about.html'
      }
    }
  });

  $urlRouterProvider.otherwise('/app/home/read');
})

.run(function($ionicPlatform) {
  $ionicPlatform.ready(function() {
    // Hide the accessory bar by default (remove this to show the accessory bar above the keyboard
    // for form inputs)
    if(window.cordova && window.cordova.plugins.Keyboard) {
      cordova.plugins.Keyboard.hideKeyboardAccessoryBar(true);
    }
    if(window.StatusBar) {
      StatusBar.styleDefault();
    }
  });
});
