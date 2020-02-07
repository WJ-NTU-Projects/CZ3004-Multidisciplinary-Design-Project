package ntu.mdp.android.mdptestkotlin.simulation;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import ntu.mdp.android.mdptestkotlin.App;
import ntu.mdp.android.mdptestkotlin.arena.RobotController;

public class FastestPath {
    private final RobotController robotController;
    private final AStarSearch aStarSearch;
    private final Function1<? super Callback, Unit> callback;
    private final Function2<? super RobotController.Broadcast, ? super boolean[], Unit> broadcastCallback;
    private boolean started = false;
    private List<int[]> pathList = new ArrayList<>();

    public FastestPath(RobotController robotController, Function1<? super Callback, Unit> callback) {
        this.robotController = robotController;
        aStarSearch = new AStarSearch(robotController);
        this.callback = callback;

        broadcastCallback = (Function2<RobotController.Broadcast, boolean[], Unit>) (broadcast, sensorData) -> {
            processBroadcast();
            return null;
        };

        robotController.registerForBroadcast(broadcastCallback);
    }

    public void start() {
        pathList = aStarSearch.fastestPathChallenge();
        robotController.turnRobotJava(pathList.get(0)[2]);
    }

    public void end() {
        started = false;
        robotController.deregisterForBroadcast(broadcastCallback);
    }

    private void processBroadcast() {
        if (!started) {
            started = true;
            callback.invoke(Callback.START_CLOCK);
        }

        final int[] robotPosition = robotController.getRobotPosition();
        final int x = robotPosition[0];
        final int y = robotPosition[1];

        if (robotController.isGoalPointExact(x, y)) {
            callback.invoke(Callback.COMPLETE);
            end();
            return;
        }

        int[] coordinates = pathList.get(0);
        pathList.remove(0);
        robotController.moveRobot(coordinates);
    }
}
