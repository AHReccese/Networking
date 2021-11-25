package com.company;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Thread.sleep;

public class MME {

    private Thread sgwCommunication;

    // they are initially Thread Safe.
    private ArrayList<Thread> eNodeBsCommunication;
    private ArrayList<ServerSocket> eNodeBServerSockets;


    // All have been Thread Safe by synchronization.
    private HashMap<Integer, Double> usersDistances;
    private HashMap<Integer, Integer> usersGotDistancesNum;
    private HashMap<Integer, ServerSocket> userToWhichENode;
    private HashMap<Integer, ServerSocket> temp_userToWhichNode;

    // map between eNodeB IDs and it's serverSocket.
    // All have been Thread Safe by synchronization
    private HashMap<ServerSocket, Integer> eNodeBServerSocketTable;
    private HashMap<ServerSocket, Socket> serverSocketToSocketTable;

    private HashMap<Integer, ServerSocket> previousConnected;

    private ServerSocket sgwServerSocket;

    public MME() {
        eNodeBServerSocketTable = new HashMap<>();
        serverSocketToSocketTable = new HashMap<>();
        eNodeBServerSockets = new ArrayList<>();

        eNodeBsCommunication = new ArrayList<>();

        usersDistances = new HashMap<>();
        usersGotDistancesNum = new HashMap<>();
        userToWhichENode = new HashMap<>();
        previousConnected = new HashMap<>();
        temp_userToWhichNode = new HashMap<>();

    }


    public void initialize(SGW sgw, ArrayList<eNodeB> eNodeBs) {
        setUpSgwSocket(sgw);
        setUpNodeSockets(eNodeBs);
    }

