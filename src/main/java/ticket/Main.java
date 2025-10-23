package ticket;

import ticket.Services.TrainService;
import ticket.Services.UserBookingService;
import ticket.entities.Train;
import ticket.entities.User;
import util.UserServiceUtil;

import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Running Train Booking System");
        Scanner sc = new Scanner(System.in);
        int option = 0;
        UserBookingService userBookingService;
        Train trainSelectedForBooking = null;

        try {
            userBookingService = new UserBookingService();
            System.out.println("✅ System initialized successfully");
        } catch(IOException ex) {
            System.out.println("❌ There is something wrong initializing the system: " + ex.getMessage());
            return;
        }

        while(option != 7) {
            System.out.println("\n=== Train Booking System ===");
            System.out.println("1. Sign up");
            System.out.println("2. Log in");
            System.out.println("3. Fetch Booking");
            System.out.println("4. Search Trains");
            System.out.println("5. Book a Seat");
            System.out.println("6. Cancel my Booking");
            System.out.println("7. Exit the app");
            System.out.print("Choose Option: ");

            try {
                option = sc.nextInt();
            } catch (Exception e) {
                System.out.println("❌ Invalid input. Please enter a number.");
                sc.nextLine(); // clear the buffer
                continue;
            }

            switch(option) {
                case 1:
                    System.out.print("Enter the username to signup: ");
                    String nameToSignUp = sc.next();
                    System.out.print("Enter the password to signup: ");
                    String passwordToSignUp = sc.next();
                    User userToSignUp = new User(nameToSignUp, passwordToSignUp,
                            UserServiceUtil.hashPassword(passwordToSignUp),
                            new ArrayList<>(), UUID.randomUUID().toString());
                    boolean signupResult = userBookingService.signUp(userToSignUp);
                    if (signupResult) {
                        System.out.println("✅ Sign up successful!");
                    } else {
                        System.out.println("❌ Sign up failed!");
                    }
                    break;

                case 2:
                    System.out.print("Enter the userName to Login: ");
                    String nameLogin = sc.next();
                    System.out.print("Enter the password to login: ");
                    String passwordToLogin = sc.next();

                    // Create a temporary user for login (UUID doesn't matter for login)
                    User userLogin = new User(nameLogin, passwordToLogin,
                            UserServiceUtil.hashPassword(passwordToLogin), // This will be ignored during login
                            new ArrayList<>(),
                            "temp-id"); // Temporary ID, will be replaced during login

                    try {
                        userBookingService = new UserBookingService(userLogin);
                        if (userBookingService.loginUser()) {
                            System.out.println("✅ Login successful!");
                        } else {
                            System.out.println("❌ Login failed! Please check your credentials.");
                            userBookingService = new UserBookingService(); // Reset to non-logged-in service
                        }
                    } catch(IOException ex) {
                        System.out.println("❌ Login failed: " + ex.getMessage());
                        try {
                            userBookingService = new UserBookingService(); // Reset to non-logged-in service
                        } catch (IOException e) {
                            System.out.println("❌ System error: " + e.getMessage());
                            return;
                        }
                    }
                    break;

                case 3:
                    System.out.println("Fetching your bookings...");
                    if (userBookingService != null) {
                        userBookingService.fetchBookings();
                    } else {
                        System.out.println("❌ Please login first!");
                    }
                    break;

                case 4:
                    System.out.print("Type your source station: ");
                    String source = sc.next();
                    System.out.print("Type your destination station: ");
                    String destination = sc.next();

                    List<Train> trains = userBookingService.getTrains(source, destination);

                    if (trains.isEmpty()) {
                        System.out.println("❌ No trains found between " + source + " and " + destination);
                        System.out.println("💡 Try: Guntur, Mangalagiri, Vijayawada");
                        break;
                    }

                    System.out.println("\n=== Available Trains ===");
                    int index = 1;
                    for (Train t : trains) {
                        System.out.println(index + ". " + t.getTrainInfo());
                        System.out.println("   Route: " + String.join(" → ", t.getStations()));
                        System.out.println("   Schedule:");
                        for (Map.Entry<String, String> entry : t.getStationTimes().entrySet()) {
                            System.out.println("     " + entry.getKey() + " : " + entry.getValue());
                        }
                        System.out.println();
                        index++;
                    }

                    System.out.print("Select a train by number (1, 2, 3...): ");
                    int selectedIndex = sc.nextInt();

                    if (selectedIndex < 1 || selectedIndex > trains.size()) {
                        System.out.println("❌ Invalid train selection.");
                        trainSelectedForBooking = null;
                        break;
                    }

                    trainSelectedForBooking = trains.get(selectedIndex - 1);
                    System.out.println("✅ Selected: " + trainSelectedForBooking.getTrainInfo());
                    break;

                case 5:
                    if (trainSelectedForBooking == null) {
                        System.out.println("❌ Please search and select a train first!");
                        break;
                    }

                    System.out.println("Select a seat for: " + trainSelectedForBooking.getTrainInfo());
                    List<List<Integer>> seats = userBookingService.fetchSeats(trainSelectedForBooking);

                    System.out.println("\n=== Available Seats ===");
                    System.out.println("0 = Available, 1 = Booked");
                    System.out.println("Rows: 0-3, Columns: 0-5");
                    System.out.println("-----------------------");
                    for (int i = 0; i < seats.size(); i++) {
                        System.out.print("Row " + i + ": ");
                        for (Integer val : seats.get(i)) {
                            System.out.print(val + " ");
                        }
                        System.out.println();
                    }

                    System.out.print("Enter the row (0-3): ");
                    int row = sc.nextInt();
                    System.out.print("Enter the column (0-5): ");
                    int col = sc.nextInt();

                    System.out.println("Booking your seat...");
                    Boolean booked = userBookingService.bookTrainSeat(trainSelectedForBooking, row, col);
                    if (booked) {
                        System.out.println("✅ Booked! Enjoy your journey");
                        // Refresh the train data after successful booking
                        try {
                            TrainService trainService = new TrainService();
                            List<Train> currentTrains = trainService.getTrainList();
                            // Create a final reference for use in lambda
                            final String currentTrainId = trainSelectedForBooking.getTrainId();
                            Optional<Train> updatedTrain = currentTrains.stream()
                                    .filter(t -> t.getTrainId().equals(currentTrainId))
                                    .findFirst();
                            if (updatedTrain.isPresent()) {
                                trainSelectedForBooking = updatedTrain.get();
                            }
                        } catch (IOException e) {
                            System.out.println("⚠️ Could not refresh train data");
                        }
                    } else {
                        System.out.println("❌ Can't book this seat. It might be already booked or invalid.");
                    }
                    break;

                case 6:
                    if (userBookingService == null) {
                        System.out.println("❌ Please login first!");
                        break;
                    }
                    System.out.print("Enter the ticket ID to cancel: ");
                    String removeTicketId = sc.next();
                    boolean cancelResult = userBookingService.cancelBooking(removeTicketId);
                    if (cancelResult) {
                        System.out.println("✅ Booking cancelled successfully!");
                    } else {
                        System.out.println("❌ Failed to cancel booking. Check ticket ID.");
                    }
                    break;

                case 7:
                    System.out.println("Thank you for using Train Booking System. Goodbye!");
                    break;

                default:
                    System.out.println("❌ Invalid option. Please choose 1-7.");
                    break;
            }
        }
        sc.close();
    }
}