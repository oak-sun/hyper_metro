package metro;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        loadMap(args[0]).ifPresent(Main::parse);
    }
    private static Optional<LinkedHashMap<String, HyperMetro>>
                                              loadMap(String path) {
        var file = Paths.get(path);
        if (Files.notExists(file)) {
            System.out.println(
                    "Error! Such a file doesn't exist!");
            return Optional.empty();
        } else {
            try {
                var br =
                        Files.newBufferedReader(file);
                var json =
                        JsonParser
                                .parseReader(br)
                                .getAsJsonObject();
                LinkedHashMap<String, HyperMetro> lineMap =
                        new LinkedHashMap<>();
                ArrayList<String[]> transList = new ArrayList<>();
                for (var obj: json.entrySet()) {
                    var lineName = obj
                                        .getKey()
                                        .replace("\"", "");
                    var metro = new HyperMetro(lineName);
                    ArrayList<Map<String, List<String>>> nextMapList =
                            new ArrayList<>();
                    ArrayList<Map<String, List<String>>> prevMapList =
                            new ArrayList<>();
                    for (var entObj : obj
                                       .getValue()
                                       .getAsJsonArray()) {
                        var e =
                                entObj.getAsJsonObject();
                        var stationName = e
                                .getAsJsonPrimitive("name")
                                .getAsString()
                                .replace("\"", "");
                        var timeEntry = e
                                .getAsJsonPrimitive("time");
                        var time = timeEntry == null ?
                                0 : timeEntry.getAsInt();
                        metro.createStation(stationName, time);
                        ArrayList<String> nextNames = new ArrayList<>();
                        for (var nextSt : e.getAsJsonArray("next")) {
                            nextNames.add(nextSt.getAsString());
                        }
                        nextMapList.add(Collections.singletonMap(
                                stationName, nextNames));
                        ArrayList<String> prevNames = new ArrayList<>();
                        for (var prevSt : e.getAsJsonArray("prev")) {
                            prevNames.add(prevSt.getAsString());
                        }
                        prevMapList.add(Collections.singletonMap(
                                stationName, prevNames));
                        var transfer = e
                                .getAsJsonArray("transfer");
                        for (var el : transfer) {
                            var transferLine = el
                                    .getAsJsonObject()
                                    .get("line")
                                    .getAsString()
                                    .replace("\"", "");
                            var transferSt = el
                                    .getAsJsonObject()
                                    .get("station")
                                    .getAsString()
                                    .replace("\"", "");
                            transList.add(new String[]{
                                    lineName,
                                    stationName,
                                    transferLine,
                                    transferSt});
                        }
                    }
                    for (var map : nextMapList) {
                        map.forEach(metro::setNext);
                    }
                    for (var map : prevMapList) {
                        map.forEach(metro::setPrev);
                    }
                    lineMap.put(lineName, metro);
                }
                for (String[] data : transList) {
                    lineMap
                            .get(data[0])
                            .addTransfer(
                                    data[1],
                                    lineMap.get(data[2]),
                                    data[3]);
                }
                return Optional.of(lineMap);
            } catch (IOException e) {
                System.out.println("Incorrect file.");
                return Optional.empty();
            }
        }
    }

    private static void parse(LinkedHashMap<String, HyperMetro> metroMap) {
        var sc = new Scanner(System.in);
        while (true) {
            var input = sc.nextLine();
            if ("/exit".equals(input)) {
                break;
            } else {
                List<String> parseList = new ArrayList<>();
                var m = Pattern
                        .compile("([^\"]\\S*|\".+?\")\\s*")
                        .matcher(input);
                while (m.find())
                    parseList
                            .add(m.group(1)
                            .replace("\"", ""));
                var commands = parseList.toArray(String[]::new);
                if (commands.length == 5 &&
                        ("/route".equals(commands[0])
                        || "/fastest-route".equals(commands[0]))
                        && metroMap.containsKey(commands[1])
                        && metroMap.containsKey(commands[3])) {
                    var metro1 = metroMap.get(commands[1]);
                    var metro2 = metroMap.get(commands[3]);
                    if ("/route".equals(commands[0])) {
                        HyperMetro.printRoute(
                                metro1,
                                commands[2],
                                metro2,
                                commands[4]);
                    } else {
                        HyperMetro.printFastestRoute(
                                metro1,
                                commands[2],
                                metro2,
                                commands[4]);
                    }
                } else {
                    System.out.println("Invalid command.");
                }
            }
        }
    }
}