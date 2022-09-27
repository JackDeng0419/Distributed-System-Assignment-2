#!/bin/bash

verification_file="./verificationFiles/Client_GET_connection_retry.txt"
output_file="./testOutputFiles/Client_GET_connection_retry.txt" 

cd ../

# start a client without running AG
java GETClient 127.0.0.1:4567 GC1 > ./Test/testOutputFiles/Client_GET_connection_retry.txt

cd Test

echo 
echo "========Test Result========"

# compare the ATOMFeed.xml with the verification file
if cmp -s $output_file $verification_file; then
    printf 'The file %s is the same as %s\n' "$output_file" "$verification_file"
    printf 'GETClient connection retry test passed!\n'
else
    printf 'The file %s is different from %s\n' "$output_file" "$verification_file"
    printf 'GETClient connection retry test test failed!\n'
fi 

./reset_data.sh >> /dev/null 2>&1





