# Rating update tool for IMDB ratings in Plex libraries

A tool to update the IMDB ratings for Plex libraries that contain movies via the IMDB, TMDB and TVDB agents.

## Important

~~**This tool does not work with the newly released Plex Movie Agent. Libraries that use this agent will be ignored and not modified. Refer to [this issue](https://github.com/mynttt/UpdateTool/issues/28) for more information.**~~ - **Works now with [Beta 1.20.1](https://forums.plex.tv/t/native-plex-agents-allow-access-to-external-provider-ids-for-media-eg-imdb-tmdb-tvdb/619090/129)!**

**While the DB issues are fixed now and using this tool is likely safe to use and never caused issues for myself and a large part of the user base it shall still be noted that this tool should be used with caution and that I'm not responsible for any damages within your PMS database. The database interaction of this tool is minimal and within the milliseconds realm and the queries are executed cleanly by using SQLite's transactions.**

#### This tool could in theory break if either the Plex database schema changes or IMDB stops providing a public rating dataset! This would not be dangerous tho as it stops when something goes wrong. 

#### If you want to run this on Windows without docker look [here](https://github.com/mynttt/UpdateTool/wiki/Installation-on-Windows)! There is also a GUI for the tool that is discussed [here](#GUI).

#### TV Show updates are done by aired season / aired episode. If you use DVD order for some reason please create an issue and I will implement a flag for you to switch the tool between. Plex uses aired season / aired episode by default tho!

## What does this do?

The Plex IMDB agent is kind of meh... Sometimes it is not able to retrieve the ratings for newly released movies even tho it matches the IMDB ID with them. It is impossible to comfortable add the rating manually from the users perspective.

This tool allows you to update the database that stores this data with the correct IMDB ratings. It will correct outdated and missing ratings and also set a flag to display the IMDB badge next to the ratings. The source used to retrieve the ratings is the official IMDB dataset that is updated daily and free for non-commercial use. Its download and refreshment will be handled automatically by this tool.

An advantage is that it works outside Plex by manipulating the local Plex database. Thus, no metadata refresh operations have to be done within Plex. It is faster and will not lead into the unforeseen consequences that one sometimes experiences with a Plex metadata refresh (missing or changed posters if not using a custom poster).

Before (Not IMDB matched)            |  After Match
:-------------------------:|:-------------------------:
![](img/star.PNG)  |  ![](img/imdb.PNG)

*Above are two different movies, that why the genres changed*

### This tool processes the following libraries:

Type | Description | Agent
:-------------------------:|:-------------------------:|:-------------------------:
MOVIE | Plex Movie Agent with IMDB | com.plexapp.agents.imdb
MOVIE | TMDB Movie Agent (if TMDB API key set) | com.plexapp.agents.themoviedb
MOVIE | New Plex Movie Agent (if IMDB as ratings source set, currently only in Beta 1.20.1) | tv.plex.agents.movie
TV SHOW | TMDB Series Agent (if TMDB API key set) | com.plexapp.agents.themoviedb
TV SHOW | TVDB Series Agent (if TVDB auth string set) |com.plexapp.agents.thetvdb

In my library with 1800 movies it transformed entries for 698 items and 1000+ entries for series. If it is left running, every few days when a new IMDB data set is pulled a few ratings will update as well.

# Docker

Docker is on [dockerhub](https://hub.docker.com/r/mynttt/updatetool) | [UnRaid instructions](#docker-on-unraid)

## Environment Variables Guide

Name | Description
:-------------------------:|:-------------------------:|
TMDB_API_KEY|Enables TMDB Movie/Series library processing
TVDB_API_KEY|Enables TVDB Series library processing
IGNORE_LIBS|Ignore libraries with certain IDs ([more here](#Ignore-libraries-from-being-updated))
CAPABILITIES|Custom flags for the tool ([more here](#supply-custom-capability-flags))
JVM_MAX_HEAP|Specify max. heap allocatable by the JVM (default 256m). Can be useful if you have a really large library (40000+ items) and you run in memory related crashes. Must be specified in bytes (i.e. 256m, 1g, 2g, 512m)

Deprecated variables can still be used although their usage is discouraged.

### Deprecated Environment Variables
Name | Description | Deprecation
:-------------------------:|:-------------------------:|:-------------------------:|
TVDB_AUTH_STRING|Enables TVDB Series library processing|API Key is enough for this tool to work

## Docker on UnRaid

There is a template repository available now: https://github.com/mynttt/unraid-templates

You can add that repository under `Template repositories:` in the UnRaid Docker section, hit save and thus will be able to add UpdateTool via the `Add Container` button.

A configuration skeleton will be added and you will have to set up the `Plex Media Server` directory and optionally your TVDB / TMDB API Keys in case you want to use TMBD / TVDB resolvement.

After completing the configuration steps, you can start the container. If it has errors, it will stop. The log in the config folder shows you what it does or why it crashed if that happens.

*TMDB and TVDB are optional settings that are not required for base movie imdb operations! TMDB unlocks matching for movies that have a TMBD match for whatever reason, TMDB matched series libraries and TVDB allows to update series as well!*

## To run your docker without UnRaid templates:

```bash
docker pull mynttt/updatetool

# Without TMDB fallback

docker run -dit -e RUN_EVERY_N_HOURS=12 \
    -v "/mnt/data/Plex Media Server":/plexdata \
    -v "/mnt/data/imdpupdaterconfig":/config \
    mynttt/updatetool

# With TMDB fallback

docker run -dit -e RUN_EVERY_N_HOURS=12 \
    -e TMDB_API_KEY=yourkey \
    -v "/mnt/data/Plex Media Server":/plexdata \
    -v "/mnt/data/imdpupdaterconfig":/config \
    mynttt/updatetool
    
# With TMDB fallback and TVDB resolvement for series

docker run -dit -e RUN_EVERY_N_HOURS=12 \
    -e TMDB_API_KEY=yourkey \
    -e TVDB_API_KEY=tvdbapikey \
    -v "/mnt/data/Plex Media Server":/plexdata \
    -v "/mnt/data/imdpupdaterconfig":/config \
    mynttt/updatetool
```

Explained:

```bash
docker run -dit 
     # Invoke every 12h
    -e RUN_EVERY_N_HOURS=12 \
     # Optional parameter: will try to get an IMDB ID from TMDB matched items
    -e TMDB_API_KEY=yourkey \
     # Three items are required to auth with TVDB username, userkey, apikey
     # Supply these as semicolon seperated values. Example: username;DAWIDK9CJKWFJAWKF;e33914feabd52e8192011b0ce6c8
    -e TVDB_API_KEY=tvdbapikey \
     # The plex data root (that contains Plug-ins, Metadata, ...
     # https://support.plex.tv/articles/202915258-where-is-the-plex-media-server-data-directory-located/
    -v "/mnt/data/Plex Media Server":/plexdata \
     # A path where you want to store the log and state files
    -v "/mnt/data/imdpupdaterconfig":/config \
    mynttt/updatetool
```

[Where is the data folder of the Plex Media Server located on my system?](https://support.plex.tv/articles/202915258-where-is-the-plex-media-server-data-directory-located/)

[TVDB API Key](https://thetvdb.com/dashboard/account/apikey)

*"/mnt/data/Plex Media Server" and "/mnt/data/imdpupdaterconfig" are just sample paths! Set your own paths there or it will probably not work!*

*On windows the \ syntax to make the command multiline will not work. You have to remove those and make the command a single line command!*

## Other configuration values

### Ignore libraries from being updated

You can ignore libraries by setting the environment variable **IGNORE_LIBS**.

IGNORE_LIBS takes a list of library ids as a semicolon seperated string. You can find the id of a plex library by opening it in plex and then looking at the URL in the browser.

```
Lets find the ID for the library 'Movies':
1.) Select 'Movies' in the Plex web interface
2.) Look at the URL in the browser, it should end like this: &context=content.library&source=1
3.) source=1 tells us that the ID for 'Movies' is 1
```

**Examples:**

Ignoring only the library with ID 1: 

```
IGNORE_LIBS="1"
```

Ignoring the libraries with the IDs 1, 5, 8: 

```
IGNORE_LIBS="1;5;8"
```

### Supply custom capability flags

You can apply custom flags to the tool via the **CAPABILITIES** environment variable.

Currently the following flags exist:

Flag | Description
:-------------------------:|:-------------------------:|
NO_TV |Ignore all TV Show libraries
NO_MOVIE | Ignore all Movie libraries

Multiple flags can be supplied as a semicolon separated string.

**Examples:**

Don't process TV libraries

```
CAPABILITIES="NO_TV"
```

Render the tool useless by skipping TV and Movie libraries

```
CAPABILITIES="NO_TV;NO_MOVIE";
```

# Technical details

This tool supplies one mode at the moment:

### docker mode
Provides a watchdog that once started will run every N hours over all IMDB supported libraries.

# Runtime requirements

- Java >= 11

# Usage

### Docker mode:

In docker mode the tool will read the environment variable PLEX_DATA_DIR and optionally TMDB_API_KEY if TMDB fallback should be enabled.

It can then be invoked with:
- no args (every 12h)
- the schedule arg (every n hour(s))

```
java -jar UpdateTool-xxx.jar imdb-docker {}
java -jar UpdateTool-xxx.jar imdb-docker {schedule=12}
```

Example:

```bash
# Export variables if on Linux/Mac
PLEX_DATA_DIR="/mnt/user/Plex Media Server"
export PLEX_DATA_DIR

# Export variables if on Windows
set PLEX_DATA_DIR=C:\User\Data\Plex Media Server

# Default start
java -jar UpdateTool-xxx.jar imdb-docker {}
# Run every 5 hours
java -jar UpdateTool-xxx.jar imdb-docker {schedule=5}
```

### GUI

There is also a GUI to assist users that feel uncomfortable with the CLI way of interacting with the tool. It supports Windows, MacOS and Linux and only [requires the Java 11+ runtime](https://adoptopenjdk.net/?variant=openjdk11&jvmVariant=hotspot) and can be downloaded [here](https://github.com/mynttt/UpdateTool/releases/tag/g1.0.0).

![](https://raw.githubusercontent.com/mynttt/UpdateTool/master/img/gui.PNG)

To use the GUI just double click on the jar file. The GUI will automatically download and update the used version of UpdateTool. You are only required to set the path to the Plex Media Server data folder and submit a path to your java executable. If you want to enable TMDB/TVDB resolvement simply tick the boxes and supply the API keys via the text fields. To start and stop the tool use the respective buttons.
