#!/bin/bash

ssh upstairs_cam "sudo poweroff"

while ping -q -c 1 192.168.0.7
do
	sleep 0.5s
done

printf "Shutdown Successful\n"
