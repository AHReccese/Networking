package com.company.practice;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class GreetingServer extends Thread {
    private ServerSocket serverSocket;

    public GreetingServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(10000);
    }

    public void run() {

        while (true) {
            try {
                System.out.println("Waiting for client on port " +
                        serverSocket.getLocalPort() + "...");
                Socket server = serverSocket.accept();

                System.out.println("Server: Just connected to " + server.getRemoteSocketAddress());

                while (true) {
                    DataInputStream in = new DataInputStream(server.getInputStream());
                    System.out.println(in.readUTF());

                    DataOutputStream out = new DataOutputStream(server.getOutputStream());
                    out.writeUTF("Thank you for connecting to " + server.getLocalSocketAddress()
                            + "\nGoodbye!");
                    //server.close();
                }

            } catch (SocketTimeoutException s) {
                System.out.println("Socket timed out!");
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String[] args) {
        // Testing Greeting Server.
    }

}