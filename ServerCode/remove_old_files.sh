#!/bin/bash

find videos/* -mtime +7 -exec rm {} \;
find archive/* -mtime +7 -exec rm {} \;
find pitures/* -mtime +7 -exec rm {} \;
