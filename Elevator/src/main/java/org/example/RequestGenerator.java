package org.example;

import java.util.*;

public class RequestGenerator implements Runnable {
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
                Thread.sleep(random.nextInt(2500) + 400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