    public void setUpSgwSocket(SGW sgw) {

        try {
            int mmePort = Util.getMmePort();
            sgwServerSocket = new ServerSocket(mmePort);
            sgw.setMmePort(mmePort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        sgwCommunication = new Thread(() -> {

            try {
                System.out.println("MME is Waiting for SGW on port " +
                        sgwServerSocket.getLocalPort() + "...");

                Socket sgwSocket = sgwServerSocket.accept();

                System.out.println("MME: Just connected to SGW by" + sgwSocket.getRemoteSocketAddress());

                while (true) {

                    DataInputStream in = new DataInputStream(sgwSocket.getInputStream());
                    compileSGWRequest(in.readUTF(), sgwSocket);
                }

            } catch (SocketTimeoutException s) {
                System.out.println("Socket timed out!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        sgwCommunication.start();

    }

    public void setUpNodeSockets(ArrayList<eNodeB> eNodeBs) {

        for (eNodeB nodeB : eNodeBs) {
            try {
                int mmePort = Util.getMmePort();
                eNodeBServerSockets.add(new ServerSocket(mmePort));
                nodeB.setMmePort(mmePort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (ServerSocket nodeSocket : eNodeBServerSockets) {

            eNodeBsCommunication.add(new Thread(() -> {

                try {

                    System.out.println("MME is Waiting for eNodeB with on port " +
                            nodeSocket.getLocalPort() + "...");
                    Socket serverToENodeBSocket = nodeSocket.accept();

                    synchronized (this) {
                        serverSocketToSocketTable.put(nodeSocket, serverToENodeBSocket);
                    }

                    System.out.println("MME: Just connected to eNodeB with portNumber of " + serverToENodeBSocket.getRemoteSocketAddress());

                    while (true) {

                        DataInputStream in = new DataInputStream(serverToENodeBSocket.getInputStream());
                        eNobeCompile(in.readUTF(), nodeSocket);

                        /*DataOutputStream out = new DataOutputStream(server.getOutputStream());
                        out.writeUTF("Thank you for connecting to " + server.getLocalSocketAddress()
                                     + "\nGoodbye!");*/

                        //server.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }));

        }

        for (Thread thread : eNodeBsCommunication) {
            thread.start();
        }

    }

    private void eNobeCompile(String inputData, ServerSocket serverSocket) {

        String title = inputData.substring(0, inputData.indexOf(":"));

        if (title.equals(Message.eNodeB_MME_Connection)) {
            Integer eNodeUID = Integer.parseInt(inputData.substring(inputData.indexOf(":") + 1));

            synchronized (this) {
                eNodeBServerSocketTable.put(serverSocket, eNodeUID);
            }

            System.out.println("MME Got the eNodeB(ID): " + eNodeUID);
            return;
        }

        if (title.equals(Message.user_Distance)) {

            synchronized (this) {

                int userID = Integer.parseInt(inputData.substring(inputData.indexOf(":") + 1, inputData.indexOf("#")));
                double distance = Double.parseDouble(inputData.substring(inputData.indexOf("#") + 1, inputData.indexOf("/")));
                String isReceiver = inputData.substring(inputData.indexOf("/") + 1);

                System.out.println("MME Got the distance(userID): " + userID +
                        " from ENodeB with UID: " + eNodeBServerSocketTable.get(serverSocket) +
                        " distance: " + distance);

                if (!usersDistances.containsKey(userID)) {

                    temp_userToWhichNode.put(userID, serverSocket);
                    usersDistances.put(userID, distance);
                    usersGotDistancesNum.put(userID, 1);

                    if (eNodeBServerSockets.size() == 1) {

                        usersDistances.remove(userID);
                        usersGotDistancesNum.remove(userID);
                        // no need to disconnecting previous node because
                        // there isn't any

                        previousConnected.put(userID, userToWhichENode.get(userID));
                        userToWhichENode.put(userID, temp_userToWhichNode.get(userID));
                        // telling NearestENodeB to connect to User
                        tellNewENodeToConnectToUser(userID, "sender");
                    }

                } else {

                    double prevMinUserDist = usersDistances.get(userID);

                    if (prevMinUserDist > distance) {
                        usersDistances.put(userID, distance);
                        temp_userToWhichNode.put(userID, serverSocket);
                    }

                    int numberOfGotRequests = usersGotDistancesNum.get(userID);
                    // add one :)
                    numberOfGotRequests++;

                    usersGotDistancesNum.put(userID, numberOfGotRequests);

                    if (numberOfGotRequests == eNodeBServerSockets.size()) {

                        System.out.println("ENodeB with min distance is " + eNodeBServerSocketTable.get(temp_userToWhichNode.get(userID))
                                + " with distance of: " + usersDistances.get(userID));

                        previousConnected.put(userID, userToWhichENode.get(userID));
                        userToWhichENode.put(userID, temp_userToWhichNode.get(userID));
                        usersDistances.remove(userID);
                        usersGotDistancesNum.remove(userID);

                        boolean changeOrFirstConnection = previousConnected.get(userID) == null || !previousConnected.get(userID).equals(userToWhichENode.get(userID));

                        if (changeOrFirstConnection) {
                            // if there is a change.

                            if (isReceiver.equals("false")) {
                                // sender
                                tellNewENodeToConnectToUser(userID, "sender");
                            } else {
                                // receiver
                                tellOlderENodeToDisconnectUser(userID);
                        /*try {
                            sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }*/
                                tellNewENodeToConnectToUser(userID, "receiver");
                            }
                        }

                    }

                }
                return;
            }
        }

        if (title.equals(Message.create_Session)) {

            // request format : "create_session:receiver#sender"
            int startIndex = inputData.indexOf(":") + 1;
            int finalIndex = inputData.indexOf("#");
            int receiverID = Integer.parseInt(inputData.substring(startIndex, finalIndex));
            int senderID = Integer.parseInt(inputData.substring(finalIndex + 1));

            System.out.println("MME got the creation session from(eNodeB with UID: " +
                    eNodeBServerSocketTable.get(serverSocket) + ") user with id: "
                    + senderID + " to user with id: " + receiverID);


            Socket eNodeConnectedToReceiver = serverSocketToSocketTable.get(userToWhichENode.get(receiverID));

            try {
                DataOutputStream out = new DataOutputStream(eNodeConnectedToReceiver.getOutputStream());
                out.writeUTF(Message.create_Session + ":" + receiverID + "#" + senderID);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        if (title.equals(Message.create_Session_Ack)) {

            // request format : "ack_create_session:receiver#sender"
            int startIndex = inputData.indexOf(":") + 1;
            int finalIndex = inputData.indexOf("#");
            int receiverID = Integer.parseInt(inputData.substring(startIndex, finalIndex));
            int senderID = Integer.parseInt(inputData.substring(finalIndex + 1));


            System.out.println("MME got the ack of creation session from(eNodeB with UID: " +
                    eNodeBServerSocketTable.get(serverSocket) + ") user with id: "
                    + senderID + " to user with id: " + receiverID);

            Socket eNodeConnectedToReceiver = serverSocketToSocketTable.get(userToWhichENode.get(receiverID));

            try {
                DataOutputStream out = new DataOutputStream(eNodeConnectedToReceiver.getOutputStream());
                out.writeUTF(Message.create_Session_Ack + ":" + receiverID + "#" + senderID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (title.equals(Message.send_Me_Buffered_Data)) {

            int userID = Integer.parseInt(inputData.substring(inputData.indexOf(":") + 1));
            Socket prevSocket = serverSocketToSocketTable.get(previousConnected.get(userID));
            int uid = eNodeBServerSocketTable.get(previousConnected.get(userID));
            System.out.println("MME: is telling ENodeB with UID: " + uid + " to get BufferedData ");
            // call previous eNodeB

            try {
                DataOutputStream out = new DataOutputStream(prevSocket.getOutputStream());
                out.writeUTF(Message.send_Me_Buffered_Data + ":" + userID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }
        if (title.equals(Message.buffered_Data)) {

            int userID = Integer.parseInt(inputData.substring(inputData.indexOf("#") + 1));
            String data = inputData.substring(inputData.indexOf(":") + 1, inputData.indexOf("#"));
            Socket nowSocket = serverSocketToSocketTable.get(userToWhichENode.get(userID));

            try {
                DataOutputStream out = new DataOutputStream(nowSocket.getOutputStream());
                out.writeUTF(Message.buffered_Data + ":" + data + "#" + userID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }
        if (title.equals(Message.handover_Complete)) {

            int userID = Integer.parseInt(inputData.substring(inputData.indexOf(":") + 1));
            Socket nowSocket = serverSocketToSocketTable.get(userToWhichENode.get(userID));

            try {
                DataOutputStream out = new DataOutputStream(nowSocket.getOutputStream());
                out.writeUTF(Message.handover_Complete + ":" + userID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void tellOlderENodeToDisconnectUser(int userID) {

        // it is for receiver
        try {

            ServerSocket previousServerSocket = previousConnected.get(userID);
            if (previousServerSocket == null) {
                return;
            }
            Socket socket = serverSocketToSocketTable.get(previousServerSocket);

            System.out.println("MME is telling OlderENodeB with UID: " +
                    eNodeBServerSocketTable.get(previousServerSocket) +
                    " to Disconnect from User with ID: " + userID);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(Message.user_deregistration + ":" + userID);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tellNewENodeToConnectToUser(Integer userID, String type) {
        // type : Receiver or sender.

        try {

            Socket socket = serverSocketToSocketTable.get(userToWhichENode.get(userID));

            System.out.println("MME is telling ENodeB with UID: " +
                    eNodeBServerSocketTable.get(userToWhichENode.get(userID)) +
                    " to connect to User with ID: " + userID);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(Message.user_Registration1 + ":" + userID + "#" + type);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void compileSGWRequest(String inputData, Socket socket) {

        String title = inputData.substring(0, inputData.indexOf(":"));
        if (title.equals(Message.data_Carrier)) {

            int receiver = Integer.parseInt(inputData.substring(inputData.indexOf(":") + 1, inputData.indexOf("#")));
            int sender = Integer.parseInt(inputData.substring(inputData.indexOf("#") + 1));
            int uidOfConnectedENodeB = eNodeBServerSocketTable.get(userToWhichENode.get(receiver));

            System.out.println("MME got SGW's Data Carrier request: sender: " + sender +
                    " to receiver: " + receiver + " -> use ENodeB with UID: " + uidOfConnectedENodeB);

            try {
                // telling sgw the uid
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF(Message.data_Carrier + ":" + receiver + "#" + sender + "/" + uidOfConnectedENodeB);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) {

        String inputData = "amir:123#54";
        int startIndex = inputData.indexOf(":") + 1;
        int finalIndex = inputData.indexOf("#");
        int receiverID = Integer.parseInt(inputData.substring(startIndex, finalIndex));
        int senderID = Integer.parseInt(inputData.substring(finalIndex + 1));
        System.out.println(receiverID);
        System.out.println(senderID);

    }
}
