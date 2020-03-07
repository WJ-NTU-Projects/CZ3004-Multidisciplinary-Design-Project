package model.algorithm;
import model.entity.Grid;
import model.entity.Robot;
import model.entity.Cell;
import model.util.SocketMgr;

import javax.swing.*;
import java.util.*;

import static constant.RobotConstants.LEFT;
import static constant.RobotConstants.RIGHT;

/**
 * Algorithm for exploration (time-limited)
 */

public class TimeExplorationAlgorithmRunner implements AlgorithmRunner{

    private int sleepDuration;
    private static final int START_X = 0;
    private static final int START_Y = 17;
    public TimeExplorationAlgorithmRunner(int speed){
        sleepDuration = 1000 / speed;
    }

    @Override
    public void run(Grid grid, Robot robot, boolean realRun) {
        grid.reset();
        robot.reset();
        if (realRun) {
            String msg = SocketMgr.getInstance().receiveMessage(false);
            while (!msg.equals("exs")) {
                msg = SocketMgr.getInstance().receiveMessage(false);
            }
        }
        int minutes = -1;
        int seconds = -1;
        do{
            try{
                String input = JOptionPane.showInputDialog(null, "Please enter the time limit in MINUTES:", "Enter Time Limit (Minutes)", JOptionPane.INFORMATION_MESSAGE);
                if(input.equals(JOptionPane.CANCEL_OPTION)){
                    break;
                }else{
                    minutes = Integer.parseInt(input);
                }
            }catch(NumberFormatException e){
                JOptionPane.showMessageDialog(null, "Please enter an integer!", "Error!", JOptionPane.ERROR_MESSAGE);
            }
        }while(minutes < 0);

        do{
            try{
                String input = JOptionPane.showInputDialog(null, "Please enter the time limit in SECONDS:", "Enter Time Limit (Seconds)", JOptionPane.INFORMATION_MESSAGE);
                if(input.equals(JOptionPane.CANCEL_OPTION)){
                    break;
                }else{
                    seconds = Integer.parseInt(input);
                }
            }catch(NumberFormatException e){
                JOptionPane.showMessageDialog(null, "Please enter an integer!", "Error!", JOptionPane.ERROR_MESSAGE);
            }
        }while(seconds < 0);

        int totalTime = (minutes*60) + seconds;
        System.out.println("Total time: " + totalTime + " seconds");
        timeLimitedAlgorithm(grid, robot, totalTime, realRun);
        //grid.generateDescriptor();
    }

    private void timeLimitedAlgorithm(Grid grid, Robot robot, int totalTime, boolean realRun){
        LinkedList<Cell> pathTaken = new LinkedList<>();
        System.out.println("Time-Limit = "+totalTime+" Seconds.");
        int millisecondsTotal = totalTime * 1000;
        boolean endZoneFlag = false;
        boolean startZoneFlag = false;
        while (millisecondsTotal > 0 && (!endZoneFlag || !startZoneFlag)) {
            Cell position = new Cell(robot.getPosX(), robot.getPosY());
            pathTaken.push(position);
            robot.sense(realRun,"I");
            if (robot.isObstacleAhead()) {
                if (robot.isObstacleRight() && robot.isObstacleLeft()) {
                    System.out.println("OBSTACLE DETECTED! (ALL 3 SIDES) U-TURNING");
                    robot.turn(RIGHT);
                    stepTaken();
                    robot.turn(RIGHT);
                    stepTaken();
                    millisecondsTotal = millisecondsTotal - (sleepDuration * 2);
                } else if (robot.isObstacleLeft()) {
                    System.out.println("OBSTACLE DETECTED! (FRONT + LEFT) TURNING RIGHT");
                    robot.turn(RIGHT);
                    stepTaken();
                    millisecondsTotal = millisecondsTotal - sleepDuration;
                } else {
                    System.out.println("OBSTACLE DETECTED! (FRONT) TURNING LEFT");
                    robot.turn(LEFT);
                    stepTaken();
                    millisecondsTotal = millisecondsTotal - sleepDuration;
                }
                robot.sense(realRun,"I");
                System.out.println("-----------------------------------------------");
            } else if (!robot.isObstacleLeft()) {
                System.out.println("NO OBSTACLES ON THE LEFT! TURNING LEFT");
                robot.turn(LEFT);
                stepTaken();
                millisecondsTotal = millisecondsTotal - sleepDuration;
                robot.sense(realRun,"I");
                System.out.println("-----------------------------------------------");
            }
            robot.move();
            stepTaken();
            millisecondsTotal = millisecondsTotal - sleepDuration;
            System.out.println(millisecondsTotal);
            if(Grid.isInEndZone(robot.getPosX(), robot.getPosY())){
                endZoneFlag = true;
            }
            if(endZoneFlag && Grid.isInStartZone(robot.getPosX()+2, robot.getPosY())){
                startZoneFlag = true;
            }
        }

        Robot fakeRobot = new Robot(grid, new ArrayList<>());
        fakeRobot.setPosX(robot.getPosX());
        fakeRobot.setPosY(robot.getPosY());
        fakeRobot.setHeading(robot.getHeading());
        List<String> returnPath = AlgorithmRunner.runAstar(robot.getPosX(), robot.getPosY(), START_X, START_Y, grid, fakeRobot);

        if (returnPath != null) {
            System.out.println("Algorithm finished, executing actions");
            System.out.println(returnPath.toString());

            for (String action : returnPath) {
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
                stepTaken();
            }
        }else {
            System.out.println("Fastest path not found!");
        }
    }

    private void stepTaken(){
        /*
            MAKE IT MOVE SLOWLY SO CAN SEE STEP BY STEP MOVEMENT
             */
        try {
            Thread.sleep(sleepDuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
