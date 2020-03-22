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
            Grid grid = new Grid();
            Sensor sensor1 = new Sensor(1, 2, 0, MIDDLE, 3);
            Sensor sensor2 = new Sensor(1, 1, 0, MIDDLE, 3);
            Sensor sensor3 = new Sensor(1, 0, 0, MIDDLE, 3); 
            Sensor sensor4 = new Sensor(1, 0, 0, LEFT, 3);
            Sensor sensor5 = new Sensor(1, 0, 2, LEFT, 3);
            Sensor sensor6 = new Sensor(5, 2, 1, RIGHT, 1);
            List<Sensor> sensors = new ArrayList<>();
            sensors.add(sensor1);
            sensors.add(sensor2);
            sensors.add(sensor3);
            sensors.add(sensor4);
            sensors.add(sensor5);
            sensors.add(sensor6);
            Robot robot = new Robot(grid, sensors);
            Simulator simulator = new Simulator(grid, robot);
            new RealRunButtonListener(simulator, grid, robot);
            new RealRunCheckBoxListener(simulator);
            simulator.setVisible(true);
            System.out.println("Simulator started.");
        });
    }
}