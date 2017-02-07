#!/bin/bash

space_on_server=$( df -h | grep /dev/root | awk '{print $5}' )
space_on_server=${space_on_server%\%*}

#ensure there is sufficient space on the device
if [ $space_on_server -gt 75 ]
then
	curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$heather\",\"data\":{\"title\":\"System Alert\",\"detail\":\"The Server is over 75% full. Please delete some files.\"},\"priority\":10}"
	curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"The server is over 75% full. Please delete some files.\"},\"priority\":10}"
fi
