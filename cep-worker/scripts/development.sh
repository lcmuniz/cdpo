#!/bin/sh

FILE_DIR=`perl -e 'use Cwd "abs_path";print abs_path(shift)' $0`
BASE_DIR=`dirname $FILE_DIR`/..

PROJECT_NAME="cep-worker"

CYAN='\033[0;36m'
NC='\033[0m'
RED='\033[0;31m'

verify () {
  if [ $? != 0 ]; then
    printf "${RED}$1${NC}"
    exit 2
  fi
}

if [ "$1" = "start" ]; then
  printf "${CYAN}"
  echo "Starting service $PROJECT_NAME"
  printf "${NC}"

  #create rabbitmq
  sudo docker-compose up -d rabbitmq-cep
  
  sleep 10

  #create redis
  sudo docker-compose up -d redis-cep

  #setup rabbitmq
  #sudo docker exec rabbitmq-cep rabbitmqctl add_user worker1 12345
  sudo docker exec rabbitmq-cep rabbitmq-plugins enable rabbitmq_management
  #sudo docker exec rabbitmq-cep rabbitmqctl set_permissions -p / worker1 ".*" ".*" ".*"
  #sudo docker exec rabbitmq-cep rabbitmqctl set_user_tags worker1 administrator

  docker exec rabbitmq-cep rabbitmqctl add_user worker1 12345 && docker exec rabbitmq-cep rabbitmqctl set_permissions -p / worker1 ".*" ".*" ".*" && docker exec rabbitmq-cep rabbitmqctl set_user_tags worker1 administrator


  sudo docker exec rabbitmq-cep rabbitmqctl add_user sender 12345
  sudo docker exec rabbitmq-cep rabbitmqctl set_permissions -p / sender ".*" ".*" ".*"
  sudo docker exec rabbitmq-cep rabbitmqctl set_user_tags sender administrator

  #create cep-worker
  sudo docker-compose up cep-worker
  

  sudo docker-compose up -d
  verify "Cannot run the container. For more information take a look in docker's log"

 
  
fi

if [ "$1" = "stop" ]; then
  printf "${CYAN}"
  echo "Stopping service $PROJECT_NAME"
  printf "${NC}"


  sudo docker-compose stop
  verify "You have some issue when we try to stop the container. Verify it manually"

  printf "${CYAN}"
  echo "####################################"
  echo "     The container is stopped."
  echo "####################################"
  printf "${NC}"
fi

if [ "$1" = "exec" ]; then
  printf "${CYAN}"
  echo "service does not accept commands"
  printf "${NC}"
fi

if [ "$1" = "test" ]; then
  printf "${CYAN}"
  echo "Tests are already executed in setup"
  printf "${NC}"
fi

