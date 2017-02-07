#!/bin/bash

nohup ./pause_upstairs.sh $1 >pause_upstairs.out 2> pause_upstairs.err < /dev/null &
nohup ./pause_downstairs.sh $1 >pause_downstairs.out 2> pause_downstairs.err < /dev/null &

printf "System Paused for $1 minutes\n"
