package ntu.mdp.android.mdptestkotlin.simulation;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ntu.mdp.android.mdptestkotlin.arena.RobotController;

import static java.lang.Math.abs;

class AStarSearch {
    private final RobotController robotController;

    AStarSearch(RobotController robotController) {
        this.robotController = robotController;
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
                    int x = (select == 0) ? parentNode.x : parentNode.x + offset;
                    int y = (select == 0) ? parentNode.y + offset : parentNode.y;
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

                double penalty = (parentNode.facing == successor.facing) ? 1.0 : 10.0;
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

        ArrayList<int[]> pathList = new ArrayList<>();
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
        int x;
        int y;
        int facing;
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
