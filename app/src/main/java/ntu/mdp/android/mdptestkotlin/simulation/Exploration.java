package ntu.mdp.android.mdptestkotlin.simulation;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import ntu.mdp.android.mdptestkotlin.App;
import ntu.mdp.android.mdptestkotlin.arena.RobotController;

import static java.lang.Math.abs;

public class Exploration {
    private final RobotController robotController;
    private final AStarSearch aStarSearch;
    private final Function1<? super Callback, Unit> callback;
    private final Function2<? super RobotController.Broadcast, ? super boolean[], Unit> broadcastCallback;
    private boolean wallHug = false;
    private boolean do180 = false;
    private boolean started = false;
    private boolean goingHome = false;
    private List<AStarSearch.GridNode> pathList = new ArrayList<>();

    public Exploration(RobotController robotController, Function1<? super Callback, Unit> callback) {
        this.robotController = robotController;
        aStarSearch = new AStarSearch(robotController);
        this.callback = callback;

        broadcastCallback = (Function2<RobotController.Broadcast, boolean[], Unit>) (broadcast, sensorData) -> {
            if (goingHome) goHome();
            else processBroadcast(broadcast, sensorData);
            return null;
        };

        robotController.registerForBroadcast(broadcastCallback);
    }

    public void start() {
        wallHug = true;
        goingHome = false;
        do180 = false;
        robotController.getInitialSurrounding();
    }

    public void end() {
        started = false;
        robotController.deregisterForBroadcast(broadcastCallback);
    }

    private void endExploration() {
        pathList.clear();
        goHome();
    }

    private void processBroadcast(RobotController.Broadcast broadcastType, boolean[] sensorData) {
        if (goingHome) return;

        if (!started) {
            if (broadcastType == RobotController.Broadcast.TURN_COMPLETE) {
                robotController.getInitialSurrounding();
                return;
            }

            if (broadcastType == RobotController.Broadcast.MOVE_COMPLETE) {
                started = true;
                callback.invoke(Callback.WALL_HUGGING);
                callback.invoke(Callback.START_CLOCK);
                CHAAAAARGE();
                return;
            }
        }

        if (robotController.coverageReached()) {
            endExploration();
            return;
        }

        boolean frontObstructed = sensorData[0];
        boolean rightObstructed = sensorData[1];
        boolean leftObstructed = sensorData[2];

        if (wallHug) {
            final int[] robotPosition = robotController.getRobotPosition();
            final int x = robotPosition[0];
            final int y = robotPosition[1];

            if (robotController.isStartPointExact(x, y)) {
                wallHug = false;
                callback.invoke(Callback.SEARCHING);
                processBroadcast(broadcastType, sensorData);
                return;
            }

            switch (broadcastType) {
                case MOVE_COMPLETE:
                    if (!rightObstructed) {
                        robotController.turnRobot(RobotController.Direction.RIGHT);
                        return;
                    }

                    CHAAAAARGE();
                    break;

                case TURN_COMPLETE:
                    if (do180) {
                        do180 = false;
                        robotController.turnRobot(RobotController.Direction.RIGHT);
                        return;
                    }

                    CHAAAAARGE();
                    break;

                case OBSTRUCTED:
                    if (frontObstructed && rightObstructed && leftObstructed) {
                        do180 = true;
                        robotController.turnRobot(RobotController.Direction.RIGHT);
                        return;
                    }

                    if (!rightObstructed) {
                        robotController.turnRobot(RobotController.Direction.RIGHT);
                        return;
                    }

                    if (!leftObstructed) {
                        robotController.turnRobot(RobotController.Direction.LEFT);
                        return;
                    }

                    break;
            }

            return;
        }

        if (!robotController.hasUnexploredGrid()) {
            endExploration();
            return;
        }

        if (!robotController.isGridExplored(RobotController.Direction.FORWARD) && !frontObstructed) {
            CHAAAAARGE();
            return;
        }

        if (pathList.isEmpty() || broadcastType == RobotController.Broadcast.OBSTRUCTED) {
            final int[] nearestCoordinates = findNearestUnexploredGrid();

            if (!robotController.isValidCoordinates(nearestCoordinates, true)) {
                endExploration();
                return;
            }

            final int[] robotPosition = robotController.getRobotPosition();
            final List<AStarSearch.GridNode> fastestPathToNearest = aStarSearch.findFastestPath(robotPosition, nearestCoordinates);
            pathList.clear();
            pathList = fastestPathToNearest;

            if (pathList.isEmpty()) {
                endExploration();
                return;
            }
        }

        AStarSearch.GridNode node = pathList.get(0);
        int[] coordinates = new int[] {node.x, node.y};
        pathList.remove(0);
        robotController.moveRobot(coordinates);
    }

