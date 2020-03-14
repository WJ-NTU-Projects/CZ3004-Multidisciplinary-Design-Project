package model.entity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Observable;

import static constant.MapConstants.*;

/**
 * Grid for representing the map
 */
public class Grid extends Observable {

    private Cell[][] cells;

    public Grid() {
        cells = new Cell[MAP_COLS][MAP_ROWS];
        for (int x = 0; x < MAP_COLS; x++) {
            for (int y = 0; y < MAP_ROWS; y++) {
                cells[x][y] = new Cell();
            }
        }
        reset();
    }

    public Cell[][] getCells() {
        return cells;
    }

    public static boolean isInStartZone(int x, int y) {
        return x < ZONE_SIZE && x >= 0
                && y < MAP_ROWS && y >= MAP_ROWS - ZONE_SIZE;
    }

    public static boolean isInEndZone(int x, int y) {
        return x < MAP_COLS && x >= MAP_COLS - ZONE_SIZE
                && y < ZONE_SIZE && y >= 0;
    }
    
    public void markEverythingExplored() {
    	for (int x = 0; x < MAP_COLS; x++) {
            for (int y = 0; y < MAP_ROWS; y++) {
                setExplored(x, y, true);
            }
        }
    }

    public boolean getIsObstacle(int x, int y) {
        return isOutOfArena(x, y) || cells[x][y].getIsObstacle();
    }

    public boolean isOutOfArena(int x, int y) {
        return x < 0 || y < 0 || x >= MAP_COLS || y >= MAP_ROWS;
    }

    public void setIsObstacle(int x, int y, boolean isObstacle) {
        if (isOutOfArena(x, y))
            return;
        cells[x][y].setIsObstacle(isObstacle);
        setChanged();
//        notifyObservers();
    }

    public void setObstacleProbability(int x, int y, int value) {
        if (isOutOfArena(x, y))
            return;
        cells[x][y].updateCounter(value);
        System.out.println("x: "+Integer.toString(x)+", y: "+Integer.toString(y)+"reliability: "+Integer.toString(value));
    }

    public void setExplored(int x, int y, boolean explored) {
        if (isOutOfArena(x, y))
            return;
        cells[x][y].setExplored(explored);
        setChanged();
//        notifyObservers();
    }

    public boolean getIsExplored(int x, int y) {
        return !isOutOfArena(x, y) && cells[x][y].getExplored();
    }

    public double checkExploredPercentage() {
        double totalCells = 0.0;
        double cellsExplored = 0.0;
        for (int x = 0; x < MAP_COLS; x++) {
            for (int y = 0; y < MAP_ROWS; y++) {
                if (cells[x][y].getExplored()) {
                    cellsExplored += 1;
                }
                totalCells += 1;
            }
        }
        return (cellsExplored / totalCells) * 100;
    }

    public void loadSpecialFromDisk(String path) throws IOException {
        this.reset();

        BufferedReader reader = new BufferedReader(new FileReader(path));

        for (int i = 0; i < MAP_ROWS; i++) {
            String line = reader.readLine();
            String[] numberStrings = line.trim().split("\\s+");
            for (int j = 0; j < MAP_COLS; j++) {
                if (numberStrings[j].equals("1")) {
                    this.setObstacleProbability(j, i, 10000);
                } else {
                	this.setObstacleProbability(j, i, -10000);
                }
            }
        }
    }
    
    public void loadFromDisk(String path) throws IOException {
    	this.reset();
    	BufferedReader reader = new BufferedReader(new FileReader(path));
    	
    	for (int i = 0; i < MAP_ROWS; i++) {
            String line = reader.readLine();
            String[] numberStrings = line.trim().split("\\s+");
            for (int j = 0; j < MAP_COLS; j++) {
                if (numberStrings[j].equals("1")) {
                    this.setIsObstacle(j, i, true);
                } else {
                    this.setIsObstacle(j, i, false);
                }
            }
        }	
    }

    public String generateDescriptorPartOne() {
        StringBuilder builder;

        // first build string for exploration status
        builder = new StringBuilder();
        builder.append(11);
        for (int y = MAP_ROWS - 1; y >= 0; y--) {
            for (int x = 0; x < MAP_COLS; x++) {
                if (getIsExplored(x, y)) {
                    builder.append(1);
                } else {
                    builder.append(0);
                }
            }
        }
        builder.append(11);
        String part1 = builder.toString();
        builder = new StringBuilder();
        for (int i = 0; i < part1.length() / 4; i++) {
            builder.append(Integer.toHexString(Integer.parseInt(part1.substring(i * 4, (i + 1) * 4), 2)));
        }
        System.out.println("Map descriptor part 1:");
        System.out.println(builder.toString());

        return builder.toString();
    }

    public String generateDescriptorPartTwo() {
        // second build string for obstacle status
        StringBuilder builder = new StringBuilder();
        for (int y = MAP_ROWS - 1; y >= 0; y--) {
            for (int x = 0; x < MAP_COLS; x++) {
                if (getIsExplored(x, y)) {
                    if (getIsObstacle(x, y)) {
                        builder.append(1);
                    } else {
                        builder.append(0);
                    }
                }
            }
        }
        while (0 != (builder.length() % 8)) {
            builder.append(0);
        }
        String part2 = builder.toString();
        builder = new StringBuilder();
        for (int i = 0; i < part2.length() / 4; i++) {
            builder.append(Integer.toHexString(Integer.parseInt(part2.substring(i * 4, (i + 1) * 4), 2)));
        }
        System.out.println("Map descriptor part 2:");
        System.out.println(builder.toString());

        return builder.toString();
    }
    
    public String generateAllForAndroid() {
    	String msg = generateDescriptorPartOne() + "," + generateDescriptorPartTwo();
    	return msg;
    }

    public String generateForAndroid() {
        StringBuilder builder;

        builder = new StringBuilder();
        for (int y = 0; y < MAP_ROWS; y++) {
            for (int x = 0; x < MAP_COLS; x++) {
                if (getIsExplored(x, y)) {
                    if (getIsObstacle(x, y)) {
                        builder.append(1);
                    } else {
                        builder.append(0);
                    }
                } else {
                    builder.append(0);
                }
            }
        }
        String part1 = builder.toString();
        builder = new StringBuilder();
        for (int i = 0; i < part1.length() / 4; i++) {
            builder.append(Integer.toHexString(Integer.parseInt(part1.substring(i * 4, (i + 1) * 4), 2)));
        }

        return builder.toString();
    }

    /**
     * Set all cells to unexplored
     */
    public void reset() {
        for (int x = 0; x < MAP_COLS; x++) {
            for (int y = 0; y < MAP_ROWS; y++) {
                if (!isInStartZone(x, y) && !isInEndZone(x, y))
                    setExplored(x, y, false);
                else
                    setExplored(x, y, true);
            }
        }
    }

    public void clearObstacles() {
        for (int x = 0; x < MAP_COLS; x++) {
            for (int y = 0; y < MAP_ROWS; y++) {
                setIsObstacle(x, y, false);
            }
        }
    }
}