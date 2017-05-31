package com.kingfisher;

import java.net.*;

public class BBSClient {

    public static void main(String[] args) throws Exception {
        int beginRange = 11100;
        int endRange = 11110;
        int portRange = endRange - beginRange;
        int portNum = Integer.parseInt(args[0]);
        //        int portNum = 11105;
        int clientID = portNum - beginRange;
        byte[] sendData;
        byte[] receiveData = new byte[1024];
        sendData = "probe".getBytes();
        DatagramSocket sock = new DatagramSocket(portNum);
        int defaultSoTimeout = sock.getSoTimeout();
        sock.setSoTimeout(2000);
        InetAddress address = InetAddress.getByName("localhost");

        int nextHopPortNum = -1;
        int prevHopPortNum = -1;
        int probePortNum = portNum;

        System.out.println("Forming ring...");
        // Loop until client has established next & prev hops
        while(nextHopPortNum < 0 || prevHopPortNum < 0) {
            // First step of discovery phase
            // Step through port range and probe each address

            if(nextHopPortNum < 0) {
                probePortNum++;
                if(probePortNum > endRange) probePortNum -= portRange;
                else if(probePortNum == portNum) probePortNum++;
                //System.out.println("Sending to port " + probePortNum);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, probePortNum);
                sock.send(sendPacket);
            }
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                sock.receive(receivePacket);
                String data = new String(receivePacket.getData());
                if(data.startsWith("probe")) {
                    //System.out.println("got probe from " + receivePacket.getPort());
                    byte[] probeReplyData = "OKprobe".getBytes();
                    sock.send(new DatagramPacket(probeReplyData, probeReplyData.length, address, receivePacket.getPort()));
                    prevHopPortNum = receivePacket.getPort();
                    System.out.println("Found prev hop! " + prevHopPortNum);
                }
                else if(data.startsWith("OKprobe")) {
                    nextHopPortNum = receivePacket.getPort();
                    System.out.println("Found next hop! " + nextHopPortNum);
                }
            } catch(SocketTimeoutException e) {
                //if(nextHopPortNum < 0) System.out.println("No response.");
            }
        }
        System.out.println("Ready!");
        while(true) {
            sock.setSoTimeout(defaultSoTimeout);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            sock.receive(receivePacket);
            String data = new String(receivePacket.getData());
            if(data.startsWith("probe") && receivePacket.getPort() > prevHopPortNum) {
                //System.out.println("got probe from " + receivePacket.getPort());
                byte[] probeReplyData = "OKprobe".getBytes();
                sock.send(new DatagramPacket(probeReplyData, probeReplyData.length, address, receivePacket.getPort()));
                probeReplyData = "reconfig".getBytes();
                sock.send(new DatagramPacket(probeReplyData, probeReplyData.length, address, prevHopPortNum));
                prevHopPortNum = receivePacket.getPort();
                System.out.println("New prev hop! " + prevHopPortNum);

            }
            else if(data.startsWith("OKprobe")) {
                nextHopPortNum = receivePacket.getPort();
                System.out.println("Found next hop! " + nextHopPortNum);
            }
        }

    }
}
