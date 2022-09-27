#!/bin/bash

verification_file="./verificationFiles/Multiple_Client_GET_to_AS.txt"
output_file_GC1="./testOutputFiles/Multiple_Client_GET_to_AS_GC1.txt" 
output_file_GC2="./testOutputFiles/Multiple_Client_GET_to_AS_GC2.txt" 
output_file_GC3="./testOutputFiles/Multiple_Client_GET_to_AS_GC3.txt" 

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
java GETClient 127.0.0.1:4567 GC1 > ./Test/testOutputFiles/Multiple_Client_GET_to_AS_GC1.txt

java GETClient 127.0.0.1:4567 GC2 > ./Test/testOutputFiles/Multiple_Client_GET_to_AS_GC2.txt

java GETClient 127.0.0.1:4567 GC3 > ./Test/testOutputFiles/Multiple_Client_GET_to_AS_GC3.txt

sleep 3
kill $AG_PID
kill $CS1_PID

cd Test

echo 
echo "========Test Result========"

# compare the ATOMFeed.xml with the verification file

passed='true'

if cmp -s $output_file_GC1 $verification_file; then
    printf 'The file %s is the same as %s\n' "$output_file_GC1" "$verification_file"
else
    printf 'The file %s is different from %s\n' "$output_file_GC1" "$verification_file"
    let passed='false'
fi 

if cmp -s $output_file_GC2 $verification_file; then
    printf 'The file %s is the same as %s\n' "$output_file_GC2" "$verification_file"
else
    printf 'The file %s is different from %s\n' "$output_file_GC2" "$verification_file"
    let passed='false'
fi 

if cmp -s $output_file_GC3 $verification_file; then
    printf 'The file %s is the same as %s\n' "$output_file_GC3" "$verification_file"
else
    printf 'The file %s is different from %s\n' "$output_file_GC3" "$verification_file"
    let passed='false'
fi 

if [ $passed == 'true' ]
then
    printf 'Multiple clients getting feed test passed!\n'
else
    printf 'Multiple clients getting feed test failed!\n'
fi 

./reset_data.sh >> /dev/null 2>&1





