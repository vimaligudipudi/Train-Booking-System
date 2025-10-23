package ticket.entities;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String name;
    private String password;
    private String hashedPassword;
    private String userId;
    private List<Ticket> ticketsBooked;

    public User(String name, String password, String hashedPassword, List<Ticket> ticketsBooked, String userId) {
        this.name = name;
        this.password = password;
        this.hashedPassword = hashedPassword;
        this.ticketsBooked = ticketsBooked != null ? ticketsBooked : new ArrayList<>();
        this.userId = userId;
    }

    public User() {}

    public String getName() { return name; }
    public String getPassword() { return password; }
    public String getHashedPassword() { return hashedPassword; }
    public List<Ticket> getTicketsBooked() { return ticketsBooked; }
    public String getUserId() { return userId; }

    public void setName(String name) { this.name = name; }
    public void setPassword(String password) { this.password = password; }
    public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }
    public void setTicketsBooked(List<Ticket> ticketsBooked) { this.ticketsBooked = ticketsBooked; }
    public void setUserId(String userId) { this.userId = userId; }

    public void printTickets() {
        if (ticketsBooked == null || ticketsBooked.isEmpty()) {
            System.out.println("No tickets booked yet.");
            return;
        }

        for (int i = 0; i < ticketsBooked.size(); i++) {
            Ticket ticket = ticketsBooked.get(i);
            if (ticket != null) {
                System.out.println((i + 1) + ". " + ticket.getTicketInfo());
            }
        }
    }
}