package ntu.mdp.android.mdptestkotlin.simulation;

import android.util.Log;
import android.util.Pair;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import ntu.mdp.android.mdptestkotlin.App;
import ntu.mdp.android.mdptestkotlin.MainActivityController;
import ntu.mdp.android.mdptestkotlin.arena.RobotController;

import static java.lang.Math.abs;

public class Exploration extends Thread {
    private final RobotController robotController;
    private final AStarSearch aStarSearch;
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final Function0<Unit> callback;

    public Exploration(MainActivityController activityController, Function0<Unit> callback) {
        robotController = activityController.getRobotController();
        aStarSearch = new AStarSearch(robotController);
        this.callback = callback;
    }

    public void end() {
        stop.set(true);
    }

    @Override
    public void run() {
        stop.set(false);
        int[] robotPosition = robotController.getRobotPosition();
        int x = robotPosition[0];
        int y = robotPosition[1];
        int facing;

        if (robotController.isRobotMovable(x + 1, y)) facing = 90;
        else if (robotController.isRobotMovable(x, y + 1)) facing = 0;
        else if (robotController.isRobotMovable(x, y - 1)) facing = 180;
        else facing = 270;
        robotController.turnRobotJava(facing);

        boolean inTunnel = false;
        boolean wallHug = true;

        while (!stop.get()) {
            try {
                sleep(App.getSimulationDelay());
            } catch (InterruptedException e) {
                Log.e("GG", "GG");
            }

            if (robotController.coverageReached()) break;

            if (wallHug) {
                if (robotController.canMoveJava(RobotController.Direction.RIGHT).join() && !inTunnel) {
                    robotController.moveRobotJava(RobotController.Direction.RIGHT).join();
                    if (robotController.isStartPointExact(robotController.getRobotPosition())) wallHug = false;
                    continue;
                }

                if (robotController.canMoveJava(RobotController.Direction.FORWARD).join() && !inTunnel) {
                    robotController.moveRobotJava(RobotController.Direction.FORWARD).join();
                    if (robotController.isStartPointExact(robotController.getRobotPosition())) wallHug = false;
                    continue;
                }

                if (robotController.canMoveJava(RobotController.Direction.LEFT).join()) {
                    inTunnel = false;
                    robotController.moveRobotJava(RobotController.Direction.LEFT).join();
                    if (robotController.isStartPointExact(robotController.getRobotPosition())) wallHug = false;
                    continue;
                }

                inTunnel = true;

                if (robotController.canMoveJava(RobotController.Direction.REVERSE).join()) {
                    robotController.moveRobotJava(RobotController.Direction.REVERSE).join();
                    if (robotController.isStartPointExact(robotController.getRobotPosition())) wallHug = false;
                    continue;
                }

                if (robotController.canMoveJava(RobotController.Direction.RIGHT).join()) {
                    inTunnel = false;
                    robotController.moveRobotJava(RobotController.Direction.RIGHT).join();
                    if (robotController.isStartPointExact(robotController.getRobotPosition())) wallHug = false;
                    continue;
                }

                wallHug = false;
            }

            if (!robotController.hasUnexploredGrid()) break;

            int[] nearestCoordinates = findNearestUnexploredGrid();
            if (!robotController.isValidCoordinates(nearestCoordinates, true)) break;

            robotPosition = robotController.getRobotPosition();
            Pair<Double, List<int[]>> fastestPathToNearest = aStarSearch.findFastestPath(robotPosition, nearestCoordinates);
            List<int[]> pathList = fastestPathToNearest.second;
            if (pathList.isEmpty()) break;

            for (int[] pathCoordinates : pathList) {
                robotController.moveRobotJava(pathCoordinates).join();
                break;
            }
        }


        robotPosition = robotController.getRobotPosition();
        x = robotPosition[0];
        y = robotPosition[1];

        if (robotController.isStartPointExact(x, y)) {
            callback.invoke();
            return;
        }

        Pair<Double, List<int[]>> fastestPathToStart = aStarSearch.findFastestPath(robotPosition, robotController.getStartPosition());
        List<int[]> pathList = fastestPathToStart.second;
        if (pathList.isEmpty()) return;

        for (int[] pathCoordinates : pathList) {
            if (stop.get()) return;

            try {
                sleep(App.getSimulationDelay());
            } catch (InterruptedException e) {
                Log.e("GG", "GG");
            }

            robotController.moveRobotJava(pathCoordinates).join();
        }

        callback.invoke();
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
