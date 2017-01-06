#!/bin/bash

DBUSER=gestaltdev
DBPASS=password
DBNAME=gestalt-security
SECURITYIMG=galacticfog/gestalt-security:2.2.5

set -o errexit
set -o nounset
set -o pipefail
# set -x

# it's easier to remove it and then start a new one than to try to restart it if it exists with fallback on creation

echo Starting database in docker
docker pull postgres:9.4
db=$(docker run -P -d -e POSTGRES_DB=$DBNAME -e POSTGRES_USER=$DBUSER -e POSTGRES_PASSWORD=$DBPASS postgres:9.4)

DOCKERIP=$(docker inspect $db | jq -r '.[0].NetworkSettings.Ports."5432/tcp"[0].HostIp')
if [ "$DOCKERIP" == "0.0.0.0" ]; then 
  DOCKERIP="localhost"
fi
DBPORT=$(docker inspect $db | jq -r '.[0].NetworkSettings.Ports."5432/tcp"[0].HostPort')
DBHOST=$(docker inspect $db | jq -r '.[0].NetworkSettings.IPAddress')
echo "
DB running at $DOCKERIP:$DBPORT / $DBHOST:5432
"

export DATABASE_HOSTNAME=$DBHOST
export DATABASE_NAME=$DBNAME
export DATABASE_PORT=5432
export DATABASE_USERNAME=$DBUSER
export DATABASE_PASSWORD=$DBPASS

echo "
Running gestalt-security
"
docker pull $SECURITYIMG
security=$(docker run -P -d -e DATABASE_HOSTNAME -e DATABASE_NAME -e DATABASE_PORT -e DATABASE_USERNAME -e DATABASE_PASSWORD $SECURITYIMG)

SECPORT=$(docker inspect $security | jq -r '.[0].NetworkSettings.Ports."9000/tcp"[0].HostPort')
SECHOST=$(docker inspect $security | jq -r '.[0].NetworkSettings.IPAddress')
echo "
Security running at $DOCKERIP:$SECPORT / $SECHOST:9000
"

cleanup_docker() {
echo ""
echo Stopping db container
echo Stopped $(docker stop $db)
echo Removing db container
echo Removed $(docker rm $db)
echo Stopping security container
echo Stopped $(docker stop $security)
echo Removing security container
echo Removed $(docker rm $security)

echo Killing play app
if [ -e ./target/universal/stage/RUNNING_PID ]; then
  kill `cat ./target/universal/stage/RUNNING_PID`
fi 

echo ""
echo "List of running docker containers; make sure I didn't leave anything behind"
docker ps
}

trap cleanup_docker EXIT SIGSTOP SIGTERM

echo "
Initializing gestalt-security
"
sleep 10

init=$(http $DOCKERIP:$SECPORT/init username=testuser password=testpassword)

export GESTALT_SECURITY_HOSTNAME=$DOCKERIP
export GESTALT_SECURITY_PORT=$SECPORT
export GESTALT_SECURITY_PROTOCOL=http
export GESTALT_SECURITY_KEY=$(echo $init | jq -r '.[0].apiKey')
export GESTALT_SECURITY_SECRET=$(echo $init | jq -r '.[0].apiSecret')
echo "
Gestalt security environment variables for app:"
env | grep GESTALT

echo "
Running example app in the background
"

sbt clean update test stage
./target/universal/stage/bin/gestalt-framework-auth &

sleep 10

echo "
Auth attempt with no credentials:"
bad=$(http localhost:9000/ Authorization: || true)
if [[ `echo $bad | jq -r '.code'` -ne 401 ]]; then 
  echo "***** DID NOT GET EXPECTED 401"
  exit 1
else
  echo Got expected 401
fi

echo "
Auth attempt with bad credentials:"
bad=$(http -a BAD:CREDS localhost:9000/ || true)
if [[ `echo $bad | jq -r '.code'` -ne 401 ]]; then 
  echo "***** DID NOT GET EXPECTED 401"
  exit 1
else
  echo Got expected 401
fi

echo "
Auth attempt with good credentials:"
http --check-status -a "$GESTALT_SECURITY_KEY":"$GESTALT_SECURITY_SECRET" localhost:9000/

echo "
Manual tests passed.
"

exit 0
