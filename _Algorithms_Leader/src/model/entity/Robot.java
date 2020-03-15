package model.entity;

import model.util.SocketMgr;

import java.net.SocketException;
import java.util.List;
import java.util.Observable;

import static constant.CommConstants.TARGET_ARDUINO;
import static constant.RobotConstants.*;

/**
 * Represents the physical robot
 */
public class Robot extends Observable {
    private int mPosX = START_POS_X; // upper left of robot
    private int mPosY = START_POS_Y; // upper left of robot
    private int mHeading = NORTH;
    private boolean needToCheckRight = false;
    private Grid mGrid;
    private List<Sensor> mSensors;
    private String sensorV;

    public Robot(Grid grid, List<Sensor> sensors) {
        mGrid = grid;
        mSensors = sensors;
        for (Sensor sensor : sensors) {
            sensor.setRobot(this);
        }
    }

    public boolean isInRobot(int x, int y) {
        return x < getPosX()+2 && x >= getPosX()
                && y < getPosY()+2 && y >= getPosY();
    }

    public int getPosX() {
        return mPosX;
    }

    public int getPosY() {
        return mPosY;
    }

    public void setPosX(int posX) {
        mPosX = posX;
    }

    public void setPosY(int posY) {
        mPosY = posY;
    }

    public int getCenterPosX() {
        return mPosX + 1;
    }

    public int getCenterPosY() {
        return mPosY + 1;
    }

    public int getHeading() {
        return mHeading;
    }
    
    public boolean getNeedToCheckRight() {
    	return needToCheckRight;
    }
    
    public void setNeedToCheckRight(boolean need) {
    	needToCheckRight = need;
    }

    public void setHeading(int heading) {
        mHeading = heading;
    }

