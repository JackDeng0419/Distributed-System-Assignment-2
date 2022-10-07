# Distributed System Assignment 2
## 1. Major design decision
1. GET Client: the GET Client sends a get request to the Aggregation Server, receives an aggregation XML file,then parses the XML file, and finally output the feed content on the cmd. 
2. Content Server: the Content Server reads the txt feed file and then parses it to a XML file, which is sent to the Aggregation Server by a put request. After receiving the response of successful uploading from the Aggregation Server, the Content Server starts to send heat beat signal to the Aggregation Server. 
3. Aggregation Server: 
    1. For each get feed request and put feed request, the Aggregation Server assigns a thread from the thread pool to handle it. 
    2. Each request contains a lamport clock value. The request will be pushed into a priority queue and then the Aggregation Server will pull the request from the priority queue to process. 
    3. When the Aggregation Server needs to write the aggregation XML file (`ATOMFeed.xml`), to avoid race condition, a producer-consumer model is used. The Aggregation Server (producer) adds an operation message to the message queue, from which the Aggregator (consumer, Aggregator.java) on another thread polls the operation and then write the XML file. 
    4. The Aggregation Server stores all the feeds in a queue (`BlockingQueue<Feed>`). When the size of the queue is larger than 20, the first feed, which is the earliest one, will be removed from the queue. Moreover, every time the aggregation XML file (`ATOMFeed.xml`) needs to be updated, the Aggregation Server will only update the feed queue, and then generate a new aggregation XML file based on the feed queue. 
4. Heart The Aggregation Server sets a 12 seconds timer for each Content Server. The task of the timer is to delete all the feeds from the corresponding Content Server. When the Aggregation Server receives a feed or heart beat from a Content Server, it will create a new 12-second timer and cancel the old one. In this way, if the Aggregation Server does not receive any message from a Content Server for 12 seconds, the corresponding feeds will be deleted. 

## 2. Compile the code
Run `./compile.sh`

## 3. Running the program
1. For Aggregation Server (AG):
    Run `java AggregationServer`
2. For Content Server (CS):
    Run `java ContentServer <AG_URL> <feedFilename> <contentServerId>`
    - Example: 
        ```
        java ContentServer 127.0.0.1:4567 contentServerFeed1.txt CS1
        ```
3. For GET client (GC):
    Run `java GETClient <AG_URL> <contentServerId>`
     - Example: 
        ```
        java GETClient 127.0.0.1:4567 GC1
        ```
**Notice:** Before starting the CS or GC, please make sure the AG is running. 

### 3.1 An example 
1. Start an AG: `java AggregationServer`
2. Start a CS and upload a feed: `java ContentServer 127.0.0.1:4567 contentServerFeed1.txt CS1`
3. Start a GC to get the feed: `java GETClient 127.0.0.1:4567 GC1`

## 4. Test cases

To run the following test scripts, please go to the *Test* directory by running `cd ./Test` and make sure you run the test scripts under the *Test* directory. 

*You can run an overall test that covers all the test cases below by running:  `./test_all.sh`.*

1. One GC getting feed from the AG
    - Run: `./Client_GET_to_AS.sh`
    - Test logic: 
        
2. Multiple GCs getting feed from the AG
    - Run: `./Multiple_Client_GET_to_AS.sh`
    - Test logic:
        1. Start the AG
        2. Start a CS and put a feed to the AG
        3. Start 3 GC to get the feed from the AG
        4. Compare the each GC output with the verification output
3. One CS putting feed to the AG
    - Run: `./CS_PUT_to_AS.sh`
    - Test logic:
        1. Start the AG
        2. Start a CS and put a feed to the AG
        3. Compare the `ATOMFeed.xml` with the verification XML file
4. Multiple CSs putting feed to the AG
    - Run: `./Multiple_CS_PUT_to_AS.sh`
    - Test logic:
        1. Start the AG
        2. Start 3 CS at the same time to put feeds to the AG
        3. Start a GC to get feed from the AG
        4. Compare the GC output with the verification output, which contains the feeds from 3 CS. (The output of GC is sorted, so the output will not be affected by the order of putting feed.)
5. CS heartbeat signal
    - Run: `./CS_Heartbeat.sh`
    - Test logic:
        1. Start the AG
        2. Start a CS (CS1) to put a feed to the AG
        3. Start another CS (CS2) to put a feed to the AG
        4. Kill the CS1 and wait 12 seconds
        5. Start a GC to get the feed from the AG and compare the output with the verification output, which only contains the feed put by CS2. 
6. AG recovery
    - Run: `./AG_Recovery.sh`
    - Test logic: 
        1. Start the AG
        2. Start a CS to put a feed
        3. Start a GC (GC1) to get the feed and store the output
        4. Kill the AG and then restart it
        5. Start a GC (GC2) to get the feed and store the output
        6. Compare the output from both GC1 and GC2, which should be the same. 
7. GC connection retry
    - Run: `./Client_GET_connection_retry.sh`
    - Test logic:
        1. Only start a GC
        2. Compare the GC output with the verification output, which contains 3 retry messages. 
8. CS connection retry
    - Run: `./CS_connection_retry.sh`
    - Test logic:
        1. Only start a CS 
        2. Compare the CS output with the verification output, which contains 3 retry messages. 
9. AG responds 204 to no content request
    - Run: `./AG_respond_204`
    - Test logic: 
        1. Start the AG
        2. Start a CS to put an empty feed
        3. Compare the CS output with the verification output, which shows the 204 status code. 
10. AG responds 500 to the non-ATOM feed
    - Run: `./AG_respond_500`
    - Test logic: 
        1. Start the AG
        2. Start a CS to put an non-ATOM feed
        3. Compare the CS output with the verification output, which shows the 500 status code. 



