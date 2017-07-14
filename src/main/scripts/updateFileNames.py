#! /usr/bin/python

# Use the GitHub API to obtain the filenames in Endless-Sky/data are
# and then replace the dataFileNames.txt with the new list.
# You may need to run `pip install requests` from your command line first.

import os
import requests

key = ''
url = 'https://api.github.com/repos/endless-sky/endless-sky/contents/data'
dataFileRelPath = '../../../data/dataFileNames.txt'
authRelPath = '../../../gitauth.txt'
useAuth = False
gotData = False

# Orient the script within the filesystem.
scriptDir = os.path.dirname(os.path.abspath(__file__))
dataFilePath = os.path.abspath(os.path.join(scriptDir, *dataFileRelPath.split('/')))
backupPath = os.path.join(os.path.dirname(dataFilePath), 'dfn.old')
authPath = os.path.abspath(os.path.join(scriptDir, *authRelPath.split('/')))

if os.path.exists(dataFilePath):
    os.rename(dataFilePath,backupPath)

# Read OAuth token for GitHub, if possible.
if os.path.exists(authPath):
    useAuth = True
    key = open(authPath,'r').read().splitlines()[0]

if useAuth:
    # 5k requests per hour.
    headers = {'Authorization':'token %s' % key}
    params = {'per_page': 100}
    r = requests.get(url, params=params, headers=headers)
    if r.status_code == 200:
        gotData = True
else:
    # 60 requests per hour maximum.
    r = requests.get(url)
    if r.status_code == 200 and len(r.json()) != 30:
        gotData = True

# Parse the returned contents of the es data/ directory.
filenames = ''
if gotData:
    numFiles = len(r.json())
    for fileblob in r.json():
        filenames += fileblob['name'].replace(' ','%20') + '\n'

# Write the new names to the datafile.
if len(filenames) > 0:
    with open(dataFilePath,'w') as output:
        for line in filenames:
            output.write(line)
    print 'Completed update operation.'
