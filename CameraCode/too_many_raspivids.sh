#!/bin/bash

raspivids=$( ps -e | grep raspivid | awk '{print $1}' )

kill $raspivids
