package wjayteo.mdp.android.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import wjayteo.mdp.android.arena.ArenaMap;

import static java.lang.Math.abs;

public class AStarSearch {
    private final ArenaMap robotController;

    public AStarSearch(ArenaMap robotController) {
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

        int waypointX = robotController.getWaypointPosition()[0];
        int waypointY = robotController.getWaypointPosition()[1];
        int goalX = robotController.getGoalPosition()[0];
        int goalY = robotController.getGoalPosition()[1];
        ArrayList<int[]> waypointEntranceList = new ArrayList<>();
        ArrayList<int[]> goalEntranceList = new ArrayList<>();

        // FIND VIABLE ENTRANCES TO WAYPOINT
        for (int offsetY = -1; offsetY <= 1; offsetY++) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                if ((offsetY == offsetX && offsetY == 0) || (offsetX != 0 && offsetY != 0)) continue;

                int x = waypointX + offsetX;
                int y = waypointY + offsetY;
                if (robotController.isRobotMovable(x, y)) waypointEntranceList.add(new int[] {x, y});

                x = goalX + offsetX;
                y = goalY + offsetY;
                if (robotController.isRobotMovable(x, y)) goalEntranceList.add(new int[] {x, y});
            }
        }

        List<GridNode> finalPath = new ArrayList<>();
        double previousCost = Double.MAX_VALUE;

        for (int[] waypointEntrance : waypointEntranceList) {
            final List<GridNode> path1 = findFastestPath(startX, startY, startFacing1, waypointEntrance[0], waypointEntrance[1]);
            GridNode path1End = path1.get(path1.size() - 1);
            final List<GridNode> path1ToWp = findFastestPath(path1End.x, path1End.y, path1End.facing, waypointX, waypointY);
            path1.addAll(path1ToWp);
            path1End = path1.get(path1.size() - 1);

            List<GridNode> finalGoalPath = new ArrayList<>();
            double previousGoalCost = Double.MAX_VALUE;

            for (int[] goalEntrance : goalEntranceList) {
                final List<GridNode> goalPath = findFastestPath(path1End.x, path1End.y, path1End.facing, goalEntrance[0], goalEntrance[1]);
                final GridNode goalPathEnd = goalPath.get(goalPath.size() - 1);
                final List<GridNode> pathToGoal = findFastestPath(goalPathEnd.x, goalPathEnd.y, goalPathEnd.facing, goalX, goalY);
                goalPath.addAll(pathToGoal);

                double pathCost = 0.0;
                int previousFacing = goalPath.get(0).facing;

                for (GridNode node : goalPath) {
                    pathCost += node.f;
                    if (node.facing != previousFacing) pathCost += 500;
                    previousFacing = node.facing;
                }

                if (pathCost <= previousGoalCost) {
                    finalGoalPath = goalPath;
                    previousGoalCost = pathCost;
                }
            }

            path1.addAll(finalGoalPath);

            double cost1 = 0.0;
            int previousFacing = path1.get(0).facing;

            for (GridNode node : path1) {
                cost1 += node.f;
                if (node.facing != previousFacing) cost1 += 500;
                previousFacing = node.facing;
            }

            final List<GridNode> path2 = findFastestPath(startX, startY, startFacing2, waypointEntrance[0], waypointEntrance[1]);
            GridNode path2End = path2.get(path2.size() - 1);
            final List<GridNode> path2ToWp = findFastestPath(path2End.x, path2End.y, path2End.facing, waypointX, waypointY);
            path2.addAll(path2ToWp);
            path2End = path2.get(path2.size() - 1);

            finalGoalPath = new ArrayList<>();
            previousGoalCost = Double.MAX_VALUE;

            for (int[] goalEntrance : goalEntranceList) {
                final List<GridNode> goalPath = findFastestPath(path2End.x, path2End.y, path2End.facing, goalEntrance[0], goalEntrance[1]);
                final GridNode goalPathEnd = goalPath.get(goalPath.size() - 1);
                final List<GridNode> pathToGoal = findFastestPath(goalPathEnd.x, goalPathEnd.y, goalPathEnd.facing, goalX, goalY);
                goalPath.addAll(pathToGoal);

                double pathCost = 0.0;
                previousFacing = goalPath.get(0).facing;

                for (GridNode node : goalPath) {
                    pathCost += node.f;
                    if (node.facing != previousFacing) pathCost += 500;
                    previousFacing = node.facing;
                }

                if (pathCost <= previousGoalCost) {
                    finalGoalPath = goalPath;
                    previousGoalCost = pathCost;
                }
            }

            path2.addAll(finalGoalPath);

            double cost2 = 0.0;
            previousFacing = path2.get(0).facing;

            for (GridNode node : path2) {
                cost2 += node.f;
                if (node.facing != previousFacing) cost2 += 500;
                previousFacing = node.facing;
            }

            List<GridNode> path;
            double cost;

            if (cost2 <= cost1) {
                path = path2;
                cost = cost2;
            } else {
                path = path1;
                cost = cost1;
            }

            if (cost <= previousCost) {
                finalPath = path;
                previousCost = cost;
            }
        }

        ArrayList<int[]> path = new ArrayList<>();

        for (GridNode node : finalPath) {
            path.add(new int[] {node.x, node.y, node.facing});
        }

        return path;
    }

    List<GridNode> findFastestPath(int[] startArray, int[] endArray) {
        if (startArray.length < 3 || endArray.length < 2) return new ArrayList<>();
        return findFastestPath(startArray[0], startArray[1], startArray[2], endArray[0], endArray[1]);
    }

    private List<GridNode> findFastestPath(int startX, int startY, int startFacing, int goalX, int goalY) {
        if (!robotController.isValidCoordinates(startX, startY, false)) return new ArrayList<>();
        if (!robotController.isValidCoordinates(goalX, goalY, false)) return new ArrayList<>();

        boolean found = false;
        final ArrayList<GridNode> openList = new ArrayList<>();
        final ArrayList<GridNode> closedList = new ArrayList<>();
        final ArrayList<GridNode> successors = new ArrayList<>();
        GridNode parentNode = new GridNode(startX, startY, startFacing, startFacing, 0.0, 0.0, 0.0, -1, -1);
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
                    int direction = facing;
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
                    successors.add(new GridNode(x, y, facing, direction, 0.0, 0.0, 0.0, parentNode.x, parentNode.y));
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

                double penalty = (parentNode.facing == successor.facing) ? 1.0 : 1.5;
                //if (abs(successor.direction - parentNode.direction) == 180) penalty += 4.0;
                successor.g = (abs(successor.x - successor.parentX) + abs(successor.y - successor.parentY) * penalty) + parentNode.g + (penalty - 1);
                successor.h = 1.0 * (abs(successor.x - goalX) + abs(successor.y - goalY));
                successor.f = successor.g + successor.h;

                for (GridNode openNode : openList) {
                    if (openNode.x == successor.x && openNode.y == successor.y && successor.f < openNode.f) {
                        openNode.facing = successor.facing;
                        openNode.direction = successor.direction;
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

        final ArrayList<GridNode> nodeList = new ArrayList<>();

        if (found) {
            while (parentNode.parentX != -1 && parentNode.parentY != -1) {
                nodeList.add(parentNode);

                for (GridNode closedNode : closedList) {
                    if (closedNode.x == parentNode.parentX && closedNode.y == parentNode.parentY) {
                        parentNode = closedNode;
                        break;
                    }
                }
            }
        }

        Collections.reverse(nodeList);
        return nodeList;
    }


    static class GridNode {
        final int x;
        final int y;
        int facing;
        int direction;
        double f;
        double g;
        double h;
        int parentX;
        int parentY;

        GridNode(int x, int y, int facing, int direction, double f, double g, double h, int parentX, int parentY) {
            this.x = x;
            this.y = y;
            this.facing = facing;
            this.direction = direction;
            this.f = f;
            this.g = g;
            this.h = h;
            this.parentX = parentX;
            this.parentY = parentY;
        }
    }
}
