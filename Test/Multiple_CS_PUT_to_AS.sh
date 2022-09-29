#!/bin/bash

verification_file="./verificationFiles/Multiple_CS_PUT_to_AS.txt"
output_file="./testOutputFiles/Multiple_CS_PUT_to_AS.txt" 

cd ../
# start AggregationServer in the background
java AggregationServer &

AG_PID=$!

sleep 1

# start 3 content servers with id CS1, CS2, and CS3 in the background
java ContentServer 127.0.0.1:4567 contentServerFeed1.txt CS1 &
CS1_PID=$!

java ContentServer 127.0.0.1:4567 contentServerFeed2.txt CS2 &
CS2_PID=$!

java ContentServer 127.0.0.1:4567 contentServerFeed3.txt CS3 &
CS3_PID=$!

sleep 3

# start a user to get the feed
java GETClient 127.0.0.1:4567 GC1 > ./Test/testOutputFiles/Multiple_CS_PUT_to_AS.txt
sleep 1

kill $AG_PID
kill $CS1_PID
kill $CS2_PID
kill $CS3_PID

cd Test

echo 
echo "========Test Result========"

# compare the ATOMFeed.xml with the verification file
if cmp -s $output_file $verification_file; then
    printf 'The file %s is the same as %s\n' "$output_file" "$verification_file"
    printf 'Multiple content server putting content test passed!\n'
else
    printf 'The file %s is different from %s\n' "$output_file" "$verification_file"
    printf 'Multiple content server putting content test failed!\n'
fi 

./reset_data.sh >> /dev/null 2>&1