    private void goHome() {
        if (!goingHome) goingHome = true;
        final int[] robotPosition = robotController.getRobotPosition();
        final int x = robotPosition[0];
        final int y = robotPosition[1];

        if (robotController.isStartPointExact(x, y)) {
            callback.invoke(Callback.COMPLETE);
            end();
            return;
        }

        if (pathList.isEmpty()) {
            callback.invoke(Callback.GOING_HOME);
            pathList = aStarSearch.findFastestPath(robotPosition, robotController.getStartPosition());

            if (pathList.isEmpty()) {
                end();
                return;
            }
        }

        AStarSearch.GridNode node = pathList.get(0);
        int[] coordinates = new int[] {node.x, node.y};
        pathList.remove(0);
        robotController.moveRobot(coordinates);
    }

    private void CHAAAAARGE() {
        pathList.clear();
        robotController.moveRobot(RobotController.Direction.FORWARD);
    }

    private int[] findNearestUnexploredGrid() {
        final int[] coordinatesMovable = new int[] {-1, -1};
        final int[] coordinatesUnmovable = new int[] {-1, -1};
        int shortestDistanceMovable = Integer.MAX_VALUE;
        int shortestDistanceUnmovable = Integer.MAX_VALUE;

        for (int y = 19; y >= 0; y--) {
            for (int x = 0; x <= 14; x++) {
                if (robotController.isGridExplored(x, y)) continue;
                final int[] robotPosition = robotController.getRobotPosition();
                final int distance = abs(x - robotPosition[0]) + abs(y - robotPosition[1]);
                if (distance >= shortestDistanceMovable) continue;

                if (robotController.isRobotMovable(x, y)) {
                    shortestDistanceMovable = distance;
                    coordinatesMovable[0] = x;
                    coordinatesMovable[1] = y;
                    continue;
                }

                if (distance >= shortestDistanceUnmovable) continue;
                shortestDistanceUnmovable = distance;
                coordinatesUnmovable[0] = x;
                coordinatesUnmovable[1] = y;
            }
        }

        if (robotController.isRobotMovable(coordinatesMovable)) return coordinatesMovable;

        final int x = coordinatesUnmovable[0];
        final int y = coordinatesUnmovable[1];
        final int[] coordinates = new int[] {-1, -1};
        int shortestDistance;
        boolean found = false;
        boolean repeat = false;

        for (int i = 0; i < 2; i++) {
            shortestDistance = Integer.MAX_VALUE;
            int bound = (repeat) ? 2 : 1;

            for (int offsetY = (bound * -1); offsetY <= bound; offsetY++) {
                if (found) break;

                for (int offsetX = (bound * -1); offsetX <= bound; offsetX++) {
                    if (repeat && offsetX != 0 && offsetY != 0) continue;
                    final int x1 = x + offsetX;
                    final int y1 = y + offsetY;
                    final int[] robotPosition = robotController.getRobotPosition();
                    final int distance = abs(x1 - robotPosition[0]) + abs(y1 - robotPosition[1]);
                    if (!robotController.isRobotMovable(x1, y1) || distance >= shortestDistance) continue;

                    shortestDistance = distance;
                    coordinates[0] = x1;
                    coordinates[1] = y1;
                    found = true;
                    break;
                }
            }

            if (!repeat) {
                if (found) return coordinates;
                repeat = true;
            }
        }

        if (found) return coordinates;
        coordinates[1] = 15 * y + x;
        return coordinates;
    }
}
