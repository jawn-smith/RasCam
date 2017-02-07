#!/bin/bash

targets=$( ps -e | grep status_check | awk '{print $1}' )

kill -9 $targets
