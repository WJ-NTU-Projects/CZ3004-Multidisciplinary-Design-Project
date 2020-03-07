package model.algorithm;

import model.entity.Grid;
import model.entity.Robot;
import model.util.MessageMgr;
import model.util.SocketMgr;

import java.util.ArrayList;
import java.util.List;

import static constant.CommConstants.TARGET_ARDUINO;
import static constant.MapConstants.MAP_ROWS;
import static constant.RobotConstants.*;

/**
 * Fastest path algorithm using A* search + customized score functions
 */
public class FastestPathAlgorithmRunner implements AlgorithmRunner {

    private int sleepDuration;
    private int mWayPointX = -1;
    private int mWayPointY = -1;
    private int timePenalty = 10;
    // if not going through WayPoint, penalty is 10s

    private static final int START_X = 0;
    private static final int START_Y = 17;
    private static final int GOAL_X = 12;
    private static final int GOAL_Y = 0;

    public FastestPathAlgorithmRunner(int speed) {
        sleepDuration = 1000 / speed;
    }

    public FastestPathAlgorithmRunner(int speed, int x, int y) {
        sleepDuration = 1000 / speed;
        mWayPointX = x;
        mWayPointY = y;
    }

    @Override
    public void run(Grid grid, Robot robot, boolean realRun) {
        robot.reset();

        // wait for start up message
        if (realRun) {
            String msg = SocketMgr.getInstance().receiveMessage(false);
            while (!msg.equals("beginFastest")) {
            	System.out.println("waiting for begin Fastest");
                msg = SocketMgr.getInstance().receiveMessage(false);
            }
        }

        // receive waypoint
        int wayPointX = mWayPointX;
        int wayPointY = mWayPointY;
        //if (realRun && wayPointX == -1 && wayPointY == -1) {
        //    // receive from Android
        //    System.out.println("Waiting for waypoint");
        //    //SocketMgr.getInstance().clearInputBuffer();
        //    String msg = SocketMgr.getInstance().receiveMessage();
        //    List<Integer> waypoints;
        //    while ((waypoints = MessageMgr.parseMessage(msg)) == null) {
        //        msg = SocketMgr.getInstance().receiveMessage();
        //    }
        //    // the coordinates in fastest path search is different from real grid coordinate
        //    wayPointX = waypoints.get(0)-1;
        //    wayPointY = waypoints.get(1)-1;
        //} else if (!realRun) {
        if (!realRun) {
            // ignore waypoint for simulation
            wayPointX = START_X;
            wayPointY = START_Y;
        }

        // run from start to waypoint and from waypoint to goal
        System.out.println("Fastest path algorithm started with waypoint " + wayPointX + "," + wayPointY);
        Robot fakeRobot = new Robot(new Grid(), new ArrayList<>());
        List<String> path1 = AlgorithmRunner.runAstar(START_X, START_Y, wayPointX, wayPointY, grid, fakeRobot);
        List<String> path2 = AlgorithmRunner.runAstar(wayPointX, wayPointY, GOAL_X, GOAL_Y, grid, fakeRobot);
//        List<String> pathWithoutWayPoint = AlgorithmRunner.runAstar(START_X, START_Y, GOAL_X, GOAL_Y, grid, fakeRobot);

        int distanceWithWayPoint = AlgorithmRunner.estimateDistanceToGoal(START_X, START_Y, wayPointX, wayPointY) + AlgorithmRunner.estimateDistanceToGoal(wayPointX, wayPointY, GOAL_X, GOAL_Y);
//        int distanceWithoutWayPoint = AlgorithmRunner.estimateDistanceToGoal(START_X, START_Y, GOAL_X, GOAL_Y) + 15/6 * timePenalty;
        System.out.println("distanceWithWayPoint" + distanceWithWayPoint);
//        System.out.println("distanceWithoutWayPoint" + distanceWithoutWayPoint);

//        if (distanceWithWayPoint <= distanceWithoutWayPoint){
            System.out.println("Algorithm with WayPoint");
            if (path1 != null && path2 != null) {
                System.out.println("Algorithm finished, executing actions");
                path1.addAll(path2);
                System.out.println(path1.toString());
                if (realRun) {
                    //// INITIAL CALIBRATION
                    //if (realRun) {
                    //    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
                    //    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
                    //    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
                    //    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
                    //}
                    // SEND ENTIRE PATH AT ONCE
//                    String compressedPath = AlgorithmRunner.compressPath(path1);
                	StringBuilder builder = new StringBuilder();
                	System.out.println("fastest path:");
                	int j = path1.size()-1;
                	while(path1.get(j).compareTo("M")==0) {                		
                		j--;
                	}
                	if(path1.get(j).compareTo("L")==0) {
//                		path1.add(j,"R");
                		path1.add(j+1, "M");
                		path1.add(j-1, "M");
                	}
                	else if(path1.get(j).compareTo("R")==0 ) {
//                		path1.add(j,"L");
                		path1.add(j+1, "M");
                		path1.add(j-1, "M");
                	}
                    for(int i = 0;i< path1.size();i++) {
                    	builder.append(path1.get(i));
                    	System.out.print(path1.get(i)+' ');
                    }
                    System.out.println("");
                    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, builder.toString());
                    // SIMULATE AT THE SAME TIME
                    for (String action : path1) {
                        if (action.equals("M")) {
                            robot.move();
                        } else if (action.equals("L")) {
                            robot.turn(LEFT);
                        } else if (action.equals("R")) {
                            robot.turn(RIGHT);
                        } else if (action.equals("T")) {
                            robot.turn(LEFT);
                            robot.turn(LEFT);
                        }
//                        takeStep();
                    }
                } else {
                    for (String action : path1) {
                        if (action.equals("M")) {
                            robot.move();
                        } else if (action.equals("L")) {
                            robot.turn(LEFT);
                        } else if (action.equals("R")) {
                            robot.turn(RIGHT);
                        } else if (action.equals("T")) {
                            robot.turn(LEFT);
                            robot.turn(LEFT);
                        }
//                        takeStep();
                    }
                }
            } else {
                System.out.println("Fastest path not found!");
            }
//        }
//        else if (distanceWithWayPoint > distanceWithoutWayPoint) {
//            System.out.println("Algorithm without WayPoint");
//            if (pathWithoutWayPoint != null) {
//                System.out.println("Algorithm finished, executing actions");
//                System.out.println(pathWithoutWayPoint.toString());
//                if (realRun) {
//                    //// INITIAL CALIBRATION
//                    //if (realRun) {
//                    //    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
//                    //    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
//                    //    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "C");
//                    //    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "R");
//                    //}
//                    // SEND ENTIRE PATH AT ONCE
//                	
//                	StringBuilder builder = new StringBuilder();
//                	System.out.println("fastest path:");
//                    for(int i = 0;i< pathWithoutWayPoint.size();i++) {
//                    	builder.append(pathWithoutWayPoint.get(i));
//                    	System.out.print(pathWithoutWayPoint.get(i)+' ');
//                    }
//                    System.out.println("");
//                    SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, builder.toString());
//                  
//                	
//                	
//                    // SIMULATE AT THE SAME TIME
//                    for (String action : pathWithoutWayPoint) {
//                        if (action.equals("M")) {
//                            robot.move();
//                        } else if (action.equals("L")) {
//                            robot.turn(LEFT);
//                        } else if (action.equals("R")) {
//                            robot.turn(RIGHT);
//                        } else if (action.equals("T")) {
//                            robot.turn(LEFT);
//                            robot.turn(LEFT);
//                        }
//                        takeStep();
//                    }
//                } else {
//                    for (String action : pathWithoutWayPoint) {
//                        if (action.equals("M")) {
//                            robot.move();
//                        } else if (action.equals("L")) {
//                            robot.turn(LEFT);
//                        } else if (action.equals("R")) {
//                            robot.turn(RIGHT);
//                        } else if (action.equals("T")) {
//                            robot.turn(LEFT);
//                            robot.turn(LEFT);
//                        }
//                        takeStep();
//                    }
//                }
//            } else {
//                System.out.println("Fastest path not found!");
//            }
//        }
    }

    /**
     * Pause the simulation for sleepDuration
     */
    private void takeStep() {
        try {
            Thread.sleep(sleepDuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
