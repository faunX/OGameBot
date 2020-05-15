#!/usr/bin/env bash
function func() {
    if [ "$3" = "d" ]; then
        mkdir -p $1;
    else
        touch $1;
    fi;
    sudo chmod $2 $1
}
sudo chown "$USER":"$USER" /var/run/postgresql;
while IFS= read line
do
    IFS=', ' read -r -a array <<< "$line";
    func ${array[0]} ${array[2]} ${array[3]} &
done < "$1"