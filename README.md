# COMP6461_Project

## Introduction
This project is the implementation of Web server and client based on the principle of HTTP protocol. It can realize the server's request, response, redirection, and other functions. Besides, the server supports multi-threading and uses Select Repeat Automatic Repeat-Request(ARQ) algorithm to guarantee packet transmission over unreliable network links.

## Select Repeat Algorithm
Selective Repeat is part of the automatic repeat-request (ARQ). With selective repeat, the sender sends a number of frames specified by a window size even without the need to wait for individual ACK from the receiver as in Go- Back-N ARQ. The receiver may selectively reject a single frame, which may be retransmitted alone; this contrasts with other forms of ARQ, which must send every frame from that point again. The receiver accepts out-of-order frames and buffers them. The sender individually retransmits frames that have timed out[1].

## Client Command Parameters
1. -v: Enable a verbose output
2. -h: Pass the headers value to the HTTP operation
3. -d: Associate the body of the HTTP Request with the inline data
4. -f: Associate the body of the HTTP Request with the data from a given file

## Sample Client Command
1. httpc get 'http://localhost/data.json'
2. httpc get -v 'http://localhost/command.txt'
3. httpc post -v -d '{"Assignment": 1}' 'http://localhost/data.json'
4. httpc post -v -f 'file.json' 'http://localhost/data.json'

## Server Commanad Parameters
1. -v: Print debugging messages
2. -p: Specify the server listening port number
3. -d: Specify the directory that the server read/write

## Sample Server Command
1. httpfs -v
2. httpfs -v -p 8080
3. httpfs -v -p 8080 -d 'file/inner'

## Run the program
    git clone https://github.com/wujq95/COMP6461_Project.git
Start the server

    cd COMP6461_Project
    cd src/server
    javac UDPServer.java Packet.java Request.java Response.java Connection.java ParseRequest.java ReceivePacket.java SendPacket.java SlidingWindow.java
    cd ..
    java server/UDPServer httpfs -v
Start the client

    cd COMP6461_Project
    cd src/client
    javac UDPClient.java Packet.java Request.java Connection.java ParseArgs.java ReceivePacket.java SendPacket.java SlidingWindow.java
    cd ..
    java client/UDPClient httpc help

## References
[1] Selective Repeat. https://en.wikipedia.org/wiki/Selective_Repeat_ARQ.
 
