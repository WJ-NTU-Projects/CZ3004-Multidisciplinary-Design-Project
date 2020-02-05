package ntu.mdp.android.mdptestkotlin.simulation;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ntu.mdp.android.mdptestkotlin.arena.ArenaV2;
import ntu.mdp.android.mdptestkotlin.arena.RobotController;

import static java.lang.Math.abs;

public class AStarSearch {
    private final ArenaV2 robotController;

    public AStarSearch(ArenaV2 robotController) {
        this.robotController = robotController;
    }

    public List<int[]> fastestPathChallenge() {
        final int[] startPosition = robotController.getStartPosition();
        int startX = startPosition[0];
        int startY = startPosition[1];
        int startFacing1 = 0;
        int startFacing2 = 90;

        if (robotController.isStartPointExact(13, 1)) {
            startFacing2 = 270;
        } else if (robotController.isStartPointExact(1, 18)) {
            startFacing1 = 180;
        } else if (robotController.isStartPointExact(13, 18)) {
            startFacing1 = 180;
            startFacing2 = 270;
        }

        final Pair<Double, List<int[]>> pathToWaypoint1 = findFastestPath(new int[] {startX, startY, startFacing1}, robotController.getWaypointPosition());
        final Pair<Double, List<int[]>> pathToWaypoint2 = findFastestPath(new int[] {startX, startY, startFacing2}, robotController.getWaypointPosition());
        final List<int[]> path1 = pathToWaypoint1.second;
        final List<int[]> path2 = pathToWaypoint2.second;

        final int[] waypointPosition = robotController.getWaypointPosition();
        startX = waypointPosition[0];
        startY = waypointPosition[1];
        startFacing1 = (path1.isEmpty()) ? startFacing1 : path1.get(path1.size() - 1)[2];
        startFacing2 = (path2.isEmpty()) ? startFacing2 : path2.get(path2.size() - 1)[2];

        final List<int[]> goalPath1 = findFastestPath(new int[] {startX, startY, startFacing1}, robotController.getGoalPosition()).second;
        final List<int[]> goalPath2 = findFastestPath(new int[] {startX, startY, startFacing2}, robotController.getGoalPosition()).second;
        path1.addAll(goalPath1);
        path2.addAll(goalPath2);

        if (path1.isEmpty() && path2.isEmpty()) return path1;

        int turns1 = 0;

        if (!path1.isEmpty()) {
            int previousFacing = path1.get(0)[2];

            for (int[] path : path1) {
                if (path[2] != previousFacing) turns1++;
                previousFacing = path[2];
            }
        }

        int turns2 = 0;

        if (!path2.isEmpty()) {
            int previousFacing = path2.get(0)[2];

            for (int[] path : path2) {
                if (path[2] != previousFacing) turns2++;
                previousFacing = path[2];
            }
        }

        double cost1 = pathToWaypoint1.first;
        double cost2 = pathToWaypoint2.first;
        if (turns2 > turns1) cost2 += 100;
        else if (turns1 > turns2) cost1 += 100;

        final List<int[]> pathList = (turns2 <= turns1) ? path2 : path1;
        Log.e("TEST", turns1 + ", " + turns2 + ", " + cost1 + ", " + cost2);
        return pathList;
    }

    Pair<Double, List<int[]>> findFastestPath(int[] startArray, int[] endArray) {
        if (startArray.length < 3 || endArray.length < 2) return new Pair<>(-1.0, new ArrayList<>());
        return findFastestPath(startArray[0], startArray[1], startArray[2], endArray[0], endArray[1]);
    }

    private Pair<Double, List<int[]>> findFastestPath(int startX, int startY, int startFacing, int goalX, int goalY) {
        if (!robotController.isValidCoordinates(startX, startY, false)) return new Pair<>(-1.0, new ArrayList<>());
        if (!robotController.isValidCoordinates(goalX, goalY, false)) return new Pair<>(-1.0, new ArrayList<>());

        boolean found = false;
        final ArrayList<GridNode> openList = new ArrayList<>();
        final ArrayList<GridNode> closedList = new ArrayList<>();
        final ArrayList<GridNode> successors = new ArrayList<>();
        GridNode parentNode = new GridNode(startX, startY, startFacing, 0.0, 0.0, 0.0, -1, -1);
        openList.add(parentNode);

        while (!openList.isEmpty()) {
            successors.clear();
            double lowestF = Double.MAX_VALUE;


            for (GridNode node : openList) {
                if (node.f <= lowestF) {
                    lowestF = node.f;
                    parentNode = node;
                }
            }

            openList.remove(parentNode);
            closedList.add(parentNode);

            for (int offset = -1; offset <= 1; offset += 2) {
                for (int select = 0; select <= 1; select++) {
                    boolean continueToNext = false;
                    int facing = (offset == -1) ? (180 + (select * 90)) : (select * 90);
                    if (abs(facing - parentNode.facing) == 180) facing = parentNode.facing;
                    final int x = (select == 0) ? parentNode.x : parentNode.x + offset;
                    final int y = (select == 0) ? parentNode.y + offset : parentNode.y;
                    if (!robotController.isRobotMovable(x, y)) continue;

                    for (GridNode closedNode : closedList) {
                        if (closedNode.x == x && closedNode.y == y) {
                            continueToNext = true;
                            break;
                        }
                    }

                    if (continueToNext) continue;
                    successors.add(new GridNode(x, y, facing, 0.0, 0.0, 0.0, parentNode.x, parentNode.y));
                }
            }

            for (GridNode successor : successors) {
                boolean continueToNext = false;

                if (successor.x == goalX && successor.y == goalY) {
                    parentNode = successor;
                    found = true;
                    openList.clear();
                    break;
                }

                final double penalty = (parentNode.facing == successor.facing) ? 1.0 : 10.0;
                successor.g = (abs(successor.x - successor.parentX) + abs(successor.y - successor.parentY) * penalty) + parentNode.g + (penalty - 1);
                successor.h = 1.0 * (abs(successor.x - goalX) + abs(successor.y - goalY));
                successor.f = successor.g + successor.h;

                for (GridNode openNode : openList) {
                    if (openNode.x == successor.x && openNode.y == successor.y && successor.f < openNode.f) {
                        openNode.f = successor.f;
                        openNode.g = successor.g;
                        openNode.h = successor.h;
                        openNode.parentX = successor.parentX;
                        openNode.parentY = successor.parentY;
                        continueToNext = true;
                        break;
                    }
                }

                if (continueToNext) continue;
                openList.add(successor);
            }
        }

        final ArrayList<int[]> pathList = new ArrayList<>();
        double totalCost = 0.0;

        if (found) {
            while (parentNode.parentX != -1 && parentNode.parentY != -1) {
                pathList.add(new int[] {parentNode.x, parentNode.y, parentNode.facing});
                totalCost += parentNode.f;

                for (GridNode closedNode : closedList) {
                    if (closedNode.x == parentNode.parentX && closedNode.y == parentNode.parentY) {
                        parentNode = closedNode;
                        break;
                    }
                }
            }
        }

        Collections.reverse(pathList);
        return new Pair<>(totalCost, pathList);
    }


    static class GridNode {
        final int x;
        final int y;
        final int facing;
        double f;
        double g;
        double h;
        int parentX;
        int parentY;

        GridNode(int x, int y, int facing, double f, double g, double h, int parentX, int parentY) {
            this.x = x;
            this.y = y;
            this.facing = facing;
            this.f = f;
            this.g = g;
            this.h = h;
            this.parentX = parentX;
            this.parentY = parentY;
        }
    }
}
