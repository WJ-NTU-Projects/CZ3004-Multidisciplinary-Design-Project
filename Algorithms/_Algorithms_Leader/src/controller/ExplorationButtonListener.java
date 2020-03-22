package controller;

import model.algorithm.AlgorithmRunner;
import model.algorithm.ExplorationAlgorithmRunner;
import model.entity.Grid;
import model.entity.Robot;
import view.Simulator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Button listener
 */
public class ExplorationButtonListener implements ActionListener {

    private Simulator mView;
    private Grid mGrid;
    private Robot mRobot;

    public ExplorationButtonListener(Simulator view, Grid grid, Robot robot) {
        mView = view;
        mGrid = grid;
        mRobot = robot;
        mView.addExplorationButtonListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Exploration button pressed");
        //if (!mView.getIsRealRun()) {
//            if (mView.getRobotSpeed() == 0) {
//                JOptionPane.showMessageDialog(null, "Please set robot speed! (X Steps per second)", "Fastest path", JOptionPane.ERROR_MESSAGE);
//            }
        //}
        mView.disableButtons();
        new ExplorationWorker().execute();
    }

    class ExplorationWorker extends SwingWorker<Integer, Integer> {

        @Override
        protected Integer doInBackground() throws Exception {
            //AlgorithmRunner algorithmRunner = new ExplorationAlgorithmRunner(mView.getRobotSpeed());
            AlgorithmRunner algorithmRunner = new ExplorationAlgorithmRunner(1);
            algorithmRunner.run(mGrid, mRobot, mView.getIsRealRun());
            return 1;
        }

        @Override
        protected void done() {
            super.done();
            mView.enableButtons();
        }
    }

}
