#!/bin/bash

raspivid -o - -t 0 --annotate 12 -w 704 -h 496 | tee /home/pi/vid_dump/downstairs_stream.h264 | cvlc -vvv stream:///dev/stdin --sout '#standard{access=http,mux=ts,dst=:8090}' :demux=h264
