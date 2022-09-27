#!/bin/bash

verification_file="./verificationFiles/Multiple_CS_PUT_to_AS.xml"
output_file="../ATOMFeed.xml" 

cd ../
# start AggregationServer in the background
java AggregationServer &

AG_PID=$!

sleep 1

# start 3 content servers with id CS1, CS2, and CS3 in the background
java ContentServer 127.0.0.1:4567 contentServerFeed1.txt CS1 &
sleep 100m

CS1_PID=$!

java ContentServer 127.0.0.1:4567 contentServerFeed1.txt CS2 &
sleep 100m

CS2_PID=$!

java ContentServer 127.0.0.1:4567 contentServerFeed1.txt CS3 &
sleep 100m

CS3_PID=$!



sleep 7

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

# ./reset_data.sh >> /dev/null 2>&1