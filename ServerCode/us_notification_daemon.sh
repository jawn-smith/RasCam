#!/bin/bash

while :
do
	piccount=`ls -l Upstairs*.jpg 2>/dev/null | wc -l`
        if [ $piccount != 0 ]
        then
                saveName=$( ls -lt Upstairs*jpg | head -n 1 | awk '{print $9}' )
                saveName=${saveName:0:$(( ${#saveName} - 10 ))}
                mpv mf://Upstairs*.jpg --mf-fps=2 --ovc libx264 --ovcopts=bitrate=1200:threads=2 -o $saveName.mp4
                mv $saveName.mp4 archive/
                mv Upstairs*jpg pictures/

        fi

        vidcount=`ls -l Upstairs-Video*.h264 2>/dev/null | wc -l`
        if [ $vidcount != 0 ] && [ $( ssh upstairs_cam 'ps -e | grep raspivid | wc -l' ) -eq 0 ]
        then
                # convert video file to mp4
                video_name=$( ls Upstairs*.h264 )
                video_name=${video_name%.*}
                MP4Box -add $video_name.h264 $video_name.mp4
		mv $video_name.mp4 videos/
		rm Upstairs*h264
		#mv Upstairs*h264 archive/
        fi

	#if neither motion nor raspivid is running and the system is not paused or taking a photo, start motion

        if [ $( ssh upstairs_cam 'echo $(( $( ps -e | grep motion | wc -l ) + $( ps -e | grep raspivid | wc -l ) ))' ) -eq 0 ]
        then
                ssh upstairs_cam 'cd vid_dump; nohup motion -l /home/pi/vid_dump/upstairs_motion_log -p /home/pi/vid_dump/upstairs_pid > upstairs_motion.out 2> upstairs_motion.err < /dev/null &'
        fi

	if [ -e upstairs_motion_log ]
	then
	        #Test if motion was detected
        	if grep -q "Motion detected" upstairs_motion_log
        	then
			video_name=$( date +Upstairs-Video-%m-%d-%Y-%H-%M )

			echo $( date +%h\ %d\ %Y\ %H:%M ) > last_activity_upstairs.txt

			#timestamp=$( date +%H:%M )

			pid_string=$( ssh upstairs_cam 'ps -e | grep motion' )

			pid_string=$( echo $pid_string | awk '{print $1}' )

        	        #kill motion, start video
			ssh upstairs_cam "cd vid_dump; kill $pid_string; sleep 4s; nohup raspivid -n -hf -vf -w 960 -h 720 -t 120000 --annotate 12 -o $video_name.h264 > upstairs_vid.out 2> upstairs_vid.err < /dev/null &"

	                rm upstairs_motion_log
			#middle_pic=$( ls -lc *jpg | head -n 2 | tail -1 | awk '{print $9}'  )

	                # Alert Users by sending them middle picture
			william=$( cat user1_token.txt )
			heather=$( cat user2_token.txt )
        	        #echo "Motion was detected by the upstairs security camera in these images. Additional photos and a video have been saved to the server." | mutt -s "Security Alert" -a *.jpg -- $recipients
			curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$heather\",\"data\":{\"title\":\"Security Alert\",\"detail\":\"Motion was detected by the upstairs camera\"},\"priority\":10}"
			curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"Security Alert\",\"detail\":\"Motion was detected by the upstairs camera\"},\"priority\":10}"

	                # Archive Files
                        sleep 10s

                        if ! [ $( ls -l Upstairs*jpg | wc -l ) -eq 0 ]
			then
                                saveName=$( date +Upstairs-Motion-%m-%d-%Y-%H-%M )
                                mpv mf://Upstairs*.jpg --mf-fps=2 --ovc libx264 --ovcopts=bitrate=1200:threads=2 -o $saveName.mp4
                                sleep 5s
				mv $saveName.mp4 archive/
                                mv Upstairs*jpg pictures/
                                rm *avi
                        fi

		fi

        fi

done
