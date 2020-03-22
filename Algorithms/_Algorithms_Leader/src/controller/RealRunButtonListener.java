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
        if (mView.getIsRealRun()) {
            mView.disableButtons();
            new PhysicalRunWorker().execute();
        }
    }

    class PhysicalRunWorker extends SwingWorker<Integer, Integer> {

        @Override
        protected Integer doInBackground() throws Exception {
            AlgorithmRunner explorationRunner = new ExplorationAlgorithmRunner(1);
            explorationRunner.run(mGrid, mRobot, mView.getIsRealRun());
            return 1;
        }

        @Override
        protected void done() {
            super.done();
            mView.enableButtons();
        }
    }
}
