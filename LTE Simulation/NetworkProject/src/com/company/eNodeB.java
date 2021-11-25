package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static java.lang.Thread.sleep;

public class eNodeB {

    // this hashMap shows state of communication
    private HashMap<Integer, Boolean> isInBufferingState;
    private HashMap<String, String> buffer;

    // being Thread Safe with synchronization.
    private HashMap<String, String> bufferFromAnotherENode;


    // being Thread Safe with synchronization.
    private HashMap<ServerSocket, Integer> usersSocketTableSignallingChannel;
    private HashMap<ServerSocket, Socket> signallingServerSocketToSocketTable;
    private HashMap<ServerSocket, Socket> dataLinkServerSocketToSocketTable;

    private HashMap<Integer, ServerSocket> usersDataLink;

    // no need to be Thread Safe.
    private ArrayList<Thread> usersSignallingCommunication;
    private ArrayList<ServerSocket> userServerSockets;

    private Socket mmeSocket;
    private Socket sgwSocket;

    private int UID;
    private Place place;
    private int mmePort;
    private int sgwPort;

    eNodeB(double x, double y, int ID) {
        this.UID = ID;
        this.place = new Place(x, y);
        userServerSockets = new ArrayList<>();
        usersSignallingCommunication = new ArrayList<>();
        usersSocketTableSignallingChannel = new HashMap<>();
        signallingServerSocketToSocketTable = new HashMap<>();
        dataLinkServerSocketToSocketTable = new HashMap<>();
        usersDataLink = new HashMap<>();
        isInBufferingState = new HashMap<>();
        buffer = new HashMap<>();
        bufferFromAnotherENode = new HashMap<>();
    }

