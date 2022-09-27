#!/bin/bash

verification_file="./verificationFiles/CS_PUT_to_AS.xml"
output_file="../ATOMFeed.xml" 

cd ../
# start AggregationServer in background
java AggregationServer &

AG_PID=$!

sleep 1

# start ContentServer with id CS1 in background
java ContentServer 127.0.0.1:4567 contentServerFeed1.txt CS1 &

CS_PID=$!

sleep 7

kill $AG_PID
kill $CS_PID

cd Test

echo 
echo "========Test Result========"

# compare the ATOMFeed.xml with the verification file
if cmp -s $output_file $verification_file; then
    printf 'The file %s is the same as %s\n' "$output_file" "$verification_file"
    printf 'Single content server putting content test passed!\n'
else
    printf 'The file %s is different from %s\n' "$output_file" "$verification_file"
    printf 'Single content server putting content test failed!\n'
fi 

./reset_data.sh >> /dev/null 2>&1





