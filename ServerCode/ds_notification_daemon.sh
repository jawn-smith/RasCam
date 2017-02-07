#!/bin/bash

while :
do

	piccount=`ls -l Downstairs*.jpg 2>/dev/null | wc -l`
	if [ $piccount != 0 ]
	then
		saveName=$( ls -lt Downstairs*jpg | head -n 1 | awk '{print $9}' )
		saveName=${saveName:0:$(( ${#saveName} - 10 ))}
                mpv mf://Downstairs*.jpg --mf-fps=2 --ovc libx264 --ovcopts=bitrate=1200:threads=2 -o $saveName.mp4
                mv $saveName.mp4 archive/
                mv Downstairs*jpg pictures/

        fi

	# if a h264 file exists and raspivid is not currently running, convert the file and move it to videos folder
	vidcount=`ls -l Downstairs-Video*.h264 2>/dev/null | wc -l`
	#echo $vidcount
	if [ $vidcount != 0 ] && [ $( ssh downstairs_cam 'ps -e | grep raspivid | wc -l' ) -eq 0 ]
	then
		# convert video file to mp4
		video_name=$( ls Downstairs*.h264 )
		video_name=${video_name%.*}
                MP4Box -add $video_name.h264 $video_name.mp4
		mv $videos_name.mp4 videos/
		rm Downstairs*h264
		#mv Downstairs*h264 archive/
	fi

	vidcount=`ls -l Downstairs*.mp4 2>/dev/null | wc -l`
        #echo $vidcount
        if [ $vidcount != 0 ]
        then
                mv Downstairs*mp4 videos/
        fi

	#if neither motion nor raspivid is running and the system is not paused or taking a photo, start motion
	if [ $( ssh downstairs_cam 'echo $(( $( ps -e | grep motion | wc -l ) + $( ps -e | grep raspivid | wc -l ) ))' ) -eq 0 ] && [ $( ps -e | grep pause_downs | wc -l ) -eq 0 ] && [ $( ps -e | grep ideo_downs | wc -l ) -eq 0 ] && [ $( ps -e | grep photo_downs | wc -l ) -eq 0 ] && [ $( ps -e | grep status_c | wc -l ) -eq 0 ]
	then
		ssh downstairs_cam 'cd vid_dump; nohup motion -l /home/pi/vid_dump/downstairs_motion_log -p /home/pi/vid_dump/downstairs_pid > downstairs_motion.out 2> downstairs_motion.err < /dev/null &'
	fi

	if [ -e downstairs_motion_log ]
	then
	        #Test if motion was detected by downstairs camera
        	if grep -q "Motion detected" downstairs_motion_log
        	then
			video_name=$( date +Downstairs-Video-%m-%d-%Y-%H-%M )

			echo $( date +%h\ %d\ %Y\ %H:%M ) > last_activity_downstairs.txt

			#timestamp=$( date +%H:%M )

			pid_string=$( ssh downstairs_cam 'ps -e | grep motion' )

			pid_string=$( echo $pid_string | awk '{print $1}' )

        	        #kill motion, start video
			ssh downstairs_cam "cd vid_dump; kill $pid_string; sleep 4s; nohup raspivid -n -v -w 960 -h 720 -t 120000 --annotate 12 -o $video_name.h264 > downstairs_vid.out 2> downstairs_vid.err < /dev/null &"

	                rm downstairs_motion_log
			#middle_pic=$( ls -lc *jpg | head -n 2 | tail -1 | awk '{print $9}'  )

	                # Alert Users by sending them middle picture
			william=$( cat william_token.txt )
			heather=$( cat heather_token.txt )
	       	        #echo "Motion was detected by the downstairs security camera in these images. Additional photos and a video have been saved to the server." | mutt -s "Security Alert" -a *.jpg -- $recipients
			curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$heather\",\"data\":{\"title\":\"Security Alert\",\"detail\":\"Motion was detected by the downstairs camera\"},\"priority\":10}"
			curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"Security Alert\",\"detail\":\"Motion was detected by the downstairs camera\"},\"priority\":10}"

	                # Archive Files
        	        #mv *jpg pictures/

			sleep 10s

			if [ $( ls -l Downstairs*jpg | wc -l ) -eq 0 ]
			then
				curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"Security Alert\",\"detail\":\"No JPGs were created\"},\"priority\":10}"

			else
				saveName=$( date +Downstairs-Motion-%m-%d-%Y-%H-%M )
				mpv mf://Downstairs*.jpg --mf-fps=2 --ovc libx264 --ovcopts=bitrate=1200:threads=2 -o $saveName.mp4
				if ! [ -e $saveName.mp4 ]
				then
					curl -X POST --header "Authorization: key=AIzaSyACyMENSr9N-YeGCakqn--aISKr0473SvA" --Header "Content-Type: application/json" https://fcm.googleapis.com/fcm/send -d "{\"to\":\"$william\",\"data\":{\"title\":\"Security Alert\",\"detail\":\"The Jpgs were not turned into an mp4\"},\"priority\":10}"
				fi
				mv $saveName.mp4 archive/
				mv Downstairs*jpg pictures/
				rm *avi
			fi

		fi

        fi

done
