#!/bin/sh
mkdir -p dist/lib >/dev/null
cp kvproxy.conf log4j.properties grouploc.conf dist/
cp target/*.jar dist/
cp target/lib/*.jar dist/lib/
cp cm2_locator/subscriber.conf dist/
cp cm2_locator/target/*.jar dist/lib/
cp cm2_locator/target/lib/*.jar dist/lib/
cp run_dist.sh dist/
tar czvf kvproxy.tgz dist/

