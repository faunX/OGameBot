#!/usr/bin/env bash
for i in `find -L .`; do
    t="f";
    if [ -d "$i" ]; then
        t="d";
    fi;
    echo "$i,`stat -f "%Sp,%OLp" $i`,$t" >> permissions.txt;
done