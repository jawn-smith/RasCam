#!/bin/bash

ssh downstairs_cam 'sudo reboot'

while ! ping -q -c 1 192.168.0.12 >ping_downstairs.out 2>ping_downstairs.err
do
	printf ""
done

printf "Camera Rebooted\n"
