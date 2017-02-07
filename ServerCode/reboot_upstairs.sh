#!/bin/bash

ssh upstairs_cam 'sudo reboot'

while ! ping -q -c 1 192.168.0.7 >ping_upstairs.out 2>ping_downstairs.err
do
	printf ""
done

printf "Camera Rebooted\n"
