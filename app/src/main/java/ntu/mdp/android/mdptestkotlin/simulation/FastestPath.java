package ntu.mdp.android.mdptestkotlin.simulation;

import android.util.Log;
import android.util.Pair;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import ntu.mdp.android.mdptestkotlin.App;
import ntu.mdp.android.mdptestkotlin.arena.RobotController;

public class FastestPath extends Thread {
    private final RobotController robotController;
    private final AStarSearch aStarSearch;
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final Function0<Unit> callback;
    private boolean movable = true;

    public FastestPath(RobotController robotController, Function0<Unit> callback) {
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
        App.setROBOT_MOVABLE(false);

        final List<int[]> pathList = aStarSearch.fastestPathChallenge();

        for (int[] pathCoordinates : pathList) {
            if (stop.get()) return;

            while (!movable) {
                if (stop.get()) return;

                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    Log.e("GG", "GG3");
                }
            }

            movable = false;
            robotController.moveRobotJava(pathCoordinates).join();
        }

        if (!stop.get()) callback.invoke();
    }
}
