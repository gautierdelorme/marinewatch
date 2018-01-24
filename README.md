# Marinewatch

## Overview

### General

![marinwatch](https://user-images.githubusercontent.com/6973663/35242375-b0621ef4-ffb9-11e7-93c3-5ed068249582.png)

### Lambda Architecture

![marinwatch_spark_neo4j](https://user-images.githubusercontent.com/6973663/35242373-b020bf90-ffb9-11e7-86cf-4781b2076300.png)

### Batch processing

![marinwatch_batch_processing](https://user-images.githubusercontent.com/6973663/35242372-b004e39c-ffb9-11e7-94e0-02ce4d3f9542.png)

### Streaming Processing

![marinwatch_streaming_processing](https://user-images.githubusercontent.com/6973663/35332611-d940165c-010b-11e8-921c-60c1cd1a9414.png)

## Developer setup

### Requirements

- rbenv
- docker (+ docker-compose)

### Project setup

#### Go to the project directory

    git clone git@github.com:fogglyorg/marinewatch.git
    cd marinewatch

#### Start docker containers

    docker-compose up -d

#### Install the required version of Ruby

    rbenv install
    rbenv rehash

#### Install Bundler

    gem install bundler
    rbenv rehash

#### Install required gems for the web api
    cd ./code/web_api
    bundle install
    rbenv rehash


## Project Overview

### Technologies

- Docker as containers manager
- Batch and Streaming processing using Spark and HDFS (written in Scala)
- Neo4j as graph database
- Web and Streaming APIs built in Ruby (using Sinatra)
- CLI tool written in pure bash
- Git as versioning system

### Architecture

- `code/`: contains all the marinewatch code
  - `mwspark` Spark Scala application
    - Batch processing to generate structured data to be imported in Neo4j
    - Streaming processing to update Neo4j data in real time
  - `web_api` Web API to get shortest path between two geo coordinates (supported formats: `html`, `json` and `kml`)
  - `streaming_api` Streaming API to push new updates from boats
- `data/`: contains all data files
  - `input` Data files used by Spark jobs
  - `output` Data files generated by Spark jobs
- `neo4j/`:
  - `conf`: Neo4j config files
  - `data`: Neo4j databases
  - `logs`: Logs generated by Neo4j
  - `plugins`: Plugins used by Neo4j
- `docker-compose.yml`: Docker config file
- `marinewatch-cli`: CLI tool used to manage the app

## How it works

**Important:**

- You need to have docker running

#### Run  Batch Spark job with specified accuracy

    ./marinewatch-cli -b 40

#### Run Streaming Spark job listening on specified address

    ./marinewatch-cli -c

#### Create Neo4j database

    ./marinewatch-cli -u dbname
    # create new database named dbname
    # import new data inside, start the database
    # create an index on (latitude,longitude)

#### Start existing Neo4j database

    ./marinewatch-cli -d dbname

#### Start web API

    ./marinewatch-cli -s
    # You can see the result from this endpoint for example
    # http://localhost:4567/route?from=39.425,6.825&to=6.225,103.050

#### Batch processing + New database + Web Server in a single command

    ./marinewatch-cli -s -b 40 -u dbname

#### Full CLI Documentation

    $ ./marinewatch-cli -h
    Usage: marinewatch-cli [-h] [-b <int>] [-c <string>] [-u <string>] [-d <string>] [-s] [-t]

      -h  Help. Display this message and quit.
      -b  <int>  Run batch process with specified accuracy.
      -c  <string>  Run streaming process listening on specified address.
      -u  <string>  Create new database with name.
      -d  <string>  Start database with name.
      -s  Start web server.
      -t  Start streaming server.


## Improvements to do

- ✅ ~~Improve speed~~
- ✅ ~~Use datasets with better accuracy (1/40)~~
- ✅ ~~Add Spark Streaming processing~~
- Do not restrict cost to boats density
- ...

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
