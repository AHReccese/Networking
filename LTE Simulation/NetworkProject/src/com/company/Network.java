package com.company;

import java.util.ArrayList;

import static java.lang.Thread.sleep;

public class Network {

    private ArrayList<eNodeB> eNodeBs;
    private ArrayList<User> users;
    private SGW sgw;
    private MME mme;

    Network(String places) {

        eNodeBs = new ArrayList<>();
        users = new ArrayList<>();
        mme = new MME();
        sgw = new SGW();
        /////////// eNode Creation //////////
        String[] points = places.trim().split(" ");

        if (points.length == 0) {
            System.out.println("Invalid ENode Places");
            return;
        }

        int counter = 1;
        for (String string : points) {
            int commaIndex = string.indexOf(",");
            double x = Double.parseDouble(string.substring(1, commaIndex));
            double y = Double.parseDouble(string.substring(commaIndex + 1, string.length() - 1));
            eNodeB nodeB = new eNodeB(x, y, counter);
            eNodeBs.add(nodeB);
            counter++;

        }

        // todo -> link Speed

    }

    public void addUser(String userID, String places, Integer timeInterval) {

        // network Topology is Ready :)
        User user = new User(userID, places, timeInterval);
        users.add(new User(userID, places, timeInterval));

        for (eNodeB nodeB : eNodeBs) {
            nodeB.setSignallingChannel(user);
        }

        user.startTimer();

    }

    // overLoaded.
    public void addUser(User user) {

        users.add(user);

        for (eNodeB nodeB : eNodeBs) {
            nodeB.setSignallingChannel(user);
        }

        user.startTimer();

    }


    public void initNetwork() {
        mme.initialize(sgw, eNodeBs);
        sgw.initialize(eNodeBs);

        ArrayList<Thread> threads = new ArrayList<>();

        int i = 0;
        for (eNodeB nodeB : eNodeBs) {
            Thread thread = new Thread(nodeB::initialSockets);
            threads.add(thread);
            thread.start();
        }

        // waiting to join threads
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // all socket's get ready and alive.
    }

    public static void main(String[] args) {

        /*Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sleep(10000);
                    System.out.println("Hello");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        */

        String places = "(7.5,10) (7.5,8) (7.5,6)";
        Network network = new Network(places);
        network.initNetwork();

        // in ms
        int timeInterval = 1000;

        User user1 = new User("123", "(5,10) (5,8) (5,6)", timeInterval);
        User user2 = new User("124", "(10,6) (10,8) (10,10)", timeInterval);

        User user3 = new User("125", "(6,11) (7,11) (8,11)", timeInterval);
        User user4 = new User("126", "(8,5) (7,5) (6,5)", timeInterval);

        network.addUser(user1);
        network.addUser(user2);

        network.addUser(user3);
        network.addUser(user4);

        user2.sessionCreation(Integer.parseInt(user1.getId()));
        user4.sessionCreation(Integer.parseInt(user3.getId()));

    }

    public void giveUserPortToDataLinkConnection(int userID, int dataLinkPort) {
        for (User user : users) {
            if (user.getId().equals(String.valueOf(userID))) {
                user.createDataLinkChannel(dataLinkPort);
                break;
            }
        }
    }
}