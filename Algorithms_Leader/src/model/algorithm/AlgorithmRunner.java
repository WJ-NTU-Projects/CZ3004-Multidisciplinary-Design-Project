package model.algorithm;

import model.entity.Cell;
import model.entity.Grid;
import model.entity.Robot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static constant.MapConstants.MAP_COLS;
import static constant.MapConstants.MAP_ROWS;
import static constant.RobotConstants.*;
import static constant.RobotConstants.LEFT;
import static constant.RobotConstants.RIGHT;

/**
 * Interface for algorithms
 */
public interface AlgorithmRunner {
    int INFINITY = 1000000;
    void run(Grid grid, Robot robot, boolean realRun);

    /**
     * Run the A* algorithm from point (startX, startY) to (endX, endY)
     * @param startX Start point x coordinate
     * @param startY Start point y coordinate
     * @param endX End point x coordinate
     * @param endY End point y coordinate
     * @param grid Map
     * @param robot Robot
     * @return A list of actions for the robot to take to reach the goal
     */
    static List<String> runAstar(int startX, int startY, int endX, int endY, Grid grid, Robot robot) {
        // initialization
    	System.out.println("startX: "+Integer.toString(startX)+",startY: "+Integer.toString(startY)+",endX: "+Integer.toString(endX)+",endY: "+Integer.toString(endY));
        boolean[][] closedSet;
        List<Cell> openSet;
        HashMap<Cell, Cell> cameFrom;
        int[][] gScore;
        int[][] fScore;
        Cell[][] cells;
        closedSet = new boolean[MAP_COLS - 2][MAP_ROWS - 2];
        openSet = new ArrayList<>();
        cameFrom = new HashMap<>();
        gScore = new int[MAP_COLS - 2][MAP_ROWS - 2];
        fScore = new int[MAP_COLS - 2][MAP_ROWS - 2];
        cells = new Cell[MAP_COLS - 2][MAP_ROWS - 2];

        for (int x = 0; x < MAP_COLS - 2; x++)
            for (int y = 0; y < MAP_ROWS - 2; y++) {
                gScore[x][y] = INFINITY;
                fScore[x][y] = INFINITY;
                closedSet[x][y] = false;
                cells[x][y] = new Cell(x, y);
            }
        gScore[startX][startY] = 0;
        fScore[startX][startY] = estimateDistanceToGoal(startX, startY, endX, endY);
        cells[startX][startY].setDistance(fScore[startX][startY]);
        openSet.add(cells[startX][startY]);

        // run algorithm
        while (!openSet.isEmpty()) {
            Cell current = getCurrent(openSet, fScore);
            if (current.getX() == endX && current.getY() == endY) {
                return reconstructPath(robot, current, cameFrom);
            }

            openSet.remove(current);
            closedSet[current.getX()][current.getY()] = true;

            for (Cell neighbor : generateNeighbor(grid, current, cells)) {
                if (closedSet[neighbor.getX()][neighbor.getY()])
                    continue;

                if (!openSet.contains(neighbor))
                    openSet.add(neighbor);

                int tentativeGScore = gScore[current.getX()][current.getY()] + 1;
                Cell previousCell = cameFrom.get(current);
                if (previousCell != null && previousCell.getX() != neighbor.getX() && previousCell.getY() != neighbor.getY())
                    tentativeGScore += 1; // penalize turns
                if (tentativeGScore >= gScore[neighbor.getX()][neighbor.getY()])
                    continue;

                cameFrom.put(neighbor, current);
                gScore[neighbor.getX()][neighbor.getY()] = tentativeGScore;
                fScore[neighbor.getX()][neighbor.getY()] = tentativeGScore + estimateDistanceToGoal(neighbor.getX(), neighbor.getY(), endX, endY);
            }
        }
        return null;
    }

    /**
     * Select a cell from the openset with lowest f score.
     * @param openSet
     * @param fScore
     * @return
     */
    static Cell getCurrent(List<Cell> openSet, int[][] fScore) {
        Cell minCell = null;
        int minF = INFINITY;
        for (Cell cell : openSet) {
            if (fScore[cell.getX()][cell.getY()] < minF) {
                minF = fScore[cell.getX()][cell.getY()];
                minCell = cell;
            }
        }

        return minCell;
    }