    public void initialSockets() {

        Thread connectToMME = new Thread(() -> {

            try {

                System.out.println("eNodeB: " + UID + " is Connecting to MME on port " + mmePort);

                mmeSocket = new Socket(Util.getMME_ServerName(), mmePort);
                System.out.println("eNodeB: " + UID + " Just Connected to MME on " + mmeSocket.getRemoteSocketAddress() +
                        " port " + " with this localPort " + mmeSocket.getLocalSocketAddress());

                OutputStream outToServer = mmeSocket.getOutputStream();
                DataOutputStream out = new DataOutputStream(outToServer);

                // telling MME my UID
                out.writeUTF(Message.eNodeB_MME_Connection + ":" + UID);

                Thread listenerThread = new Thread(() -> {

                    while (true) {
                        try {
                            // todo handling MME answers
                            InputStream inFromServer = mmeSocket.getInputStream();
                            DataInputStream in = new DataInputStream(inFromServer);
                            mmeCompile(in.readUTF());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                });
                listenerThread.start();


                // we wanna mmeSocket to continue alive
                //mmeSocket.close();


            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        connectToMME.start();

        Thread connectToSGW = new Thread(() -> {

            try {

                System.out.println("eNodeB: " + UID + " is Connecting to SGW on port" + sgwPort);
                sgwSocket = new Socket(Util.getSGW_ServerName(), sgwPort);
                System.out.println("eNodeB: " + UID + " Just Connected to SGW on" + sgwSocket.getRemoteSocketAddress() +
                        "port" + " with this localPort " + sgwSocket.getLocalSocketAddress());

                OutputStream outToServer = sgwSocket.getOutputStream();
                DataOutputStream out = new DataOutputStream(outToServer);

                // telling SGW my UID
                out.writeUTF(Message.eNodeB_SGW_Connection + ":" + UID);

                Thread listenerThread = new Thread(() -> {

                    while (true) {

                        try {
                            DataInputStream in = new DataInputStream(sgwSocket.getInputStream());
                            compileSGWResponses(in.readUTF());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                });
                listenerThread.start();

                // we wanna sgwSocket to continue alive
                //sgwSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        connectToSGW.start();

        try {
            connectToMME.join();
            connectToSGW.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        eNodeB nodeB = (eNodeB) o;
        return UID == nodeB.UID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(UID);
    }

    public void setMmePort(int mmePort) {
        this.mmePort = mmePort;
    }

    public void setSgwPort(int sgwPort) {
        this.sgwPort = sgwPort;
    }

    public void setSignallingChannel(User user) {

        // todo dependency injection

        ServerSocket userSocket = null;
        try {
            int nodePort = Util.getNodePort();
            userSocket = new ServerSocket(nodePort);
            userServerSockets.add(userSocket);
            user.createSignalChannel(nodePort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ServerSocket finalUserSocket = userSocket;
        Thread thisUserSignalChannelCommunication = new Thread(() -> {

            try {

                System.out.println("eNodeB is Waiting for User(SignallingChannel) on port " +
                        finalUserSocket.getLocalPort() + "...");

                Socket server = finalUserSocket.accept();

                synchronized (this) {
                    signallingServerSocketToSocketTable.put(finalUserSocket, server);
                }

                System.out.println("eNodeB: Just connected to User(SignallingChannel) by" + server.getRemoteSocketAddress());

                while (true) {
                    DataInputStream in = new DataInputStream(server.getInputStream());
                    userCompile(in.readUTF(), finalUserSocket);

                    /*
                    DataOutputStream out = new DataOutputStream(server.getOutputStream());
                    out.writeUTF("Thank you for connecting to " + server.getLocalSocketAddress()
                            + "\nGoodbye!");
                     */

                    //server.close();
                }
            } catch (SocketTimeoutException s) {
                System.out.println("Socket timed out!");
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }

        });

        usersSignallingCommunication.add(thisUserSignalChannelCommunication);
        thisUserSignalChannelCommunication.start();

    }

    private void userCompile(String inputData, ServerSocket serverSocket) {
        String title = inputData.substring(0, inputData.indexOf(":"));

        if (title.equals(Message.signaling_Channel_Setup)) {
            Integer userID = Integer.parseInt(inputData.substring(inputData.indexOf(":") + 1));

            synchronized (this) {
                usersSocketTableSignallingChannel.put(serverSocket, userID);
            }

            System.out.println("eNodeB with UID: " + UID + " got the User ID: " + userID + " (SignallingChannel) ");

        }
        if (title.equals(Message.my_Location)) {

            String string = inputData.substring(inputData.indexOf(":") + 1, inputData.indexOf("#"));

            String isReceiver = inputData.substring(inputData.indexOf("#") + 1);

            double x = Double.parseDouble(string.substring(string.indexOf("=") + 1, string.indexOf(",")));
            double y = Double.parseDouble(string.substring(string.indexOf(",") + 3));
            // extracting Location of user
            double distance = this.place.distance(new Place(x, y));

            //// Telling MME the distance ////
            // getting the ID of User

            int userID = usersSocketTableSignallingChannel.get(serverSocket);

            System.out.println("eNodeB with UID: " + UID + " got the User(ID): " + userID + " Location: " +
                    "x:" + x + " y:" + y + " (SignallingChannel) ");

            //
            try {
                OutputStream outToServer = mmeSocket.getOutputStream();
                DataOutputStream out = new DataOutputStream(outToServer);
                // telling  MME my distance from User
                out.writeUTF(Message.user_Distance + ":" + userID + "#" + distance + "/" + isReceiver);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    private void mmeCompile(String inputData) {

        String title = inputData.substring(0, inputData.indexOf(":"));
        if (title.equals(Message.user_Registration1)) {

            // type is sender or receiver.
            String type = inputData.substring(inputData.indexOf("#") + 1);

            int userID = Integer.parseInt(inputData.substring(inputData.indexOf(":") + 1, inputData.indexOf("#")));

            if (usersDataLink.containsKey(userID)) {
                // it has already connected to the user.
                System.out.println("ENodeB with UID: " + UID + " got MME message but " +
                        "it have been created the dataLink.");
                return;
            }

            Socket signallingSocket = getUserSignallingSocket(userID);
            System.out.println("ENodeB with UID: " + UID + " got the MME message" +
                    " to connect to user with ID: " + userID);

            int dataLinkPort = Util.getNodePort();

            try {

                // todo -> remove previous dataLinkConnection(increase memory health)
                ServerSocket dataLinkServerSocket = new ServerSocket(dataLinkPort);
                usersDataLink.put(userID, dataLinkServerSocket);


                synchronized (this) {
                    if (type.equals("receiver")) {
                        isInBufferingState.put(userID, true);
                    } else {
                        isInBufferingState.put(userID, false);
                    }
                    buffer.put(String.valueOf(userID), "");
                }

                Thread dataLinkThreadListener = new Thread(() -> {

                    try {
                        System.out.println("ENodeB with UID: " + UID + "is Waiting for user with ID: " + userID +
                                " (dataLinkCreation)" +
                                dataLinkServerSocket.getLocalPort() + "...");

                        Socket dataLinkSocket = dataLinkServerSocket.accept();

                        if (type.equals("receiver")) {
                            // gettingBufferedData.
                            tellMMEToSendBufferedDataFromPreviousENodeB(userID);
                        }

                        dataLinkServerSocketToSocketTable.put(dataLinkServerSocket, dataLinkSocket);

                        System.out.println("ENodeB with UID: " + UID + "just Connected to user with ID: " + userID +
                                " (dataLinkCreation)" + dataLinkSocket.getRemoteSocketAddress());

                        while (true) {
                            DataInputStream in = new DataInputStream(dataLinkSocket.getInputStream());
                            compileDataLinkData(in.readUTF(), userID);
                        }

                    } catch (SocketTimeoutException s) {
                        System.out.println("Socket timed out!");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                dataLinkThreadListener.start();

            } catch (IOException e) {
                e.printStackTrace();
            }


            try {
                DataOutputStream out = new DataOutputStream(signallingSocket.getOutputStream());
                out.writeUTF(Message.user_Registration2 + ":" + dataLinkPort);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (title.equals(Message.user_deregistration)) {

            int userID = Integer.parseInt(inputData.substring(inputData.indexOf(":") + 1));
            System.out.println("ENodeB with UID: " + UID + " is deregistrating user with ID: " + userID);
            // removing from dataLink's HashMap.
            // start buffering state
            synchronized (this) {
                //usersDataLink.remove(userID);
                isInBufferingState.put(userID, true);
                buffer.put(String.valueOf(userID), "");
                System.out.println("ENodeB with UID: " + UID + " starts buffering data of the user with ID: " + userID);
            }

            return;
        }

        if (title.equals(Message.create_Session)) {

            // request format : "create_session:receiver#sender"

            // todo log
            int startIndex = inputData.indexOf(":") + 1;
            int finalIndex = inputData.indexOf("#");
            int receiverID = Integer.parseInt(inputData.substring(startIndex, finalIndex));
            int senderID = Integer.parseInt(inputData.substring(finalIndex + 1));

            Socket receiverSocket = dataLinkServerSocketToSocketTable.get(usersDataLink.get(receiverID));

            try {

                DataOutputStream out = new DataOutputStream(receiverSocket.getOutputStream());

                out.writeUTF(Message.create_Session + ":" + senderID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        if (title.equals(Message.create_Session_Ack)) {

            // todo log
            int startIndex = inputData.indexOf(":") + 1;
            int finalIndex = inputData.indexOf("#");
            int receiverID = Integer.parseInt(inputData.substring(startIndex, finalIndex));
            int senderID = Integer.parseInt(inputData.substring(finalIndex + 1));

            Socket receiverSocket = dataLinkServerSocketToSocketTable.get(usersDataLink.get(receiverID));


            try {
                DataOutputStream out = new DataOutputStream(receiverSocket.getOutputStream());
                out.writeUTF(Message.create_Session_Ack + ":" + senderID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (title.equals(Message.send_Me_Buffered_Data)) {

            try {
                String userID = inputData.substring(inputData.indexOf(":") + 1);
                DataOutputStream out = new DataOutputStream(mmeSocket.getOutputStream());

                System.out.println("ENodeB with UID: " + UID + " is going to send bufferedData for user with ID: " + userID);

                while (isInBufferingState.get(Integer.valueOf(userID))) {

                    synchronized (this) {

                        String bufferedData = buffer.get(userID);

                        System.out.println("ENodeB with UID: " + UID +
                                " has buffer: " + bufferedData + " for user with ID: " + userID);

                        if (!bufferedData.equals("")) {
                            // give MME bufferedData of previous ENodeB
                            try {
                                out.writeUTF(Message.buffered_Data + ":" + bufferedData + "#" + userID);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            buffer.put(userID, "");

                        } else {
                            break;
                        }
                    }

                    // maybe getting more bufferedData.
                    try {
                        sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

                // tell MME end of handover

                try {
                    out.writeUTF(Message.handover_Complete + ":" + userID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (title.equals(Message.buffered_Data)) {

            String userID = inputData.substring(inputData.indexOf("#") + 1);
            String data = inputData.substring(inputData.indexOf(":") + 1, inputData.indexOf("#"));

            synchronized (this) {
                if (bufferFromAnotherENode.containsKey(userID)) {
                    String untilNow = bufferFromAnotherENode.get(userID);
                    untilNow = untilNow.concat(data);
                    bufferFromAnotherENode.put(userID, untilNow);
                } else {
                    bufferFromAnotherENode.put(userID, data);
                }
            }
            return;
        }

        if (title.equals(Message.handover_Complete)) {

            String userID = inputData.substring(inputData.indexOf(":") + 1);

            synchronized (this) {

                String fromAnotherNode = bufferFromAnotherENode.get(userID);

                String current = buffer.get(userID);

                // no bufferedData and just directly handOverDone
                if (fromAnotherNode != null) {
                    current = fromAnotherNode.concat(current);
                }

                // emptying buffer
                buffer.put(userID, "");
                // sending until now to dest.
                Socket dataLinkOfThisUser = dataLinkServerSocketToSocketTable.get(usersDataLink.get(Integer.valueOf(userID)));

                try {
                    DataOutputStream out = new DataOutputStream(dataLinkOfThisUser.getOutputStream());
                    // syntax: data_carrier:sender#current.
                    out.writeUTF(Message.data_Carrier + ":" + "buffer" + "#" + current);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                isInBufferingState.put(Integer.valueOf(userID), false);
            }

        }

    }


    private void compileDataLinkData(String inputData, int senderID) {

        String title = inputData.substring(0, inputData.indexOf(":"));
        if (title.equals(Message.create_Session)) {

            // sending mme the create session request
            String receiverID = inputData.substring(inputData.indexOf(":") + 1);

            System.out.println("ENodeB with UID: " + UID + " got the creation session from user with id: "
                    + senderID + " to user with id: " + receiverID);

            try {
                DataOutputStream out = new DataOutputStream(mmeSocket.getOutputStream());

                out.writeUTF(Message.create_Session + ":" + receiverID + "#" + senderID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (title.equals(Message.create_Session_Ack)) {

            // sending mme the create session ack request
            String ackDest = inputData.substring(inputData.indexOf(":") + 1);

            System.out.println("ENodeB with UID: " + UID + " got the ack of creation session from user with id: "
                    + senderID + " to user with id: " + ackDest);


            try {
                DataOutputStream out = new DataOutputStream(mmeSocket.getOutputStream());
                out.writeUTF(Message.create_Session_Ack + ":" + ackDest + "#" + senderID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (title.equals(Message.data_Carrier)) {

            // sending sgw the data session request
            String receiverID = inputData.substring(inputData.indexOf(":") + 1, inputData.indexOf("#"));
            String data = inputData.substring(inputData.indexOf("#") + 1);

            System.out.println("ENodeB with UID: " + UID + " got DataCarrier from user with id: "
                    + senderID + " to user with id: " + receiverID);

            try {

                DataOutputStream out = new DataOutputStream(sgwSocket.getOutputStream());

                out.writeUTF(Message.data_Carrier + ":" + receiverID + "#" + data + "/" + senderID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

    }

    public Socket getUserSignallingSocket(int userID) {
        for (ServerSocket serverSocket : usersSocketTableSignallingChannel.keySet()) {
            if (usersSocketTableSignallingChannel.get(serverSocket).equals(userID)) {
                return signallingServerSocketToSocketTable.get(serverSocket);
            }
        }
        System.out.println("ENodeB: ServerSocket for this userID not Found");
        return null;
    }

    public void compileSGWResponses(String inputData) {

        String title = inputData.substring(0, inputData.indexOf(":"));
        String receiver = inputData.substring(inputData.indexOf(":") + 1, inputData.indexOf("#"));
        String sender = inputData.substring(inputData.indexOf("#") + 1, inputData.indexOf("/"));
        String data = inputData.substring(inputData.indexOf("/") + 1);

        if (title.equals(Message.data_Carrier)) {

            System.out.println("ENodeB with UID: " + UID + " get's data from sgw from user: " + sender +
                    " and gives it to receiver: " + receiver + " and content is: " + data);


            // new ENodeB and user isn't fetched yet.
            while (isInBufferingState.get(Integer.valueOf(receiver)) == null) {

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            Socket receiverSocket = dataLinkServerSocketToSocketTable.get(usersDataLink.get(Integer.valueOf(receiver)));

            if (isInBufferingState.get(Integer.valueOf(receiver))) {

                if (!buffer.containsKey(receiver)) {

                    synchronized (this) {
                        //start buffering
                        buffer.put(receiver, "");
                    }

                } else {

                    String untilNow = buffer.get(receiver);
                    untilNow = untilNow.concat(data);

                    synchronized (this) {
                        // add to buffer
                        buffer.put(receiver, untilNow);
                    }

                }
                return;
            }

            // it is not in buffering state so sends it.

            try {
                DataOutputStream out = new DataOutputStream(receiverSocket.getOutputStream());
                out.writeUTF(Message.data_Carrier + ":" + sender + "#" + data);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    private void tellMMEToSendBufferedDataFromPreviousENodeB(int userID) {

        // tell MME to send bufferedData of previous ENodeB
        try {
            System.out.println("ENodeB with UID: " + UID + " is telling MME to get " +
                    "buffeted Data of previousConnected ENodeB.");
            DataOutputStream out = new DataOutputStream(mmeSocket.getOutputStream());
            out.writeUTF(Message.send_Me_Buffered_Data + ":" + userID);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        Boolean a = null;
        if (a) {

        }
    }

}
