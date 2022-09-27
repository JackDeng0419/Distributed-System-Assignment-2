#!/bin/bash

verification_file="./verificationFiles/CS_connection_retry.txt"
output_file="./testOutputFiles/CS_connection_retry.txt" 

cd ../

# start a content server without running AG
java ContentServer 127.0.0.1:4567 contentServerFeed1.txt CS1 > ./Test/testOutputFiles/CS_connection_retry.txt

cd Test

echo 
echo "========Test Result========"

# compare the ATOMFeed.xml with the verification file
if cmp -s $output_file $verification_file; then
    printf 'The file %s is the same as %s\n' "$output_file" "$verification_file"
    printf 'Content server connection retry test passed!\n'
else
    printf 'The file %s is different from %s\n' "$output_file" "$verification_file"
    printf 'Content server connection retry test test failed!\n'
fi 

./reset_data.sh >> /dev/null 2>&1





