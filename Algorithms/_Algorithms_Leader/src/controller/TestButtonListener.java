package controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import model.entity.Grid;
import model.entity.Robot;
import model.util.MessageMgr;
import model.util.SocketMgr;
import test.CommTester;
import view.Simulator;
import static constant.CommConstants.TARGET_ANDROID;
import static constant.CommConstants.TARGET_ARDUINO;

public class TestButtonListener implements ActionListener {
	private Simulator mView;
    private Grid mGrid;
    private Robot mRobot;
    private CommTester tester;
    private String[] optionList;

    public TestButtonListener(Simulator view, Grid grid, Robot robot) {
        mView = view;
        mGrid = grid;
        mRobot = robot;
        mView.addTestButtonListener(this);
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("Test button pressed");
		tester = new CommTester();
		optionList = new String[] {"general test", "connection", "send arduino", "send Android", "receive"};
		int choice = JOptionPane.showOptionDialog(null, "testing options",  "test",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE, null,optionList,null);
		
		boolean result = false;
		
		if(choice==0) {
			System.out.println("general test");
			result = tester.test_general();
//			mRobot.setPosX(2);
//			mRobot.setPosY(16);
//			result = tester.test_astar(mRobot.getPosX(),mRobot.getPosY(),0,17,mGrid, mRobot);
		}
		else if(choice == 1) {
			System.out.println("test connection");
			result = tester.testConnection();
		}
		else if(choice == 2) {
			SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "I");
//			mRobot.sense(true);
//			System.out.println("test Arduino");
//			String instruction = JOptionPane.showInputDialog(null, "Please input your instruction", "Arduino Instruction", JOptionPane.QUESTION_MESSAGE);
//			result = tester.testSendingMessage(TARGET_ARDUINO, instruction);
		}
		
		else if(choice == 3) {
			System.out.println("test Android");
			String msg =  MessageMgr.generateMapDescriptorMsg(mGrid.generateAllForAndroid(), mRobot.getCenterPosX(), mRobot.getCenterPosY(), mRobot.getHeading());
			System.out.println(msg);
			result = tester.testSendingMessage(TARGET_ANDROID,msg);
		}
		else if(choice == 4) {
			System.out.println("test receive message");
			String expectation = JOptionPane.showInputDialog(null, "Please input your expectation, leave it blank if no expectation", "Arduino Instruction", JOptionPane.QUESTION_MESSAGE);
			System.out.println("expectation: "+expectation);
			result = tester.testReceivingMessage(expectation);
		}
		
		if(!result) {
			JOptionPane.showMessageDialog(null, "Test not passed", "Test Result", JOptionPane.ERROR_MESSAGE);
		}
		else {
			JOptionPane.showMessageDialog(null, "Test Passed", "Test Result", JOptionPane.INFORMATION_MESSAGE);
		}
		
		
	}

}
