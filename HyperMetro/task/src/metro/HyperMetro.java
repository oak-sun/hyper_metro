package metro;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class HyperMetro {
    private final String name;
    private final HashMap<String, Station> metroMap;

    public HyperMetro(String name) {
        this.name = name;
        this.metroMap = new HashMap<>();
    }

    public void createStation(String name,
                              int next) {
        metroMap
                .put(name, new Station(name, next));
    }

    public void setNext(String stationName,
                        List<String> nextNames) {
        var nextSet =
                metroMap.get(stationName).getNext();
        nextSet
                .addAll(nextNames.stream()
                .map(metroMap::get)
                .collect(Collectors.toSet()));
    }

    public void setPrev(String stationName,
                        List<String> prevNames) {
        var prevSet =
                metroMap.get(stationName).getPrevious();
        prevSet.addAll(prevNames
                              .stream()
                              .map(metroMap::get)
                              .collect(Collectors.toSet()));
    }

    public void addTransfer(String name,
                            HyperMetro other,
                            String transferTo) {
        var station = metroMap.get(name);
        station
                .getTransfer()
                .add(other.metroMap.get(transferTo));
    }

    private static Optional<ArrayList<Station>> route(HyperMetro metro1,
                                                      String go,
                                                      HyperMetro metro2,
                                                      String stop) {
        class Route {
            final Station station;
            final ArrayList<Station> dirList;

            public Route(Station station,
                         List<Station> dirList) {
                this.station = station;
                this.dirList = new ArrayList<>(dirList);
                this.dirList.add(station);
            }
        }
        ArrayDeque<Route> queue = new ArrayDeque<>();
        HashSet<Station> visited = new HashSet<>();
        var startSt =
                Optional.ofNullable(metro1.metroMap.get(go));
        var endSt =
                Optional.ofNullable(metro2.metroMap.get(stop));
        if (startSt.isPresent() &&
                endSt.isPresent()) {
            queue.add(new Route(
                    startSt.get(),
                    new ArrayList<>()));
            visited.add(startSt.get());
            visited.addAll(startSt.get().getTransfer());
            while (!queue.isEmpty()) {
                var current = queue.pollFirst();
                if (current.station.equals(endSt.get())) {
                    return Optional.of(current.dirList);
                } else {
                    var currentSt = current.station;
                    BiConsumer<HashSet<Station>, List<Station>> addStations =
                            (set, list) ->
                                    set
                                            .stream()
                                            .filter(Predicate.not(visited::contains))
                                            .forEach(s -> {
                                queue.add(new Route(s, list));
                                visited.add(s);
                                visited.addAll(s.getTransfer());
                            });
                    addStations.accept(currentSt.getNext(),
                                       current.dirList);
                    addStations.accept(currentSt.getPrevious(),
                                       current.dirList);
                    if (!currentSt
                            .getTransfer()
                            .isEmpty()) {
                        for (var transfer : currentSt.getTransfer()) {
                            ArrayList<Station> transferList =
                                    new ArrayList<>(current.dirList);
                            transferList.add(transfer);
                            addStations.accept(transfer.getNext(),
                                               transferList);
                            addStations.accept(transfer.getPrevious(),
                                              transferList);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static void printRoute(HyperMetro first,
                                  String start,
                                  HyperMetro second,
                                  String end) {
        var route =
                route(first, start, second, end);
        if (route.isPresent()) {
            printRoute(route.get());
        } else {
            System.out.println("No route exists!");
        }
    }

    private static void printRoute(List<Station> stList) {
        var lineName = stList.get(0).getLineName();
        for (var station : stList) {
            if (!lineName.equals(station.getLineName())) {
                System.out.printf(
                        "Transition to line %s%n",
                        station.getLineName());
                lineName = station.getLineName();
            }
            System.out.println(station.getName());
        }
    }

    private static Optional<Map<Integer, ArrayList<Station>>>
                                                fastestRoute(
                                              HyperMetro metro1,
                                              String go,
                                              HyperMetro metro2,
                                              String stop) {
        class Route {
            final Station station;
            final ArrayList<Station> dirList;
            final int time;

            public Route(Station station,
                         List<Station> dirList,
                         int time) {
                this.station = station;
                this.dirList = new ArrayList<>(dirList);
                this.dirList.add(station);
                this.time = time;
            }
        }
        PriorityQueue<Route> queue = new PriorityQueue<>(
                Comparator.comparingInt((Route r) -> r.time));
        HashSet<Station> visited = new HashSet<>();
        var startSt =
                Optional.ofNullable(metro1.metroMap.get(go));
        var endSt =
                Optional.ofNullable(metro2.metroMap.get(stop));
        if (startSt.isPresent() &&
                endSt.isPresent()) {
            queue.add(new Route(
                    startSt.get(),
                    new ArrayList<>(),
                    0));
            while (!queue.isEmpty()) {
                var current = queue.poll();
                if (visited.contains(current.station)) {
                } else if (current.station.equals(endSt.get())) {
                    return Optional.of(Collections.singletonMap(
                                                  current.time,
                                                  current.dirList));
                } else {
                    var currentSt = current.station;
                    visited.add(currentSt);
                    BiConsumer<HashSet<Station>, Character>
                            addStations = (set, dir) ->
                             set
                            .stream()
                            .filter(Predicate.not(visited::contains))
                            .forEach(s -> queue.add(
                                    new Route(s, current.dirList,
                                            current.time +
                                            (dir == 't' ?
                                                    currentSt.getTransferTime() :
                                                    (dir == 'n' ?
                                                            currentSt.getNextTime() :
                                                            s.getNextTime())))));
                    addStations.accept(currentSt.getNext(), 'n');
                    addStations.accept(currentSt.getPrevious(), 'p');
                    addStations.accept(currentSt.getTransfer(), 't');
                }
            }
        }
        return Optional.empty();
    }

    public static void printFastestRoute(HyperMetro metro1,
                                         String go,
                                         HyperMetro metro2,
                                         String stop) {
        var route =
                fastestRoute(metro1, go, metro2, stop);
        if (route.isPresent()) {
            Map<Integer, ArrayList<Station>> solutionMap =
                    route.get();
            solutionMap.forEach((k, v) ->
            {
                printRoute(v);
                System.out.printf(
                        "Total: %d minutes in the way%n", k);
            });
        } else {
            System.out.println("No route exists!");
        }
    }

    @Getter
    @Setter
    private class Station {
        private final String name;
        private final HashSet<Station> previous;
        private final HashSet<Station> next;
        private final int nextTime;
        private final HashSet<Station> transfer;

        private Station(String name, int nextTime) {
            this.name = name;
            this.previous = new HashSet<>();
            this.next = new HashSet<>();
            this.transfer = new HashSet<>();
            this.nextTime = nextTime;
        }
        public String getLineName() {
            return HyperMetro.this.name;
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
                return this
                        .name.equals(otherSt.name)
                        && HyperMetro.this
                        .name.equals(otherSt.getLineName());
            }
        }
        public int getTransferTime() {
            return 5;
        }
    }
}