    /**
     * Test if the robot can calibrate on the left
     * @return
     */
    public boolean canCalibrateLeft() {
        for (int i = 0; i < ROBOT_SIZE; i++) {
            if (i == 1) continue;
            if (mHeading == NORTH) {
                // DIRECTLY BESIDE OF ROBOT
                if (!mGrid.getIsObstacle(mPosX - 1, mPosY + i)) {
                    return false;
                }
            } else if (mHeading == SOUTH) {
                // DIRECTLY BESIDE OF ROBOT
                if (!mGrid.getIsObstacle(mPosX + 3, mPosY + i)) {
                    return false;
                }
            } else if (mHeading == EAST) {
                // DIRECTLY BESIDE OF ROBOT
                if (!mGrid.getIsObstacle(mPosX + i, mPosY - 1)) {
                    return false;
                }
            } else if (mHeading == WEST) {
                // DIRECTLY BESIDE OF ROBOT
                if (!mGrid.getIsObstacle(mPosX + i, mPosY + 3)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Test if the robot can calibrate in front
     * @return
     */
    public boolean canCalibrateFront() {
        for (int i = 0; i < ROBOT_SIZE; i++) {
            if (i == 1) continue;
            if (mHeading == NORTH) {
                // DIRECTLY IN FRONT OF ROBOT
                if (!mGrid.getIsObstacle(mPosX + i, mPosY - 1)) {
                    return false;
                }
            } else if (mHeading == SOUTH) {
                // DIRECTLY IN FRONT OF ROBOT
                if (!mGrid.getIsObstacle(mPosX + i, mPosY + 3)) {
                    return false;
                }
            } else if (mHeading == EAST) {
                // DIRECTLY IN FRONT OF ROBOT
                if (!mGrid.getIsObstacle(mPosX + 3, mPosY + i)) {
                    return false;
                }
            } else if (mHeading == WEST) {
                // DIRECTLY IN FRONT OF ROBOT
                if (!mGrid.getIsObstacle(mPosX - 1, mPosY + i)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isObstacleAhead() {
        for (int i = 0; i < ROBOT_SIZE; i++) {
            if (mHeading == NORTH) {
                // DIRECTLY IN FRONT OF ROBOT
                if (mGrid.getIsObstacle(mPosX + i, mPosY - 1)) {
                    return true;
                }
            } else if (mHeading == SOUTH) {
                // DIRECTLY IN FRONT OF ROBOT
                if (mGrid.getIsObstacle(mPosX + i, mPosY + 3)) {
                    return true;
                }
            } else if (mHeading == EAST) {
                // DIRECTLY IN FRONT OF ROBOT
                if (mGrid.getIsObstacle(mPosX + 3, mPosY + i)) {
                    return true;
                }
            } else if (mHeading == WEST) {
                // DIRECTLY IN FRONT OF ROBOT
                if (mGrid.getIsObstacle(mPosX - 1, mPosY + i)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isObstacleRight() {
        for (int i = 0; i < ROBOT_SIZE; i++) {
            if (mHeading == NORTH) {
                // DIRECTLY BESIDE OF ROBOT
                if (mGrid.getIsObstacle(mPosX + 3, mPosY + i)) {
                    return true;
                }
            } else if (mHeading == SOUTH) {
                // DIRECTLY BESIDE OF ROBOT
                if (mGrid.getIsObstacle(mPosX - 1, mPosY + i)) {
                    return true;
                }
            } else if (mHeading == EAST) {
                // DIRECTLY BESIDE OF ROBOT
                if (mGrid.getIsObstacle(mPosX + i, mPosY + 3)) {
                    return true;
                }
            } else if (mHeading == WEST) {
                // DIRECTLY BESIDE OF ROBOT
                if (mGrid.getIsObstacle(mPosX + i, mPosY - 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isObstacleLeft() {
        for (int i = 0; i < ROBOT_SIZE; i++) {
            if (mHeading == NORTH) {
                // DIRECTLY BESIDE OF ROBOT
                if (mGrid.getIsObstacle(mPosX - 1, mPosY + i)) {
                    return true;
                }
            } else if (mHeading == SOUTH) {
                // DIRECTLY BESIDE OF ROBOT
                if (mGrid.getIsObstacle(mPosX + 3, mPosY + i)) {
                    return true;
                }
            } else if (mHeading == EAST) {
                // DIRECTLY BESIDE OF ROBOT
                if (mGrid.getIsObstacle(mPosX + i, mPosY - 1)) {
                    return true;
                }
            } else if (mHeading == WEST) {
                // DIRECTLY BESIDE OF ROBOT
                if (mGrid.getIsObstacle(mPosX + i, mPosY + 3)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Move robot towards the front, set robot's location to "not obstacle" and "explored"
     */
    public void move() {
        // TODO: make sure it won't go beyond the arena
        if (mHeading == NORTH) { // Limit position to prevent wall crash
            mPosY--;
            for (int i = 0; i < 3; ++i) {
                mGrid.setObstacleProbability(mPosX + i, mPosY, -1000);
                mGrid.setExplored(mPosX + i, mPosY, true);
            }
        } else if (mHeading == SOUTH) {// Limit position to prevent wall crash
            mPosY++;
            for (int i = 0; i < 3; ++i) {
                mGrid.setObstacleProbability(mPosX + i, mPosY + 2, -1000);
                mGrid.setExplored(mPosX + i, mPosY + 2, true);
            }
        } else if (mHeading == WEST) { // Limit position to prevent wall crash
            mPosX--;
            for (int i = 0; i < 3; ++i) {
                mGrid.setObstacleProbability(mPosX, mPosY + i, -1000);
                mGrid.setExplored(mPosX, mPosY + i, true);
            }
        } else if (mHeading == EAST) { // Limit position to prevent wall crash
            mPosX++;
            for (int i = 0; i < 3; ++i) {
                mGrid.setObstacleProbability(mPosX + 2, mPosY + i, -1000);
                mGrid.setExplored(mPosX + 2, mPosY + i, true);
            }
        }
        setChanged();
//        notifyObservers();
    }

    public void turn(int direction) {
        /*
        NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3, LEFT = 4, RIGHT = 5
         */
        if (direction == LEFT) {
            /*
            NORTH BECOMES WEST
            WEST BECOMES SOUTH
            SOUTH BECOMES EAST
            EAST BECOMES NORTH
             */
            mHeading += 4;
            mHeading = (mHeading - 1) % 4;
        } else if (direction == RIGHT) {
            /*
            NORTH BECOMES EAST
            EAST BECOMES SOUTH
            SOUTH BECOMES WEST
            WEST BECOMES NORTH
             */
            mHeading = (mHeading + 1) % 4;
        }
        setChanged();
//        notifyObservers();
    }

    public void reset() {
        mPosX = START_POS_X;
        mPosY = START_POS_Y;
        mHeading = NORTH;
        setChanged();
//        notifyObservers();
    }

    /**
     * Updates the simulator's map according to sensor reading. If the sensor reading
     * is smaller or equal to its range, it means there is an obstacle at that distance.
     * If the sensor reading is greater than its range, it means there is no obstacle
     * within its detectable range.
     * @param returnedDistance
     * @param heading
     * @param range
     * @param x
     * @param y
     */
    private void updateMap(int returnedDistance, int heading, int range, int x, int y, boolean realRun, int reliability) {
        int xToUpdate = x, yToUpdate = y;
        int distance = Math.min(returnedDistance, range);
        boolean obstacleAhead = returnedDistance <= range;

        for (int i = 1; i <= distance; i++) {
            if (heading == NORTH) {
                yToUpdate = yToUpdate - 1;
            } else if (heading == SOUTH) {
                yToUpdate = yToUpdate + 1;
            } else if (heading == WEST) {
                xToUpdate = xToUpdate - 1;
            } else if (heading == EAST) {
                xToUpdate = xToUpdate + 1;
            }
            mGrid.setExplored(xToUpdate, yToUpdate, true);
            // if this cell is an obstacle
            if (i == distance && obstacleAhead) {
                if (realRun) {
                	System.out.println("reliability:"+Integer.toString(reliability*(range-i+1)));
                    mGrid.setObstacleProbability(xToUpdate, yToUpdate, reliability); // increment by reliability
                } else {
                    mGrid.setIsObstacle(xToUpdate, yToUpdate, true);
                }
            } else { // if this cell is not an obstacle
                if (realRun) {
                	System.out.println("reliability:"+Integer.toString(reliability*(range-i+1)));
                    mGrid.setObstacleProbability(xToUpdate, yToUpdate, -reliability); // decrement by reliability
                } else {
                    mGrid.setIsObstacle(xToUpdate, yToUpdate, false);
                }
            }
        }
    }

    /**
     * Sense the robot's surrounding environment
     * @param realRun whether it's the physical robot
     */
    public boolean sense(boolean realRun, String command) {
        if (realRun) {
//            SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "I");
        	String sensorData;
        	if(command.compareTo("W")==0) {
        		sensorData = sensorV;
        	}
        	else { 
        		sensorData = SocketMgr.getInstance().receiveMessage(true);
        	}
            int timeOutCount = 0;
            while (sensorData == null) {
            	timeOutCount += 1;
            	if(timeOutCount>=2) {
//            		SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "I");
            		timeOutCount = 0;
            	}
//                SocketMgr.getInstance().sendMessage(TARGET_ARDUINO, "I");
                sensorData = SocketMgr.getInstance().receiveMessage(true);
            }
            System.out.println(sensorData);
            sensorV = sensorData;
            String[] sensorReadings = sensorData.split("#", mSensors.size()+1);
            System.out.println("sensor reading length:");
            System.out.println(sensorReadings.length);
            
            for (int i = 0; i < mSensors.size(); i++) {
//            	System.out.println("index: "+Integer.toString(i));
                int returnedDistance = Integer.parseInt(sensorReadings[i]); 
                int heading = mSensors.get(i).getActualHeading();
                int range = mSensors.get(i).getRange();
                int x = mSensors.get(i).getActualPosX();
                int y = mSensors.get(i).getActualPosY();
//                if(i==5) {
//                	if(returnedDistance<2)continue;
//                	if(returnedDistance>=3&&returnedDistance<=4)
//                		setNeedToCheckRight(true);
//                }
                if(command.compareTo("M")!=0) {
//                	if(i==5&&returnedDistance>=3&&returnedDistance<=4)
//                		continue;
                	updateMap(returnedDistance, heading, range, x, y, true, mSensors.get(i).getReliability());
                }
            }
            return sensorReadings[6].compareTo("1")==0;
        } else {
            for (Sensor sensor : mSensors) {
                int returnedDistance = sensor.sense(mGrid);
                int heading = sensor.getActualHeading();
                int range = sensor.getRange();
                int x = sensor.getActualPosX();
                int y = sensor.getActualPosY();
                updateMap(returnedDistance, heading, range, x, y, false, sensor.getReliability());
            }
            return true;
        }
    }
}