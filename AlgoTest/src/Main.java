import algorithm.Exploration;
import connection.WifiSocket;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input = "";
        WifiSocket socket = WifiSocket.getInstance();

        while (!input.equalsIgnoreCase("exit")) {
            input = scanner.nextLine();

            if (input.equalsIgnoreCase("c")) {
                if (!socket.isConnected()) socket.connect();
                continue;
            }

            if (input.equalsIgnoreCase("d")) {
                if (socket.isConnected()) socket.connect();
                continue;
            }

            if (input.equalsIgnoreCase("w")) {
                String write = scanner.nextLine();
                if (socket.isConnected()) socket.write("Z", write);
            }

            if (input.equalsIgnoreCase("exs")) {
                (new Exploration()).test();
            }
        }
    }
}