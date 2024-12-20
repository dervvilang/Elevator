package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class ElevatorPanel extends JPanel {
    private final ElevatorManager manager;

    public ElevatorPanel(ElevatorManager manager) {
        this.manager = manager;
        javax.swing.Timer timer = new javax.swing.Timer(100, e -> repaint());
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int elevatorWidth = 60;
        int elevatorHeight = 80;
        int spacing = 200;
        int floorHeight = (getHeight() - 100) / 6;
        int baseOffset = 50;

        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 6; i++) {
            int y = getHeight() - baseOffset - (i + 1) * floorHeight;
            g.drawLine(0, y, getWidth(), y);
            g.drawString("Этаж " + (i + 1), 10, y - 5);
        }

        g.setColor(Color.RED);
        synchronized (manager) {
            for (Map.Entry<Integer, Integer> entry : manager.getFloorRequests().entrySet()) {
                int floor = entry.getKey();
                int count = entry.getValue();
                int passengerX = spacing / 2;
                int passengerY = getHeight() - baseOffset - (floor * floorHeight) - 20;

                for (int i = 0; i < count; i++) {
                    g.fillPolygon(
                            new int[]{passengerX + i * 15, passengerX - 10 + i * 15, passengerX + 10 + i * 15},
                            new int[]{passengerY, passengerY + 20, passengerY + 20},
                            3
                    );
                }
            }
        }

        for (int i = 0; i < manager.getElevators().size(); i++) {
            Elevator elevator = manager.getElevators().get(i);

            int x = (i + 1) * spacing - 100;
            int y = getHeight() - baseOffset - (elevator.getCurrentFloor() * floorHeight) - elevatorHeight;

            g.setColor(new Color(139, 69, 19));
            g.fillRect(x, y, elevatorWidth, elevatorHeight);

            g.setColor(Color.WHITE);
            g.drawString("Лифт " + elevator.getId(), x + 10, y + elevatorHeight / 2);
        }
    }
}
