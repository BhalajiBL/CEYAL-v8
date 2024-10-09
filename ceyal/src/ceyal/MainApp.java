package ceyal;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MainApp extends Application {
    private TableView<EventLog> tableView;
    private ObservableList<EventLog> logData;
    private Pane petriNetPane;
    private ScrollPane scrollPane;
    private Slider zoomSlider;
    private double scaleFactor = 1.0;
    private List<Circle> places;
    private Label totalTimeLabel;
    private double simulationTime = 0; // Variable to store total simulation time

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Process Mining Software - Enhanced GUI");

        logData = FXCollections.observableArrayList();
        tableView = new TableView<>(logData);
        initializeTableColumns();

        TextField filterField = new TextField();
        filterField.setPromptText("Filter by Event, Resource, Cost, or Duration...");
        setupFilterField(filterField);

        Button loadButton = new Button("Load Event Log");
        loadButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        loadButton.setOnAction(e -> loadEventLog(primaryStage));

        Button visualizeButton = new Button("Visualize Process");
        visualizeButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px;");
        visualizeButton.setOnAction(e -> visualizeProcess());

        Button simulateButton = new Button("Run Simulation");
        simulateButton.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-size: 14px;");
        simulateButton.setOnAction(e -> simulateProcess());

        zoomSlider = createZoomSlider();
        scrollPane = new ScrollPane();
        petriNetPane = new Pane();
        setupScrollPane();

        VBox leftPanel = createLeftPanel(loadButton, visualizeButton, simulateButton, zoomSlider, filterField);
        SplitPane splitPane = createSplitPane();
        totalTimeLabel = new Label("Total Simulation Time: 0.0");
        totalTimeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        BorderPane mainLayout = new BorderPane();
        mainLayout.setLeft(leftPanel);
        mainLayout.setCenter(splitPane);
        mainLayout.setBottom(new VBox(totalTimeLabel));

        Scene scene = new Scene(mainLayout, 1000, 600);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm()); // Load external CSS
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void initializeTableColumns() {
        TableColumn<EventLog, String> eventColumn = new TableColumn<>("Event");
        eventColumn.setCellValueFactory(cellData -> cellData.getValue().eventProperty());

        TableColumn<EventLog, String> timestampColumn = new TableColumn<>("Timestamp");
        timestampColumn.setCellValueFactory(cellData -> cellData.getValue().timestampProperty());

        TableColumn<EventLog, String> resourceColumn = new TableColumn<>("Resource");
        resourceColumn.setCellValueFactory(cellData -> cellData.getValue().resourceProperty());

        TableColumn<EventLog, String> costColumn = new TableColumn<>("Cost");
        costColumn.setCellValueFactory(cellData -> cellData.getValue().costProperty());

        TableColumn<EventLog, String> durationColumn = new TableColumn<>("Duration");
        durationColumn.setCellValueFactory(cellData -> cellData.getValue().durationProperty());

        tableView.getColumns().addAll(eventColumn, timestampColumn, resourceColumn, costColumn, durationColumn);
    }

    private void setupFilterField(TextField filterField) {
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            tableView.setItems(logData.filtered(log ->
                    log.eventProperty().get().toLowerCase().contains(newValue.toLowerCase()) ||
                    log.resourceProperty().get().toLowerCase().contains(newValue.toLowerCase()) ||
                    log.costProperty().get().toLowerCase().contains(newValue.toLowerCase()) ||
                    String.valueOf(log.durationProperty().get()).contains(newValue)));
        });
    }

    private Slider createZoomSlider() {
        Slider slider = new Slider(0.5, 2.0, 1.0);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(0.5);
        slider.setMinorTickCount(5);
        slider.setSnapToTicks(true);

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            scaleFactor = newVal.doubleValue();
            petriNetPane.setScaleX(scaleFactor);
            petriNetPane.setScaleY(scaleFactor);
        });

        return slider;
    }

    private void setupScrollPane() {
        petriNetPane.setMinWidth(500);
        petriNetPane.setMinHeight(800);
        scrollPane.setContent(petriNetPane);

        petriNetPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                double delta = event.getDeltaY() > 0 ? 1.1 : 0.9;
                scaleFactor *= delta;
                petriNetPane.setScaleX(scaleFactor);
                petriNetPane.setScaleY(scaleFactor);
            }
            event.consume();
        });
    }

    private VBox createLeftPanel(Button loadButton, Button visualizeButton, Button simulateButton, Slider zoomSlider, TextField filterField) {
        VBox leftPanel = new VBox(loadButton, visualizeButton, simulateButton, zoomSlider, filterField);
        leftPanel.setSpacing(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setStyle("-fx-background-color: #ECEFF1; -fx-padding: 10px;");
        return leftPanel;
    }

    private SplitPane createSplitPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(tableView, scrollPane);
        splitPane.setDividerPositions(0.4);
        return splitPane;
    }

    private void loadEventLog(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Event Log File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            readEventLog(file);
        }
    }

    private void readEventLog(File file) {
        logData.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            br.readLine(); // Skip the header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    logData.add(new EventLog(parts[0], parts[1], parts[2], parts[3], parts[4]));
                } else {
                    throw new IOException("Invalid format");
                }
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error reading the file: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void visualizeProcess() {
        petriNetPane.getChildren().clear();
        places = new ArrayList<>();
        drawProcessModelFromLog();
    }

    private void drawProcessModelFromLog() {
        double startX = 300;
        double startY = 100;
        double verticalGap = 100;

        for (int i = 0; i < logData.size(); i++) {
            EventLog event = logData.get(i);
            Circle activityNode = createActivityNode(startX, startY + (i * verticalGap));
            places.add(activityNode);

            Label activityLabel = new Label(event.eventProperty().get());
            activityLabel.setLayoutX(startX - 40);
            activityLabel.setLayoutY(startY + (i * verticalGap) - 20);
            activityLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            petriNetPane.getChildren().add(activityLabel);

            if (i > 0) {
                Circle previousNode = places.get(i - 1);
                double duration = Double.parseDouble(logData.get(i).durationProperty().get());
                int frequency = i + 1; // Simulate a frequency metric
                drawArrowWithLabel(previousNode, activityNode, duration, frequency);
            }
        }
    }

    private Circle createActivityNode(double x, double y) {
        Stop[] stops = new Stop[] { new Stop(0, Color.LIGHTBLUE), new Stop(1, Color.DARKBLUE) };
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, null, stops);
        Circle circle = new Circle(30);
        circle.setFill(gradient);
        circle.setCenterX(x);
        circle.setCenterY(y);
        circle.setEffect(new DropShadow(10, Color.GRAY));
        petriNetPane.getChildren().add(circle);
        return circle;
    }
    private Rectangle createActivityNode(double x, double y, int frequency) {
        Color color = Color.LIGHTBLUE.interpolate(Color.DARKBLUE, frequency / 10.0); // Adjust frequency-based color
        Rectangle activity = new Rectangle(x - 30, y - 20, 60, 40);
        activity.setFill(color);
        activity.setStroke(Color.BLACK);
        activity.setStrokeWidth(2);
        
        DropShadow shadow = new DropShadow();
        shadow.setOffsetX(5);
        shadow.setOffsetY(5);
        shadow.setColor(Color.GRAY);
        activity.setEffect(shadow);

        petriNetPane.getChildren().add(activity);
        return activity;
    }


    private void drawArrowWithLabel(Circle from, Circle to, double duration, int frequency) {
        Line arrow = new Line(from.getCenterX(), from.getCenterY(), to.getCenterX(), to.getCenterY());
        petriNetPane.getChildren().add(arrow);

        Label durationLabel = new Label(String.format("Duration: %.2f, Freq: %d", duration, frequency));
        durationLabel.setLayoutX((from.getCenterX() + to.getCenterX()) / 2);
        durationLabel.setLayoutY((from.getCenterY() + to.getCenterY()) / 2);
        petriNetPane.getChildren().add(durationLabel);
    }

    private void simulateProcess() {
        simulationTime = 0; // Reset simulation time
        for (EventLog event : logData) {
            double duration = Double.parseDouble(event.durationProperty().get());
            simulateEventProcessing(duration);
            simulationTime += duration;
        }
        totalTimeLabel.setText("Total Simulation Time: " + simulationTime);
    }

    private void simulateEventProcessing(double duration) {
        // Simulation logic can be implemented here
        // For demonstration, we will animate the processing
        for (Circle place : places) {
            TranslateTransition transition = new TranslateTransition(Duration.seconds(duration), place);
            transition.setByY(50); // Move the place down by 50 units
            transition.setCycleCount(1);
            transition.play();
        }
    }
}
