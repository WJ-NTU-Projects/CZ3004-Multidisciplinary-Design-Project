package algorithm;

class Arena {
    static final int GRID_UNKNOWN = 0;
    static final int GRID_EXPLORED = 1;
    static final int GRID_SUSPECT = 2;
    static final int GRID_OBSTACLE = 3;

    static final int FORWARD = 0;
    static final int LEFT = 1;
    static final int RIGHT = 2;
    private static final int UP = 0;
    private static final int DOWN = 3;

    private int[][] gridArray = new int[20][15];
    private int[][] exploreArray = new int[20][15];
    private int[][] obstacleArray = new int[20][15];
    private int facing = UP;

    private Coordinates currentPosition;
    private Coordinates startPosition;
    private Coordinates goalPosition;
    private Coordinates waypointPosition;

    Arena() {
        startPosition = new Coordinates(1, 1);
        goalPosition = new Coordinates(13, 18);
        currentPosition = new Coordinates(1, 1);
        waypointPosition = new Coordinates(-1, -1);
        updateCurrentPosition(startPosition.x, startPosition.y);
    }

    void move(int direction, int distance) {
        switch (direction) {
            case FORWARD:
                switch (facing) {
                    case UP:
                        updateCurrentPosition(currentPosition.x, currentPosition.y + distance);
                        break;
                    case LEFT:
                        updateCurrentPosition(currentPosition.x - distance, currentPosition.y);
                        break;
                    case RIGHT:
                        updateCurrentPosition(currentPosition.x + distance, currentPosition.y);
                        break;
                    case DOWN:
                        updateCurrentPosition(currentPosition.x, currentPosition.y - distance);
                        break;
                } break;

            case LEFT:
                switch (facing) {
                    case UP:
                        facing = LEFT;
                        break;
                    case LEFT:
                        facing = DOWN;
                        break;
                    case RIGHT:
                        facing = UP;
                        break;
                    case DOWN:
                        facing = RIGHT;
                        break;
                } break;

            case RIGHT:
                switch (facing) {
                    case UP:
                        facing = RIGHT;
                        break;
                    case LEFT:
                        facing = UP;
                        break;
                    case RIGHT:
                        facing = DOWN;
                        break;
                    case DOWN:
                        facing = LEFT;
                        break;
                } break;
        }
    }

    boolean isMovable(int direction) {
        int x = currentPosition.x;
        int y = currentPosition.y;

        switch (direction) {
            case FORWARD:
                switch (facing) {
                    case UP:
                        y += 1;
                        break;
                    case LEFT:
                        x -= 1;
                        break;
                    case RIGHT:
                        x += 1;
                        break;
                    case DOWN:
                        y -= 1;
                        break;
                } break;

            case LEFT:
                switch (facing) {
                    case UP:
                        x -= 1;
                        break;
                    case LEFT:
                        y -= 1;
                        break;
                    case RIGHT:
                        y += 1;
                        break;
                    case DOWN:
                        x += 1;
                        break;
                } break;

            case RIGHT:
                switch (facing) {
                    case UP:
                        x += 1;
                        break;
                    case LEFT:
                        y += 1;
                        break;
                    case RIGHT:
                        y -= 1;
                        break;
                    case DOWN:
                        x -= 1;
                        break;
                } break;
        }

        for (int yOffset = -1; yOffset <= 1; yOffset++) {
            for (int xOffset = -1; xOffset <= 1; xOffset++) {
                if (isInvalidCoordinates(x + xOffset, y + yOffset, false)) return false;
                if (obstacleArray[y + yOffset][x + xOffset] == 1) return false;
            }
        }

        return true;
    }

    void setGridSensor(int sensor, int distance, int status) {
        switch (sensor) {
            case 1:
            case 2:
            case 3:
                setGridSensorFront(sensor, distance, status);
                break;
            case 4:
            case 5:
                System.out.println("SENSOR"+sensor);
                setGridSensorLeft(sensor, distance, status);
                break;
            case 6:
                setGridSensorRight(distance, status);
                break;
        }
    }

