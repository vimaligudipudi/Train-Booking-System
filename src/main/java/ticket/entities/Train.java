package ticket.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Train {
    @JsonProperty("trainId")
    private String trainId;

    @JsonProperty("trainNo")
    private int trainNo;

    @JsonProperty("seats")
    private List<List<Integer>> seats;

    @JsonProperty("stationTimes")
    private Map<String, String> stationTimes;

    @JsonProperty("stations")
    private List<String> stations;

    public Train() {}

    public Train(String trainId, int trainNo, List<List<Integer>> seats,
                 Map<String, String> stationTimes, List<String> stations) {
        this.trainId = trainId;
        this.trainNo = trainNo;
        this.seats = seats;
        this.stationTimes = stationTimes;
        this.stations = stations;
    }

    // Getters and setters...
    public List<String> getStations() { return stations; }
    public List<List<Integer>> getSeats() { return seats; }
    public void setSeats(List<List<Integer>> seats) { this.seats = seats; }
    public String getTrainId() { return trainId; }
    public Map<String, String> getStationTimes() { return stationTimes; }
    public int getTrainNo() { return trainNo; }
    public void setTrainNo(int trainNo) { this.trainNo = trainNo; }
    public void setTrainId(String trainId) { this.trainId = trainId; }
    public void setStationTimes(Map<String, String> stationTimes) { this.stationTimes = stationTimes; }
    public void setStations(List<String> stations) { this.stations = stations; }

    public String getTrainInfo() {
        return String.format("Train ID: %s, Train No: %d", trainId, trainNo);
    }
}