#!/bin/bash

kill $( ps -e | grep us_notif | awk '{print $1}' )

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

        pid_string=$( ssh upstairs_cam 'ps -e | grep motion' )

        pid_string=$( echo $pid_string | awk '{print $1}' )

        ssh upstairs_cam "kill $pid_string"

fi

#calculate number of seconds to sleep
secs=$( echo $(( $1 * 60 )) )
printf "Camera Paused for $1 minutes"

nohup ./pause_upstairs_daemon.sh $secs >pause_upstairs.out 2>pause_upstairs.err </dev/null &

