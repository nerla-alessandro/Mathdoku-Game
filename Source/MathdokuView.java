import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MathdokuView extends Application {

    static int GRID_SIZE = 6;
    static GridPane mathdokuGridPane;
    static BorderPane gameRootPane;
    static Button undoButton;
    static Button redoButton;
    static Button clearButton;
    static VBox mainOptionsMenuBox;
    static Stage mainStage;
    static Scene mainScene;
    static Popup numberKeypadPopup;
    static CheckBox keyPadFlag;
    static Slider gridSizeSlider;
    static MathdokuCell focusedCell;
    static CheckBox showMistakes;
    static Tab gameTab;
    static TabPane rootTabPane;
    static int tempHintValue;
    static ComboBox<String> fontSizeChoice;
    static CheckBox checkUniqueBox;


    @Override
    public void start(Stage stage) {

        stage.setTitle("Mathdoku");
        //stage.getIcons().add(new Image());

        BorderPane gameRoot =  new BorderPane();
        GridPane mathdokuGrid =  new GridPane();
        mathdokuGrid.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(4))));
        gameRoot.setPadding(new Insets(10));
        gameRoot.setCenter(mathdokuGrid);

        mathdokuGridPane = mathdokuGrid;
        gameRootPane = gameRoot;

        Button undo =  new Button("Undo");
        Button clear = new Button("Clear");
        Button redo =  new Button("Redo");
        undoButton = undo;
        clearButton = clear;
        redoButton = redo;
        undo.setDisable(true);
        redo.setDisable(true);
        undo.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> MathdokuController.undoRequest());
        clear.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> confirmPopup());
        redo.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> MathdokuController.redoRequest());

        HBox optionBar1 = new HBox();
        optionBar1.setAlignment(Pos.CENTER);
        optionBar1.getChildren().addAll(undo, clear, redo);
        optionBar1.setSpacing(10);
        Pane hintViewPane = new Pane();
        try {
            Image hint = new Image(new FileInputStream(new File("./HintButton.png")), 20, 20, false, false);
            ImageView hintView = new ImageView(hint);
            hintViewPane.getChildren().add(hintView);
        } catch (FileNotFoundException e) {
            Label hintLabel = new Label("Hint?");
            hintLabel.setAlignment(Pos.CENTER);
            hintViewPane.getChildren().add(hintLabel);
        }

        hintViewPane.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            if(focusedCell.textField.getText().isEmpty()){
                tempHintValue = 0;
            } else {
                tempHintValue = Integer.parseInt(focusedCell.textField.getText());
            }
            focusedCell.setFontSize();
            focusedCell.textField.setText(String.valueOf(MathdokuModel.solution[focusedCell.gridPos]));
        });
        hintViewPane.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            if(tempHintValue != 0) {
                focusedCell.textField.setText(String.valueOf(tempHintValue));
            } else {
                focusedCell.textField.setText("");
            }
        });
        
        Label showMistakesLabel = new Label("Show Mistakes");
        CheckBox showMistakesBox = new CheckBox();
        showMistakes = showMistakesBox;
        showMistakesBox.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> {MathdokuController.checkMistakes(); focusedCell.textField.requestFocus();});
        Button solve = new Button("Solve");
        solve.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            MathdokuModel.solved = true;
            for (int i = 0; i < Math.pow(GRID_SIZE, 2); i++) {
                MathdokuController.moveRequest(new MathdokuModel.Move((MathdokuCell) mathdokuGrid.getChildren().get(i), MathdokuModel.solution[i]));
            }
        });
        HBox optionBar2 = new HBox();
        optionBar2.setAlignment(Pos.CENTER);
        optionBar2.getChildren().addAll(hintViewPane, showMistakesLabel, showMistakesBox, solve);
        optionBar2.setSpacing(10);

        VBox mainOptions = new VBox();
        mainOptions.getChildren().addAll(optionBar1, optionBar2);
        mainOptions.setPadding(new Insets(10));
        mainOptions.setSpacing(10);

        gameRoot.setBottom(mainOptions);

        mainOptionsMenuBox = mainOptions;

        FileChooser fileChooser = new FileChooser();
        Button loadFromFile = new Button("Load from Text File");
        loadFromFile.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                    new FileChooser.ExtensionFilter("All Files", "*.*"));
            fileChooser.setTitle("Select a File");
            String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
            fileChooser.setInitialDirectory(new File(currentPath));
            File templateFile = fileChooser.showOpenDialog(stage);
            String s = MathdokuController.fileParser(templateFile);
            if(s != null) {
                MathdokuController.textParser(s);
            }
        });

        TextArea textTemplate = new TextArea();
        textTemplate.maxWidthProperty().bind(stage.widthProperty().divide(4).multiply(3));
        Button loadFromText = new Button("Load from Text Input");
        loadFromText.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> MathdokuController.textParser(textTemplate.getText()));

        Label sizeLabel = new Label("Grid Size: ");
        Slider sizeSlider = new Slider();
        gridSizeSlider = sizeSlider;
        sizeSlider.setMin(2);
        sizeSlider.setMax(8);
        sizeSlider.setShowTickLabels(true);
        sizeSlider.setShowTickMarks(true);
        sizeSlider.setMajorTickUnit(1);
        sizeSlider.setSnapToTicks(true);
        sizeSlider.setMinorTickCount(0);
        sizeSlider.setValue(8);
        Button createNewGame = new Button("Generate Random Game");
        createNewGame.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            Popup loading = new Popup();

            Timeline loadingPopup = new Timeline(new KeyFrame(
                    Duration.ONE,
                    ae -> {
                        VBox loadBox = new VBox();
                        Label loadLabel;
                        if(sizeSlider.getValue() > 6) {
                            loadLabel = new Label("Checking for unique solution, this may take some time due to the chosen Grid Size");
                        } else {
                            loadLabel = new Label("Checking for unique solution");
                        }
                        ProgressIndicator loadCircle = new ProgressIndicator();
                        loadCircle.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                        loadCircle.setVisible(true);
                        loadBox.getChildren().addAll(loadLabel, loadCircle);
                        loadBox.setAlignment(Pos.CENTER);
                        loadBox.setSpacing(20);
                        loadBox.setPadding(new Insets(20));
                        loadBox.setStyle("-fx-background-color: white; -fx-border-style: solid; -fx-border-width: 3;");
                        loadCircle.setVisible(true);
                        loading.getContent().add(loadBox);
                        loading.show(mainStage);
                        loading.setAnchorX(mainStage.getX() + (mainStage.getWidth() / 2) - (loading.getWidth() / 2));
                        loading.setAnchorY(mainStage.getY() + (mainStage.getHeight() / 2) - (loading.getHeight() / 2));
                        rootTabPane.setDisable(true);
                    }
            ));
            Timeline createGame = new Timeline(new KeyFrame(
                    Duration.ONE,
                    ae -> MathdokuController.createGame((int) sizeSlider.getValue())
            ));
            createGame.setDelay(Duration.millis(750));
            loadingPopup.setOnFinished(loadedPopup -> createGame.play());
            createGame.setOnFinished(gameCreated -> {loading.hide(); rootTabPane.setDisable(false);});
            if(checkUniqueBox.isSelected()){
                loadingPopup.play();
            } else {
                createGame.setDelay(Duration.ZERO);
                createGame.play();
            }
        });
        HBox sizeBox = new HBox();
        sizeBox.getChildren().addAll(sizeLabel, sizeSlider, createNewGame);
        sizeBox.setSpacing(15);
        sizeBox.setAlignment(Pos.CENTER);

        Label uniqueLabel = new Label("Check for Unique Solution ");
        CheckBox uniqueCheckBox = new CheckBox();
        uniqueCheckBox.setSelected(true);
        checkUniqueBox = uniqueCheckBox;
        HBox checkUnique = new HBox(uniqueLabel, uniqueCheckBox);
        checkUnique.setSpacing(10);
        checkUnique.setAlignment(Pos.CENTER);


        Label fontSizeLabel =  new Label("Font Size: ");
        ComboBox<String> fontSizeChooser =  new ComboBox<>();
        fontSizeChooser.getItems().addAll("Small", "Medium", "Large");
        fontSizeChooser.setValue(fontSizeChooser.getItems().get(1));
        MathdokuCell.fontSize = 20;
        MathdokuCell.labelFontSize = 14;
        HBox fontSizeBox =  new HBox();
        fontSizeBox.getChildren().addAll(fontSizeLabel, fontSizeChooser);
        fontSizeBox.setSpacing(15);
        fontSizeBox.setAlignment(Pos.CENTER);
        fontSizeChooser.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (fontSizeChooser.getValue().equalsIgnoreCase("Small")) {
                MathdokuCell.fontSize = 15;
                MathdokuCell.labelFontSize = 12;
            }
            if (fontSizeChooser.getValue().equalsIgnoreCase("Medium")) {
                MathdokuCell.fontSize = 25;
                MathdokuCell.labelFontSize = 15;
            }
            if (fontSizeChooser.getValue().equalsIgnoreCase("Large")) {
                MathdokuCell.fontSize = 40;
                MathdokuCell.labelFontSize = 18;
            }
            for (int i = 0; i < Math.pow(GRID_SIZE, 2); i++) {
                MathdokuCell tempCell = (MathdokuCell) mathdokuGrid.getChildren().get(i);
                tempCell.setFontSize();
            }
            rebindSize();
        });
        fontSizeChoice = fontSizeChooser;
        Label showKeypadLabel = new Label("Show Keypad");
        CheckBox showKeypadCheck = new CheckBox();
        HBox showKeypadBox = new HBox();
        showKeypadBox.getChildren().addAll(showKeypadLabel, showKeypadCheck);
        showKeypadBox.setSpacing(15);
        showKeypadBox.setAlignment(Pos.CENTER);
        keyPadFlag = showKeypadCheck;


        VBox optionsMenu =  new VBox();
        optionsMenu.getChildren().addAll(sizeBox, checkUnique,  new Separator(), loadFromFile, new Separator(), textTemplate, loadFromText, new Separator(), fontSizeBox, new Separator(), showKeypadBox);
        optionsMenu.setAlignment(Pos.CENTER);
        optionsMenu.setSpacing(20);
        optionsMenu.setStyle(" -fx-background-color: #ffffff");

        TabPane menu = new TabPane();
        rootTabPane = menu;
        ScrollPane gameRootScroll = new ScrollPane(gameRoot);
        gameRootScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        gameRootScroll.setFitToHeight(true);
        gameRootScroll.setFitToWidth(true);
        Tab game = new Tab("Game", gameRootScroll);
        gameTab = game;
        game.setClosable(false);
        Tab options = new Tab("Options", optionsMenu);
        options.setClosable(false);
        menu.getTabs().addAll(game, options);

        Scene scene = new Scene(menu, 300, 410);
        stage.setScene(scene);

        //Mantains proportions of the stage
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            stage.setMaxHeight(stage.getWidth() + 134);
            stage.setHeight(stage.getMaxHeight());
        });

        //Mantains proportions of the stage
        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if((double) newVal < (double) oldVal && stage.getWidth() != ((double) newVal - 134)){
                stage.setHeight((double) oldVal);
            }
        });

        //Mantains proportions of grid when maximized
        stage.maximizedProperty().addListener((obs, oldVal, newVal) ->{
            if(newVal){
                mathdokuGrid.minHeightProperty().unbind();
                mathdokuGrid.setMaxSize(mathdokuGrid.getWidth(), mathdokuGrid.getWidth());
            } else {
                mathdokuGrid.setMaxSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
                rebindSize();
            }
        });


        mathdokuGridPane = mathdokuGrid;
        gameRootPane = gameRoot;

        stage.setMinHeight(470);
        stage.setMinWidth(336);
        stage.setHeight(670);
        stage.setWidth(536);
        stage.setMaximized(false);
        stage.show();
        mainStage = stage;
        mainScene = scene;
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (numberKeypadPopup != null && event.getPickResult().getIntersectedNode() != numberKeypadPopup.getOwnerNode()) {
                numberKeypadPopup.hide();
            }
            if(event.getTarget() instanceof MathdokuCell){
                focusedCell = (MathdokuCell) event.getTarget();
                focusedCell.textField.requestFocus();
            }
        });

        stage.minWidthProperty().bind(stage.widthProperty().subtract(stage.widthProperty()).add(130).add(MathdokuView.GRID_SIZE*(50 + MathdokuCell.fontSize *3)));
        stage.minHeightProperty().bind(stage.minWidthProperty().add(134));
        MathdokuController.createGame(GRID_SIZE);
    }

    static void createGrid() {
        mathdokuGridPane.getChildren().clear();
        gridSizeSlider.setValue(GRID_SIZE);
        for(int j=0; j<GRID_SIZE; j++){
            for(int i=0; i<GRID_SIZE; i++){
                mathdokuGridPane.add(new MathdokuCell(), i, j);
            }
        }

        for(int i=0; i<Math.pow(GRID_SIZE, 2); i++){
            MathdokuCell temp = (MathdokuCell) mathdokuGridPane.getChildren().get(i);
            temp.setPos(i);
        }

        rebindSize();
    }

    public static void rebindSize(){
        if(!mainStage.isMaximized()) {
            mainStage.minWidthProperty().unbind();
            mainStage.minHeightProperty().unbind();
            for (int i = 0; i < Math.pow(MathdokuView.GRID_SIZE, 2); i++) {
                MathdokuCell tempCell = (MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i);
                tempCell.rebindSize();
            }
            mathdokuGridPane.minHeightProperty().bind(mainStage.heightProperty().subtract(170));
            mainStage.minWidthProperty().bind(mainStage.widthProperty().subtract(mainStage.widthProperty()).add(370).add(MathdokuView.GRID_SIZE * 50));
            mainStage.minHeightProperty().bind(mainStage.minWidthProperty().add(134));
        } else {
            //NEW
            for (int i = 0; i < Math.pow(MathdokuView.GRID_SIZE, 2); i++) {
                MathdokuCell tempCell = (MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i);
                tempCell.rebindSize();
            }
        }
    }

    public static void openKeypadPopup(MathdokuCell cell){
        final int BUTTON_SIZE = 50;
        Popup keypad =  new Popup();
        numberKeypadPopup =  keypad;
        FlowPane numberPad = new FlowPane();
        numberPad.setPrefWrapLength(3*BUTTON_SIZE);
        keypad.setWidth(210);
        keypad.setAutoFix(true);
        numberPad.setAlignment(Pos.CENTER);
        Button clear = new Button("Clear");
        clear.setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
        clear.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            cell.textField.clear();
            MathdokuController.moveRequest(new MathdokuModel.Move(cell, -1));
            keypad.hide();
            numberKeypadPopup = null;
        });
        numberPad.getChildren().add(clear);
        for(int i=1; i<=GRID_SIZE; i++){
            Button tempButton = new Button(String.valueOf(i));
            tempButton.setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
            int finalI = i;
            numberPad.getChildren().add(tempButton);
            tempButton.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
                cell.textField.setText(String.valueOf(finalI));
                MathdokuController.moveRequest(new MathdokuModel.Move(cell, finalI));
                cell.textField.setText(String.valueOf(finalI));
                keypad.hide();
                numberKeypadPopup = null;
            });
        }

        numberPad.setPadding(new Insets(10));
        numberPad.setStyle("-fx-background-color: white; -fx-border-style: solid; -fx-border-width: 3;");
        keypad.getContent().add(numberPad);
        keypad.show(mainStage);
        keypad.setAnchorX(mainStage.getX() + (mainStage.getWidth() / 2) - (keypad.getWidth() / 2));
        keypad.setAnchorY(mainStage.getY() + (mainStage.getHeight() / 2) - (keypad.getHeight() / 2));

    }

    public static void confirmPopup(){
        rootTabPane.setDisable(true);

        Label confirmLabel = new Label("Are you sure?");
        Button confirm = new Button("Yes");
        Button cancel =  new Button("No");
        HBox confirmOptions = new HBox();
        confirmOptions.getChildren().addAll(cancel, confirm);
        confirmOptions.setAlignment(Pos.CENTER);
        confirmOptions.setSpacing(20);
        VBox confirmMenu = new VBox();
        confirmMenu.getChildren().addAll(confirmLabel, confirmOptions);
        confirmMenu.setAlignment(Pos.CENTER);
        confirmMenu.setSpacing(20);
        confirmMenu.setStyle("-fx-background-color: white; -fx-border-style: solid; -fx-border-width: 3;");
        confirmMenu.setMinSize(200, 50);
        confirmMenu.setPadding(new Insets(20));
        Popup clearConfirm = new Popup();
        clearConfirm.getContent().addAll(confirmMenu);
        clearConfirm.show(mainStage);
        clearConfirm.setAnchorX(mainStage.getX() + (mainStage.getWidth() / 2) - (clearConfirm.getWidth() / 2));
        clearConfirm.setAnchorY(mainStage.getY() + (mainStage.getHeight() / 2) - (clearConfirm.getHeight() / 2));
        confirm.setOnMouseClicked(event -> { clearConfirm.hide(); MathdokuController.clearRequest(); rootTabPane.setDisable(false); });
        cancel.setOnMouseClicked(event -> { clearConfirm.hide(); rootTabPane.setDisable(false); });


    }

    public static void winAnimation(){
        gameRootPane.setDisable(true);

        Popup popupPane = showGenericPopup("Good job! You succesfully completed the Mathdoku! What about a harder one?", "Bring it on!");

        ArrayList<String> colorList = new ArrayList<>(List.of(
                "#FF355E",
                "#FD5B78",
                "#FF6037",
                "#FF9966",
                "#FF9933",
                "#FFCC33",
                "#FFFF66",
                "#CCFF00",
                "#66FF66",
                "#AAF0D1",
                "#50BFE6",
                "#FF6EFF",
                "#FF00CC"
        ));

        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(80),
                ae -> {
                    for (int i = 0; i < Math.pow(MathdokuView.GRID_SIZE, 2); i++) {
                        MathdokuCell tempCell = (MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i);
                        tempCell.borderStyle = "";
                        tempCell.textField.setText("");
                        tempCell.topLabel.setText("");
                        tempCell.backgroundStyle = " -fx-background-color: " + MathdokuController.selectRandomElementFrom(colorList);
                        tempCell.setStyle();
                    }
                }
        ));

        Timeline newGame = new Timeline(new KeyFrame(
                Duration.ONE,
                ae -> {
                    if(GRID_SIZE < 8) {
                        GRID_SIZE++;
                    }
                    MathdokuController.createGame(GRID_SIZE);
                    gameRootPane.setDisable(false);
                }
        ));

        timeline.setCycleCount(10);
        newGame.setCycleCount(1);
        popupPane.setOnHiding(windowEvent -> timeline.play());
        timeline.setOnFinished(event -> newGame.play());
    }

    public static void showNoSolutionPopup(){
        Popup noSolution = new Popup();
        VBox noSolutionBox = new VBox();
        HBox buttonBox = new HBox();
        Button continueImport = new Button("Import");
        Button generate = new Button("Generate");
        continueImport.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> noSolution.hide());
        generate.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {noSolution.hide(); MathdokuController.createGame(MathdokuView.GRID_SIZE);});
        buttonBox.getChildren().addAll(continueImport, generate);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setSpacing(20);
        Label noSolutionLabel = new Label("The imported game has no solution. Do you want to generate a random one or continue the import?");
        noSolutionBox.setPadding(new Insets(20));
        noSolutionBox.setSpacing(20);
        noSolutionBox.setAlignment(Pos.CENTER);
        noSolutionBox.setStyle("-fx-background-color: white; -fx-border-style: solid; -fx-border-width: 3;");
        noSolutionBox.getChildren().addAll(noSolutionLabel, buttonBox);
        noSolution.getContent().add(noSolutionBox);
        noSolution.show(MathdokuView.mainStage);
        noSolution.setAnchorX(mainStage.getX() + (mainStage.getWidth() / 2) - (noSolution.getWidth() / 2));
        noSolution.setAnchorY(mainStage.getY() + (mainStage.getHeight() / 2) - (noSolution.getHeight() / 2));
    }

    public static Popup showGenericPopup(String message, String buttonText){
        rootTabPane.setDisable(true);
        Popup popup = new Popup();
        VBox popupVBox = new VBox();
        Button close = new Button(buttonText);
        close.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {rootTabPane.setDisable(false); popup.hide();});
        Label popupLabel = new Label(message);
        popupVBox.setPadding(new Insets(20));
        popupVBox.setSpacing(20);
        popupVBox.setAlignment(Pos.CENTER);
        popupVBox.setStyle("-fx-background-color: white; -fx-border-style: solid; -fx-border-width: 3;");
        popupVBox.getChildren().addAll(popupLabel, close);
        popup.getContent().add(popupVBox);
        popup.show(mainStage);
        popup.setAnchorX(mainStage.getX() + (mainStage.getWidth() / 2) - (popup.getWidth() / 2));
        popup.setAnchorY(mainStage.getY() + (mainStage.getHeight() / 2) - (popup.getHeight() / 2));
        return popup;
    }

    public static void main(String[] args) {
        launch();
    }

    static class MathdokuCell extends BorderPane {

        TextField textField;
        int gridPos;
        int lastValue = -1;
        static int fontSize = 15;
        static int labelFontSize = 25;
        String basicStyle = "-fx-border-style: solid;";
        String borderStyle = "-fx-border-width: 2;";
        String backgroundStyle = " -fx-background-color: #ffffff";
        Label topLabel;
        Label bottomLabel;
        String textFieldBasicStyle;
        MathdokuModel.Cage parentCage;
        boolean columnMistake = false;
        boolean rowMistake = false;
        boolean cageMistake = false;

        MathdokuCell(){
            Label topLabel =  new Label(" ");
            this.setTop(topLabel);
            topLabel.setAlignment(Pos.TOP_LEFT);
            this.topLabel = topLabel;
            Label bottomLabel =  new Label(" ");
            this.setBottom(bottomLabel);
            this.bottomLabel = bottomLabel;
            TextField textFieldTemplate = new TextField();
            this.setCenter(textFieldTemplate);
            textField = textFieldTemplate;

            this.textFieldBasicStyle = "-fx-display-caret: false; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color: transparent; -fx-accent: #cbe3e6; -fx-highlight-text-fill: #000000;";
            textFieldTemplate.setStyle(textFieldBasicStyle);

            textFieldTemplate.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, Event::consume);

            textFieldTemplate.prefHeightProperty().bind((mathdokuGridPane.widthProperty().divide(GRID_SIZE).subtract(36)));
            textFieldTemplate.prefWidthProperty().bind(mathdokuGridPane.widthProperty().divide(GRID_SIZE));

            textFieldTemplate.setTextFormatter(new TextFormatter<String>(change ->
                    (change.getControlNewText().length() <= 1 && change.getControlNewText().matches("[1-" + GRID_SIZE + "]+"))
                            || change.getControlNewText().isBlank() && !this.textField.getText().isEmpty() ? change : null));
            textFieldTemplate.setAlignment(Pos.CENTER);

            this.setStyle(basicStyle + borderStyle + " -fx-background-color: #ffffff;");

            textFieldTemplate.addEventHandler(KeyEvent.KEY_TYPED, event -> {
                if(event.getCharacter().matches("[1-" + GRID_SIZE + "]+")){
                    MathdokuController.moveRequest(new MathdokuModel.Move(this, Integer.parseInt(event.getCharacter())));
                }
            });

            textFieldTemplate.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if(event.getCode() == KeyCode.BACK_SPACE && !MathdokuModel.moveStack.isEmpty() && !(MathdokuModel.moveStack.peek().cell.equals(this) && MathdokuModel.moveStack.peek().value == -1)){
                    MathdokuController.moveRequest(new MathdokuModel.Move(this, -1));
                } else if(event.getCode() == KeyCode.LEFT){
                    if(focusedCell.getLeftCell() != null) {
                        focusedCell.getLeftCell().textField.requestFocus();
                    }
                } else if(event.getCode() == KeyCode.RIGHT){
                    if(focusedCell.getRightCell() != null) {
                        focusedCell.getRightCell().textField.requestFocus();
                    }
                } else if(event.getCode() == KeyCode.UP){
                    if(focusedCell.getTopCell() != null) {
                        focusedCell.getTopCell().textField.requestFocus();
                    }
                } else if(event.getCode() == KeyCode.DOWN){
                    if(focusedCell.getBottomCell() != null) {
                        focusedCell.getBottomCell().textField.requestFocus();
                    }
                }
            });

            this.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                if(keyPadFlag.isSelected()){
                    openKeypadPopup(this);
                }
            });

            textFieldTemplate.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                if(keyPadFlag.isSelected()){
                    openKeypadPopup(this);
                }
            });

            textFieldTemplate.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if(newVal){
                    this.setStyle(basicStyle + borderStyle + "-fx-background-color: #cbe3e6");
                    focusedCell = this;
                } else {
                    this.setStyle();
                }
            });
            this.setFontSize();
        }

        void rebindSize(){
            this.minHeightProperty().bind(mathdokuGridPane.widthProperty().subtract(10).divide(GRID_SIZE));
            textField.prefHeightProperty().unbind();
            textField.prefWidthProperty().unbind();
            textField.prefHeightProperty().bind((mathdokuGridPane.widthProperty().divide(GRID_SIZE).subtract(3 * labelFontSize)));
            textField.prefWidthProperty().bind(mathdokuGridPane.widthProperty().divide(GRID_SIZE));
        }

        int getPos() {
            return gridPos;
        }

        void setPos(int gridPos) {
            this.gridPos = gridPos;
        }

        boolean isAdjacent(MathdokuCell cellCompare){
            return (Math.abs(this.getPos() - cellCompare.getPos()) == 1 || Math.abs(this.getPos() - cellCompare.getPos()) == GRID_SIZE);
        }

        MathdokuCell getLeftCell(){
            if(this.gridPos % GRID_SIZE == 0){
                return null;
            }
            return (MathdokuCell) mathdokuGridPane.getChildren().get(this.gridPos - 1);
        }

        MathdokuCell getBottomCell(){
            if((Math.pow(GRID_SIZE, 2) - this.gridPos) <= GRID_SIZE){
                return null;
            }
            return (MathdokuCell) mathdokuGridPane.getChildren().get(this.gridPos + GRID_SIZE);
        }

        MathdokuCell getRightCell(){
            if(this.gridPos % GRID_SIZE == GRID_SIZE - 1){
                return null;
            }
            return (MathdokuCell) mathdokuGridPane.getChildren().get(this.gridPos + 1);
        }

        MathdokuCell getTopCell(){
            if(this.gridPos - GRID_SIZE < 0){
                return null;
            }
            return (MathdokuCell) mathdokuGridPane.getChildren().get(this.gridPos - GRID_SIZE);
        }

        void setStyle() {
            this.setStyle(basicStyle + borderStyle + backgroundStyle);
        }

        void setFontSize() {
            this.topLabel.setStyle("-fx-font-size: " + labelFontSize + ";");
            this.bottomLabel.setStyle("-fx-font-size: " + labelFontSize + ";");
            this.textField.setStyle(textFieldBasicStyle + "-fx-font-size: " + fontSize + ";");
        }
    }
}
