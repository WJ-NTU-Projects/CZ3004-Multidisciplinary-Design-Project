package controller;

import model.entity.Grid;
import model.entity.Robot;
import model.util.MessageMgr;
import view.Simulator;

import javax.swing.*;

import controller.FastestPathButtonListener.FastestPathWorker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MapDescriptorButtonListener implements ActionListener {
	private Simulator mView;
    private Grid mGrid;
    private Robot mRobot;
    
    public MapDescriptorButtonListener(Simulator view, Grid grid, Robot robot) {
    	mView = view;
    	mGrid = grid;
    	mRobot = robot;
    	mView.addMapDescriptorButtonListener(this);
    }
    
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
        System.out.println("Generate Map Descriptor button pressed");
        String m = MessageMgr.generateFinalDescriptor(mGrid.generateDescriptorPartOne(), mGrid.generateDescriptorPartTwo());
//        String msg = MessageMgr.generateMapDescriptorMsg(mGrid.generateForAndroid(), mRobot.getCenterPosX(), mRobot.getCenterPosY(), mRobot.getHeading());
        System.out.println(m);
        JOptionPane.showMessageDialog(null, m, "MDF", JOptionPane.INFORMATION_MESSAGE);
	}

}
