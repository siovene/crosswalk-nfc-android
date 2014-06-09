#!/bin/bash

export CODE_PATH=`pwd`

rm -Rf build
ant
(cd lib/crosswalk-5.34.104.5/;
 rm -Rf nfc_example*;
 python make_apk.py --enable-remote-debugging \
                    --manifest=$CODE_PATH/nfc_example/manifest.json \
                    --extensions=$CODE_PATH/nfc
)
adb install -r $CODE_PATH/lib/crosswalk-5.34.104.5/nfc_example_0.1_x86.apk
