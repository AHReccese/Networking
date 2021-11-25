package com.company.practice;

import java.io.*;
import java.net.Socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static java.lang.Thread.sleep;

public class GreetingClient {

    public static void main(String[] args) throws IOException {

        String serverName = "localhost";
        int port = 2;
        // creating server with this port
        Thread t = new GreetingServer(port);
        t.start();
        try {

            System.out.println("Connecting to " + serverName + " on port " + port);

            // block until the server connected to socket
            Socket client = new Socket(serverName, port);

            System.out.println("client: " + client.getLocalSocketAddress());

            System.out.println("Client: Just connected to " + client.getRemoteSocketAddress());

            OutputStream outToServer = client.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);

            out.writeUTF("Hello from " + client.getLocalSocketAddress());


            InputStream inFromServer = client.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);

            System.out.println("Server says " + in.readUTF());

            //client.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}