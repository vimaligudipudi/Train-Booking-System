package ticket.Services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ticket.entities.Train;
import ticket.entities.Ticket;
import ticket.entities.User;
import util.UserServiceUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserBookingService {
    private User user;
    private List<User> userList;
    private ObjectMapper objectMapper = new ObjectMapper();
    private static final String USERS_PATH = "src/main/resources/localDb/users.json";

    // Constructor for logged-in user
    public UserBookingService(User user1) throws IOException {
        loadUsers(); // Load users first

        this.user = user1;

        // Try to find the user in the database and verify credentials
        Optional<User> existingUser = userList.stream()
                .filter(u -> u.getName().equals(user1.getName()))
                .findFirst();

        if (existingUser.isPresent()) {
            User dbUser = existingUser.get();
            // Check password against the stored hashed password
            if (UserServiceUtil.checkPassword(user1.getPassword(), dbUser.getHashedPassword())) {
                // Login successful - use the user from database (with correct UUID)
                this.user = dbUser;
                System.out.println("✅ Login successful for: " + user.getName());
            } else {
                System.out.println("❌ Login failed: Incorrect password for user: " + user1.getName());
                this.user = null;
            }
        } else {
            System.out.println("❌ Login failed: User not found: " + user1.getName());
            this.user = null;
        }
    }

    // Constructor for non-logged-in user
    public UserBookingService() throws IOException {
        loadUsers();
    }

    public List<User> loadUsers() throws IOException {
        File usersFile = new File(USERS_PATH);

        System.out.println("📁 Loading users from: " + usersFile.getAbsolutePath());

        if (!usersFile.exists()) {
            System.out.println("Creating users.json file...");
            usersFile.getParentFile().mkdirs();
            usersFile.createNewFile();
            objectMapper.writeValue(usersFile, new ArrayList<User>());
            return new ArrayList<>();
        }

        if (usersFile.length() == 0) {
            objectMapper.writeValue(usersFile, new ArrayList<User>());
            return new ArrayList<>();
        }

        try {
            // Configure ObjectMapper to ignore unknown properties
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            List<User> usersList = objectMapper.readValue(usersFile, new TypeReference<List<User>>() {});
            this.userList = usersList;
            System.out.println("✅ Loaded " + usersList.size() + " users");
            return usersList;
        } catch (Exception e) {
            System.out.println("❌ Error loading users: " + e.getMessage());
            System.out.println("🔄 Creating fresh users.json file...");

            // Create fresh file
            objectMapper.writeValue(usersFile, new ArrayList<User>());

            List<User> emptyList = new ArrayList<>();
            this.userList = emptyList;
            return emptyList;
        }
    }

    public Boolean loginUser() {
        return user != null;
    }

    public Boolean signUp(User user1) {
        try {
            // Check if user already exists
            boolean userExists = userList.stream()
                    .anyMatch(u -> u.getName().equals(user1.getName()));

            if (userExists) {
                System.out.println("❌ User already exists: " + user1.getName());
                return false;
            }

            userList.add(user1);
            saveUserListToFile();
            System.out.println("✅ User registered: " + user1.getName());

            // Auto-login after signup
            this.user = user1;
            return true;
        } catch (IOException ex) {
            System.out.println("❌ Error during signup: " + ex.getMessage());
            return false;
        }
    }

    private void saveUserListToFile() throws IOException {
        File usersFile = new File(USERS_PATH);
        objectMapper.writeValue(usersFile, userList);
    }

    public void fetchBookings() {
        try {
            if (user == null) {
                System.out.println("❌ No user logged in. Please login first.");
                return;
            }

            // Always use the user from our current state
            System.out.println("\n=== Bookings for " + user.getName() + " ===");
            user.printTickets();

        } catch (Exception e) {
            System.out.println("❌ Error fetching bookings: " + e.getMessage());
        }
    }

    public Boolean cancelBooking(String ticketId) {
        if (user == null) {
            System.out.println("❌ No user logged in");
            return false;
        }

        if (ticketId == null || ticketId.isEmpty()) {
            System.out.println("❌ Ticket ID cannot be empty");
            return false;
        }

        // Find the ticket to cancel
        Optional<Ticket> ticketToCancel = user.getTicketsBooked().stream()
                .filter(ticket -> ticket.getTicketId().equals(ticketId))
                .findFirst();

        if (ticketToCancel.isPresent()) {
            Ticket ticket = ticketToCancel.get();
            String trainId = ticket.getTrain().getTrainId();
            int seatRow = ticket.getSeatRow();
            int seatColumn = ticket.getSeatColumn();

            System.out.println("🎫 Canceling ticket: " + ticketId);
            System.out.println("🚂 Train: " + trainId);
            System.out.println("💺 Seat: Row " + seatRow + ", Column " + seatColumn);

            try {
                // STEP 1: Update the train database FIRST
                TrainService trainService = new TrainService();
                boolean seatUpdated = trainService.updateSpecificTrainSeat(trainId, seatRow, seatColumn, 0);

                if (!seatUpdated) {
                    System.out.println("❌ Failed to update seat in database");
                    return false;
                }

                // STEP 2: Remove the ticket from user's bookings
                boolean removed = user.getTicketsBooked().removeIf(t -> t.getTicketId().equals(ticketId));

                if (removed) {
                    // STEP 3: Update the user in the userList and save
                    Optional<User> userInList = userList.stream()
                            .filter(u -> u.getUserId().equals(user.getUserId()))
                            .findFirst();

                    if (userInList.isPresent()) {
                        userInList.get().setTicketsBooked(user.getTicketsBooked());
                        saveUserListToFile();

                        System.out.println("✅ SUCCESS: Ticket canceled and seat freed in database!");
                        return true;
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Error during cancellation: " + e.getMessage());
                return false;
            }
        } else {
            System.out.println("❌ No ticket found with ID " + ticketId);
        }
        return false;
    }

    public List<Train> getTrains(String source, String destination) {
        try {
            TrainService trainService = new TrainService();
            return trainService.searchTrains(source, destination);
        } catch (IOException ex) {
            System.out.println("❌ Error searching trains: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    public List<List<Integer>> fetchSeats(Train train) {
        if (train == null) {
            System.out.println("❌ No train selected");
            return new ArrayList<>();
        }

        try {
            // Always get FRESH data from the database, not the cached train object
            TrainService trainService = new TrainService();
            List<Train> currentTrains = trainService.getTrainList();

            // Find the current train with updated seat data
            Optional<Train> currentTrain = currentTrains.stream()
                    .filter(t -> t.getTrainId().equals(train.getTrainId()))
                    .findFirst();

            if (currentTrain.isPresent()) {
                System.out.println("🔄 Fetching updated seat data for: " + train.getTrainId());
                return currentTrain.get().getSeats();
            } else {
                System.out.println("❌ Train not found in database");
                return train.getSeats(); // fallback to old data
            }
        } catch (IOException e) {
            System.out.println("❌ Error fetching updated seats: " + e.getMessage());
            return train.getSeats(); // fallback to old data
        }
    }

    public Boolean bookTrainSeat(Train train, int row, int column) {
        try {
            if (user == null) {
                System.out.println("❌ Please login first!");
                return false;
            }

            TrainService trainService = new TrainService();

            // Get FRESH train data from database, not the cached object
            List<Train> currentTrains = trainService.getTrainList();
            Optional<Train> currentTrain = currentTrains.stream()
                    .filter(t -> t.getTrainId().equals(train.getTrainId()))
                    .findFirst();

            if (!currentTrain.isPresent()) {
                System.out.println("❌ Train not found in database");
                return false;
            }

            // Use the fresh train data from database
            Train freshTrain = currentTrain.get();
            List<List<Integer>> seats = freshTrain.getSeats();

            if (row < 0 || row >= seats.size() || column < 0 || column >= seats.get(row).size()) {
                System.out.println("❌ Invalid seat selection. Please choose row 0-3 and column 0-5");
                return false;
            }

            // Check availability using FRESH data
            if (seats.get(row).get(column) == 0) {
                System.out.println("✅ Seat is available. Booking now...");

                // Book the seat in the train
                seats.get(row).set(column, 1);
                freshTrain.setSeats(seats);
                trainService.updateTrain(freshTrain);

                // Create and save the ticket for the user
                boolean ticketCreated = createAndSaveTicket(freshTrain, row, column);

                if (ticketCreated) {
                    System.out.println("✅ Seat booked successfully at row " + row + ", column " + column);
                    return true;
                } else {
                    // Rollback seat booking if ticket creation failed
                    seats.get(row).set(column, 0);
                    freshTrain.setSeats(seats);
                    trainService.updateTrain(freshTrain);
                    System.out.println("❌ Booking failed: Could not create ticket");
                    return false;
                }
            } else {
                System.out.println("❌ Seat is already booked. Please choose another seat.");
                System.out.println("💡 Current seat status in database: " + seats.get(row).get(column));
                return false;
            }
        } catch (IOException ex) {
            System.out.println("❌ Error booking seat: " + ex.getMessage());
            return false;
        }
    }

    private boolean createAndSaveTicket(Train train, int row, int column) {
        try {
            // Get source and destination from train route
            List<String> stations = train.getStations();
            String source = stations.get(0);
            String destination = stations.get(stations.size() - 1);

            // Create new ticket WITH SEAT INFORMATION
            String ticketId = "TKT_" + System.currentTimeMillis();
            String travelDate = java.time.LocalDate.now().plusDays(1).toString();

            Ticket newTicket = new Ticket(
                    ticketId,
                    user.getUserId(),
                    source,
                    destination,
                    travelDate,
                    train,
                    row,      // Add seat row
                    column    // Add seat column
            );

            // Add ticket to current user's bookings
            if (user.getTicketsBooked() == null) {
                user.setTicketsBooked(new ArrayList<>());
            }
            user.getTicketsBooked().add(newTicket);

            // Update the user in userList and save to file
            Optional<User> userInList = userList.stream()
                    .filter(u -> u.getUserId().equals(user.getUserId()))
                    .findFirst();

            if (userInList.isPresent()) {
                userInList.get().setTicketsBooked(user.getTicketsBooked());
            }

            saveUserListToFile();

            System.out.println("🎫 Ticket created: " + newTicket.getTicketInfo());
            return true;
        } catch (Exception e) {
            System.out.println("❌ Error creating ticket: " + e.getMessage());
            return false;
        }
    }

    // Helper method to check if user is logged in
    public boolean isUserLoggedIn() {
        return user != null;
    }

    // Helper method to get current username
    public String getCurrentUsername() {
        return user != null ? user.getName() : "No user logged in";
    }
}