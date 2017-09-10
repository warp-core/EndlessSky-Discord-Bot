# EndlessSky-Discord-Bot
[![Build Status](https://travis-ci.org/MCOfficer/EndlessSky-Discord-Bot.svg?branch=master)](https://travis-ci.org/MCOfficer/EndlessSky-Discord-Bot)
[![license](https://img.shields.io/aur/license/yaourt.svg)](https://github.com/MCOfficer/EndlessSky-Discord-Bot/tree/master/LICENSE)

Meet James, the Discord Bot made specifically for the Endless Sky Server.

## Installation
### Requirements
- A JDK (recommended Java 8 or better)
- gradle on Linux, on Ẃindows use depr_gradlw.bat instead
- Python 2.7+ and the [requests module](http://docs.python-requests.org/en/master/)
### Setup
1. Clone or fork this repository
2. Using the Discord API, make a new Bot and save the "bot token" as token.txt in James' top-level directory (follow [this guide](https://github.com/DV8FromTheWorld/JDA/wiki/3%29-Getting-Started) until "2. Setup JDA Project")
3. Replace the 'HOST_RAW_URL' and 'HOST_PUBLIC_URL' and 'CONTENT_URL' strings with relevant URL destinations.
4. Start James by executing `gradle run` or `depr_gradlew run`

## Features
- Displays portions of the Endless Sky data files (Ships & their variants, Outfits, Sprites/Thumbnails, Missions)
- Links to PRs, commits and issues of the Endless Sky repository
- Links to the wiki at endless-sky.wikia.com
- Plays music using [lavaplayer](https://github.com/sedmelluq/lavaplayer)
- Performs text corrections based on the contents of '/data/spellErrors.txt'
- Posts both text- and image-based memes (limited to those defined in /data/)
- Basic Moderation Commands (currently only message purging, but more to come)
- Reacts when Members join/leave/get banned and gives them the merchant role, if necessary (disabled for now)
- Converts audio files to ES-compatible .wav files
- Some fun commands (random dogs and cats, NASA pictures, etc.)

## Credit
Original Creator: @Wrzlprnft

Original Maintainer / Hoster: @Nechochwen-D

Current Maintainer / Hoster: @MCOfficer

Contributors / PR Bots:
 - @tehhowch

24/7 Development Support: @MinnDevelopment and the JDA Discord Server
