#!/bin/bash

## Color renderin

Color_Off='\e[0m';
# ---- High Intensity ----
Red='\e[0;91m';  Green='\e[0;92m';

CODE_DIR=/deploy/PayFlash
SQL="psql -h localhost -U postgres"

#Install x4 

checkRetVal () {
  if [ $? -ne 0 ]; then
      exit $?
  else 
  
    echo -e "${Green}Service install success $Color_Off " &&  sleep 1 
 
  fi
}

#cd $CODE_DIR/payment-manager 
#./setup_x4_service.sh 
#cp /root/HtmlPane.js /deploy/x4/ui/client/resources/sap/b/controls/panes

#checkRetVal 

#Install payment service

cd $CODE_DIR/payment-service
#mvn package 
$SQL  -c 'drop table "BYD"."pfPayment"'
$SQL   --set ON_ERROR_STOP=ON -f  dbInit_PostgreSQL.sql 

checkRetVal

#Install payment frontend

cd  $CODE_DIR/payment-frontend
npm install 

checkRetVal



