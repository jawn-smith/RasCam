sleep $1

#restart motion daemon
if [ -e upstairs_motion_log ]
then
        rm upstairs_motion_log
fi

nohup ./us_notification_daemon.sh > upstairs_daemon.out 2> upstairs_daemon.err < /dev/null &
