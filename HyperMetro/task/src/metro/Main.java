package metro;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
                var br = Files
                        .newBufferedReader(file);
                var json = JsonParser
                        .parseReader(br).getAsJsonObject();
                LinkedHashMap<String, HyperMetro> metroMap =
                        new LinkedHashMap<>();
                ArrayList<String[]> transfersList = new ArrayList<>();
                for (var obj: json.entrySet()) {
                    var lineName = obj
                            .getKey()
                            .replace("\"", "");
                    var metro = new HyperMetro(lineName);
                    for (var js : obj
                            .getValue()
                            .getAsJsonObject()
                            .entrySet()) {
                        var stationName = js
                                .getValue()
                                .getAsJsonObject()
                                .getAsJsonPrimitive("name")
                                .getAsString()
                                .replace("\"", "");
                        var timeEntry = js
                                .getValue()
                                .getAsJsonObject()
                                .get("time");
                        int time;
                        if (timeEntry == null || timeEntry.isJsonNull()) {
                            time = 0;
                        } else {
                            time = timeEntry.getAsInt();
                        }
                        metro.append(stationName, time);
                        var transfer = js
                                .getValue()
                                .getAsJsonObject()
                                .getAsJsonArray("transfer");
                        if (transfer.size() != 0) {
                            var transferLine = transfer
                                    .get(0)
                                    .getAsJsonObject()
                                    .get("line")
                                    .getAsString()
                                    .replace("\"", "");
                            var transferSt = transfer
                                    .get(0)
                                    .getAsJsonObject()
                                    .get("station")
                                    .getAsString()
                                    .replace("\"", "");
                            transfersList.add(new String[]{
                                    lineName,
                                    stationName,
                                    transferLine,
                                    transferSt});
                        }
                    }
                    metroMap.put(lineName, metro);

                }

                for (String[] data : transfersList) {
                    metroMap.get(data[0]).addTransfer(
                                    data[1],
                                    metroMap.get(data[2]),
                                    data[3]);
                }
                return Optional.of(metroMap);
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
                    parseList.add(
                            m.group(1).replace("\"", ""));
                var commands = parseList
                        .toArray(String[]::new);
                if (commands.length == 2 &&
                        "/output".equals(commands[0])) {
                    if (metroMap
                            .containsKey(commands[1])) {
                        metroMap
                                .get(commands[1]).printLine();
                    } else {
                        System.out.println(
                                "Invalid command.");
                    }
                } else if (commands.length == 3 &&
                        metroMap.containsKey(commands[1])) {
                    var metro =
                            metroMap.get(commands[1]);
                    switch (commands[0]) {
                        case "/append" ->
                                metro.append(commands[2],
                                        0);
                        case "/add-head" ->
                                metro.addHead(commands[2],
                                        0);
                        case "/remove" -> {
                            if (!metro.remove(commands[2])) {
                                System.out.println(
                                        "Invalid command.");
                            }
                        }
                        default ->
                                System.out.println(
                                        "Invalid command.");
                    }
                } else if (commands.length == 4 &&
                        metroMap.containsKey(commands[1]) &&
                        "/add".equals(commands[0])){
                    var metro =
                            metroMap.get(commands[1]);
                    metro.append(
                            commands[2],
                            Integer.parseInt(commands[3]));
                }
                else if (commands.length == 5 &&
                        ("/connect".equals(commands[0])
                        || "/route".equals(commands[0])
                        || "/fastest-route".equals(commands[0]))
                        && metroMap.containsKey(commands[1])
                        && metroMap.containsKey(commands[3])) {
                    var metro1 =
                            metroMap.get(commands[1]);
                    var metro2 =
                            metroMap.get(commands[3]);
                    if ("/connect".equals(commands[0])) {
                        metro1.addTransfer(
                                commands[2],
                                metro2,
                                commands[4]);
                        metro2.addTransfer(
                                commands[4],
                                metro1,
                                commands[2]);
                    }
                    else if ("/route".equals(commands[0])) {
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