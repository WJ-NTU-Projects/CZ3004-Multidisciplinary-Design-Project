package model.entity;


/**
 * Represents a cell in the grid
 */
public class Cell implements Comparable<Cell> {

    private int mX;
    private int mY;
    private int mDistance;
    private boolean mExplored = false;
    private boolean mIsObstacle = false;
    private int counter = 0;

    Cell() {}

    public Cell(int x, int y) {
        mX = x;
        mY = y;
    }

    // set whether a cell is an obstacle according to an counter
    void updateCounter(int value) {
        counter += value;
        mIsObstacle = counter > 0;
    }

    void setExplored(boolean explored) {
        mExplored = explored;
    }

    void setIsObstacle(boolean isObstacle) {
        mIsObstacle = isObstacle;
    }

    public boolean getExplored() {
        return mExplored;
    }

    public boolean getIsObstacle() {
        return mIsObstacle;
    }

    public int getX() {
        return mX;
    }

    public int getY() {
        return mY;
    }

    public void setDistance(int distance) {
        mDistance = distance;
    }

    private int getDistance() {
        return mDistance;
    }

    @Override
    public int compareTo(Cell o) {
        if (mDistance < o.getDistance())
            return -1;
        else if (mDistance > o.getDistance())
            return 1;
        else
            return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Cell) {
            Cell otherCell = (Cell)obj;
            if (otherCell.getX() == getX() && otherCell.getY() == getY())
                return true;
        }
        return false;
    }
}