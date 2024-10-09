package ceyal;

import javafx.beans.property.SimpleStringProperty;

public class EventLog {
    private final SimpleStringProperty event;
    private final SimpleStringProperty timestamp;
    private final SimpleStringProperty resource;
    private final SimpleStringProperty cost;
    private final SimpleStringProperty duration;

    public EventLog(String event, String timestamp, String resource, String cost, String duration) {
        this.event = new SimpleStringProperty(event);
        this.timestamp = new SimpleStringProperty(timestamp);
        this.resource = new SimpleStringProperty(resource);
        this.cost = new SimpleStringProperty(cost);
        this.duration = new SimpleStringProperty(duration);
    }

    public SimpleStringProperty eventProperty() {
        return event;
    }

    public SimpleStringProperty timestampProperty() {
        return timestamp;
    }

    public SimpleStringProperty resourceProperty() {
        return resource;
    }

    public SimpleStringProperty costProperty() {
        return cost;
    }

    public SimpleStringProperty durationProperty() {
        return duration;
    }
}
