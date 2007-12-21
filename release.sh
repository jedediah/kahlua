#!/bin/bash
version=`svn info | grep Revision | cut -d " " -f 2`
timestamp=`date +%Y-%m-%d`

zipname=kahlua-$timestamp-$version.zip
rm -rf kahlua

svn export . kahlua

rm -rf $zipname
zip -r $zipname kahlua

rm -rf release
mv kahlua release


