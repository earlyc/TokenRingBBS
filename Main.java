package com.kingfisher;

import java.net.*;

public class Main {

    public static void main(String[] args) throws Exception {

        // First step is to get config info from file
        // config file contains:
        // beginning and end of port range
        // port number for this client
        // client's join time
        // client's leave time

        int beginRange = 11100;
        int endRange = 11110;
        int portNum = Integer.parseInt(args[0]);
        int portRange = endRange - beginRange;
        int leaderPortNum = -1;

        byte[] sendData;
        byte[] receiveData = new byte[1024];
        DatagramSocket sock = new DatagramSocket(portNum);
        int defaultSoTimeout = sock.getSoTimeout();
        InetAddress address = InetAddress.getByName("localhost");

        int nextHopPortNum = -1;
        int prevHopPortNum = -1;
        int probePortNum = portNum;

        System.out.println("Forming ring...");
        // Loop until client has established next & prev hops
        while(true) {
            // First step of discovery phase
            // Step through port range and probe each address

            if(nextHopPortNum < 0) {
                probePortNum++;
                if(probePortNum > endRange) probePortNum -= portRange;
                else if(probePortNum == portNum) probePortNum++;
                //System.out.println("Sending to port " + probePortNum);
                sendData = "probe".getBytes();
                sock.send(new DatagramPacket(sendData, sendData.length, address, probePortNum));
            }
            try {
                if(nextHopPortNum > -1) sock.setSoTimeout(defaultSoTimeout);
                else sock.setSoTimeout(2000);
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                sock.receive(receivePacket);
                String data = new String(receivePacket.getData());
                if(data.startsWith("probe")) {
                    //System.out.println("got probe from " + receivePacket.getPort());
                    byte[] probeReplyData = "OKprobe".getBytes();
                    sock.send(new DatagramPacket(probeReplyData, probeReplyData.length, address, receivePacket.getPort()));
                    if(prevHopPortNum > -1) {
                        probeReplyData = "reconfig".getBytes();
                        sock.send(new DatagramPacket(probeReplyData, probeReplyData.length, address, prevHopPortNum));
                    }
                    prevHopPortNum = receivePacket.getPort();
                    System.out.println("Found prev hop! " + prevHopPortNum);
                }
                else if(data.startsWith("OKprobe")) {
                    nextHopPortNum = receivePacket.getPort();
                    System.out.println("Found next hop! " + nextHopPortNum);
                    sendData = ("election," + portNum).getBytes();
                    sock.send(new DatagramPacket(sendData, sendData.length, address, nextHopPortNum));
                }
                else if(data.startsWith("reconfig")) {
                    nextHopPortNum = -1;
                    probePortNum = portNum;
                }
                else if(data.startsWith("election")) {
                    leaderPortNum = Integer.parseInt((data.substring(data.indexOf(",") + 1)).trim());
                    if(nextHopPortNum > 0 && leaderPortNum != portNum) {
                        if(leaderPortNum < portNum) sendData = ("election," + portNum).getBytes();
                        else sendData = ("election," + leaderPortNum).getBytes();
                        sock.send(new DatagramPacket(sendData, sendData.length, address, nextHopPortNum));
                    }
                    else if(leaderPortNum == portNum) {
                        System.out.println("I am the leader");
                    }
                }
            } catch(SocketTimeoutException e) {
                //if(nextHopPortNum < 0) System.out.println("No response.");
            }
        }
    }
}
