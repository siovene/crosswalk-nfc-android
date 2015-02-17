angular.module('xwalk-nfc-christmas-tree')

.factory('NfcService', function() {
  var _data = {
    adapter: null,
    watches: [],
  };

  function _init() {
    navigator.nfc.NFC.findAdapters().then(function(adapters) {
      _data.adapter = adapters[0];
    });
  }

  function _watch(scope) {
    return new Promise(function(resolve, rejec) {
      var i, w;

      if (_data.adapter === null) {
        reject("Adapter not found.");
        return;
      }

      // Prevent watching the same scope multiple times.
      for (i = 0; i < _data.watches.length; i++) {
        if (watches[i].scope == scope) {
          resolve(watches[i].uuid);
          return;
        }
      }

      _data.adapter.watch({scope: scope}).then(function(watchId) {
        _data.watches.push({uuid: watchId, scope: scope});
        resolve(watchId);
      });
    });
  }

  _init();

  return {
    data: _data,
    watch: _watch
  };
});
