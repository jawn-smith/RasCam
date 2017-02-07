#!/bin/bash

motions=$( ps -e | grep motion | awk '{print $1}' )

kill -9 $motions
