package controller;

import model.algorithm.AlgorithmRunner;

import model.algorithm.ExplorationAlgorithmRunner;
import model.algorithm.FastestPathAlgorithmRunner;
import model.entity.Grid;
import model.entity.Robot;
import model.util.MessageMgr;
import model.util.SocketMgr;
import view.Simulator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Created by koallen on 11/10/17.
 */
public class RealRunButtonListener implements ActionListener {

    private Simulator mView;
    private Grid mGrid;
    private Robot mRobot;

    public RealRunButtonListener(Simulator view, Grid grid, Robot robot) {
        mView = view;
        mGrid = grid;
        mRobot = robot;
        mView.addRealRunButtonListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Physical run button pressed, real run: " + mView.getIsRealRun());
        if (mView.getIsRealRun()) {
            if (mView.getRobotSpeed() == 0) {
                JOptionPane.showMessageDialog(null, "Please set robot speed (X Steps per second)!", "Fastest path", JOptionPane.ERROR_MESSAGE);
            }
            mView.disableButtons();
            new PhysicalRunWorker().execute();
        }
    }

    class PhysicalRunWorker extends SwingWorker<Integer, Integer> {

        @Override
        protected Integer doInBackground() throws Exception {
            // receive way point
//            String msg = SocketMgr.getInstance().receiveMessage(false);
            List<Integer> waypoints = Arrays.asList(5,5);
//            List<Integer> waypoints;
//            while ((waypoints = MessageMgr.parseMessage(msg)) == null) {
//                msg = SocketMgr.getInstance().receiveMessage(false);
//                System.out.println("wating for waypoint");
//            }

            // do exploration
            AlgorithmRunner explorationRunner = new ExplorationAlgorithmRunner(mView.getRobotSpeed());
            explorationRunner.run(mGrid, mRobot, mView.getIsRealRun());

            // do fastest path
            AlgorithmRunner fastestPathRunner = new FastestPathAlgorithmRunner(mView.getRobotSpeed(),
                    waypoints.get(0) - 1, waypoints.get(1) - 1);
//            0, 17);
            fastestPathRunner.run(mGrid, mRobot, mView.getIsRealRun());

            return 1;
        }

        @Override
        protected void done() {
            super.done();
            mView.enableButtons();
        }
    }
}
