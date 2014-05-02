#!/bin/bash

xcodebuild -workspace Pdef.xcworkspace -scheme Pdef -sdk iphonesimulator test | xcpretty -t
echo

