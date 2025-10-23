package ticket.Services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import ticket.entities.Train;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TrainService {
    private List<Train> trainList;
    private ObjectMapper objectMapper;
    private static final String TRAIN_DB_PATH = "src/main/resources/localDb/trains.json";

    public TrainService() throws IOException {
        // Configure ObjectMapper to ignore unknown fields
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        File trainsFile = new File(TRAIN_DB_PATH);

        System.out.println("🔍 Loading trains from: " + trainsFile.getAbsolutePath());
        System.out.println("📁 File exists: " + trainsFile.exists());

        if (!trainsFile.exists()) {
            System.out.println("Creating trains.json file with default data...");
            trainsFile.getParentFile().mkdirs();
            trainsFile.createNewFile();
            createDefaultTrains(trainsFile);
        }

        try {
            trainList = objectMapper.readValue(trainsFile, new TypeReference<List<Train>>() {});
            System.out.println("✅ Successfully loaded " + trainList.size() + " trains");

            // If file was corrupted and we loaded 0 trains, create default data
            if (trainList.isEmpty()) {
                System.out.println("File appears to be corrupted. Creating default trains...");
                createDefaultTrains(trainsFile);
                trainList = objectMapper.readValue(trainsFile, new TypeReference<List<Train>>() {});
            }

        } catch (Exception e) {
            System.out.println("❌ Error loading trains: " + e.getMessage());
            System.out.println("🔄 Creating fresh trains.json file...");
            createDefaultTrains(trainsFile);
            trainList = objectMapper.readValue(trainsFile, new TypeReference<List<Train>>() {});
            System.out.println("✅ Created fresh trains data with " + trainList.size() + " trains");
        }
    }

    public List<Train> searchTrains(String source, String destination) {
        System.out.println("🔍 Searching trains from: '" + source + "' to '" + destination + "'");

        String src = source.trim().toLowerCase();
        String dest = destination.trim().toLowerCase();

        System.out.println("📊 Total trains available: " + trainList.size());

        // Debug: print all trains and their stations
        for (Train train : trainList) {
            System.out.println("🚂 Train: " + train.getTrainId() + " - Stations: " + train.getStations());
        }

        List<Train> result = trainList.stream()
                .filter(train -> validTrain(train, src, dest))
                .collect(Collectors.toList());

        System.out.println("🎯 Found " + result.size() + " trains matching the route");
        return result;
    }

    private boolean validTrain(Train train, String source, String destination) {
        try {
            if (train.getStations() == null || train.getStations().isEmpty()) {
                System.out.println("❌ Train " + train.getTrainId() + " has no stations");
                return false;
            }

            List<String> stationOrderLower = train.getStations().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            System.out.println("🔎 Checking train " + train.getTrainId() + ": " + stationOrderLower);

            int sourceIndex = stationOrderLower.indexOf(source);
            int destinationIndex = stationOrderLower.indexOf(destination);

            System.out.println("   Source '" + source + "' at index: " + sourceIndex);
            System.out.println("   Destination '" + destination + "' at index: " + destinationIndex);

            boolean stationsExist = (sourceIndex != -1 && destinationIndex != -1);
            boolean correctOrder = (sourceIndex < destinationIndex);

            boolean isValid = stationsExist && correctOrder;

            if (isValid) {
                System.out.println("✅ Train " + train.getTrainId() + " is valid for route " + source + " → " + destination);
            } else if (!stationsExist) {
                System.out.println("❌ Train " + train.getTrainId() + " doesn't have both stations");
            } else {
                System.out.println("❌ Train " + train.getTrainId() + " has wrong station order");
            }

            return isValid;
        } catch (Exception e) {
            System.out.println("❌ Error validating train " + train.getTrainId() + ": " + e.getMessage());
            return false;
        }
    }

    public void updateTrain(Train updatedTrain) {
        try {
            System.out.println("🔄 Updating train: " + updatedTrain.getTrainId());

            // Find the train in the list
            Optional<Train> existingTrain = trainList.stream()
                    .filter(train -> train.getTrainId().equals(updatedTrain.getTrainId()))
                    .findFirst();

            if (existingTrain.isPresent()) {
                // Remove the old train
                trainList.remove(existingTrain.get());
                // Add the updated train
                trainList.add(updatedTrain);

                // Save to file
                saveTrainListToFile();
                System.out.println("✅ Successfully updated train: " + updatedTrain.getTrainId());
            } else {
                System.out.println("❌ Train not found for update: " + updatedTrain.getTrainId());
                // Add as new train if not found
                trainList.add(updatedTrain);
                saveTrainListToFile();
                System.out.println("✅ Added new train: " + updatedTrain.getTrainId());
            }
        } catch (Exception e) {
            System.out.println("❌ Error updating train: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public boolean updateSpecificTrainSeat(String trainId, int row, int column, int newStatus) {
        try {
            System.out.println("🔄 Updating train " + trainId + " seat [" + row + "][" + column + "] to " + newStatus);

            // Find the train
            Optional<Train> trainToUpdate = trainList.stream()
                    .filter(train -> train.getTrainId().equals(trainId))
                    .findFirst();

            if (trainToUpdate.isPresent()) {
                Train train = trainToUpdate.get();
                List<List<Integer>> seats = train.getSeats();

                if (row >= 0 && row < seats.size() && column >= 0 && column < seats.get(row).size()) {
                    System.out.println("📊 Before: Seat [" + row + "][" + column + "] = " + seats.get(row).get(column));

                    // Update the seat
                    seats.get(row).set(column, newStatus);
                    train.setSeats(seats);

                    System.out.println("📊 After: Seat [" + row + "][" + column + "] = " + seats.get(row).get(column));

                    // Save to file
                    saveTrainListToFile();
                    System.out.println("✅ Successfully updated seat in database");
                    return true;
                } else {
                    System.out.println("❌ Invalid seat coordinates");
                }
            } else {
                System.out.println("❌ Train not found: " + trainId);
            }
        } catch (Exception e) {
            System.out.println("❌ Error updating seat: " + e.getMessage());
        }
        return false;
    }

    private void saveTrainListToFile() {
        try {
            File trainsFile = new File(TRAIN_DB_PATH);
            objectMapper.writeValue(trainsFile, trainList);
            System.out.println("💾 Saved trains data to file");
        } catch (IOException e) {
            System.out.println("❌ Error saving trains: " + e.getMessage());
        }
    }

    private void createDefaultTrains(File trainsFile) throws IOException {
        List<Train> defaultTrains = new ArrayList<>();

        // Train 1: Guntur to Vijayawada
        Map<String, String> stationTimes1 = new HashMap<>();
        stationTimes1.put("Guntur", "08:00:00");
        stationTimes1.put("Mangalagiri", "08:25:00");
        stationTimes1.put("Vijayawada", "08:45:00");

        List<List<Integer>> seats1 = Arrays.asList(
                Arrays.asList(0, 0, 0, 0, 0, 0),
                Arrays.asList(0, 0, 0, 0, 0, 0),
                Arrays.asList(0, 0, 0, 0, 0, 0),
                Arrays.asList(0, 0, 0, 0, 0, 0)
        );

        Train train1 = new Train("GNT_VZD_001", 11021, seats1, stationTimes1,
                Arrays.asList("Guntur", "Mangalagiri", "Vijayawada"));

        // Train 2: Vijayawada to Guntur
        Map<String, String> stationTimes2 = new HashMap<>();
        stationTimes2.put("Vijayawada", "18:00:00");
        stationTimes2.put("Mangalagiri", "18:20:00");
        stationTimes2.put("Guntur", "18:45:00");

        List<List<Integer>> seats2 = Arrays.asList(
                Arrays.asList(0, 0, 0, 0, 0, 0),
                Arrays.asList(0, 0, 0, 0, 0, 0),
                Arrays.asList(0, 0, 0, 0, 0, 0),
                Arrays.asList(0, 0, 0, 0, 0, 0)
        );

        Train train2 = new Train("VZD_GNT_002", 11022, seats2, stationTimes2,
                Arrays.asList("Vijayawada", "Mangalagiri", "Guntur"));

        defaultTrains.add(train1);
        defaultTrains.add(train2);

        objectMapper.writeValue(trainsFile, defaultTrains);
        System.out.println("✅ Created default trains data with 2 trains");

        // Print what was created for verification
        System.out.println("📋 Created trains:");
        for (Train train : defaultTrains) {
            System.out.println("   - " + train.getTrainId() + ": " + train.getStations());
        }
    }

    public List<Train> getTrainList() {
        return trainList != null ? trainList : new ArrayList<>();
    }
}