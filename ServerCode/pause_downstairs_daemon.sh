sleep $1

#restart motion daemon
if [ -e downstairs_motion_log ]
then
        rm downstairs_motion_log
fi

nohup ./ds_notification_daemon.sh > downstairs_daemon.out 2> downstairs_daemon.err < /dev/null &
