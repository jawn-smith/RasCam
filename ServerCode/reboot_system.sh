#!/bin/bash

ssh downstairs_cam 'sudo reboot'

while ! ping -q -c 1 192.168.0.12
do
	printf ""
done

printf "Camera Rebooted\n"

ssh upstairs_cam 'sudo reboot'

while ! ping -q -c 1 192.168.0.7
do
	printf ""
done

printf "Camera Rebooted\n"
