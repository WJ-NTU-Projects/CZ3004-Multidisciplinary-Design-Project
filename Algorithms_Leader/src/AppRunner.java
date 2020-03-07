import controller.*;
import model.entity.Grid;
import model.entity.Robot;
import model.entity.Sensor;
import model.util.SocketMgr;
import view.Simulator;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static constant.RobotConstants.*;

/**
 * Entry of the application
 */
public class AppRunner {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // models
            Grid grid = new Grid();
            //setObstaclesMap(grid);
            //long range 15 to 60
            //short range 5 to 35
            Sensor sensor1 = new Sensor(3, 2, 0, MIDDLE, 2);
            Sensor sensor2 = new Sensor(3, 1, 0, MIDDLE, 2);
            Sensor sensor3 = new Sensor(3, 0, 0, MIDDLE, 2); 
            Sensor sensor4 = new Sensor(3,  0, 0, LEFT, 2);
            Sensor sensor5 = new Sensor(3, 0, 2, LEFT, 2);
            Sensor sensor6 = new Sensor(6, 2, 1, RIGHT, 1);
            List<Sensor> sensors = new ArrayList<>();
            sensors.add(sensor1);
            sensors.add(sensor2);
            sensors.add(sensor3);
            sensors.add(sensor4);
            sensors.add(sensor5);
            sensors.add(sensor6);
            Robot robot = new Robot(grid, sensors);

            // view
            Simulator simulator = new Simulator(grid, robot);

            // controller
            new CoverageLimitedButtonListener(simulator, grid, robot);
            new ExplorationButtonListener(simulator, grid, robot);
            new FastestPathButtonListener(simulator, grid, robot);
            new LoadMapButtonListener(simulator, grid, robot);
            new TimeLimitedButtonListener(simulator, grid, robot);
            new RealRunButtonListener(simulator, grid, robot);
            new RealRunCheckBoxListener(simulator);
            new TestButtonListener(simulator, grid, robot);
            new MapDescriptorButtonListener(simulator, grid, robot);

            simulator.setVisible(true);
            System.out.println("Simulator started.");
        });
    }
}