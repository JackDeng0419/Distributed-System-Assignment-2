# Distributed System Assignment 2
## 1. Compile the code
Run `./compile.sh`

## 2. Running the program
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

## 3. Test cases
To run the following test scripts, please go to the *Test* directory by running `cd ./Test`.

1. One GC getting feed from the AG
    - Run: `./Client_GET_to_AS.sh`
2. Multiple GCs getting feed from the AG
    - Run: `./Multiple_Client_GET_to_AS.sh`
3. One CS putting feed to the AG
    - Run: `./CS_PUT_to_AS.sh`
4. Multiple CSs putting feed to the AG
    - Run: `./Multiple_CS_PUT_to_AS.sh`
5. CS heartbeat signal
    - Run: `./CS_Heartbeat.sh`
6. AG recovery
    - Run: `./AG_Recovery.sh`
6. GC connection retry
    - Run: `./Client_GET_connection_retry.sh`
7. CS connection retry
    - Run: `./CS_connection_retry.sh`
