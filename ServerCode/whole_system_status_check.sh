
#!/bin/bash

# check if upstairs cam is powered on.
if ping -c 3 -q 192.168.0.7>ping.out
then
	upstairs_on=1
	#TODO: configure wake on LAN

else
	upstairs_on=0
	printf "The upstairs camera is not responding to pings.\n"
fi

# check if downstairs cam is powered on. If so, start motion daemon
if ping -c 3 -q 192.168.0.12>ping2.out
then
	downstairs_on=1

	#TODO: configure wake on LAN
else
	downstairs_on=0
	printf "the downstairs camera is not responding to pings.\n"
fi

# check if notification daemon is running on server
if ! [ $( ps -e | grep notification | wc -l ) -eq 0 ]
then
	notification=1
	#printf "The notification daemon is running on the server.\n"
else
	notification=0
	printf "The notification daemon is not running.\n"
fi

#check to make sure both cameras have the server mounted
if ! [ $( ssh downstairs_cam 'df -h | grep vid_dump | wc -l' ) -eq 0 ]
then
	#printf "The downstairs camera has mounted the server.\n"
	downstairs_mounted=1
else
	printf "The server is not mounted on the downstairs camera.\n"
	downstairs_mounted=0
fi

if ! [ $( ssh upstairs_cam 'df -h | grep vid_dump | wc -l' ) -eq 0 ]
then
	upstairs_mounted=1
	#printf "The upstairs camera has mounted the server.\n"
else
	upstairs_mounted=0
	printf "The server is not mounted on the upstairs camera.\n"
fi

if [ $downstairs_on -eq 1 ] && [ $upstairs_on -eq 1 ] && [ $notification -eq 1 ] && [ $downstairs_mounted -eq 1 ] && [ $upstairs_mounted -eq 1 ]
then
	ds_act=$( cat last_activity_downstairs.txt )
	us_act=$( cat last_activity_upstairs.txt )
	#printf "The system is running properly.\nThe last activity downstairs was $ds_act.\nThe last activity upstairs was $us_act.\n"
	printf "The system is running properly.\n"
fi

# Start dameons to ensure motion detection is working properly on both cameras. Hopefully these daemons can run from the server
