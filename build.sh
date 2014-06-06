#!/bin/bash

export CODE_PATH=`pwd`

rm -Rf build
ant
(cd lib/crosswalk-5.34.104.5/;
 rm -Rf echo_app*;
 python make_apk.py --enable-remote-debugging \
                    --manifest=$CODE_PATH/app/manifest.json \
                    --extensions=$CODE_PATH/echo
)
adb install -r $CODE_PATH/lib/crosswalk-5.34.104.5/echo_app_0.1_x86.apk