    private void setGridSensorFront(int sensor, int distance, int status) {
        int x = currentPosition.x;
        int y = currentPosition.y;

        switch (facing) {
            case UP:
                x += (sensor - 2);
                y += 1;
                setGridStatusInternal(x, y + distance, status);
                for (int offset = distance - 1; offset > 0; offset--) setGridStatusInternal(x, y + offset, GRID_EXPLORED);
                break;
            case LEFT:
                x -= 1;
                y += (sensor - 2);
                setGridStatusInternal(x - distance, y, status);
                for (int offset = distance - 1; offset > 0; offset--) setGridStatusInternal(x - offset, y, GRID_EXPLORED);
                break;
            case RIGHT:
                x += 1;
                y -= (sensor - 2);
                setGridStatusInternal(x + distance, y, status);
                for (int offset = distance - 1; offset > 0; offset--) setGridStatusInternal(x + offset, y, GRID_EXPLORED);
                break;
            case DOWN:
                x -= (sensor - 2);
                y -= 1;
                setGridStatusInternal(x, y - distance, status);
                for (int offset = distance - 1; offset > 0; offset--) setGridStatusInternal(x, y - offset, GRID_EXPLORED);
                break;
        }
    }

    private void setGridSensorLeft(int sensor, int distance, int status) {
        int x = currentPosition.x;
        int y = currentPosition.y;

        switch (facing) {
            case UP:
                x -= 1;
                y -= (sensor - 4);
                setGridStatusInternal(x - distance, y, status);
                for (int offset = distance - 1; offset > 0; offset--) setGridStatusInternal(x - offset, y, GRID_EXPLORED);
                break;
            case LEFT:
                x += (sensor - 4);
                y -= 1;
                setGridStatusInternal(x, y - distance, status);
                for (int offset = distance - 1; offset > 0; offset--) setGridStatusInternal(x, y - offset, GRID_EXPLORED);
                break;
            case RIGHT:
                x -= (sensor - 4);
                y += 1;
                setGridStatusInternal(x, y + distance, status);
                for (int offset = distance - 1; offset > 0; offset--) setGridStatusInternal(x, y + offset, GRID_EXPLORED);
                break;
            case DOWN:
                x += 1;
                y += (sensor - 4);
                setGridStatusInternal(x + distance, y, status);
                for (int offset = distance - 1; offset > 0; offset--) setGridStatusInternal(x + offset, y, GRID_EXPLORED);
                break;
        }
    }

    private void setGridSensorRight(int distance, int status) {
        int x = currentPosition.x;
        int y = currentPosition.y;

        switch (facing) {
            case UP:
                x += 1;
                setGridStatusInternal(x + distance, y, status);
                for (int offset = distance - 1; offset > 0; offset--) setGridStatusInternal(x + offset, y, GRID_EXPLORED);
                break;
            case LEFT:
                y += 1;
                setGridStatusInternal(x, y + distance, status);
                for (int offset = distance - 1; offset > 0; offset--) setGridStatusInternal(x, y + offset, GRID_EXPLORED);
                break;
            case RIGHT:
                y -= 1;
                setGridStatusInternal(x, y - distance, status);
                for (int offset = distance - 1; offset > 0; offset--) setGridStatusInternal(x, y - offset, GRID_EXPLORED);
                break;
            case DOWN:
                x -= 1;
                setGridStatusInternal(x - distance, y, status);
                for (int offset = distance - 1; offset > 0; offset--) setGridStatusInternal(x - offset, y, GRID_EXPLORED);
                break;
        }
    }

    private void updateCurrentPosition(int x, int y) {
        if (isInvalidCoordinates(x, y, true)) return;
        currentPosition.x = x;
        currentPosition.y = y;

        for (int yOffset = -1; yOffset <= 1; yOffset++) {
            for (int xOffset = -1; xOffset <= 1; xOffset++) {
                setGridStatusInternal(x + xOffset, y + yOffset, GRID_EXPLORED);
            }
        }
    }

    void displayGrid() {
        System.out.println();
        for (int y = 19; y >= 0; y--) {
            for (int x = 0; x <= 14; x++) {
                int status = gridArray[y][x];

                switch (status) {
                    case GRID_EXPLORED:
                        System.out.print("O ");
                        break;

                    case GRID_SUSPECT:
                        System.out.print("x ");
                        break;

                    case GRID_OBSTACLE:
                        System.out.print("X ");
                        break;

                    default:
                        System.out.print("? ");
                        break;
                }
            }

            System.out.println();
        }
        System.out.println();
    }

    private void setGridStatusInternal(int x, int y, int status) {
        if (isInvalidCoordinates(x, y, false)) return;
        if (gridArray[y][x] == GRID_OBSTACLE) return;
        gridArray[y][x] = status;

        if (status != GRID_UNKNOWN) exploreArray[y][x] = 1;
        if (status == GRID_OBSTACLE) obstacleArray[y][x] = 1;
    }

    private boolean isInvalidCoordinates(int x, int y, boolean robot) {
        if (robot) return (x < 1 || x > 13 || y < 1 || y > 18);
        else return (x < 0 || x > 14 || y < 0 || y > 19);
    }

    private class Coordinates {
        private int x;
        private int y;

        private Coordinates(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
