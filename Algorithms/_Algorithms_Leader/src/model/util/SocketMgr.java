package model.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * A singleton class for using the socket
 */
public class SocketMgr {

    private static SocketMgr mInstance;
    private Socket mSocket;
    private PrintWriter mSocketWriter;
    private BufferedReader mSocketReader;
    private static final int PORT = 9123;
    private static final String ADDRESS = "192.168.16.16";

    private SocketMgr() { }

    public static SocketMgr getInstance() {
        if (mInstance == null)
            mInstance = new SocketMgr();
        return mInstance;
    }

    public boolean openConnection() {
        try {
            mSocket = new Socket(ADDRESS, PORT);
            mSocketWriter = new PrintWriter(mSocket.getOutputStream(), true);
            mSocketReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            System.out.println("Socket connection successful");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Socket connection failed");
            return false;
        }
    }

    public void closeConnection() {
        mSocketWriter.close();
        try {
            mSocketReader.close();
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Socket connection closed");
    }

    public boolean isConnected() {
        return mSocket != null && mSocket.isConnected();
    }

    public void sendMessage(String dest, String msg) {
        mSocketWriter.println(dest + msg);
        System.out.println("Sent message: " + dest + msg);
    }

    public String receiveMessage() {
        try {
            mSocket.setSoTimeout(0);
        } catch (SocketException e) {
        	e.printStackTrace();
        }

        try {
        	String msg;
        	msg = mSocketReader.readLine();
            System.out.println("Received message: " + msg);
            return msg;
        } catch (SocketTimeoutException e) {
//            System.out.println("Sensor reading timeout!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void clearInputBuffer() {
        String input;
        try {
            while ((input = mSocketReader.readLine()) != null) {
                System.out.println("Discarded message: " + input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
