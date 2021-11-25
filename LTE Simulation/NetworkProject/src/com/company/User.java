package com.company;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import static java.lang.Thread.sleep;

public class User {

    Boolean isReceiver = false;

    // initially Thread Safe.
    ArrayList<Socket> signallingSockets;

    // Being Thread Safe.
    Socket dataLinkSocket;

    // it is been Thread safe by synchronization
    HashMap<String, String> gotData;

    private Place[] places;
    private String id;

    // in mSeconds
    private Integer timeInterval;

    public User(String id, String places, Integer timeInterval) {
        signallingSockets = new ArrayList<>();
        this.id = id;
        this.timeInterval = timeInterval;
        gotData = new HashMap<>();
        compileToPoints(places);
    }


    private void compileToPoints(String places) {

        String[] points = places.trim().split(" ");

        if (points.length == 0) {
            System.out.println("Invalid User Places");
            return;
        }

        this.places = new Place[points.length];
        int counter = 1;
        for (String string : points) {
            int commaIndex = string.indexOf(",");
            double x = Double.parseDouble(string.substring(1, commaIndex));
            double y = Double.parseDouble(string.substring(commaIndex + 1, string.length() - 1));
            Place place = new Place(x, y);
            this.places[counter - 1] = place;
            counter++;
        }
    }

    public void createSignalChannel(int nodePort) {

        try {
            System.out.println("User: " + id + " is Connecting to eNodeB(Signalling) on port " + nodePort);

            Socket signalSocket = new Socket(Util.getNode_ServerName(), nodePort);
            signallingSockets.add(signalSocket);

            System.out.println("User: " + id + " Just Connected to eNode(Signalling) on " + signalSocket.getRemoteSocketAddress() +
                    " port " + " with this localPort " + signalSocket.getLocalSocketAddress());

            OutputStream outToServer = signalSocket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);

            // telling eNodeB my ID
            out.writeUTF(Message.signaling_Channel_Setup + ":" + id);

            Thread listenerToENodeBResponses = new Thread(() -> {

                while (true) {

                    try {
                        InputStream inFromServer = signalSocket.getInputStream();
                        DataInputStream in = new DataInputStream(inFromServer);
                        compileENodeB_signalling_Responses(in.readUTF());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            });

            listenerToENodeBResponses.start();


            //signalSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void createDataLinkChannel(int nodePort) {

        try {

            System.out.println("User: " + id + " is Connecting to eNodeB(DataLinkChannel) on port " + nodePort);

            synchronized (this) {
                dataLinkSocket = new Socket(Util.getNode_ServerName(), nodePort);
            }

            System.out.println("User: " + id + " Just Connected to eNode(DataLinkChannel) on " + dataLinkSocket.getRemoteSocketAddress() +
                    " port " + " with this localPort " + dataLinkSocket.getLocalSocketAddress());


            Thread dataLinkCommunicationListener = new Thread(() -> {

                while (true) {

                    try {
                        InputStream inFromServer = dataLinkSocket.getInputStream();
                        DataInputStream in = new DataInputStream(inFromServer);
                        compileDataLink(in.readUTF());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            });

            dataLinkCommunicationListener.start();

            //dataLinkSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void startTimer() {
        Thread tellingAddress = new Thread(() -> {

            // calling eNodes it's location
            int counter = 1;
            while (true) {

                System.out.println("User: " + id + " is telling his Location(" + counter + ") to ALL eNodeBs(Signalling)");

                for (Socket socket : signallingSockets) {

                    try {
                        OutputStream outToServer = socket.getOutputStream();
                        DataOutputStream out = new DataOutputStream(outToServer);
                        // telling eNodeB my Location
                        out.writeUTF(Message.my_Location + ":" + places[counter - 1] + "#" + isReceiver);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                if (counter == places.length) {
                    break;
                }

                counter++;
                // wait timeInterval :)
                try {
                    sleep(timeInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }


        });
        tellingAddress.start();

    }

    public String getId() {
        return id;
    }

    public Place[] getPlaces() {
        return places;
    }

    public Integer getTimeInterval() {
        return timeInterval;
    }


    private void compileENodeB_signalling_Responses(String inputData) {
        String title = inputData.substring(0, inputData.indexOf(":"));

        if (title.equals(Message.user_Registration2)) {
            int dataLinkLayerPort = Integer.parseInt(inputData.substring(inputData.indexOf(":") + 1));
            createDataLinkChannel(dataLinkLayerPort);
        }
    }


    public void sessionCreation(int receiverID) {

        // wait until the dataLink initializes
        while (dataLinkSocket == null) {

            try {
                sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        try {

            System.out.println("user with Id: " + getId() + " requests creating session with " +
                    "user with Id: " + receiverID);

            OutputStream outToServer = dataLinkSocket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);
            // telling eNodeB that I wanna sessionCreation
            out.writeUTF(Message.create_Session + ":" + receiverID);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void compileDataLink(String inputData) {
        String title = inputData.substring(0, inputData.indexOf(":"));

        if (title.equals(Message.create_Session)) {

            String senderID = inputData.substring(inputData.indexOf(":") + 1);

            synchronized (this) {
                isReceiver = true;
                System.out.println("User with ID: " + id + " becomes receiver");
            }

            System.out.println("user with Id: " + getId() + " got creating session request with " +
                    "user with Id: " + senderID);


            try {

                OutputStream outToServer = dataLinkSocket.getOutputStream();
                DataOutputStream out = new DataOutputStream(outToServer);
                out.writeUTF(Message.create_Session_Ack + ":" + senderID);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        if (title.equals(Message.create_Session_Ack)) {

            String ackSenderID = inputData.substring(inputData.indexOf(":") + 1);

            System.out.println("user with Id: " + getId() + " got ack of creating session with " +
                    "user with Id: " + ackSenderID);

            sendDataToReceiver(ackSenderID);
            return;
        }

        if (title.equals(Message.data_Carrier)) {

            String sender = inputData.substring(inputData.indexOf(":") + 1, inputData.indexOf("#"));
            String data = inputData.substring(inputData.indexOf("#") + 1);

            System.out.println("user with id: " + id + " got data from user: " + sender + " and the" +
                    " content is: " + data);


            if (sender.equals("buffer")) {
                System.out.println("User: these Data Came from Buffer: " + data);
                return;
            }

            synchronized (this) {
                if (gotData.containsKey(sender)) {
                    String untilNow = gotData.get(sender);
                    untilNow = untilNow.concat(data);
                    gotData.put(sender, untilNow);
                } else {
                    gotData.put(sender, data);
                }
            }
            System.out.println(gotData.get(sender));
        }

    }

    public void sendDataToReceiver(String receiverID) {

        Object[] loadedData = readDataFromUrl();

        try {

            for (Object object : loadedData) {

                String frag = (String) object;
                System.out.println("user with Id: " + getId() + " is sending data to user with Id: " + receiverID);
                OutputStream outToServer = dataLinkSocket.getOutputStream();
                DataOutputStream out = new DataOutputStream(outToServer);
                // telling eNodeB that I wanna sessionCreation
                out.writeUTF(Message.data_Carrier + ":" + receiverID + "#" + frag);

                try {
                    sleep(timeInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        } catch (
                IOException e) {
            e.printStackTrace();
        }

    }

    public static Object[] readDataFromUrl() {

        ArrayList<String> result = new ArrayList<>();
        try {
            File myObj = new File("filename.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                result.add(myReader.nextLine());
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return result.toArray();

    }

    public static void main(String[] args) {
        for (Object object : readDataFromUrl()) {
            System.out.println((String) object);
        }
    }

}