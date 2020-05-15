#!/usr/bin/env bash
while IFS= read line
do
    IFS=', ' read -r -a array <<< "$line";
    if [ "${array[3]}" = "d" ]; then
        mkdir -p ${array[0]};
    else
        touch ${array[0]};
    fi;
    chmod ${array[2]} ${array[0]};
done <"$1"

