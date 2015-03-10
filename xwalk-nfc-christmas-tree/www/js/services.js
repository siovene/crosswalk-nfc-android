angular.module('xwalk-nfc-christmas-tree')

.factory('NfcService', function($rootScope) {
  var _data = {
    adapter: null,
    watches: [],
    readEvents: []
  };

  function _init() {
    navigator.nfc.NFC.findAdapters().then(function(adapters) {
      _data.adapter = adapters[0];
      _data.adapter.onread = function(readEvent) {
        $rootScope.$apply(function() {
          readEvent.timestamp = Date.now();
          _data.readEvents.push(readEvent);
        });
      };
    });
  }

  function _watch(scope, onRead) {
    return new Promise(function(resolve, rejec) {
      var i;

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
        var w = new navigator.nfc.NfcWatch(scope, watchId);
        w.timestamp = Date.now();
        _data.watches.push(w);
        resolve(watchId);
      });
    });
  }

  function _write(data, scope) {
    return _data.adapter.write(data, scope);
  }

  _init();

  return {
    data: _data,
    watch: _watch,
    write: _write,
    findReadEventByUuid: _findReadEventByUuid
  };
});
