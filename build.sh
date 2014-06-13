#!/bin/bash

export CODE_PATH=`pwd`
export CROSSWALK_VERSION='5.34.104.5'

rm -Rf build
ant
(cd lib/crosswalk-${CROSSWALK_VERSION}/;
 python make_apk.py --enable-remote-debugging \
                    --manifest=$CODE_PATH/nfc_example/manifest.json \
                    --extensions=$CODE_PATH/nfc \
                    --verbose
) &&
adb install -r $CODE_PATH/lib/crosswalk-${CROSSWALK_VERSION}/nfc_example_0.1_arm.apk
