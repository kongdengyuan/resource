#!/bin/bash
## This script is for star or stop payment-servicetend service
## Color renderin
 
Color_Off='\e[0m';
# ---- High Intensity ----
Red='\e[0;91m';  Green='\e[0;92m';

PID=`ps -ef | grep "node" | grep -v grep |awk '{print $2}'`
DIR=/deploy/PayFlash/payment-manager

stop_pay_manager() {

netstat -tnlp | grep 3000 > /dev/null

if [ $? -eq 0 ]; then

  kill -9 $PID  &&  echo -e "${Red}Payment manager stop ok $Color_Off " 

else 
  echo -e "${Red}Payment manager is not running $Color_Off "
fi
}

case "$1" in 

        start)
              cd $DIR && ./start_x4_service.sh  ;;

        stop)
 
          stop_pay_manager  ;;

         *) 

          echo -e "${Red}This script only support start or stop parameter $Color_Off "

 esac 
