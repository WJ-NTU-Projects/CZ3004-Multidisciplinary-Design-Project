package algorithm;

import connection.MessageListener;
import connection.WifiSocket;

public class Exploration implements MessageListener {
    private WifiSocket socket;

    public Exploration() {
        socket = WifiSocket.getInstance();
        socket.setMessageListener(this);
    }

    @Override
    public void messageReceived(String message) {
        System.out.println(message);
        test();
    }

    public void test() {
        socket.write("A", "I");
        socket.write("A", "I");
        socket.write("A", "I");
        socket.write("A", "I");
        socket.write("A", "I");
    }
}
