#!/bin/bash

# Get branch names and commits
stable=$(git rev-list --format=%s --max-count=1 stable 2>/dev/null)
hasStable=$?
master=$(git rev-list --format=%s --max-count=1 master 2>/dev/null)
hasMaster=$?

if [ "$hasStable" == 0 ]; then
  echo "$stable" > "./data/stable_version.txt"
fi
if [ "$hasMaster" == 0 ]; then
  echo "$master" > "./data/master_version.txt"
fi

currentName=$(git rev-parse --abbrev-ref HEAD)
if [ "$currentName" == "master" ]; then
  exit 0
fi
if [ "$currentName" == "stable" ]; then
  exit 0
fi

currentInfo=$(git rev-list --format=%s --max-count=1 "$currentName" 2>/dev/null)
echo "$currentInfo" > "./data/""$currentName""_version.txt"
