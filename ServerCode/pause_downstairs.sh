#!/bin/bash

kill $( ps -e | grep ds_notif | awk '{print $1}' )

#kill motion daemon if it is running
motion_running=$( ssh downstairs_cam 'ps -e | grep motion | wc -l' )
raspivid_running=$( ssh downstairs_cam 'ps -e | grep raspivid | wc -l' )

if [ $motion_running -gt 0 ]
then

        pid_string=$( ssh downstairs_cam 'ps -e | grep motion' )

        pid_string=$( echo $pid_string | awk '{print $1}' )

        ssh downstairs_cam "kill $pid_string"

elif [ $raspivid_running -gt 0 ]
then

        pid_string=$( ssh downstairs_cam 'ps -e | grep motion' )

        pid_string=$( echo $pid_string | awk '{print $1}' )

        ssh downstairs_cam "kill $pid_string"

fi

#calculate number of seconds to sleep
secs=$( echo $(( $1 * 60 )) )
printf "Camera Paused for $1 minutes"

nohup ./pause_downstairs_daemon.sh $secs >pause_downstairs.out 2>pause_downstairs.err </dev/null &

