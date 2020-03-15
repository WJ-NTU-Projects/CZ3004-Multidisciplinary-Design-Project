package view;

import model.entity.Cell;
import model.entity.Grid;
import model.entity.Robot;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;

import static constant.MapConstants.*;
import static constant.RobotConstants.*;

/**
 * Map UI
 */
public class MapPanel extends JPanel implements Observer {

    private Grid mGrid;
    private Robot mRobot;

    MapPanel(Grid grid, Robot robot) {
        mGrid = grid;
        mRobot = robot;
        initializeMap();
    }

    private void initializeMap() {
        setPreferredSize(new Dimension(CELL_SIZE * MAP_COLS, CELL_SIZE * MAP_ROWS));
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        /* paint map */
        Cell[][] cells = mGrid.getCells();
        for (int x = 0; x < MAP_COLS; x++) {
            for (int y = 0; y < MAP_ROWS; y++) {
                /* draw cells */
                if (Grid.isInStartZone(x, y))
                    g2d.setColor(Color.YELLOW);
                else if (Grid.isInEndZone(x, y))
                    g2d.setColor(Color.BLUE);
                else if (cells[x][y].getExplored()) {
                    if (cells[x][y].getIsObstacle())
                        g2d.setColor(Color.BLACK);
                    else
                        g2d.setColor(Color.WHITE);
                } else {
                    g2d.setColor(Color.LIGHT_GRAY);
                }
                g2d.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                /* draw border */
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        /* paint robot */
        g2d.setColor(Color.MAGENTA);
        g2d.fillOval(mRobot.getPosX() * CELL_SIZE + PAINT_PIXEL_OFFSET,
                mRobot.getPosY() * CELL_SIZE + PAINT_PIXEL_OFFSET,
               CELL_SIZE * ROBOT_SIZE - 2 * PAINT_PIXEL_OFFSET,
                CELL_SIZE * ROBOT_SIZE - 2 * PAINT_PIXEL_OFFSET);

        /* paint robot heading */
        g2d.setColor(Color.WHITE);
        if (mRobot.getHeading() == NORTH) {
            g2d.fillOval((mRobot.getPosX() + 1) * CELL_SIZE + (CELL_SIZE - HEADING_PIXEL_SIZE) / 2,
                    mRobot.getPosY() * CELL_SIZE + PAINT_PIXEL_OFFSET,
                    HEADING_PIXEL_SIZE, HEADING_PIXEL_SIZE);
        } else if (mRobot.getHeading() == SOUTH) {
            g2d.fillOval((mRobot.getPosX() + 1) * CELL_SIZE + (CELL_SIZE - HEADING_PIXEL_SIZE) / 2,
                    (mRobot.getPosY() + 2) * CELL_SIZE + CELL_SIZE - HEADING_PIXEL_SIZE - PAINT_PIXEL_OFFSET,
                    HEADING_PIXEL_SIZE, HEADING_PIXEL_SIZE);
        } else if (mRobot.getHeading() == WEST) {
            g2d.fillOval(mRobot.getPosX() * CELL_SIZE + PAINT_PIXEL_OFFSET,
                    (mRobot.getPosY() + 1) * CELL_SIZE + (CELL_SIZE - HEADING_PIXEL_SIZE) / 2,
                    HEADING_PIXEL_SIZE, HEADING_PIXEL_SIZE);
        } else if (mRobot.getHeading() == EAST) {
            g2d.fillOval((mRobot.getPosX() + 2) * CELL_SIZE + CELL_SIZE - HEADING_PIXEL_SIZE - PAINT_PIXEL_OFFSET,
                    (mRobot.getPosY() + 1) * CELL_SIZE + (CELL_SIZE - HEADING_PIXEL_SIZE) / 2,
                    HEADING_PIXEL_SIZE, HEADING_PIXEL_SIZE);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        //System.out.println("Map updated, repainting");
        this.repaint();
    }
}
