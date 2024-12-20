package org.example;

import java.util.*;
import java.util.concurrent.*;

public class ElevatorManager {
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
