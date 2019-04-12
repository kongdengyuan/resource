#!/bin/bash
## This script is for star or stop payment-frontend service
## Color renderin
 
Color_Off='\e[0m';
# ---- High Intensity ----
Red='\e[0;91m';  Green='\e[0;92m';

DATE=`date +%F-%T`
DIR=/deploy/PayFlash/payment-frontend
LOG_DIR=/var/log/payment
LOG_FILE=${LOG_DIR}/payment-frontend.log
PID=`ps -ef | grep "ng serve" | grep -v grep |awk '{print $2}'`

if [ ! -d $LOG_DIR ] ; then

  mkdir $LOG_DIR

fi  

start_pay_fron() {

netstat -tnlp | grep 4200 > /dev/null

if [ $? -eq 0 ]; then

  echo -e "${Green}Payment-frontend service is running $Color_Off "

else 

  echo -e "${Red}Begin to start Payment-frontend service $Color_Off" 

  mv $LOG_FILE ${LOG_FILE}_${DATE}.bak  

  cd $DIR && nohup  ng serve --disableHostCheck true > $LOG_FILE 2>&1 &   

  sleep 6

  if [ $? -eq 0 ]; then
     echo -e "${Green}Payment-frontend service start ok $Color_Off "
  fi

fi
}

stop_pay_fron() {

netstat -tnlp | grep 4200 > /dev/null

if [ $? -eq 0 ]; then

  kill -9 $PID  &&  echo -e "${Red}Payment-frontend service stop ok $Color_Off " 

else 
  echo -e "${Red}Payment-frontend service is not running $Color_Off "
fi
}

case "$1" in 
        start) 
           
          start_pay_fron;;

        stop)
 
          stop_pay_fron  ;;

         *) 

          echo -e "${Red}This script only support start or stop parameter $Color_Off "

 esac 
