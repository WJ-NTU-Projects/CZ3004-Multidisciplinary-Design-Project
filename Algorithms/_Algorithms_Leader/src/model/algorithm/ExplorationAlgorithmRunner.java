package model.algorithm;

import model.entity.Grid;
import model.entity.Robot;
import model.util.MessageMgr;
import model.util.SocketMgr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import java.util.Date;
import java.util.HashMap;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import static constant.CommConstants.TARGET_ANDROID;
import static constant.CommConstants.TARGET_ARDUINO;
import static constant.CommConstants.TARGET_RPIIMAGE;
import static constant.MapConstants.MAP_COLS;
import static constant.MapConstants.MAP_ROWS;
import static constant.RobotConstants.*;

/**
 * Algorithm for exploration phase (full exploration)
 */
public class ExplorationAlgorithmRunner implements AlgorithmRunner {
    private static final int START_X = 0;
    private static final int START_Y = 17;
    private int wayPointX = -1;
    private int wayPointY = -1;
//    private List<String> pastMovements = new ArrayList<>();
//    private List<Integer> pastX = new ArrayList<>();
//    private List<Integer> pastY = new ArrayList<>();
    
    public ExplorationAlgorithmRunner(int speed){

    }

    @Override
    public void run(Grid grid, Robot robot, boolean realRun) {
        if (!realRun) return;
        grid.reset();
        robot.reset();
        calibrateAtStart();

        if (wayPointX == -1 && wayPointY == -1) {
            String msg = SocketMgr.getInstance().receiveMessage();
            List<Integer> waypoints;

            while (true) {
                System.out.println("Waiting for waypoint");

                while (msg == null) {
                    msg = SocketMgr.getInstance().receiveMessage();
                }

                waypoints = MessageMgr.parseMessage(msg);
                if (waypoints == null) continue;
                wayPointX = waypoints.get(0) - 1;
                wayPointY = waypoints.get(1) - 1;
                break;
            }
        }

        grid.clearObstacles();
        String msg = SocketMgr.getInstance().receiveMessage();

        while (!msg.equals("exs")) {
            System.out.println("Waiting for exs");
            msg = SocketMgr.getInstance().receiveMessage();
        }

        runExplorationAlgorithmThorough(grid, robot);
        calibrateAndTurn(robot);
        SocketMgr.getInstance().sendMessage(TARGET_ANDROID, "exe");
        String m =  MessageMgr.generateMapDescriptorMsg(grid.generateAllForAndroid(), robot.getCenterPosX(), robot.getCenterPosY(), robot.getHeading());
        SocketMgr.getInstance().sendMessage(TARGET_ANDROID, m);
        
        AlgorithmRunner algorithmRunner = new FastestPathAlgorithmRunner(1, wayPointX, wayPointY);
        algorithmRunner.run(grid, robot, true);
    }

    private void calibrateAndTurn(Robot robot) {
        SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "M");
        SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
        SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "L");
        robot.turn(LEFT);
        SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
        SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "L");
        robot.turn(LEFT);

        while (robot.getHeading() != NORTH) {
            robot.turn(RIGHT);
            SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
            SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
        }
    }

    private void runExplorationAlgorithmThorough(Grid grid, Robot robot) {
        boolean endZoneFlag = false;
        boolean startZoneFlag = false;
        System.out.println("start exploration");
        SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "I");
        robot.sense();

        while (!endZoneFlag || !startZoneFlag) {
        	do {
                leftWallFollower(robot);
        	} while(robot.isObstacleAhead());

            SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "M");
            boolean haveMoved = senseAndUpdateAndroid(robot, grid);

            if (haveMoved) {
                robot.move();
                SocketMgr.getInstance().sendMessage(TARGET_RPIIMAGE, "P");
            } else {
                continue;
            }

            if (Grid.isInEndZone(robot.getPosX(), robot.getPosY())) endZoneFlag = true;
            if (endZoneFlag && Grid.isInStartZone(robot.getPosX() + 2, robot.getPosY())) startZoneFlag = true;

            if(grid.checkExploredPercentage() == 100 && !startZoneFlag){
                Robot fakeRobot = new Robot(grid, new ArrayList<>());
                fakeRobot.setPosX(robot.getPosX());
                fakeRobot.setPosY(robot.getPosY());
                fakeRobot.setHeading(robot.getHeading());
                List<String> returnPath = AlgorithmRunner.runAstar(robot.getPosX(), robot.getPosY(), START_X, START_Y, grid, fakeRobot);

                if (returnPath == null) {
                	System.out.println("unreachable");
                	continue;
                }
                
                List<String> pathSegments = new ArrayList<>();
                for(int i = 0; i< returnPath.size(); i+=5) {
                	int j = i;
                	StringBuilder builder = new StringBuilder();

                	while(j<returnPath.size()&&j<i+5) {
                		builder.append(returnPath.get(j));
                		j++;
                	}

                	pathSegments.add(builder.toString());
                	pathSegments.add("C");
                }

                fakeRobot.setPosX(robot.getPosX());
                fakeRobot.setPosY(robot.getPosY());
                fakeRobot.setHeading(robot.getHeading());
                System.out.println("Algorithm finished, executing actions");
                System.out.println(returnPath.toString());

                for (String pathSegment : pathSegments) {
                    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, pathSegment);
                }

                for (String action : returnPath) {
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

                    //SocketMgr.getInstance().sendMessage(TARGET_ANDROID, MessageMgr.generateMapDescriptorMsg(grid.generateForAndroid(), robot.getCenterPosX(), robot.getCenterPosY(), robot.getHeading()));
                }

                if (endZoneFlag && Grid.isInStartZone(robot.getPosX() + 2, robot.getPosY())) startZoneFlag = true;
            }
        }

        grid.markEverythingExplored();
        System.out.println("EXPLORATION COMPLETED!");
    }

    private void leftWallFollower(Robot robot) {
        if (robot.isObstacleAhead()) {
            if (robot.isObstacleRight() && robot.isObstacleLeft()) {
                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
                robot.sense();
                robot.turn(RIGHT);
                SocketMgr.getInstance().sendMessage(TARGET_RPIIMAGE, "P");

                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
                robot.sense();
                robot.turn(RIGHT);
                SocketMgr.getInstance().sendMessage(TARGET_RPIIMAGE, "P");
            } else if (robot.isObstacleLeft()) {
                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
                robot.sense();
                robot.turn(RIGHT);
                SocketMgr.getInstance().sendMessage(TARGET_RPIIMAGE, "P");
            } else {
                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "L");
                robot.sense();
                robot.turn(LEFT);
                SocketMgr.getInstance().sendMessage(TARGET_RPIIMAGE, "P");
            }
        } else if (!robot.isObstacleLeft()) {
            SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "L");
            robot.sense();
            robot.turn(LEFT);
            SocketMgr.getInstance().sendMessage(TARGET_RPIIMAGE, "P");
        }
    }

    private void calibrateAtStart() {
        SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "S");
    }

    private boolean senseAndUpdateAndroid(Robot robot, Grid grid) {
        boolean result = robot.sense();
        SocketMgr.getInstance().sendMessage(TARGET_ANDROID, MessageMgr.generateMapDescriptorMsg(grid.generateAllForAndroid(), robot.getCenterPosX(), robot.getCenterPosY()+1, robot.getHeading()));
        return result;
    }
}