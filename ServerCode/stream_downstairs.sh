#!/bin/bash

kill -9 $( ps -e | grep ds_notification | awk '{print $1}' )

#kill motion daemon if it is running
motion_running=$( ssh downstairs_cam 'ps -e | grep motion | wc -l' )
raspivid_running=$( ssh downstairs_cam 'ps -e | grep raspivid | wc -l' )

if [ $motion_running -gt 0 ]
then

	pid_string=$( ssh downstairs_cam 'ps -e | grep motion' )

	pid_string=$( echo $pid_string | awk '{print $1}' )

	printf "killing $pid_string\n"

	ssh downstairs_cam "kill $pid_string"

elif [ $raspivid_running -gt 0 ]
then

        pid_string=$( ssh downstairs_cam 'ps -e | grep raspivid' )

        pid_string=$( echo $pid_string | awk '{print $1}' )

        ssh downstairs_cam "kill $pid_string"

fi

while [ $( ssh downstairs_cam "echo $(( $( ps -e | grep motion | wc -l ) + $( ps -e | grep raspivid | wc -l ) ))" ) -gt 0 ]
do
	sleep 0.5s
done

while [ $( ssh downstairs_cam "raspistill -n -v" |& grep 'Failed to run camera app' | wc -l ) -gt 0 ]
do
        sleep 0.5s
done

sleep 2s

ssh downstairs_cam './stream.sh > vid_dump/downstairs_stream.out 2> vid_dump/downstairs_stream.err < /dev/null &'

while ! grep -q "packet with too strange dts" downstairs_stream.err
do
	sleep 0.5s
done