    /**
     * Reconstructs the path of an Astar run after reaching the goal
     * @param robot
     * @param current
     * @param cameFrom
     * @return
     */
    static List<String> reconstructPath(Robot robot, Cell current, HashMap<Cell, Cell> cameFrom) {
        // construct the path first
        List<Cell> path = new LinkedList<>();
        path.add(current);
        while (cameFrom.get(current) != null) {
            current = cameFrom.get(current);
            path.add(0, current);
        }
        path.remove(0); // remove the starting point

        // convert path to robot movement
        List<String> actions = new ArrayList<>();
        int calibrationCounter = 0;
        for (Cell cell : path) {

            //calibrationCounter++;
            //if (robot.canCalibrateFront()) {
            //    actions.add("C");
            //    calibrationCounter = 0;
            //} else if (calibrationCounter >= 5 && robot.canCalibrateLeft()) {
            //    actions.add("L");
            //    actions.add("C");
            //    actions.add("R");
            //    calibrationCounter = 0;
            //}

            // see if we need to turn
            int nextHeading = 0;
            if (robot.getCenterPosX() < cell.getX() + 1)
                nextHeading = EAST;
            else if (robot.getCenterPosX() > cell.getX() + 1)
                nextHeading = WEST;
            else if (robot.getCenterPosY() < cell.getY() + 1)
                nextHeading = SOUTH;
            else if (robot.getCenterPosY() > cell.getY() + 1)
                nextHeading = NORTH;

            if (nextHeading != robot.getHeading()) {
                if ((robot.getHeading() + 1) % 4 == nextHeading) {
                    actions.add("R");
                    robot.turn(RIGHT);
                } else if ((robot.getHeading() + 3) % 4 == nextHeading){
                    actions.add("L");
                    robot.turn(LEFT);
                } else {
                    actions.add("T");
                    robot.turn(LEFT);
                    robot.turn(LEFT);
                }
            }
            actions.add("M");
            robot.move();
        }

        return actions;
    }

    /**
     * Generate a list of neighbors available for moving (i.e. it
     * cannot be out of arena, or an obstacle, or unexplored)
     * @param grid
     * @param current
     * @param cells
     * @return
     */
    static List<Cell> generateNeighbor(Grid grid, Cell current, Cell[][] cells) {
        boolean left = true, right = true, front = true, back = true;
        List<Cell> neighbors = new ArrayList<>();

        int trueX = current.getX() + 1, trueY = current.getY() + 1;
        // check north
        for (int i = -1; i <= 1; ++i) {
            if (grid.isOutOfArena(trueX + i, trueY - 2) ||
                    grid.getIsObstacle(trueX + i, trueY - 2) ||
                    !grid.getIsExplored(trueX + i, trueY - 2))
                front = false;
        }
        if (front)
            neighbors.add(cells[current.getX()][current.getY() - 1]);

        // check south
        for (int i = -1; i <= 1; ++i) {
            if (grid.isOutOfArena(trueX + i, trueY + 2) ||
                    grid.getIsObstacle(trueX + i, trueY + 2) ||
                    !grid.getIsExplored(trueX + i, trueY + 2))
                back = false;
        }
        if (back)
            neighbors.add(cells[current.getX()][current.getY() + 1]);

        // check west
        for (int i = -1; i <= 1; ++i) {
            if (grid.isOutOfArena(trueX - 2, trueY + i) ||
                    grid.getIsObstacle(trueX - 2, trueY + i) ||
                    !grid.getIsExplored(trueX - 2, trueY + i))
                left = false;
        }
        if (left)
            neighbors.add(cells[current.getX() - 1][current.getY()]);

        // check east
        for (int i = -1; i <= 1; ++i) {
            if (grid.isOutOfArena(trueX + 2, trueY + i) ||
                    grid.getIsObstacle(trueX + 2, trueY + i) ||
                    !grid.getIsExplored(trueX + 2, trueY + i))
                right = false;
        }
        if (right)
            neighbors.add(cells[current.getX() + 1][current.getY()]);

        return neighbors;
    }

    /**
     * Calculates the estimated distance from (curX, curY) to (goalX, goalY).
     * The estimation is based on the Manhattan distance. If a turn is unavoidable,
     * a penalty of 1 is added to the distance.
     */
     static int estimateDistanceToGoal(int curX, int curY, int goalX, int goalY) {
        int distance = Math.abs(goalX - curX) + Math.abs(goalY - curY);
        if (curX != goalX && curY != goalY) // we must turn at least once
            distance += 1;

        return distance;
    }

    /**
     * Convert the list of actions into a single string for sending
     * to Arduino. Specifically, consecutive moves are compressed to the
     * format "M5" to represent moving 5 cells at once.
     * @param actions Actions to perform
     * @return A string representing the actions
     */
    static String compressPath(List<String> actions) {
        int moveCounter = 0;
        StringBuilder builder = new StringBuilder();

        for (String action : actions) {
            if (action.equals("L") || action.equals("R") || action.equals("U") || action.equals("C")) {
                if (moveCounter != 0) {
                    builder.append("M");
                    builder.append(moveCounter);
                    moveCounter = 0;
                }
                builder.append(action);
            } else if (action.equals("M")) {
                moveCounter++;
            }
        }
        if (moveCounter != 0) {
            builder.append("M");
            builder.append(moveCounter);
        }

        return builder.toString();
    }

    static String compressPathForExploration(List<String> actions, Robot fakeRobot) {
        List<String> actionWithCalibration = new ArrayList<>();
        for (String action : actions) {
            actionWithCalibration.add(action); // copy action to new list
            // execute action on fake robot
            if (action.equals("L")) {
                fakeRobot.turn(LEFT);
            } else if (action.equals("R")) {
                fakeRobot.turn(RIGHT);
            } else if (action.equals("T")) {
                fakeRobot.turn(LEFT);
                fakeRobot.turn(LEFT);
            } else if (action.equals("M")) {
                fakeRobot.move();
            }
            // check calibration
            if (fakeRobot.canCalibrateFront()) {
                actionWithCalibration.add("C");
            }
        }

        return compressPath(actionWithCalibration);
    }
}
