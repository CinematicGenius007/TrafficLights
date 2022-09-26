package traffic;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
  static class TrafficLight {
    public final String s;
    public int time;
    public boolean isOpen;
    public TrafficLight(String s, int time, boolean isOpen) {
      this.s = s;
      this.time = time;
      this.isOpen = isOpen;
    }
  }
  static class TempThread extends Thread {
    private volatile boolean keepAlive = true;
    private volatile int systemState = 0;
    private final int roadsLength;
    private final Queue<TrafficLight> roads;
    private final int interval;
    private final LocalDateTime ldt;
    ScheduledExecutorService executorService = null;
    int []systemCalls = {0};

    public TempThread(String name, LocalDateTime ldt, int roads, int interval) {
      super(name);
      this.ldt = ldt;
      this.roadsLength = roads;
      this.roads = new ArrayBlockingQueue<>(roads);
      this.interval = interval;

    }

    @Override
    public void run() {
      int i = 0;
      while (keepAlive) {
        i = (i + 1) % 1000;
        if (systemState == 1 && executorService == null) {
          executorService = Executors.newSingleThreadScheduledExecutor();
          final int[] temp = {0};
          roads.forEach(road -> {
            if (temp[0] == 0) {
              road.isOpen = true;
              road.time = interval;
            } else {
              road.isOpen = false;
              road.time = interval + (temp[0] - 1) * interval;
            }
            temp[0]++;
          });
          for (int t = 0; t <= systemCalls[0]; t++)  // (roads.size() == 2 && t < 1) || (roads.size() == 3 && t < 2) || (roads.size() > 3 && t < (interval + Duration.between(ldt, LocalDateTime.now()).getSeconds()))
            advanceTrafficLights();
          try {
            executorService.scheduleAtFixedRate(() -> {
              try {
                if (System.getProperty("os.name").contains("Windows"))
                  new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                else
                  Runtime.getRuntime().exec("clear");
                System.out.printf("%ds have passed\n", Duration.between(ldt, LocalDateTime.now()).getSeconds());
                System.out.printf("Number of roads: %d\n", roadsLength);
                System.out.printf("Interval: %d\n\n", interval);

                roads.forEach(road -> {
                  System.out.printf("%sm%s %s for %ds.\u001B[0m\n", road.isOpen ? "\u001B[32m" : (road.time == 1 ? "\u001B[33m" : "\u001B[31m"), road.s, road.isOpen ? "open" : "closed", road.time);
                });

                advanceTrafficLights();
                systemCalls[0]++;

                System.out.println("\nenter to open menu:");
              } catch (IOException | InterruptedException ignored) {}
            }, 1, 1, TimeUnit.SECONDS);
          } catch (java.util.concurrent.RejectedExecutionException ignored) {}
        }
      }
    }

    public void shut() {
      this.keepAlive = false;
    }

    public void setOpenSystem() {
      this.systemState = 1;
    }
    public void setCloseSystem() {
      this.systemState = 0;
      if (executorService != null)
        executorService.shutdownNow();
      executorService = null;
    }
    public void addRoad(String name) {
      if (roads.size() == roadsLength) {
        System.out.println("queue is full");
      } else {
        roads.add(new TrafficLight(name, 0, false));
        System.out.printf("%s added\n", name);
      }
    }
    public void deleteRoad() {
      if (roads.isEmpty()) {
        System.out.println("queue is empty");
      } else {
        System.out.printf("%s deleted\n", roads.peek());
        roads.remove();
      }
    }

    private void advanceTrafficLights() {
      roads.forEach(road -> {
        road.time--;
        if (road.time <= 0) {
          if (road.isOpen) {
            road.time = ((roads.size() <= 1 ? 2 : roads.size()) - 1) * interval;
            road.isOpen = roads.size() == 1;
          } else {
            road.time = interval;
            road.isOpen = true;
          }
        }
      });
    }
  }

  public static void main(String[] args) throws InterruptedException {
    Scanner scanner = new Scanner(System.in);
    System.out.println("Welcome to the traffic management system!");
    System.out.print("Input the number of roads: ");
    int roads = getInput(scanner, "incorrect input again ");
    System.out.print("Input the interval: ");
    int interval = getInput(scanner, "Error: Incorrect input. Try again ");
    TempThread t = new TempThread("QueueThread", LocalDateTime.now(), roads, interval);
    t.start();
    int option;
    printMenu();
    do {
      while (true) {
        try {
          option = Integer.parseInt(scanner.nextLine().trim());
          break;
        } catch (NumberFormatException nfe) {
          System.out.println("Incorrect option");
        }
      }
      switch (option) {
        case 1 -> {
          System.out.print("Input for new road: ");
          t.addRoad(scanner.nextLine().trim());
        }
        case 2 -> t.deleteRoad();
        case 3 -> {
          t.setOpenSystem();
          scanner.nextLine();
          t.setCloseSystem();
          printMenu();
        }
        case 0 -> System.out.println("Bye!");
        default -> System.out.println("Incorrect option");
      }
    } while (option != 0);
    t.shut();

  }

  private static void printMenu() {
    System.out.println("""
            Options:
            1. Add road
            2. Delete road
            3. Open system
            0. Quit""");
  }

  private static int getInput(Scanner scanner, String message) {
    int variable;
    while (true) {
      try {
        variable = Integer.parseInt(scanner.nextLine().trim());
        if (variable < 1) {
          throw new NumberFormatException();
        }
        break;
      } catch (NumberFormatException nfe) {
        System.out.print(message);
      }
    }
    return variable;
  }
}
