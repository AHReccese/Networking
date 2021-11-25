package com.company;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Util {

    private static int mmePort = 0;
    private static int sgwPort = 0;
    private static int nodePort = 0;


    public static String getMME_ServerName() {
        return "127.0.0.1";
    }

    public static String getSGW_ServerName() {
        return "127.0.0.2";
    }

    public static String getNode_ServerName() {
        return "127.0.0.3";
    }

    public static int getMmePort() {
        mmePort++;
        return mmePort;
    }

    public static int getSgwPort() {
        //sgwPort++;
        //return sgwPort;
        return getMmePort();
    }

    public static int getNodePort() {
        //nodePort++;
        //return nodePort;
        return getMmePort();
    }


    public static void main(String[] args) {

        String a = null;
        if(a == null | a.equals("")){

        }

    }
}
