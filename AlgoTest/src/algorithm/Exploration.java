package algorithm;

import connection.MessageListener;
import connection.WifiSocket;

import static algorithm.Arena.*;

public class Exploration implements MessageListener {
    private WifiSocket socket;
    private Arena arena;
    private boolean started = false;
    private int previousCommand;

    public Exploration() {
        socket = WifiSocket.getInstance();
        socket.setMessageListener(this);
        arena = new Arena();
        socket.write("A", "I");
        previousCommand = FORWARD;
    }

    @Override
    public void messageReceived(String message) {
        switch (message) {
            case "M":
                arena.move(FORWARD, 1);
                previousCommand = FORWARD;
                socket.write("A", "I");
                break;
            case "L":
                arena.move(LEFT, 1);
                previousCommand = LEFT;
                socket.write("A", "I");
                break;
            case "R":
                arena.move(RIGHT, 1);
                previousCommand = RIGHT;
                socket.write("A", "I");
                break;
            case "exs":
                start();
                break;
            default:
                if (message.contains("#")) {
                    parseSensorReadings(message);
                    arena.displayGrid();
                    if (started) test();
                }
        }
    }

    public void start() {
        started = true;
        test();
    }

    private void test() {
        if (arena.isMovable(LEFT)) {
            if (previousCommand == FORWARD) {
                socket.write("A", "L");
                return;
            }
        }

        if (arena.isMovable(FORWARD)) {
            socket.write("A", "M");
            return;
        }

        socket.write("A", "R");
    }

    private void parseSensorReadings(String message) {
        String[] readings = message.split("#");

        if (readings.length == 6) {
            System.out.println();
            System.out.println(message);

            for (int i = 0; i <= 5; i++) {
                if (i == 1) continue;
                double reading = Double.parseDouble(readings[i]);
                int status = GRID_EXPLORED;
                int distance = 1;

                if (reading > 28) {
                    distance = 3;
                } else if (reading > 20) {
                    status = GRID_SUSPECT;
                    distance = 3;
                } else if (reading > 10) {
                    status = GRID_SUSPECT;
                    distance = 2;
                } else if (reading > 0) {
                    status = GRID_OBSTACLE;
                } else {
                    continue;
                }

                arena.setGridSensor(i + 1, distance, status);
            }

            double reading1 = Double.parseDouble(readings[0]);
            double reading2 = Double.parseDouble(readings[1]);
            double reading3 = Double.parseDouble(readings[2]);

            if (reading1 > 0 && reading1 <= 10 && reading3 > 0 && reading3 <= 10) {
                arena.setGridSensor(2, 1, GRID_OBSTACLE);
            } else {
                int status = GRID_EXPLORED;
                int distance;

                if (reading2 > 28) {
                    distance = 3;
                } else if (reading2 > 20) {
                    status = GRID_SUSPECT;
                    distance = 3;
                } else if (reading2 > 10) {
                    status = GRID_SUSPECT;
                    distance = 2;
                } else {
                    return;
                }

                arena.setGridSensor(2, distance, status);
            }
        }
    }
}
