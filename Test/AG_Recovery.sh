#!/bin/bash

#!/bin/bash

# verification_file="./verificationFiles/Client_GET_to_AS.txt"
output_file_before_die="./testOutputFiles/AG_Recovery_1.txt" 
output_file_after_restart="./testOutputFiles/AG_Recovery_2.txt" 

cd ../
# start AggregationServer in the background
java AggregationServer &
AG_PID_1=$!

sleep 1

# start a content servers with id CS1 in the background
java ContentServer 127.0.0.1:4567 contentServerFeed1.txt CS1 &
CS1_PID=$!

sleep 1

# start a client with id GC1 to get the feed
java GETClient 127.0.0.1:4567 GC1 > ./Test/testOutputFiles/AG_Recovery_1.txt

sleep 2

# kill the AG
kill $AG_PID_1
sleep 1

# restart the AG
java AggregationServer &
AG_PID_2=$!

sleep 2

# start a client with id GC1 to get the feed again
java GETClient 127.0.0.1:4567 GC1 > ./Test/testOutputFiles/AG_Recovery_2.txt

kill $AG_PID_2
kill $CS1_PID

cd Test

echo 
echo "========Test Result========"

# compare the 2 output files  
if cmp -s $output_file_before_die $output_file_after_restart; then
    printf 'The file %s is the same as %s\n' "$output_file_before_die" "$output_file_after_restart"
    printf 'Aggregation recovery test passed!\n'
else
    printf 'The file %s is different from %s\n' "$output_file_before_die" "$output_file_after_restart"
    printf 'Aggregation recovery test failed!\n'
fi 

# reset the data for the whole system
./reset_data.sh >> /dev/null 2>&1





