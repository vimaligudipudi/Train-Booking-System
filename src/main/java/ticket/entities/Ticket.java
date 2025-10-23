package ticket.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Ticket {
    private String ticketId;
    private String userId;
    private String source;
    private String destination;
    private String dateOfTravel;
    private Train train;
    private int seatRow;
    private int seatColumn;

    public Ticket() {}

    public Ticket(String ticketId, String userId, String source, String destination,
                  String dateOfTravel, Train train, int seatRow, int seatColumn) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.source = source;
        this.destination = destination;
        this.dateOfTravel = dateOfTravel;
        this.train = train;
        this.seatRow = seatRow;
        this.seatColumn = seatColumn;
    }

    // Add these getter methods
    public int getSeatRow() {
        return seatRow;
    }

    public int getSeatColumn() {
        return seatColumn;
    }

    // Add setter methods too
    public void setSeatRow(int seatRow) {
        this.seatRow = seatRow;
    }

    public void setSeatColumn(int seatColumn) {
        this.seatColumn = seatColumn;
    }

    // Your existing getters and setters...
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getDateOfTravel() { return dateOfTravel; }
    public void setDateOfTravel(String dateOfTravel) { this.dateOfTravel = dateOfTravel; }
    public Train getTrain() { return train; }
    public void setTrain(Train train) { this.train = train; }

    public String getTicketInfo() {
        String trainInfo = (train != null) ? train.getTrainInfo() : "No train info";
        return String.format("Ticket ID: %s | User: %s | Route: %s to %s | Date: %s | Train: %s | Seat: %d-%d",
                ticketId, userId, source, destination, dateOfTravel, trainInfo, seatRow, seatColumn);
    }
}