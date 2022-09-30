#!/bin/bash

./reset_data.sh >> /dev/null 2>&1

sleep 2
echo

./AG_Recovery.sh
echo
sleep 2

./Client_GET_connection_retry.sh
echo
sleep 2

./Client_GET_to_AS.sh
echo
sleep 2

./CS_connection_retry.sh
echo
sleep 2

./CS_Heartbeat.sh
echo
sleep 2

./CS_PUT_to_AS.sh
echo
sleep 2

./Multiple_Client_GET_to_AS.sh
echo
sleep 2

./Multiple_CS_PUT_to_AS.sh
echo
sleep 2

./AG_respond_204.sh
echo
sleep 2

./AG_respond_500.sh
echo
sleep 2

./reset_data.sh >> /dev/null 2>&1
