package model.algorithm;

import model.entity.Grid;
import model.entity.Robot;
import model.util.MessageMgr;
import model.util.SocketMgr;

import java.util.ArrayList;
import java.util.List;

import static constant.CommConstants.TARGET_ANDROID;
import static constant.CommConstants.TARGET_ARDUINO;
import static constant.MapConstants.MAP_ROWS;
import static constant.RobotConstants.*;

/**
 * Fastest path algorithm using A* search + customized score functions
 */
public class FastestPathAlgorithmRunner implements AlgorithmRunner {

    private int mWayPointX = -1;
    private int mWayPointY = -1;
    private static final int START_X = 0;
    private static final int START_Y = 17;
    private static final int GOAL_X = 12;
    private static final int GOAL_Y = 0;

    public FastestPathAlgorithmRunner(int speed) {
    }

    public FastestPathAlgorithmRunner(int speed, int x, int y) {
        mWayPointX = x;
        mWayPointY = y;
    }

    @Override
    public void run(Grid grid, Robot robot, boolean realRun) {
        if (!realRun) return;
        robot.reset();

        // receive waypoint
        int wayPointX = mWayPointX;
        int wayPointY = mWayPointY;

        // run from start to waypoint and from waypoint to goal
        System.out.println("Fastest path algorithm started with waypoint " + wayPointX + "," + wayPointY);
        Robot fakeRobot = new Robot(new Grid(), new ArrayList<>());
        List<String> path1 = AlgorithmRunner.runAstar(START_X, START_Y, wayPointX, wayPointY, grid, fakeRobot);
        List<String> path2 = AlgorithmRunner.runAstar(wayPointX, wayPointY, GOAL_X, GOAL_Y, grid, fakeRobot);

        if (path1 != null && path2 != null) {
            path1.addAll(path2);
            System.out.println(path1.toString());
            StringBuilder builder = new StringBuilder();
            System.out.println("fastest path:");
            int j = path1.size()-1;

            while(path1.get(j).compareTo("M")==0) {
                j--;
            }

            if(path1.get(j).compareTo("L")==0) {
                path1.add(j+1, "M");
                path1.add(j-1, "M");
            }

            else if(path1.get(j).compareTo("R")==0 ) {
                path1.add(j+1, "M");
                path1.add(j-1, "M");
            }

            for (String s : path1) {
                builder.append(s);
                System.out.print(s + ' ');
            }

            String path = builder.toString();

            if (path.charAt(0) == 'R') {
                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
                robot.turn(RIGHT);
                path = path.substring(1);
            }

            String msg = SocketMgr.getInstance().receiveMessage();

            while (!msg.equals("beginFastest")) {
                System.out.println("waiting for begin Fastest");
                msg = SocketMgr.getInstance().receiveMessage();
            }

            SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, path);
            // SIMULATE AT THE SAME TIME
            for (String action : path1) {
                switch (action) {
                    case "M":
                        robot.move();
                        break;
                    case "L":
                        robot.turn(LEFT);
                        break;
                    case "R":
                        robot.turn(RIGHT);
                        break;
                    case "T":
                        robot.turn(LEFT);
                        robot.turn(LEFT);
                        break;
                }
            }

            msg = SocketMgr.getInstance().receiveMessage();

            while (!msg.equals("fe")) {
                System.out.println("waiting for fe");
                msg = SocketMgr.getInstance().receiveMessage();
            }

            SocketMgr.getInstance().sendMessage(TARGET_ANDROID, "fe");
        } else {
            System.out.println("Fastest path not found!");
        }
    }
}
