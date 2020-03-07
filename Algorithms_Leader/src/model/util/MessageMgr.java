package model.util;

import java.util.ArrayList;
import java.util.List;

import static constant.MapConstants.MAP_ROWS;
import static constant.RobotConstants.*;

/**
 * Message generator
 */
public class MessageMgr {

    public static String generateFinalDescriptor(String part1, String part2) {
        return "{finaldescriptor:\"" + part1 + "," + part2 + "\"}";
    }

    /**
     * Generate map string for Android communication, note that on Android the coordinate of
     * the robot is the upper right corner.
     * @param descriptor Map descriptor in Android format
     * @param x Robot's x coordinates
     * @param y Robot's y coordinates
     * @param heading Robot's heading
     * @return Message string for sending to Android
     */
    public static String generateMapDescriptorMsg(String descriptor, int x, int y, int heading) {
        StringBuilder builder = new StringBuilder();
        builder.append("#r:");
        builder.append(descriptor);
        builder.append(",");
        builder.append(x);
        builder.append(",");
        builder.append(MAP_ROWS - y);
        builder.append(",");
        if (heading == NORTH) {
            builder.append(0);
        } else if (heading == EAST) {
            builder.append(90);
        } else if (heading == SOUTH) {
            builder.append(180);
        } else if (heading == WEST) {
            builder.append(270);
        }
        return builder.toString();
    }

    /**
     * Parse waypoint message from Android, the Y coordinate received
     * starts from the bottom, so it's reversed.
     * @param msg
     * @return
     */
    public static List<Integer> parseMessage(String msg) {
    	if(msg.contains("waypoint:")) {
    		String[] splitStringTest = msg.split(":", 2);
    		msg = splitStringTest[1];
    		System.out.println(splitStringTest[0]);
    		System.out.println(splitStringTest[1]);
    		msg = msg.trim();
    		String[] splitString = msg.split(", ", 2);
            List<Integer> waypoint = new ArrayList<>();

            Integer wayPointX, wayPointY;
            try {
                wayPointX = Integer.parseInt(splitString[0]);
                wayPointY = MAP_ROWS - Integer.parseInt(splitString[1]) - 1;
                waypoint.add(wayPointX);
                waypoint.add(wayPointY);
                return waypoint;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
    	}
    	return null;
        
    }
}
