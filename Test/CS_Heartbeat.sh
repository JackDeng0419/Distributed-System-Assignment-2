#!/bin/bash

verification_file="./verificationFiles/CS_Heartbeat.txt"
output_file="./testOutputFiles/CS_Heartbeat.txt" 

cd ../
# start AggregationServer in the background
java AggregationServer &
AG_PID=$!

sleep 1

# start 2 content servers with id CS1 and CS2 in the background
java ContentServer 127.0.0.1:4567 contentServerFeed1.txt CS1 &
CS1_PID=$!
sleep 500m

java ContentServer 127.0.0.1:4567 contentServerFeed2.txt CS2 &
CS2_PID=$!
sleep 5

# kill the CS1
kill $CS1_PID
sleep 13

# start a get client
java GETClient 127.0.0.1:4567 GC1 > ./Test/testOutputFiles/CS_Heartbeat.txt
sleep 2

kill $AG_PID
kill $CS2_PID

cd Test

echo 
echo "========Test Result========"

# compare the ATOMFeed.xml with the verification file
if cmp -s $output_file $verification_file; then
    printf 'The file %s is the same as %s\n' "$output_file" "$verification_file"
    printf 'Content server heartbeat test passed!\n'
else
    printf 'The file %s is different from %s\n' "$output_file" "$verification_file"
    printf 'Conetent server heartbeat test failed!\n'
fi 

./reset_data.sh >> /dev/null 2>&1