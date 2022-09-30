#!/bin/bash

./reset_data.sh >> /dev/null 2>&1

verification_file="./verificationFiles/Client_GET_to_AS.txt"
output_file="./testOutputFiles/Client_GET_to_AS.txt" 

cd ../
# start AggregationServer in the background
java AggregationServer &

AG_PID=$!

sleep 1

# start a content servers with id CS1 in the background
java ContentServer 127.0.0.1:4567 contentServerFeed1.txt CS1 &
CS1_PID=$!

sleep 1

# start a client with id GC1 to get the feed
java GETClient 127.0.0.1:4567 GC1 > ./Test/testOutputFiles/Client_GET_to_AS.txt

sleep 3
kill $AG_PID
kill $CS1_PID

cd Test

echo 
echo "========Test Result========"

# compare the ATOMFeed.xml with the verification file
if cmp -s $output_file $verification_file; then
    printf 'The file %s is the same as %s\n' "$output_file" "$verification_file"
    printf 'Single client getting feed test passed!\n'
else
    printf 'The file %s is different from %s\n' "$output_file" "$verification_file"
    printf 'Single client getting feed test failed!\n'
fi 

./reset_data.sh >> /dev/null 2>&1





