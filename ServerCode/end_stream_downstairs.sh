#!/bin/bash

pid_string=$( ssh downstairs_cam 'ps -e | grep raspivid' )

pid_string=$( echo $pid_string | awk '{print $1}' )

pid_string2=$( ssh downstairs_cam 'ps -e | grep vlc' )

pid_string2=$( echo $pid_string2 | awk '{print $1}' )

ssh downstairs_cam "kill $pid_string $pid_string2"

nohup ./ds_notification_daemon.sh > downstairs_daemon.out 2> downstairs_daemon.err < /dev/null &
