package test;
import model.entity.Grid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;


import model.entity.Robot;
import model.util.MessageMgr;
import model.util.SocketMgr;

import model.algorithm.AlgorithmRunner;

import static constant.CommConstants.TARGET_ANDROID;
import static constant.CommConstants.TARGET_ARDUINO;
import static constant.CommConstants.TARGET_ANDROID;
import static constant.CommConstants.TARGET_BOTH;

public class CommTester {
	public boolean test_astar(int sX, int sY, int eX, int eY, Grid mGrid, Robot mRobot) {
		List<String> returnPath = AlgorithmRunner.runAstar(sX, sY, eX, eY, mGrid, mRobot);
		if(returnPath==null)return false;
		for(int i = 0;i<returnPath.size();i++) {
			System.out.print(returnPath.get(i)+' ');
		}
		System.out.println("");
		if(returnPath!=null) {
			return true;
		}
		return false;
	}
	
	public boolean test_general() {
		
		try {
//			StringBuilder builder =  new StringBuilder();
//			builder.append("C");
//			builder.append("R");
//			for(int i = 0;i<11;i++)builder.append("M");
//			builder.append("L");
//			for(int i = 0;i<18;i++) {
//				builder.append("M");
//			}builder.append("R");
//			builder.append("M");
//			builder.append("M");
//			System.out.println(builder.toString());
			testSendingMessage(TARGET_ARDUINO,"MMMMMMRMMMLMMMMMMMMMMMLMMMVVVVVVVVVVVV");
			
//			testSendingMessage(TARGET_BOTH,"M");
//			
//
//			testSendingMessage(TARGET_BOTH,"R");
//			testSendingMessage(TARGET_BOTH,"I");
//			String sensorData = SocketMgr.getInstance().receiveMessage(true);
//            while (sensorData == null) {
//                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "I");
//                sensorData = SocketMgr.getInstance().receiveMessage(true);
//            }
//			testSendingMessage(TARGET_BOTH,"R");
//			testSendingMessage(TARGET_BOTH,"I");
//			sensorData = SocketMgr.getInstance().receiveMessage(true);
//            while (sensorData == null) {
//                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "I");
//                sensorData = SocketMgr.getInstance().receiveMessage(true);
//            }
//			testSendingMessage(TARGET_BOTH,"R");
//			testSendingMessage(TARGET_BOTH,"I");
//			sensorData = SocketMgr.getInstance().receiveMessage(true);
//            while (sensorData == null) {
//                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "I");
//                sensorData = SocketMgr.getInstance().receiveMessage(true);
//            }
//			testSendingMessage(TARGET_BOTH,"L");
//			testSendingMessage(TARGET_BOTH,"I");
//			sensorData = SocketMgr.getInstance().receiveMessage(true);
//            while (sensorData == null) {
//                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "I");
//                sensorData = SocketMgr.getInstance().receiveMessage(true);
//            }
//			testSendingMessage(TARGET_BOTH,"L");
//			testSendingMessage(TARGET_BOTH,"I");
//			sensorData = SocketMgr.getInstance().receiveMessage(true);
//            while (sensorData == null) {
//                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "I");
//                sensorData = SocketMgr.getInstance().receiveMessage(true);
//            }
//			testSendingMessage(TARGET_BOTH,"L");
//			testSendingMessage(TARGET_BOTH,"I");
//			sensorData = SocketMgr.getInstance().receiveMessage(true);
//            while (sensorData == null) {
//                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "I");
//                sensorData = SocketMgr.getInstance().receiveMessage(true);
//            }
//			testSendingMessage(TARGET_BOTH,"R");
//			testSendingMessage(TARGET_BOTH,"I");
//			sensorData = SocketMgr.getInstance().receiveMessage(true);
//            while (sensorData == null) {
//                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "I");
//                sensorData = SocketMgr.getInstance().receiveMessage(true);
//            }
//			testSendingMessage(TARGET_BOTH,"L");
//			testSendingMessage(TARGET_BOTH,"L");
//			testSendingMessage(TARGET_BOTH,"I");
//			sensorData = SocketMgr.getInstance().receiveMessage(true);
//            while (sensorData == null) {
//                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "I");
//                sensorData = SocketMgr.getInstance().receiveMessage(true);
//            }
//			testSendingMessage(TARGET_BOTH,"M");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	public boolean testConnection() {
		if (!SocketMgr.getInstance().isConnected()) {
			SocketMgr.getInstance().openConnection();
		}
		return SocketMgr.getInstance().isConnected();
		
	}
	public boolean testSendingMessage(String dest, String msg) {
		if(!testConnection())return false;
		SocketMgr.getInstance().sendMessage(dest, msg);
		return true;
	}
	public boolean testReceivingMessage(String msg) {
		if(!testConnection())return false;
		String m = SocketMgr.getInstance().receiveMessage(false);
		if(msg!="") {
			return m==msg;
		}
		return msg!="";
	}
	

}
