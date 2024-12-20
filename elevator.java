package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

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

class ElevatorManager {
    private final int totalFloors;
    private final List<Elevator> elevators;
    private final Map<Integer, Integer> floorRequests;

    public ElevatorManager(int totalFloors, int numElevators) {
        this.totalFloors = totalFloors;
        this.floorRequests = new ConcurrentHashMap<>();
        this.elevators = new ArrayList<>();

        for (int i = 0; i < numElevators; i++) {
            elevators.add(new Elevator(i + 1, totalFloors, this));
        }
    }

    public synchronized void addRequest(int floor) {
        floorRequests.merge(floor, 1, Integer::sum);
        Elevator bestElevator = findBestElevator(floor);
        bestElevator.addStop(floor);
    }

    private Elevator findBestElevator(int floor) {
        return elevators.stream()
                .min(Comparator.comparingInt(elevator -> elevator.calculateCost(floor)))
                .orElseThrow();
    }

    public synchronized void removeRequest(int floor) {
        floorRequests.computeIfPresent(floor, (key, value) -> value > 1 ? value - 1 : null);
    }

    public void startElevatorThreads() {
        for (Elevator elevator : elevators) {
            new Thread(elevator).start();
        }
    }

    public List<Elevator> getElevators() {
        return elevators;
    }

    public Map<Integer, Integer> getFloorRequests() {
        return floorRequests;
    }

    public int getTotalFloors() {
        return totalFloors;
    }
}

class Elevator implements Runnable {
    private final int id;
    private final int totalFloors;
    private final PriorityQueue<Integer> stops;
    private final ElevatorManager manager;
    private int currentFloor;
    private boolean movingUp;

    public Elevator(int id, int totalFloors, ElevatorManager manager) {
        this.id = id;
        this.totalFloors = totalFloors;
        this.manager = manager;
        this.currentFloor = 1;
        this.movingUp = true;
        this.stops = new PriorityQueue<>(Comparator.comparingInt(f -> Math.abs(f - currentFloor)));
    }

    public synchronized void addStop(int floor) {
        stops.add(floor);
    }

    public int calculateCost(int floor) {
        if (stops.isEmpty()) {
            return Math.abs(currentFloor - floor);
        }
        int directionMultiplier = (movingUp && floor > currentFloor) || (!movingUp && floor < currentFloor) ? 0 : 2;
        return Math.abs(currentFloor - floor) + directionMultiplier;
    }

    @Override
    public void run() {
        while (true) {
            try {
                move();

                synchronized (this) {
                    if (stops.contains(currentFloor)) {
                        System.out.println("Лифт " + id + " остановился на " + currentFloor + " этаже и забрал пассажира.");
                        stops.remove(currentFloor);
                        manager.removeRequest(currentFloor);
                        Thread.sleep(1000);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private synchronized void move() throws InterruptedException {
        if (stops.isEmpty()) {
            return;
        }

        Integer targetFloor = stops.peek();
        if (targetFloor > currentFloor) {
            currentFloor++;
            movingUp = true;
        } else if (targetFloor < currentFloor) {
            currentFloor--;
            movingUp = false;
        }
        System.out.println("Лифт " + id + " переместился на " + currentFloor + " этаж.");
        Thread.sleep(500);
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public int getId() {
        return id;
    }
}

class RequestGenerator implements Runnable {
    private final ElevatorManager manager;

    public RequestGenerator(ElevatorManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        Random random = new Random();
        while (true) {
            try {
                int floor = random.nextInt(manager.getTotalFloors()) + 1;
                System.out.println("Запрос на вызов лифта на этаж " + floor);
                manager.addRequest(floor);
                Thread.sleep(random.nextInt(5000) + 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

class ElevatorPanel extends JPanel {
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
