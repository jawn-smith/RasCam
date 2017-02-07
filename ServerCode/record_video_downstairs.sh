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

while [ $( ssh downstairs_cam "echo $(( $( ps -e | grep motion | wc -l ) + $( ps -e | grep raspivid | wc -l ) ))" ) -gt 0 ]
do
	sleep 0.5s
done

while [ $( ssh downstairs_cam "raspistill -n -v" |& grep 'Failed to run camera app' | wc -l ) -gt 0 ]
do
        sleep 0.5s
done

sleep 2s

vid_len=$( echo $(( $1 * 1000 )) )

#printf 'recording video\n'
ssh downstairs_cam "raspivid -n -w 960 -h 720 -t $vid_len --annotate 12 -o /home/pi/vid_dump/videos/downstairs_user_vid.h264"

sleep 2s

MP4Box -add /home/pi/videos/downstairs_user_vid.h264 /home/pi/videos/downstairs_user_vid.mp4
rm /home/pi/videos/downstairs_user_vid.h264

#restart motion daemon
if [ -e downstairs_motion_log ]
then
	rm downstairs_motion_log
fi

nohup ./ds_notification_daemon.sh > downstairs_daemon.out 2> downstairs_daemon.err < /dev/null &
