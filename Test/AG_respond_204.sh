#!/bin/bash

./reset_data.sh >> /dev/null 2>&1

verification_file="./verificationFiles/AG_respond_204.txt"
output_file="./testOutputFiles/AG_respond_204.txt" 

cd ../
# start AggregationServer in background
java AggregationServer &

AG_PID=$!

sleep 1

# start ContentServer with id CS1 and send a no-content feed
java ContentServer 127.0.0.1:4567 contentServerNoContentFeed.txt CS1 > ./Test/testOutputFiles/AG_respond_204.txt &

# CS_PID=$!

sleep 3

kill $AG_PID
# kill $CS_PID

cd Test

echo 
echo "========Test Result========"

# compare the ATOMFeed.xml with the verification file
if cmp -s $output_file $verification_file; then
    printf 'The file %s is the same as %s\n' "$output_file" "$verification_file"
    printf 'AG responding 204 test passed!\n'
else
    printf 'The file %s is different from %s\n' "$output_file" "$verification_file"
    printf 'AG responding 204 test failed!\n'
fi 

./reset_data.sh >> /dev/null 2>&1





