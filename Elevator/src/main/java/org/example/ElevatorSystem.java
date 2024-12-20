package org.example;

import javax.swing.*;

public class ElevatorSystem {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ElevatorSystem::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Elevator Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 800);

        ElevatorManager manager = new ElevatorManager(6, 2);
        ElevatorPanel panel = new ElevatorPanel(manager);
        frame.add(panel);

        frame.setVisible(true);

        Thread generatorThread = new Thread(new RequestGenerator(manager));
        generatorThread.start();

        manager.startElevatorThreads();
    }
}
