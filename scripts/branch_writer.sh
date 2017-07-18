#!/bin/bash

# Get the currently checked-out branch and write to a file
branchName=$(git rev-parse --abbrev-ref HEAD)
hasName=$?

if [ "$hasName" == 0 ]; then
	echo "$branchName" > "./data/branch_name.txt"
else
	exit 1
fi
