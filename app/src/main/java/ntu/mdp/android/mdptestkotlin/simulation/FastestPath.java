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

public class FastestPath extends Thread {
    private final RobotController robotController;
    private final AStarSearch aStarSearch;
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final Function0<Unit> callback;

    public FastestPath(MainActivityController activityController, Function0<Unit> callback) {
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
        final int[] startPosition = robotController.getStartPosition();
        int startX = startPosition[0];
        int startY = startPosition[1];
        int startFacing1 = 0;
        int startFacing2 = 0;

        if (robotController.isStartPointExact(13, 1)) {
            startFacing2 = 270;
        } else if (robotController.isStartPointExact(1, 18)) {
            startFacing1 = 180;
        } else if (robotController.isStartPointExact(13, 18)) {
            startFacing1 = 180;
            startFacing2 = 270;
        }

        if (stop.get()) return;
        final Pair<Double, List<int[]>> pathToWaypoint1 = aStarSearch.findFastestPath(new int[] {startX, startY, startFacing1}, robotController.getWaypointPosition());
        final Pair<Double, List<int[]>> pathToWaypoint2 = aStarSearch.findFastestPath(new int[] {startX, startY, startFacing2}, robotController.getWaypointPosition());
        final List<int[]> path1 = pathToWaypoint1.second;
        final List<int[]> path2 = pathToWaypoint2.second;

        if (stop.get()) return;
        final int[] waypointPosition = robotController.getWaypointPosition();
        startX = waypointPosition[0];
        startY = waypointPosition[1];
        startFacing1 = (path1.isEmpty()) ? startFacing1 : path1.get(path1.size() - 1)[2];
        startFacing2 = (path2.isEmpty()) ? startFacing2 : path2.get(path2.size() - 1)[2];

        if (stop.get()) return;
        final List<int[]> goalPath1 = aStarSearch.findFastestPath(new int[] {startX, startY, startFacing1}, robotController.getGoalPosition()).second;
        final List<int[]> goalPath2 = aStarSearch.findFastestPath(new int[] {startX, startY, startFacing2}, robotController.getGoalPosition()).second;
        path1.addAll(goalPath1);
        path2.addAll(goalPath2);

        if (path1.isEmpty() && path2.isEmpty()) return;

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

        if (stop.get()) return;
        double cost1 = pathToWaypoint1.first;
        double cost2 = pathToWaypoint2.first;
        if (turns2 > turns1) cost2 += 100;
        else if (turns1 > turns2) cost1 += 100;

        final List<int[]> pathList = (turns2 <= turns1) ? path2 : path1;
        Log.e("TEST", turns1 + ", " + turns2 + ", " + cost1 + ", " + cost2);

        for (int[] pathCoordinates : pathList) {
            if (stop.get()) return;

            try {
                sleep(App.getSimulationDelay());
            } catch (InterruptedException e) {
                Log.e("GG", "GG");
            }

            robotController.moveRobotJava(pathCoordinates).join();
        }

        if (!stop.get()) callback.invoke();
    }
}
