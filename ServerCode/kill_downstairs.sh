#!/bin/bash

kill $( ps -e | grep ds_notification | awk '{print $1}' )

ssh downstairs_cam "kill $( ps -e | grep motion | awk '{print $1}' )"


