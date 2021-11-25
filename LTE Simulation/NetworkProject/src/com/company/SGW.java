package com.company;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class SGW extends Thread {

    private ArrayList<Thread> eNodeBsCommunication;
    private ArrayList<ServerSocket> eNodeBSockets;

    // ID -> buffer
    private HashMap<String, HashMap<String, String>> buffer;

    // map between eNodeB IDs and it's serverSocket.
    private HashMap<Integer, ServerSocket> eNodeBSocketTable;
    private HashMap<ServerSocket, Socket> serverSocketToSocketTable;
    int mmePort;
    private Socket mmeSocket;

    public SGW() {
        eNodeBSocketTable = new HashMap<>();
        eNodeBSockets = new ArrayList<>();
        eNodeBsCommunication = new ArrayList<>();
        buffer = new HashMap<>();
        serverSocketToSocketTable = new HashMap<>();
    }

    public void initialize(ArrayList<eNodeB> eNodeBs) {
        setUpMmeSocket();
        setUpNodeSockets(eNodeBs);
    }

    public void setUpMmeSocket() {

        try {

            System.out.println("SGW is Connecting to MME on port" + mmePort);
            mmeSocket = new Socket(Util.getMME_ServerName(), mmePort);

            System.out.println("SGW Just Connected to MME on" + mmeSocket.getRemoteSocketAddress() +
                    "port" + " with this localPort " + mmeSocket.getLocalSocketAddress());

            Thread listenerToMMEResponses = new Thread(() -> {

                while (true) {
                    try {
                        DataInputStream in = new DataInputStream(mmeSocket.getInputStream());
                        compileMmeResponse(in.readUTF());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            });

            listenerToMMEResponses.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setUpNodeSockets(ArrayList<eNodeB> eNodeBs) {

        for (eNodeB nodeB : eNodeBs) {
            try {
                int sgwPort = Util.getSgwPort();
                eNodeBSockets.add(new ServerSocket(sgwPort));
                nodeB.setSgwPort(sgwPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (ServerSocket nodeSocket : eNodeBSockets) {

            eNodeBsCommunication.add(new Thread(() -> {

                try {

                    System.out.println("SGW is Waiting for eNodeB on port " +
                            nodeSocket.getLocalPort() + "...");
                    Socket server = nodeSocket.accept();
                    System.out.println("SGW: Just connected to eNodeB with portNumber of " + server.getRemoteSocketAddress());

                    synchronized (this) {
                        serverSocketToSocketTable.put(nodeSocket, server);
                    }

                    while (true) {

                        DataInputStream in = new DataInputStream(server.getInputStream());
                        eNobeCompile(in.readUTF(), nodeSocket);

                        /*
                        DataOutputStream out = new DataOutputStream(server.getOutputStream());
                        out.writeUTF("Thank you for connecting to " + server.getLocalSocketAddress()
                                + "\nGoodbye!");
                        */


                        //server.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }));

        }

        for (
                Thread thread : eNodeBsCommunication) {
            thread.start();
        }

    }

    private void eNobeCompile(String inputData, ServerSocket serverSocket) {

        String title = inputData.substring(0, inputData.indexOf(":"));
        if (title.equals(Message.eNodeB_SGW_Connection)) {
            Integer eNodeUID = Integer.parseInt(inputData.substring(inputData.indexOf(":") + 1));
            synchronized (this) {
                eNodeBSocketTable.put(eNodeUID, serverSocket);
            }
            return;
        }

        if (title.equals(Message.data_Carrier)) {

            try {

                DataOutputStream out = new DataOutputStream(mmeSocket.getOutputStream());

                String receiverID = inputData.substring(inputData.indexOf(":") + 1, inputData.indexOf("#"));
                String data = inputData.substring(inputData.indexOf("#") + 1, inputData.indexOf("/"));
                String senderID = inputData.substring(inputData.indexOf("/") + 1);

                //todo complete log
                System.out.println("SGW: got Data Carrier from ENodeB ,sender: " + senderID +
                        " to receiver: " + receiverID + " and the content is: " + data);

                // adding to buffer.
                synchronized (this) {

                    if (buffer.containsKey(senderID)) {

                        HashMap<String, String> hashMap = buffer.get(senderID);

                        String currentBuffer = hashMap.get(receiverID);

                        // in order to show fragmenting.

                        currentBuffer = currentBuffer.concat("@" + data);
                        hashMap.put(receiverID, currentBuffer);

                        buffer.put(receiverID, hashMap);

                    } else {
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put(receiverID, data);
                        buffer.put(senderID, hashMap);
                    }

                }

                out.writeUTF(Message.data_Carrier + ":" + receiverID + "#" + senderID);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void compileMmeResponse(String inputData) {

        String title = inputData.substring(0, inputData.indexOf(":"));

        if (title.equals(Message.data_Carrier)) {

            String receiver = inputData.substring(inputData.indexOf(":") + 1, inputData.indexOf("#"));
            String sender = inputData.substring(inputData.indexOf("#") + 1, inputData.indexOf("/"));
            String uidOfENode = inputData.substring(inputData.indexOf("/") + 1);

            System.out.println("SGW got the MME's response, ENodeB with UID: " + uidOfENode +
                    " has been accepted for sending data from sender: " + sender + " to receiver: " + receiver);


            Socket destSocket = serverSocketToSocketTable.get(eNodeBSocketTable.get(Integer.valueOf(uidOfENode)));

            try {

                HashMap<String, String> destUserBufferedData = buffer.get(sender);
                String gonnaSend = destUserBufferedData.get(receiver);

                // it has been sent.
                if (gonnaSend.equals("")) {
                    return;
                }

                synchronized (this) {
                    // clearing Buffer
                    destUserBufferedData.put(receiver, "");
                }


                DataOutputStream out = new DataOutputStream(destSocket.getOutputStream());
                out.writeUTF(Message.data_Carrier + ":" + receiver + "#" + sender + "/" + gonnaSend);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setMmePort(int mmePort) {
        this.mmePort = mmePort;
    }

    public static void main(String[] args) {
        // testing SGW
    }

}
