package metro;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;

@Getter
public class HyperMetro {
    private final String name;
    private final Station depot;

    public HyperMetro(String name) {
        this.name = name;
        this.depot = new Station("depot");
        this.depot.setNext(this.depot);
        this.depot.setPrevious(this.depot);
    }

    public void append(String name,
                       int time) {
        var prev =
                this.depot.getPrevious();
        var newSt = new Station(name);
        prev.setNext(newSt);
        this.depot.setPrevious(newSt);
        this.depot.setPreviousTime(time);
        newSt.setPrevious(prev);
        newSt.setPreviousTime(prev.getNextTime());
        newSt.setNext(depot);
        newSt.setNextTime(time);
    }

    public void addHead(String name,
                        int time) {
        var next = this.depot.getNext();
        var newSt = new Station(name);
        next.setPrevious(newSt);
        next.setPreviousTime(time);
        this.depot.setNext(newSt);
        newSt.setPrevious(this.depot);
        newSt.setNext(next);
        newSt.setNextTime(time);
    }

    public boolean remove(String name) {
        var candidate =
                getStation(name);
        if (candidate.isPresent()) {
            var prev =
                    candidate.get().getPrevious();
            var next =
                    candidate.get().getNext();
            prev.setNext(next);
            next.setPrevious(prev);
            next.setPreviousTime(prev.getNextTime());
            candidate
                    .get()
                    .getTransfer()
                    .ifPresent(s -> s.setTransfer(null));
            return true;
        } else {
            return false;
        }
    }

    public void addTransfer(String name,
                            HyperMetro metro,
                            String transfer) {
        var candidate =
                getStation(name);
        var other =
                metro.getStation(transfer);
        if (candidate.isPresent() &&
                other.isPresent()) {
            var station = candidate.get();
            var transferSt = other.get();
            station.setTransfer(transferSt);
        }
    }

    public void printLine() {
        var current = this.depot;
        System.out.println(current.getName());
        if (current.getNext() == this.depot) {
            return;
        }
        current = current.getNext();
        while (current != this.depot) {
            if (current.getTransfer().isPresent()) {
                System.out.printf(
                        "%s - %s - (%s)%n",
                        current.getName(),
                        current
                                .getTransfer()
                                .get()
                                .getName(),
                        current
                                .getTransfer()
                                .get()
                                .getLineName());
            } else {
                System.out.println(current.getName());
            }
            current = current.getNext();
        }
        System.out.println(current.getName());
    }

