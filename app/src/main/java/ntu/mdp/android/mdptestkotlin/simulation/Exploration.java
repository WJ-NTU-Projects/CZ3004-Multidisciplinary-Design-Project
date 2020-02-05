package ntu.mdp.android.mdptestkotlin.simulation;

import android.util.Log;
import android.util.Pair;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import ntu.mdp.android.mdptestkotlin.arena.RobotController;

import static java.lang.Math.abs;

public class Exploration extends Thread {
    private final RobotController robotController;
    private final AStarSearch aStarSearch;
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final Function1<? super Callback, Unit> callback;
    private boolean movable = true;

    public Exploration(RobotController robotController, Function1<? super Callback, Unit> callback) {
        this.robotController = robotController;
        aStarSearch = new AStarSearch(robotController);
        this.callback = callback;

        robotController.registerForBroadcast(broadcast -> {
            if (this.isAlive() && broadcast == RobotController.Broadcast.MOVE_COMPLETE || broadcast == RobotController.Broadcast.TURN_COMPLETE) {
                movable = true;
            }

            return null;
        });
    }

    public void end() {
        stop.set(true);
    }

    @Override
    public void run() {
        stop.set(false);
        boolean wallHug = true;
        int counter = -1;
        callback.invoke(Callback.WALL_HUGGING);
        
        while (!stop.get()) {
            counter++;

            while (!movable) {
                if (stop.get()) return;

                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    Log.e("GG", "GG1");
                }
            }

            if (robotController.coverageReached()) break;

            if (robotController.isStartPointExact(robotController.getRobotPosition()) && counter > 0) {
                wallHug = false;
                callback.invoke(Callback.SEARCHING);
            }

            if (wallHug) {
                movable = false;

                if (robotController.canMove(RobotController.Direction.RIGHT)) {
                    robotController.moveRobotJava(RobotController.Direction.RIGHT).join();
                    continue;
                }

                if (robotController.canMove(RobotController.Direction.FORWARD)) {
                    robotController.moveRobotJava(RobotController.Direction.FORWARD).join();
                    continue;
                }

                if (robotController.canMove(RobotController.Direction.LEFT)) {
                    robotController.moveRobotJava(RobotController.Direction.LEFT).join();
                    continue;
                }

                robotController.turnRobotJava(RobotController.Direction.RIGHT).join();
                continue;
            }

            if (!robotController.hasUnexploredGrid()) break;

            if (!robotController.isGridExplored(RobotController.Direction.FORWARD)) {
                if (robotController.canMove(RobotController.Direction.FORWARD)) {
                    movable = false;
                    robotController.moveRobotJava(RobotController.Direction.FORWARD).join();
                    continue;
                }
            }

            final int[] nearestCoordinates = findNearestUnexploredGrid();
            if (!robotController.isValidCoordinates(nearestCoordinates, true)) break;

            final int[] robotPosition = robotController.getRobotPosition();
            final Pair<Double, List<int[]>> fastestPathToNearest = aStarSearch.findFastestPath(robotPosition, nearestCoordinates);
            final List<int[]> pathList = fastestPathToNearest.second;
            if (pathList.isEmpty()) break;

            for (int[] pathCoordinates : pathList) {
                if (stop.get()) return;

                while (!movable) {
                    if (stop.get()) return;

                    try {
                        sleep(50);
                    } catch (InterruptedException e) {
                        Log.e("GG", "GG2");
                    }
                }

                if (!robotController.isGridExplored(RobotController.Direction.FORWARD)) {
                    if (robotController.canMove(RobotController.Direction.FORWARD)) {
                        movable = false;
                        robotController.moveRobotJava(RobotController.Direction.FORWARD).join();
                        break;
                    }
                }

                movable = false;
                robotController.moveRobotJava(pathCoordinates).join();
            }
        }

        final int[] robotPosition = robotController.getRobotPosition();
        final int x = robotPosition[0];
        final int y = robotPosition[1];

        if (robotController.isStartPointExact(x, y)) {
            callback.invoke(Callback.COMPLETE);
            return;
        }

        callback.invoke(Callback.GOING_HOME);
        final Pair<Double, List<int[]>> fastestPathToStart = aStarSearch.findFastestPath(robotPosition, robotController.getStartPosition());
        final List<int[]> pathList = fastestPathToStart.second;
        if (pathList.isEmpty()) return;

        for (int[] pathCoordinates : pathList) {
            if (stop.get()) return;

            while (!movable) {
                if (stop.get()) return;

                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    Log.e("GG", "GG3");
                }
            }

            movable = false;
            robotController.moveRobotJava(pathCoordinates).join();
        }

        callback.invoke(Callback.COMPLETE);
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
