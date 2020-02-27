package connection;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class WifiSocket {
    private static final int PORT = 9123;
    private static final String ADDRESS = "192.168.16.16";

    private static WifiSocket instance;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private MessageListener messageListener;

    private WifiSocket() {}

    public static WifiSocket getInstance() {
        if (instance == null) instance = new WifiSocket();
        return instance;
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public void connect() {
        try {
            socket = new Socket(ADDRESS, PORT);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            ReceiveThread receiveThread = new ReceiveThread();
            receiveThread.start();
            System.out.println("Connected successfully.");
        } catch (IOException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            socket.close();
            socket = null;
            System.out.println("Disconnected successfully.");
        } catch (IOException e) {
            System.out.println("Disconnection failed: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return (socket != null && socket.isConnected());
    }

    public void write(String destination, String message) {
        byte[] output = (destination + message).getBytes();

        try {
            outputStream.write(output);
            System.out.println("Message sent: " + destination + message);
        } catch (IOException e) {
            System.out.println("Write failed: " + e.getMessage());
        }
    }

    private class ReceiveThread extends Thread {
        private byte[] buffer = new byte[1024];

        @Override
        public void run() {
            if (inputStream == null || outputStream == null) return;

            while (isConnected()) {
                try {
                    int result = inputStream.read(buffer);
                    if (result < 0) break;
                } catch (IOException e) {
                    System.out.println("Read failed: " + e.getMessage());
                    break;
                }

                String ret = new String(buffer, StandardCharsets.UTF_8);
                ret = ret.trim().replace("\u0000", "");
                if (messageListener != null) messageListener.messageReceived(ret);
                buffer = new byte[1024];
            }

            disconnect();
        }
    }
}