    private static Optional<ArrayList<Direction>>
                                              route
                                        (HyperMetro metro1,
                                        String go,
                                        HyperMetro metro2,
                                        String stop) {
        class Route {
            final Station station;
            final ArrayList<Direction> dirList;
            public Route(Station station,
                         ArrayList<Direction> dirList,
                         Direction direction) {
                this.station = station;
                this.dirList = new ArrayList<>(dirList);
                if (direction != null)
                    this.dirList.add(direction);
            }
        }
        ArrayDeque<Route> queue = new ArrayDeque<>();
        HashSet<Station> visited = new HashSet<>();
        var startSt =
                metro1.getStation(go);
        var endSt =
                metro2.getStation(stop);
        if (startSt.isPresent() && endSt.isPresent()) {
            queue.add(new Route(
                    startSt.get(),
                    new ArrayList<>(),
                    null));
            visited.add(startSt.get());
            startSt
                    .get()
                    .getTransfer()
                    .ifPresent(visited::add);
            while (!queue.isEmpty()) {
                var current = queue.pollFirst();
                if (current.station.equals(endSt.get())) {
                    return Optional.of(current.dirList);
                } else {
                    var currentSt =
                            current.station;
                    if (!currentSt.getName().equals("depot")) { //depots are sinks on the graph
                        if (!visited
                                .contains(currentSt.getNext())) {
                            queue.add(new Route(
                                    currentSt.getNext(),
                                    current.dirList,
                                    Direction.Next));
                            visited.add(currentSt.getNext());
                            currentSt
                                    .getTransfer()
                                    .ifPresent(visited::add);
                        }
                        if (!visited.contains(
                                currentSt.getPrevious())) {
                            queue.add(new Route(
                                    currentSt.getPrevious(),
                                    current.dirList,
                                    Direction.Previous));
                            visited.add(currentSt.getPrevious());
                            currentSt
                                    .getTransfer()
                                    .ifPresent(visited::add);
                        }
                        if (currentSt
                                .getTransfer()
                                .isPresent()) {
                            currentSt = currentSt
                                    .getTransfer()
                                    .get();
                            ArrayList<Direction> transferList =
                                    new ArrayList<>(current.dirList);
                            transferList.add(Direction.Transfer);
                            if (!visited.contains(currentSt.getNext())) {
                                queue.add(new Route(
                                        currentSt.getNext(),
                                        transferList,
                                        Direction.Next));
                                visited.add(currentSt.getNext());
                                currentSt
                                        .getTransfer()
                                        .ifPresent(visited::add);
                            }
                            if (!visited.contains(currentSt.getPrevious())) {
                                queue.add(new Route(
                                        currentSt.getPrevious(),
                                        transferList,
                                        Direction.Previous));
                                visited.add(currentSt.getPrevious());
                                currentSt
                                        .getTransfer()
                                        .ifPresent(visited::add);
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static void printRoute(HyperMetro metro1,
                                  String go,
                                  HyperMetro metro2,
                                  String stop) {
        var route = route(
                metro1,
                go,
                metro2,
                stop);
        if (route.isPresent()) {
            var current =
                    metro1.getStation(go).get();
            for (var direction : route.get()) {
                System.out.println(current.getName());
                switch (direction) {
                    case Next ->
                            current = current.getNext();
                    case Previous ->
                            current = current.getPrevious();
                    case Transfer ->
                    {
                        current = current.getTransfer().get();
                        System.out.printf(
                                "Transition to line %s%n",
                                current.getLineName());
                    }
                }
            }
            System.out.println(current.getName());
        } else {
            System.out.println("No route exists!");
        }
    }
    private static Optional<ArrayList<Direction>>
                                            fastestRoute
                                           (HyperMetro metro1,
                                           String go,
                                           HyperMetro metro2,
                                           String stop) {

        class Route {
            final Station station;
            final ArrayList<Direction> dirList;
            final int time;

            public Route(Station station,
                         ArrayList<Direction> dirList,
                         Direction direction,
                         int time) {
                this.station = station;
                this.dirList = new ArrayList<>(dirList);

                if (direction != null)
                    this.dirList.add(direction);
                this.time = time;
            }
        }
        PriorityQueue<Route> queue =
                new PriorityQueue<>(
                        Comparator.comparingInt(
                                (Route r) -> r.time));
        HashSet<Station> visited = new HashSet<>();
        var startSt =
                metro1.getStation(go);
        var endSt =
                metro2.getStation(stop);
        if (startSt.isPresent() &&
                endSt.isPresent()) {
            queue.add(new Route(
                    startSt.get(),
                    new ArrayList<>(),
                    null,
                    0));
            while (!queue.isEmpty()) {
                Route current = queue.poll();
                if (visited.contains(current.station)) {
                } else if
                (current.station.equals(endSt.get())) {
                    return Optional.of(current.dirList);
                } else {
                    var currentSt =
                            current.station;
                    visited.add(currentSt);
                    if (!currentSt
                            .getName()
                            .equals("depot")) {
                        if (!visited.contains(currentSt.getNext())) {
                            queue.add(new Route(
                                    currentSt.getNext(),
                                    current.dirList,
                                    Direction.Next,
                                    current.time +
                                            currentSt.getNextTime()));
                        }
                        if (!visited.contains(
                                currentSt.getPrevious())) {
                            queue.add(new Route(
                                    currentSt.getPrevious(),
                                    current.dirList,
                                    Direction.Previous,
                                    current.time +
                                    currentSt.getPreviousTime()));
                        }
                        if (currentSt
                                .getTransfer()
                                .isPresent() &&
                                !visited.contains(currentSt
                                                .getTransfer()
                                                .get())) {
                            queue.add(new Route(
                                    currentSt.getTransfer().get(),
                                    current.dirList,
                                    Direction.Transfer,
                                    current.time +
                                    currentSt.getTransferTime()));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static void printFastestRoute(HyperMetro metro1,
                                         String go,
                                         HyperMetro metro2,
                                         String stop) {
        var route = fastestRoute(
                        metro1,
                        go,
                        metro2,
                        stop);
        if (route.isPresent()) {
            var current =
                    metro1.getStation(go).get();
            int ended = 0;
            for (var direction : route.get()) {
                System.out.println(current.getName());
                switch (direction) {
                    case Next ->
                    {
                        ended += current.getNextTime();
                        current = current.getNext();
                    }
                    case Previous ->
                    {
                        ended += current.getPreviousTime();
                        current = current.getPrevious();
                    }
                    case Transfer ->
                    {
                        ended += current.getTransferTime();
                        current = current.getTransfer().get();
                        System.out.printf(
                                "Transition to line %s%n",
                                current.getLineName());
                    }
                }
            }
            System.out.println(current.getName());
            System.out.printf(
                    "Total: %d minutes in the way%n",
                    ended);
        } else {
            System.out.println("No route exists!");
        }
    }

    private Optional<Station> getStation(String name) {
        var currentSt = this.depot.getNext();
        while (currentSt != this.depot) {
            if (currentSt.getName().equals(name)) {
                return Optional.of(currentSt);
            }
            currentSt = currentSt.getNext();
        }
        return Optional.empty();
    }

    private enum Direction {Next, Previous, Transfer}

    @Getter
    @Setter
    private class Station {
        private final String name;
        private Station previous;
        private int previousTime;
        private Station next;
        private int nextTime;
        private Station transfer;
        private final int transferTime = 5;

        private Station(String name) {
            this.name = name;
            this.previous = null;
            this.next = null;
            this.transfer = null;
        }

        public String getLineName() {
            return HyperMetro.this.name;
        }

        public Optional<Station> getTransfer() {
            return Optional.ofNullable(this.transfer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(HyperMetro.this.name,
                    this.name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null) {
                return false;
            } else if
            (this.getClass() != o.getClass()) {
                return false;
            } else {
                var otherSt = (Station) o;
                return this.name.equals(otherSt.name)
                        &&
                        HyperMetro.this.name.equals(
                                otherSt.getLineName());
            }
        }
    }
}