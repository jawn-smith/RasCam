#!/bin/bash

kill -9 $( ps -e | grep us_notification | awk '{print $1}' )

#kill motion daemon if it is running
motion_running=$( ssh upstairs_cam 'ps -e | grep motion | wc -l' )
raspivid_running=$( ssh upstairs_cam 'ps -e | grep raspivid | wc -l' )

if [ $motion_running -gt 0 ]
then

	pid_string=$( ssh upstairs_cam 'ps -e | grep motion' )

	pid_string=$( echo $pid_string | awk '{print $1}' )

	ssh upstairs_cam "kill $pid_string"

elif [ $raspivid_running -gt 0 ]
then

        pid_string=$( ssh upstairs_cam 'ps -e | grep raspivid' )

        pid_string=$( echo $pid_string | awk '{print $1}' )

        ssh upstairs_cam "kill $pid_string"

fi

while [ $( ssh upstairs_cam "echo $(( $( ps -e | grep motion | wc -l ) + $( ps -e | grep raspivid | wc -l ) ))" ) -gt 0 ]
do
	sleep 0.5s
done

while [ $( ssh upstairs_cam "raspistill -n -v" |& grep 'Failed to run camera app' | wc -l ) -gt 0 ]
do
        sleep 0.5s
done

sleep 2s

ssh upstairs_cam './stream.sh > vid_dump/upstairs_stream.out 2> vid_dump/upstairs_stream.err < /dev/null &'

while ! grep -q "packet with too strange dts" upstairs_stream.err
do
	sleep 0.5s
done

