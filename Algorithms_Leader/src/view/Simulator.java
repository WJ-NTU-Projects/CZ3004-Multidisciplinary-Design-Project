package view;

import model.entity.Grid;
import model.entity.Robot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Observer;

/**
 * Simulator
 */

public class Simulator extends JFrame {

    // Swing components
    private JPanel mMapPanel;
    private JButton mExplorationButton;
    private JButton mFastestPathButton;
    private JButton mLoadMapButton;
    private JButton mTimeLimitedButton;
    private JButton mCoverageLimitedButton;
    private JButton mRealRunButton;
    private JCheckBox mRealRunCheckBox;
    private JFormattedTextField mRobotSpeedField;
    private JButton mTestButton;
    private JButton mMapDescriptorButton;

    // model
    private Grid mSimulationGrid;
    private Robot mSimulationRobot;

    public Simulator(Grid grid, Robot robot) {
        mSimulationGrid = grid;
        mSimulationRobot = robot;
        initializeUi();
    }

    private void initializeUi() {
        // create components
        mMapPanel = new MapPanel(mSimulationGrid, mSimulationRobot);
        mExplorationButton = new JButton("Exploration");
        mFastestPathButton = new JButton("Fastest path");
        mLoadMapButton = new JButton("Load map");
        mTimeLimitedButton = new JButton("Time limited");
        mCoverageLimitedButton = new JButton("Coverage limited");
        mRealRunButton = new JButton("Physical run");
        mRealRunCheckBox = new JCheckBox("Real run");
        mRealRunCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        mRobotSpeedField = new JFormattedTextField(NumberFormat.getIntegerInstance());
        mRobotSpeedField.setPreferredSize(new Dimension(50, mRobotSpeedField.getHeight()));
        mTestButton = new JButton("Test");
        mMapDescriptorButton = new JButton("generate MD");

        // set up as observer
        mSimulationRobot.addObserver((Observer) mMapPanel);
        mSimulationGrid.addObserver((Observer) mMapPanel);

        // layout components
        JPanel wrapper = new JPanel(new FlowLayout());
        wrapper.add(mMapPanel);
        this.add(wrapper, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.add(mRealRunCheckBox);
        bottomPanel.add(new JLabel("Speed"));
        bottomPanel.add(mRobotSpeedField);
        bottomPanel.add(mRealRunButton);
        bottomPanel.add(mExplorationButton);
        bottomPanel.add(mFastestPathButton);
        bottomPanel.add(mTimeLimitedButton);
        bottomPanel.add(mCoverageLimitedButton);
        bottomPanel.add(mLoadMapButton);
        bottomPanel.add(mTestButton);
        bottomPanel.add(mMapDescriptorButton);
        this.add(bottomPanel, BorderLayout.PAGE_END);

        // set up the frame
        pack();
        setTitle("2020 MDP Group 16 Simulator");
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void addExplorationButtonListener(ActionListener actionListener) {
        mExplorationButton.addActionListener(actionListener);
    }

    public void addFastestPathButtonListener(ActionListener actionListener) {
        mFastestPathButton.addActionListener(actionListener);
    }

    public void addLoadMapButtonListener(ActionListener actionListener) {
        mLoadMapButton.addActionListener(actionListener);
    }

    public void addTimeLimitedButtonListener(ActionListener actionListener) {
        mTimeLimitedButton.addActionListener(actionListener);
    }

    public void addCoverageLimitedButtonListener(ActionListener actionListener) {
        mCoverageLimitedButton.addActionListener(actionListener);
    }

    public void addRealRunCheckBoxListener(ActionListener actionListener) {
        mRealRunCheckBox.addActionListener(actionListener);
    }

    public void addRealRunButtonListener(ActionListener actionListener) {
        mRealRunButton.addActionListener(actionListener);
    }
    
    public void addTestButtonListener(ActionListener actionListener) {
    	mTestButton.addActionListener(actionListener);
    }

    public void addMapDescriptorButtonListener(ActionListener actionListener) {
    	mMapDescriptorButton.addActionListener(actionListener);
    }
    
    public void disableButtons() {
        mExplorationButton.setEnabled(false);
        mFastestPathButton.setEnabled(false);
        mLoadMapButton.setEnabled(false);
        mTimeLimitedButton.setEnabled(false);
        mCoverageLimitedButton.setEnabled(false);
        mTestButton.setEnabled(false);
        mMapDescriptorButton.setEnabled(false);
    }

    public void enableButtons() {
        mExplorationButton.setEnabled(true);
        mFastestPathButton.setEnabled(true);
        mLoadMapButton.setEnabled(true);
        mTimeLimitedButton.setEnabled(true);
        mCoverageLimitedButton.setEnabled(true);
        mTestButton.setEnabled(true);
        mMapDescriptorButton.setEnabled(true);
    }

    public void disableLoadMapButton() {
        mLoadMapButton.setEnabled(false);
    }

    public void enableLoadMapButton() {
        mLoadMapButton.setEnabled(true);
    }

    public void disableRealRunButton() {
        mRealRunButton.setEnabled(false);
    }

    public void enableRealRunButton() {
        mRealRunButton.setEnabled(true);
    }

    public void disableFastestPathButton() {
        mFastestPathButton.setEnabled(false);
    }

    public void enableFastestPathButton() {
        mFastestPathButton.setEnabled(true);
    }

    public void disableExplorationButton() {
        mExplorationButton.setEnabled(false);
    }

    public void enableExplorationButton() {
        mExplorationButton.setEnabled(true);
    }

    public boolean getIsRealRun() {
        return mRealRunCheckBox.isSelected();
    }

    public int getRobotSpeed() {
        return mRobotSpeedField.getText().equals("") ? 0 : Integer.parseInt(mRobotSpeedField.getText());
    }
}
