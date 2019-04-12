#!/bin/bash
## This script is for star or stop payment-servicetend service
## Color renderin
 
Color_Off='\e[0m';
# ---- High Intensity ----
Red='\e[0;91m';  Green='\e[0;92m';

DATE=`date +%F-%T`
DIR=/deploy/PayFlash/payment-service
LOG_DIR=/var/log/payment
LOG_FILE=${LOG_DIR}/payment-service.log
PID=`ps -ef | grep "java" | grep -v grep |awk '{print $2}'`

if [ ! -d $LOG_DIR ] ; then

  mkdir $LOG_DIR

fi  

start_pay_service() {

netstat -tnlp | grep 8080 > /dev/null

if [ $? -eq 0 ]; then

  echo -e "${Green}Payment service is running $Color_Off "

else 

  echo -e "${Red}Begin to start Payment service $Color_Off" 

  mv $LOG_FILE ${LOG_FILE}_${DATE}.bak  

  cd $DIR && nohup java -jar target/payment-service-0.0.1-SNAPSHOT.jar > $LOG_FILE 2>&1 &   

  sleep 6

  if [ $? -eq 0 ]; then
     echo -e "${Green}Payment service start ok $Color_Off "
  fi

fi
}

stop_pay_service() {

netstat -tnlp | grep 8080 > /dev/null

if [ $? -eq 0 ]; then

  kill -9 $PID  &&  echo -e "${Red}Payment service stop ok $Color_Off " 

else 
  echo -e "${Red}Payment service is not running $Color_Off "
fi
}

case "$1" in 
        start) 
           
          start_pay_service;;

        stop)
 
          stop_pay_service  ;;

         *) 

          echo -e "${Red}This script only support start or stop parameter $Color_Off "

 esac 
