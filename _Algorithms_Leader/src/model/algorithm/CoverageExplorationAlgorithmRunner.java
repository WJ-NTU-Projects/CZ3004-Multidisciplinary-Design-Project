package model.algorithm;

import model.entity.Grid;
import model.entity.Robot;
import model.entity.Cell;
import model.util.SocketMgr;

import javax.swing.*;
import java.util.*;

import static constant.RobotConstants.LEFT;
import static constant.RobotConstants.RIGHT;

public class CoverageExplorationAlgorithmRunner implements AlgorithmRunner{

    private int sleepDuration;
    private static final int START_X = 0;
    private static final int START_Y = 17;

    public CoverageExplorationAlgorithmRunner(int speed){
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
        int coveragePercentage = 0;
        do{
            try{
                String input = JOptionPane.showInputDialog(null, "Please enter the exploration percentage:", "Enter Percentage", JOptionPane.INFORMATION_MESSAGE);
                if(input.equals(JOptionPane.CANCEL_OPTION)){
                    break;
                }else{
                    coveragePercentage = Integer.parseInt(input);
                }
            }catch(NumberFormatException e){
                JOptionPane.showMessageDialog(null, "Please enter an integer more than 0!", "Error!", JOptionPane.ERROR_MESSAGE);
            }
        }while(coveragePercentage == 0);
        coverageLimitedAlgorithm(grid, robot, coveragePercentage, realRun);
        //grid.generateDescriptor();
    }

    private void coverageLimitedAlgorithm(Grid grid, Robot robot, int coveragePercentage, boolean realRun) {

        while (grid.checkExploredPercentage() < coveragePercentage) {
            Cell position = new Cell(robot.getPosX(), robot.getPosY());
            robot.sense(realRun,"I");
            if (robot.isObstacleAhead()) {
                if (robot.isObstacleRight() && robot.isObstacleLeft()) {
                    System.out.println("OBSTACLE DETECTED! (ALL 3 SIDES) U-TURNING");
                    robot.turn(RIGHT);
                    stepTaken();
                    robot.turn(RIGHT);
                    stepTaken();
                } else if (robot.isObstacleLeft()) {
                    System.out.println("OBSTACLE DETECTED! (FRONT + LEFT) TURNING RIGHT");
                    robot.turn(RIGHT);
                    stepTaken();
                } else {
                    System.out.println("OBSTACLE DETECTED! (FRONT) TURNING LEFT");
                    robot.turn(LEFT);
                    stepTaken();
                }
                robot.sense(realRun,"I");
                System.out.println("-----------------------------------------------");
            } else if (!robot.isObstacleLeft()) {
                System.out.println("NO OBSTACLES ON THE LEFT! TURNING LEFT");
                robot.turn(LEFT);
                stepTaken();
                robot.sense(realRun,"I");
                System.out.println("-----------------------------------------------");
            }
            robot.move();
            stepTaken();
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
