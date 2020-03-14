package controller;

import model.entity.Grid;
import model.entity.Robot;
import view.Simulator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * Button listener
 */
public class LoadMapButtonListener implements ActionListener {

    private Simulator mView;
    private Grid mGrid;
    private Robot mRobot;

    public LoadMapButtonListener(Simulator view, Grid grid, Robot robot) {
        mView = view;
        mGrid = grid;
        mRobot = robot;
        mView.addLoadMapButtonListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("LoadMap button pressed");
        String file_path = JOptionPane.showInputDialog(null, "Please input a map under 'maps' folder", "Map loader", JOptionPane.QUESTION_MESSAGE);
        if (null != file_path) {
            try {
                mView.disableButtons();
                mGrid.loadFromDisk("maps/" + file_path);
                System.out.println("Loaded map " + file_path);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "The file doesn't exist!", "Map loader", JOptionPane.ERROR_MESSAGE);
            } finally {
                mView.enableButtons();
            }
        }
    }
}
