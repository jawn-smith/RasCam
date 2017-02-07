#!/bin/bash

if [ $( ps -e | grep pause | wc -l ) -eq 0 ] && [ $( ps -e | grep take_photo | wc -l ) -eq 0 ] && [ $( ps -e | grep record_video | wc -l ) -eq 0 ]
then

	william=$( cat william_token.txt )

	#curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Checking Status\"},\"priority\":10}"

	# check if upstairs cam is powered on.
	if ! ping -c 3 -q 192.168.0.7
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"The upstairs camera is not responding to pings\"},\"priority\":10}"
	fi

	# check if downstairs cam is powered on. If so, start motion daemon
	if ! ping -c 3 -q 192.168.0.12
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"The downstairs camera is not responding to pings\"},\"priority\":10}"
	fi

	# check if notification daemon is running on server
	if [ $( ps -e | grep ds_notificatio | wc -l ) -eq 0 ] && [ $( ps -e | grep photo_down | wc -l ) -eq 0 ] && [ $( ps -e | grep video_down | wc -l ) -eq 0 ] && [ $( ps -e ax | grep pause_down | grep -v "grep" | wc -l ) -eq 0 ] && [ $( ssh downstairs_cam "ps -e | grep vlc | wc -l" ) -eq 0 ]
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"The downstairs notification daemon was not running. An automatic restart has been attempted\"},\"priority\":10}"
		./ds_notification_daemon.sh &
	fi

	# check if notification daemon is running on server
	if [ $( ps -e | grep us_notificatio | wc -l ) -eq 0 ] && [ $( ps -e | grep photo_upst | wc -l ) -eq 0 ] && [ $( ps -e | grep video_upst | wc -l ) -eq 0 ] && [ $( ps -e ax | grep pause_upst | grep -v "grep" | wc -l ) -eq 0 ] && [ $( ssh upstairs_cam "ps -e | grep vlc | wc -l" ) -eq 0 ]
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"The upstairs notification daemon was not running. An automatic restart has been attempted\"},\"priority\":10}"
		./us_notification_daemon.sh &
	fi

	# check if notification daemon is defunct
	if [ $( ps -e | grep ds_notificatio | grep defunct | wc -l ) -gt 0 ]
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"The notification daemon was defunct downstairs\"},\"priority\":10}"
	fi

	if [ $( ps -e | grep us_notificatio | grep defunct | wc -l ) -gt 0 ]
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"The notification daemon was defunct upstairs\"},\"priority\":10}"
	fi

	# check if notification daemon is stopped
	if [ $( ps -e ax | grep ds_notif | awk '{print $3}' | grep T | wc -l ) -gt 0 ]
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"The downstairs notification daemon was stopped. An automatic restart has been attempted\"},\"priority\":10}"
		kill -9 $( ps -e | grep ds_notif | awk '{print $1}' )
		./ds_notification_daemon.sh &
	fi

	# check if notification daemon is stopped
	if [ $( ps -e ax | grep ds_notif | awk '{print $3}' | grep T | wc -l ) -gt 0 ]
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"The upstairs notification daemon was stopped. An automatic restart has been attempted\"},\"priority\":10}"
		kill -9 $( ps -e | grep us_notif | awk '{print $1}' )
		./us_notification_daemon.sh &
	fi

	# check if notification daemon is running on server
	if [ $( ps -e | grep ds_notificatio | wc -l ) -gt 1 ]
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Multiple notification daemons were running downstairs\"},\"priority\":10}"
		daemons=$( ps -e | grep ds_notif | awk '{print $1}' )
		kill -9 $daemons
		./ds_notification_daemon.sh &
	fi

	# check if notification daemon is running on server
	if [ $( ps -e | grep us_notificatio | wc -l ) -eq 0 ]
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Multiple notification daemson were running upstairs\"},\"priority\":10}"
		daemons=$( ps -e | grep us_notif | awk '{print $1}' )
                kill -9 $daemons
                ./us_notification_daemon.sh &
	fi

	# check to make sure both cameras have the server mounted
	if [ $( ssh downstairs_cam 'df -h | grep vid_dump | wc -l' ) -eq 0 ]
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"The server was not mounted on the downstairs camera. An automatic correction has been attempted\"},\"priority\":10}"
		ssh downstairs_cam 'sshfs cam_server:/home/pi/ /home/pi/vid_dump'
	fi

	if [ $( ssh upstairs_cam 'df -h | grep vid_dump | wc -l' ) -eq 0 ]
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"The server was not mounted on the upstairs camera. An automatic correction has been attempted\"},\"priority\":10}"
		ssh upstairs_cam 'sshfs cam_server:/home/pi/ /home/pi/vid_dump'
	fi

	if [ $( ssh downstairs_cam 'ps -e | grep motion | wc -l' ) -gt 1 ]
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Multiple motion daemons were running downstairs\"},\"priority\":10}"
		ssh downstairs_cam './too_many_motions.sh'
	fi

	if [ $( ssh upstairs_cam 'ps -e | grep motion | wc -l' ) -gt 1 ]
        then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Multiple motion daemons were running upstairs\"},\"priority\":10}"
                ssh upstairs_cam './too_many_motions.sh'
        fi

	if [ $( ssh downstairs_cam 'ps -e | grep raspivid | wc -l' ) -gt 1 ]
        then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Multiple raspivids were running downstairs\"},\"priority\":10}"
                ssh downstairs_cam './too_many_raspivids.sh'
        fi

	if [ $( ssh upstairs_cam 'ps -e | grep raspivid | wc -l' ) -gt 1 ]
        then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Multiple raspivids were running upstairs\"},\"priority\":10}"
                ssh upstairs_cam './too_many_raspivids.sh'
        fi

	# Start dameons to ensure motion detection is working properly on both cameras. Hopefully these daemons can run from the server
	if grep -q "did NOT restart graceful" downstairs_motion_log
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Motion is not running properly downstairs\"},\"priority\":10}"

		rm downtairs_motion_log

		pid_string=$( ssh downstairs_cam 'ps -e | grep motion' )

	        pid_string=$( echo $pid_string | awk '{print $1}' )

		kill $( ps -e | grep ds_notif |awk '{print $1}' )

        	ssh downstairs_cam "kill -9 $pid_string"

		i=0

		while [ $( ssh downstairs_cam "raspistill -n -v" |& grep 'Failed to run camera app' | wc -l ) -gt 0 ] && [ $i -lt 10 ]
		do
			i=$(( $i + 1 ))
        		sleep 0.5s
		done

		if [ $i -eq 10 ]
		then
			curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Motion could not be killed downstairs\"},\"priority\":10}"
		fi

		./ds_notification_daemon.sh &

	fi

	if grep -q "did NOT restart graceful" upstairs_motion_log
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Motion is not running properly upstairs\"},\"priority\":10}"

		pid_string=$( ssh upstairs_cam 'ps -e | grep motion' )

                pid_string=$( echo $pid_string | awk '{print $1}' )

		rm upstairs_motion_log

		kill $( ps -e | grep us_notif |awk '{print $1}' )

                ssh upstairs_cam "kill -9 $pid_string"

		i=0

		while [ $( ssh upstairs_cam "raspistill -n -v" |& grep 'Failed to run camera app' | wc -l ) -gt 0 ] && [ $i -lt 10 ]
		do
			i=$(( $i + 1 ))
        		sleep 0.5s
		done

		if [ $i -eq 10 ]
		then
			curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Motion could not be killed upstairs\"},\"priority\":10}"
		fi

		./us_notification_daemon.sh &

	fi

	if grep -q "Device or resource busy" downstairs_motion_log
	then

		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Motion was unable to access the camera downstairs\"},\"priority\":10}"

		rm downstairs_motion_log

		pid_string=$( ssh downstairs_cam 'ps -e | grep motion' )

	        pid_string=$( echo $pid_string | awk '{print $1}' )

        	kill $( ps -e | grep ds_notif |awk '{print $1}' )

        	ssh downstairs_cam "kill -9 $pid_string"

		i=0

		while [ $( ssh downstairs_cam "raspistill -n -v" |& grep 'Failed to run camera app' | wc -l ) -gt 0 ] && [ $i -lt 10 ]
		do
			i=$(( $i + 1 ))
        		sleep 0.5s
		done

		if [ $i -eq 10 ]
		then
			curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Motion could not be killed downstairs\"},\"priority\":10}"
		fi

		./ds_notification_daemon.sh &


	fi

	if grep -q "Device or resource busy" upstairs_motion_log
	then
		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Motion was unable to access the camera upstairs\"},\"priority\":10}"

		rm upstairs_motion_log

                pid_string=$( ssh upstairs_cam 'ps -e | grep motion' )

                pid_string=$( echo $pid_string | awk '{print $1}' )

                kill $( ps -e | grep us_notif |awk '{print $1}' )

        	ssh upstairs_cam "kill -9 $pid_string"

		i=0

		while [ $( ssh upstairs_cam "raspistill -n -v" |& grep 'Failed to run camera app' | wc -l ) -gt 0 ] && [ $i -lt 10 ]
		do
			i=$(( $i + 1 ))
        		sleep 0.5s
		done

		if [ $i -eq 10 ]
		then
			curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Motion could not be killed upstairs\"},\"priority\":10}"
		fi

		./us_notification_daemon.sh &

	fi

	sleep 5s

	if ! [ -e downstairs_motion_log ] && [ $( ssh downstairs_cam 'ps -e | grep motion | wc -l' ) -eq 1 ]
	then

		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Motion was running downstairs with no log\"},\"priority\":10}"

		rm downstairs_motion_log

		pid_string=$( ssh downstairs_cam 'ps -e | grep motion' )

	        pid_string=$( echo $pid_string | awk '{print $1}' )

        	kill $( ps -e | grep ds_notif |awk '{print $1}' )

        	ssh downstairs_cam "kill -9 $pid_string"

		i=0

		while [ $( ssh downstairs_cam "raspistill -n -v" |& grep 'Failed to run camera app' | wc -l ) -gt 0 ] && [ $i -lt 10 ]
		do
			i=$(( $i + 1 ))
        		sleep 0.5s
		done

		if [ $i -eq 10 ]
		then
			curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Motion could not be killed downstairs\"},\"priority\":10}"
		fi

		./ds_notification_daemon.sh &

	fi

	if ! [ -e upstairs_motion_log ] && [ $( ssh upstairs_cam 'ps -e | grep motion | wc -l' ) -eq 1 ]
        then

		curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Motion was running upstairs with no log\"},\"priority\":10}"

		rm upstairs_motion_log

                pid_string=$( ssh upstairs_cam 'ps -e | grep motion' )

                pid_string=$( echo $pid_string | awk '{print $1}' )

                kill $( ps -e | grep us_notif |awk '{print $1}' )

        	ssh upstairs_cam "kill -9 $pid_string"

		i=0

		while [ $( ssh upstairs_cam "raspistill -n -v" |& grep 'Failed to run camera app' | wc -l ) -gt 0 ] && [ $i -lt 10 ]
		do
			i=$(( $i + 1 ))
        		sleep 0.5s
		done

		if [ $i -eq 10 ]
		then
			curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"System Alert\",\"detail\":\"Motion could not be killed upstairs\"},\"priority\":10}"
		fi

		./us_notification_daemon.sh &

	fi

fi
