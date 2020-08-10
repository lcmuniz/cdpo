#!/bin/sh

FILE_DIR=`perl -e 'use Cwd "abs_path";print abs_path(shift)' $0`
BASE_DIR=`dirname $FILE_DIR`
PROJECT_NAME="resource-adaptor"
IMAGE_NAME=smart-cities/$PROJECT_NAME
NETWORK='platform'

CYAN='\033[0;36m'
NC='\033[0m'
RED='\033[0;31m'


CURRENT_DIR=`pwd`
cd $BASE_DIR/..

sudo docker -v > /dev/null
verify "Error: You need to install docker first!"

sudo docker-compose -v > /dev/null
verify "Error: You need to install docker-compose first!"

echo "Building docker image."
sudo docker-compose build
verify "Error: building docker image."

echo "Creating shared network."
sudo docker network create platform 2> /dev/null

cd $CURRENT_DIR

echo "${CYAN}"
echo "####################################"
echo "    To start the service run :"
echo "    $ scripts/development start"
echo "####################################"
echo "${NC}"

verify () {
  if [ $? != 0 ]; then
    printf "${RED}$1${NC}"
    exit 2
  fi
}