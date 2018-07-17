# Disclaimer

I am not the author of this whole protocol:
it is a modified and simplified version of the one specified in the https://gafferongames.com/ blog posts.

# Overview

This document describes how a secure, connection-based, reliable (packet loss protected) protocol on top of UDP should function.
An easier alternative is to use both a TCP connection and a separate connection-based protocol on top of UDP.
The downside of the easier method is that given enough TCP connections their (synchronized) send windows will give no space for the UDP packets to effectively go through, therefore introducing high latency and packet loss (http://www.isoc.org/INET97/proceedings/F3/F3_1.HTM).



## Connections

**The client asks for an encryption key.**
 - The encryption key is probably requested using a lobby server session id or login credentials.
 - It is requested on an encrypted channel, eg. HTTPS or an already existing encrypted channel, eg. when in a lobby server
 - The followings are contained in the response to the encryption key request: the encryption key itself, its expiration timestamp, both of these again and the user's session-independent id, but all three of these encrypted using a key known only to the servers.

**The client sends a connection request to the server.**
 - This packet is not encrypted using the encryption key (it can be encrypted with a public key, but since the packet contains no secrets, it can go as unencrypted).
 - The client's protocol id and the encrypted data retrieved from the encryption key request are included in this packet.
 - If needed, an array of ignored bytes is also sent. This is used to combat DDoS amplification.
 - Since this is not done on a reliable channel, this packet is repeatedly sent until a response is received or a timeout happens.

**+ Packet validation.**
 - If a packet type has a fixed length then the first step in validating the packet should be using its length.
 - All packets contain a CRC in them and validating that CRC should be the second step in packet validation.
 - If a packet is invalid, then it should be ignored. Disconnecting clients for sending invalid packets is not a good idea since packets can be spoofed and they can also arrive later than intended, eg. from the previous connection.

**The server verifies the data and denies the connection or tells the client to proceed.**
 - If the array of ignored bytes is missing (and if it should be present) then the connection is denied.
 - The server denies the connection if the count of the established and pending connections are not less than the max. connection count.
 - If the protocol id sent in the packet and the server's protocol id do not match then the connection is denied.
 - The server decrypts the encryption key, its expiration timestamp and the session-independent user id. The connection is refused if the encryption key has expired.
 - The session-independent user id will be exposed to higher level code once the connection is established.
 - If these checks do not fail then the server sends a random number to the client in order to combat IP spoofing.
 - From now on, including this packet, all packets are encrypted using the encryption key. It doesn't matter whether it was accepted or not: all responses from the server must be encrypted.
 - Since the responses are not sent on a reliable channel, they are sent multiple times. The count depends on whether the connection was denied or not. If the connection was accepted, then the repeating is stopped early once a response is received.

**The client creates the connection.**
 - The client sends back the random number to the server.
 - Since this is not done on a reliable channel, this packet is repeatedly sent until any (valid, including keepalive) packet is received or a timeout happens.

**The server validates the client connection creation.**
 - The connection is denied if the sent and received random numbers do not match.
 - In the likely case of the check not failing, the connection is deemed as created by the server. The higher level code is informed of the connected client and will probably send multiple long packets.
 - The server starts sending keepalive packets regularly in case no other packets are sent.
 - Higher level code takes care of all received packets (except keepalive and invalid packets).
 - If the server does not receive any (valid) packets from the client, the client will be timed out.

**The client receives a packet.**
 - Higher level code handles the contents of this and all future valid, non-keepalive packets.
 - The client starts sending keepalive packets regularly in case no other packets are sent.
 - If the client does not receive any (valid) packets from the client, the connection will time out.

**+ Replay protection.**
 - The disconnect packet is protected against replays because once it is sent, the connection will be aborted and a new connection will use a different encryption key.
 - All other packets except the keepalive packets and the ones sent by the higher level code are protected against replays by being ignored if they are sent at a time when they do not make sense.
 - Keepalive packets contain a sequence id and vary in length mimicking other, valid packets' length. If there is a huge difference between the last received and the newly received keepalive packet's sequence id (and if we are not talking about a possible sequence id wrap-around) then we can assume a replayed packet.
 - Packets sent by the higher level code should be protected against replay attacks by the higher level code. To reduce overhead, a sequence id is not introduced here by this level of abstraction since the higher level code may define packet ids for reliability purposes.

**+ Higher level code.**
 - The higher level code implements an (optional) reliability layer on top of UDP using ACKs and packet resending.
 - This code is also able to fragment data which is larger than the MTU

**The server kicks the client or the client safely disconnects.**
 - From this point on, no keepalive packets are sent.
 - This packet is resent for a small, fixed number of times in order to combat packet loss. No response will be given to it.
 - If the packet was received by the other party, then the other party knows the exact situation, otherwise a timeout is triggered soon.



## Higher-level code

**Content types:**
 - Message: these are protected against packet loss (an ACK system is in place, they get resent until they are ACKd or until they can no longer be ACkd) and will be fragmented if they are above the MTU (~1200 bytes, https://gafferongames.com/post/packet_fragmentation_and_reassembly/).
 - Update: these are not protected against packet loss and they are ignored if they are received out of order: if the newly received update was sent before the previously received one, then the new one is ignored. They are sent regularly, eg. at 30Hz.
    - State update: only the server sends this, contains the ids of multiple objects and their serialized states
    - Input update: only the client sends this, contains all non-critical input, eg. movement, facing direction

**Packet id, ACK, ignoring out-of-order packets:**
 - Each packet has an id, this is called the packet id.
 - This id is used to ACK the packet or to ignore it if it is received out of order (the packet id is sequence id).
 - Until the packet id which contains a message is ACKd, the message is resent (using the same id) in the packets.
 - Exception: a message is stopped getting resent if there are ack-bitfield-length plus 1 newer packets after it, even if it wasn't ACKd. This is due to how the ACK system works.
 - The packet id of the highest ACKd packet id is included in outgoing packets and a bitmap of 32 older message ids follows.

**Round-trip time, congestion control:**
 - Round-trip time is the time between an original (not resent) packet is sent and an ACK for it arrives.
 - Only the update packets are influenced by congestion control, since they form the majority of the bandwidth.
 - If the RTT is above some threshold, eg. 150ms, then multiply the update send rate by a number between 0 and 1, eg. 0.75. This can only be done once every few seconds.
 - If the RTT is not above the threshold, then increase the frequency by 1.
 - The update send rate should have a lower and an upper bound, eg. 10Hz and 30Hz.

**Packet structure:**
 - 16 | Current packet id
 - 16 | ACK for the highest packet id
 - 31 | Bitmap of ACKs for previous packet ids (Nth bit -> ACK for most recent minus N id)
 - 01 | Content type (message or update)
 - ?? | Content type specific contents

**State update content:**
 - 08 | Object id
 - ?? | Object state
 - Repeat until end of message

**Input update content:**
 - ?? | Input axises' state

**Unfragmented message content:**
 - 08 | Message type
 - ?? | Message content

**Fragmented message content:**
 - 04 | Fragment sequence id (packet id minus this equals the packet id of the first fragment)
 - 04 | Fragment total count
 - ?? | Partial message content (a reassembled message's content equals an unfragmented message's content)
