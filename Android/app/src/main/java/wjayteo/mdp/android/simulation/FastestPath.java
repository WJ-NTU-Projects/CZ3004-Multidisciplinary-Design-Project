package wjayteo.mdp.android.simulation;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import wjayteo.mdp.android.arena.ArenaMapController;

public class FastestPath {
    private final ArenaMapController arenaMapController;
    private final AStarSearch aStarSearch;
    private final Function1<? super Callback, Unit> callback;
    private final Function2<? super ArenaMapController.Broadcast, ? super boolean[], Unit> broadcastCallback;
    private boolean started = false;
    private List<int[]> pathList = new ArrayList<>();

    public FastestPath(ArenaMapController arenaMapController, Function1<? super Callback, Unit> callback) {
        this.arenaMapController = arenaMapController;
        aStarSearch = new AStarSearch(arenaMapController);
        this.callback = callback;

        broadcastCallback = (Function2<ArenaMapController.Broadcast, boolean[], Unit>) (broadcast, sensorData) -> {
            processBroadcast();
            return null;
        };

        arenaMapController.registerForBroadcast(broadcastCallback);
    }

    public void start() {
        pathList = aStarSearch.fastestPathChallenge();
        arenaMapController.turnRobotJava(pathList.get(0)[2]);
    }

    public void end() {
        started = false;
        arenaMapController.deregisterForBroadcast(broadcastCallback);
    }

    private void processBroadcast() {
        if (!started) {
            started = true;
            callback.invoke(Callback.START_CLOCK);
        }

        final int[] robotPosition = arenaMapController.getRobotPosition();
        final int x = robotPosition[0];
        final int y = robotPosition[1];

        if (arenaMapController.isGoalPointExact(x, y)) {
            callback.invoke(Callback.COMPLETE);
            end();
            return;
        }

        int[] coordinates = pathList.get(0);
        pathList.remove(0);
        arenaMapController.moveRobot(coordinates, false);
    }
}
