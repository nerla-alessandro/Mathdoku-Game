import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class MathdokuController {

    static void moveRequest(MathdokuModel.Move m){
        if(m.value != m.prev_value) {
            MathdokuModel.moveStack.push(m);
            MathdokuModel.redoStack.clear();
            MathdokuView.undoButton.setDisable(false);
            MathdokuView.redoButton.setDisable(true);
            if (m.value == -1) {
                m.cell.textField.clear();
            } else {
                m.cell.textField.setText(String.valueOf(m.value));
            }
            checkMistakes();
        }
        m.cell.setFontSize();
    }

    static void clearRequest(){
        MathdokuModel.redoStack.clear();
        MathdokuModel.moveStack.clear();
        for(Node cell : MathdokuView.mathdokuGridPane.getChildren()){
            MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) cell;
            tempCell.textField.clear();
            tempCell.lastValue = -1;
            tempCell.rowMistake = false;
            tempCell.columnMistake = false;
            tempCell.cageMistake = false;
            tempCell.backgroundStyle = "-fx-background-color: #ffffff";
            tempCell.setStyle();
        }
        MathdokuView.redoButton.setDisable(true);
        MathdokuView.undoButton.setDisable(true);
    }

    static void undoRequest(){
        MathdokuModel.redoStack.push(MathdokuModel.moveStack.pop());
        if(MathdokuModel.redoStack.peek().prev_value == -1){
            MathdokuModel.redoStack.peek().cell.textField.clear();
        } else {
            MathdokuModel.redoStack.peek().cell.textField.setText(String.valueOf(MathdokuModel.redoStack.peek().prev_value));
            MathdokuModel.redoStack.peek().cell.lastValue = -1;
        }
        MathdokuView.redoButton.setDisable(false);
        if(MathdokuModel.moveStack.isEmpty()){
            MathdokuView.undoButton.setDisable(true);
        }
        checkMistakes();
        MathdokuModel.redoStack.peek().cell.setFontSize();
    }

    static void redoRequest(){
        MathdokuModel.moveStack.push(MathdokuModel.redoStack.pop());
        if(MathdokuModel.moveStack.peek().value == -1){
            MathdokuModel.moveStack.peek().cell.textField.clear();
        } else {
            MathdokuModel.moveStack.peek().cell.textField.setText(String.valueOf(MathdokuModel.moveStack.peek().value));
        }
        MathdokuView.undoButton.setDisable(false);
        if(MathdokuModel.redoStack.isEmpty()){
            MathdokuView.redoButton.setDisable(true);
        }
        checkMistakes();
        MathdokuModel.moveStack.peek().cell.setFontSize();
    }

    static void textParser(String text){
        String fontChoice = MathdokuView.fontSizeChoice.getValue();
        String[] lines;
        lines = text.split("\\R+", -1);
        StringBuilder gridSizeParse = new StringBuilder();
        for(int i=0; i<lines.length && !lines[i].isBlank(); i++) {
            gridSizeParse.append(lines[i].split(" ")[1]).append(",");
        }
        ArrayList<String> elementsString = new ArrayList<>(Arrays.asList(gridSizeParse.toString().split(",")));
        int maxValue=0;
        for (String element : elementsString) {
            if(Integer.parseInt(element) > maxValue){
                maxValue = Integer.parseInt(element);
            }
        }

        if(Math.sqrt(maxValue) == (int) Math.sqrt(maxValue)){
            MathdokuView.GRID_SIZE = (int) Math.sqrt(maxValue);
            MathdokuView.createGrid();
        } else {
            MathdokuView.showGenericPopup("The Import was aborted as the grid size was invalid, a new game will be generated randomly.", "Close");
            createGame(MathdokuView.GRID_SIZE);
            return;
        }


        for(int i=0; i<lines.length && !lines[i].isBlank(); i++) {
            String[] firstParse;
            String resultParse;
            char operatorParse;
            String[] cageElementsParse;
            firstParse = lines[i].split(" ");
            MathdokuModel.Cage tempCage;
            if(firstParse[0].length() != 1) {
                try {
                    resultParse = firstParse[0].substring(0, firstParse[0].length() - 1);
                    operatorParse = firstParse[0].charAt(firstParse[0].length() - 1);
                    tempCage = new MathdokuModel.Cage(operatorParse, Integer.parseInt(resultParse));
                } catch (NumberFormatException e){
                    resultParse = firstParse[0].substring(0, firstParse[0].length() - 2);
                    tempCage = new MathdokuModel.Cage('÷', Integer.parseInt(resultParse));
                }
            } else {
                tempCage = new MathdokuModel.Cage(' ', Integer.parseInt(firstParse[0]));
            }
            cageElementsParse = firstParse[1].split(",");
            for(String s : cageElementsParse){
                int cellIndex = Integer.parseInt(s) - 1;
                tempCage.addCell((MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(cellIndex));
            }
            if(tempCage.checkAdjacency()) {
                MathdokuModel.cageArrayList.add(tempCage);
                tempCage.drawCageBorders();
            }else{
                MathdokuView.showGenericPopup("The Import was aborted as not all Cage elements were adjacent, a new game will be generated randomly.", "Close");
                createGame(MathdokuView.GRID_SIZE);
                return;
            }
        }
        if(!MathdokuModel.Cage.checkUniqueness(MathdokuModel.cageArrayList)){
            MathdokuView.showGenericPopup("The Import was aborted as at least two Cages had elements in common, a new game will be generated randomly.", "Close");
            createGame(MathdokuView.GRID_SIZE);
            return;
        }
        Popup loading = new Popup();

        Timeline loadingPopup = new Timeline(new KeyFrame(
                Duration.ONE,
                ae -> {
                    VBox loadBox = new VBox();
                    Label loadLabel;
                    if(MathdokuView.GRID_SIZE > 6){
                        loadLabel = new Label("Solving Mathdoku, this may take some time due to the Grid Size");
                    } else {
                        loadLabel = new Label("Solving Mathdoku");
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
                    loading.show(MathdokuView.mainStage);
                    loading.setAnchorX(MathdokuView.mainStage.getX() + (MathdokuView.mainStage.getWidth() / 2) - (loading.getWidth() / 2));
                    loading.setAnchorY(MathdokuView.mainStage.getY() + (MathdokuView.mainStage.getHeight() / 2) - (loading.getHeight() / 2));
                    MathdokuView.rootTabPane.setDisable(true);
                }
        ));
        Timeline solveGame = new Timeline(new KeyFrame(
                Duration.ONE,
                ae -> {
                    MathdokuController.solve();
                }
        ));
        solveGame.setDelay(Duration.millis(1000));
        loadingPopup.setOnFinished(loadedPopup -> solveGame.play());
        solveGame.setOnFinished(gameCreated -> {loading.hide(); MathdokuView.rootTabPane.setDisable(false);});
        loadingPopup.play();
        MathdokuView.fontSizeChoice.setValue("Bruh");
        MathdokuView.fontSizeChoice.setValue(fontChoice);
        MathdokuModel.solved = false;
    }

    public static String fileParser(File templateFile) {
        try {
            StringBuilder s;
            try (Scanner scanner = new Scanner(templateFile)) {
                s = new StringBuilder();
                while (scanner.hasNextLine()) {
                    s.append(scanner.nextLine()).append("\n");
                }
            }
            return s.toString();
        } catch (FileNotFoundException | NullPointerException e) {
            MathdokuView.showGenericPopup("Select a File containing a Mathdoku template", "Sorry, will do next time");
        }
        return null;
    }

    static void checkMistakes(){
        boolean col = checkColumns();
        boolean row = checkRows();
        boolean cage = checkCages();
        if(col && row && cage){
            for(int i=0; i<Math.pow(MathdokuView.GRID_SIZE, 2); i++){
                MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i);
                tempCell.backgroundStyle = " -fx-background-color: #99ff99";
                tempCell.setStyle();
            }
            if(!MathdokuModel.solved) {
                MathdokuView.winAnimation();
            }
        } else {
            for(int i=0; i<Math.pow(MathdokuView.GRID_SIZE, 2); i++) {
                MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i);
                if (tempCell.backgroundStyle.equals(" -fx-background-color: #99ff99")) {
                    tempCell.setStyle(tempCell.basicStyle + tempCell.borderStyle + " -fx-background-color: #ffffff");
                }
                if(!MathdokuView.showMistakes.isSelected()){
                    tempCell.backgroundStyle = " -fx-background-color: #ffffff";
                    tempCell.setStyle();
                }
                if(tempCell == MathdokuView.focusedCell){
                    tempCell.setStyle(tempCell.basicStyle + tempCell.borderStyle + "-fx-background-color: #cbe3e6");
                }
            }
        }
    }

    static boolean checkColumns(){
        return checkColumns(false);
    }

    static boolean checkColumns(boolean autoSolverHelper){
        boolean checkFlag = true;
        int blankCounter;
        HashSet<String> column = new HashSet<>();
        for(int j=0; j<MathdokuView.GRID_SIZE; j++) {
            blankCounter = 0;
            column.clear();
            for (int i = j; i<Math.pow(MathdokuView.GRID_SIZE, 2); i += MathdokuView.GRID_SIZE) {
                MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i);
                if(tempCell.textField.getText().isEmpty()){
                    blankCounter++;
                } else {
                    column.add(tempCell.textField.getText());
                }

            }
            for (int i = j; i<Math.pow(MathdokuView.GRID_SIZE, 2); i += MathdokuView.GRID_SIZE) {
                MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i);
                if((column.size() + blankCounter ) < MathdokuView.GRID_SIZE) {

                    if(MathdokuView.showMistakes.isSelected()) {
                        tempCell.backgroundStyle = " -fx-background-color: rgba(255,0,0,0.49);";
                    }
                    tempCell.setStyle();
                    tempCell.columnMistake = true;
                    checkFlag = false;
                } else {
                    if(!(tempCell.rowMistake || tempCell.cageMistake)) {
                        tempCell.backgroundStyle = " -fx-background-color: #ffffff";
                        tempCell.setStyle();
                    }
                    tempCell.columnMistake = false;
                    if(blankCounter > 0 && !autoSolverHelper){
                        checkFlag=false;
                    }
                }
            }
        }
        return checkFlag;
    }

    static boolean checkRows(){
        return checkRows(false);
    }

    static boolean checkRows(boolean autoSolverHelper){
        boolean checkFlag = true;
        int blankCounter;
        HashSet<String> row = new HashSet<>();
        for(int j=0; j<Math.pow(MathdokuView.GRID_SIZE, 2); j = j + MathdokuView.GRID_SIZE) {
            blankCounter = 0;
            row.clear();
            for (int i = j; i < j+MathdokuView.GRID_SIZE; i ++) {
                MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i);
                if(tempCell.textField.getText().isEmpty()){
                    blankCounter++;
                } else {
                    row.add(tempCell.textField.getText());
                }
            }
            for (int i = j; i < j+MathdokuView.GRID_SIZE; i ++) {
                MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i);
                if((row.size() + blankCounter) < MathdokuView.GRID_SIZE) {
                    if(MathdokuView.showMistakes.isSelected()) {
                        tempCell.backgroundStyle = " -fx-background-color: rgba(255,0,0,0.49);";
                    }
                    tempCell.setStyle();
                    tempCell.rowMistake = true;
                    checkFlag = false;
                } else {
                    if(!(tempCell.columnMistake || tempCell.cageMistake)) {
                        tempCell.backgroundStyle = " -fx-background-color: #ffffff";
                        tempCell.setStyle();
                    }
                    tempCell.rowMistake = false;
                    if(blankCounter > 0 && !autoSolverHelper){
                        checkFlag=false;
                    }
                }
            }
        }
        return checkFlag;
    }

    static boolean checkCages(){
        boolean checkFlag = true;
        boolean cageFull;
        int result;
        for (MathdokuModel.Cage cage : MathdokuModel.cageArrayList) {
            cageFull = true;
            result = 0;
            if(cage.operator == ' '){
                if(cage.cageList.get(0).textField.getText().isEmpty()){
                    cageFull = false;
                } else {
                    result = Integer.parseInt(cage.cageList.get(0).textField.getText());
                }
            }
            if(cage.operator == '+'){
                for (Node n : cage.cageList) {
                    MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) n;
                    if(!tempCell.textField.getText().isEmpty()) {
                        result += Integer.parseInt(tempCell.textField.getText());
                    } else {
                        cageFull = false;
                        break;
                    }
                }
            } else if(cage.operator == 'x'){
                result = 1;
                for (Node n : cage.cageList) {
                    MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) n;
                    if(!tempCell.textField.getText().isEmpty()) {
                        result *= Integer.parseInt(tempCell.textField.getText());
                    } else {
                        cageFull = false;
                        break;
                    }
                }

            } else if(cage.operator == '-'){
                MathdokuView.MathdokuCell maxCell =  cage.cageList.get(0);
                for (Node n : cage.cageList) {
                    MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) n;
                    try {
                        if (Integer.parseInt(tempCell.textField.getText()) > Integer.parseInt(maxCell.textField.getText())) {
                            maxCell = tempCell;
                        }
                    } catch (NumberFormatException e) {
                        cageFull = false;
                        break;
                    }
                }
                if(cageFull) {
                    result = Integer.parseInt(maxCell.textField.getText());
                    for (Node n : cage.cageList) {
                        MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) n;
                        if(tempCell != maxCell){
                            result -= Integer.parseInt(tempCell.textField.getText());
                        }
                    }
                }
            } else if(cage.operator == '÷'){
                MathdokuView.MathdokuCell maxCell =  cage.cageList.get(0);
                for (Node n : cage.cageList) {
                    MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) n;
                    try {
                        if (Integer.parseInt(tempCell.textField.getText()) > Integer.parseInt(maxCell.textField.getText())) {
                            maxCell = tempCell;
                        }
                    } catch (NumberFormatException e) {
                        cageFull = false;
                        break;
                    }
                }
                if(cageFull) {
                    result = Integer.parseInt(maxCell.textField.getText());
                    for (Node n : cage.cageList) {
                        MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) n;
                        if(tempCell != maxCell){
                            result /= Integer.parseInt(tempCell.textField.getText());
                        }
                    }
                }
            }
            if (result != cage.result && cageFull){
                for (Node n : cage.cageList) {
                    MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) n;
                    if(MathdokuView.showMistakes.isSelected()) {
                        tempCell.backgroundStyle = " -fx-background-color: rgba(255,0,0,0.49);";
                    }
                    tempCell.setStyle();
                    tempCell.cageMistake = true;
                }
                checkFlag = false;
            } else {
                for (Node n : cage.cageList) {
                    MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) n;
                    if(!(tempCell.columnMistake || tempCell.rowMistake)) {
                        tempCell.backgroundStyle = " -fx-background-color: #ffffff;";
                        tempCell.setStyle();
                    }
                    tempCell.cageMistake = false;
                }
            }
        }
        return checkFlag;
    }

    static <T> T selectRandomElementFrom(ArrayList<T> arrayList){
        int randomIndex = (int) Math.floor( Math.random() * arrayList.size() );
        return arrayList.get(randomIndex);
    }

    static int lineCheck = 0;

    static MathdokuView.MathdokuCell selectRandomAdjacentCell(MathdokuView.MathdokuCell cell){
        int randomDirection;
        do {
            randomDirection = (int) Math.floor(Math.random() * 4) + 1;
        }while(randomDirection == lineCheck && MathdokuView.GRID_SIZE == 3);
        lineCheck = randomDirection;
        if(randomDirection == 1){
            return cell.getTopCell();
        } else if(randomDirection == 2){
            return cell.getRightCell();
        } else if(randomDirection == 3){
            return cell.getBottomCell();
        } else if(randomDirection == 4){
            return cell.getLeftCell();
        }
        return null;
    }

    static <T> ArrayList<T> rightShift(ArrayList<T> arrayList, int shift){
        ArrayList<T> shiftedArrayList =  new ArrayList<>();
        for (int i=0; i<arrayList.size(); i++){
            shiftedArrayList.add(arrayList.get((i+shift)%arrayList.size()));
        }
        return shiftedArrayList;
    }

    static int getRandomInt(int... probabilities){
        int randomNumber = (int) (Math.floor(Math.random()*100) + 1);
        int probabilityTotal = 0;
        for(int i=0; i<probabilities.length; i++){
            probabilityTotal += probabilities[i];
            if(randomNumber <= probabilityTotal){
                return i+1;
            }
        }
        return 0;
    }

    static void shuffleColumns(int shuffles){
        ArrayList<Integer> columnIndexes = new ArrayList<>();
        for (int i = 0; i < MathdokuView.GRID_SIZE; i++) {
            columnIndexes.add(i);
        }
        for (int i=0; i<shuffles; i++) {
            int col1;
            int col2;
            do{
                col1 = selectRandomElementFrom(columnIndexes);
                col2 = selectRandomElementFrom(columnIndexes);
            }while(col1 == col2);
            swapColumns(col1, col2);
        }
    }

    static void swapColumns(int col1, int col2){
        int tempInt;
        for (int i=0; i<MathdokuView.GRID_SIZE; i++){
            tempInt = Integer.parseInt(((MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i*MathdokuView.GRID_SIZE + col1)).textField.getText());
            ((MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i*MathdokuView.GRID_SIZE + col1)).textField.setText(((MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i*MathdokuView.GRID_SIZE + col2)).textField.getText());
            ((MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i*MathdokuView.GRID_SIZE + col2)).textField.setText(String.valueOf(tempInt));
        }
    }

    static void shuffleRows(int shuffles){
        ArrayList<Integer> rowIndexes = new ArrayList<>();
        for (int i = 0; i < MathdokuView.GRID_SIZE; i++) {
            rowIndexes.add(i*MathdokuView.GRID_SIZE);
        }
        for (int i=0; i<shuffles; i++) {
            int row1;
            int row2;
            do{
                row1 = selectRandomElementFrom(rowIndexes);
                row2 = selectRandomElementFrom(rowIndexes);
            }while(row1 == row2);
            swapRows(row1, row2);
        }
    }

    static void swapRows(int row1, int row2){
        int tempInt;
        for (int i=0; i<MathdokuView.GRID_SIZE; i++){
            tempInt = Integer.parseInt(((MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i + row1)).textField.getText());
            ((MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i + row1)).textField.setText(((MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i + row2)).textField.getText());
            ((MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i + row2)).textField.setText(String.valueOf(tempInt));
        }
    }

    static void createGame(int gridSize) {
        MathdokuView.GRID_SIZE = gridSize;
        MathdokuModel.solution = new int[(int) Math.pow(MathdokuView.GRID_SIZE, 2)];
        MathdokuView.createGrid();
        MathdokuModel.cageArrayList.clear();
        ArrayList<Integer> numberList = new ArrayList<>();
        ArrayList<MathdokuView.MathdokuCell> cellsToCage = new ArrayList<>();
        for (int i = 1; i <= gridSize; i++) {
            numberList.add(i);
        }
        ArrayList<Integer> randomizedNumberList = new ArrayList<>();
        for (int i = 1; i <= gridSize; i++) {
            Integer tempNumber = selectRandomElementFrom(numberList);
            numberList.remove(tempNumber);
            randomizedNumberList.add(tempNumber);
        }
        for (int i = 0; i < gridSize; i++) {
            numberList.add(i);
        }
        for (int i = 0; i < gridSize; i++) {
            Integer tempNumber;
            tempNumber = selectRandomElementFrom(numberList);
            numberList.remove(tempNumber);
            int rowIndex = gridSize * tempNumber;
            ArrayList<Integer> tempRow = rightShift(randomizedNumberList,i);
            for (int j = rowIndex; j < rowIndex + gridSize; j++) {
                MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(j);
                tempCell.textField.setText(String.valueOf(tempRow.get(j - rowIndex)));
                cellsToCage.add(tempCell);
            }
        }

        shuffleColumns(MathdokuView.GRID_SIZE);
        shuffleRows(MathdokuView.GRID_SIZE);
        for(int i=0; i<Math.pow(MathdokuView.GRID_SIZE, 2); i++) {
            MathdokuModel.solution[i] = Integer.parseInt(((MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i)).textField.getText());
        }

        ArrayList<MathdokuView.MathdokuCell> cagedCells = new ArrayList<>();

        while (!cellsToCage.isEmpty()) {
            ArrayList<MathdokuView.MathdokuCell> tempCageList = new ArrayList<>();
            MathdokuView.MathdokuCell rootCell = selectRandomElementFrom(cellsToCage);
            tempCageList.add(rootCell);
            cellsToCage.remove(rootCell);
            cagedCells.add(rootCell);
            for (int i = 0; i < getRandomInt(0, 40, 40, 20); i++) {
                MathdokuView.MathdokuCell tempCell = selectRandomElementFrom(tempCageList);
                if (tempCell.getLeftCell() == null || cagedCells.contains(tempCell.getLeftCell()) && tempCell.getRightCell() == null || cagedCells.contains(tempCell.getRightCell()) && tempCell.getTopCell() == null || cagedCells.contains(tempCell.getTopCell()) && tempCell.getBottomCell() == null || cagedCells.contains(tempCell.getBottomCell())) {
                    break;
                } else {
                    MathdokuView.MathdokuCell nextCell;
                    do {
                        nextCell = selectRandomAdjacentCell(tempCell);
                    } while (cagedCells.contains(nextCell) || nextCell == null);
                    cellsToCage.remove(nextCell);
                    tempCageList.add(nextCell);
                    cagedCells.add(nextCell);
                }
            }
            MathdokuModel.Cage tempCage = new MathdokuModel.Cage(tempCageList);
            MathdokuModel.cageArrayList.add(tempCage);
        }

        for(int i=0; i < 2 ; i++){
            ArrayList<MathdokuModel.Cage> cagesToRemove = new ArrayList<>();
            ArrayList<MathdokuModel.Cage> cagesToAdd = new ArrayList<>();

            for (MathdokuModel.Cage cage : MathdokuModel.cageArrayList) {
                if(gridSize == 3 ){
                    int singletonCounter = 0;
                    for (MathdokuView.MathdokuCell cagedCell : cagedCells) {
                        if (cagedCell.parentCage.cageList.size() == 1) {
                            singletonCounter++;
                        }
                    }
                    for (MathdokuModel.Cage removedCage : cagesToRemove) {
                        if (removedCage.cageList.size() == 1) {
                            singletonCounter -= cage.cageList.size();
                        }
                    }
                    if(singletonCounter < 3){
                        break;
                    }
                }
                ArrayList<MathdokuView.MathdokuCell> tempExpandedCage = new ArrayList<>();
                if (cage.cageList.size() == 1 && !cagesToRemove.contains(cage)) {
                    tempExpandedCage.add(cage.cageList.get(0));
                    if (cage.cageList.get(0).getLeftCell() != null && cage.cageList.get(0).getLeftCell().parentCage.cageList.size() == 1) {
                        tempExpandedCage.add(cage.cageList.get(0).getLeftCell());
                        cagesToRemove.add(cage.cageList.get(0).getLeftCell().parentCage);
                    }
                    if (cage.cageList.get(0).getTopCell() != null && cage.cageList.get(0).getTopCell().parentCage.cageList.size() == 1) {
                        tempExpandedCage.add(cage.cageList.get(0).getTopCell());
                        cagesToRemove.add(cage.cageList.get(0).getTopCell().parentCage);
                    }
                    if (cage.cageList.get(0).getRightCell() != null && cage.cageList.get(0).getRightCell().parentCage.cageList.size() == 1) {
                        tempExpandedCage.add(cage.cageList.get(0).getRightCell());
                        cagesToRemove.add(cage.cageList.get(0).getRightCell().parentCage);
                    }
                    if (cage.cageList.get(0).getBottomCell() != null && cage.cageList.get(0).getBottomCell().parentCage.cageList.size() == 1) {
                        tempExpandedCage.add(cage.cageList.get(0).getBottomCell());
                        cagesToRemove.add(cage.cageList.get(0).getBottomCell().parentCage);
                    }
                    cagesToRemove.add(cage);
                    cagesToAdd.add(new MathdokuModel.Cage(tempExpandedCage));
                }
            }
            MathdokuModel.cageArrayList.addAll(cagesToAdd);
            MathdokuModel.cageArrayList.removeAll(cagesToRemove);
        }


        for(MathdokuModel.Cage cage : MathdokuModel.cageArrayList){
            int[] results = cage.getRandomResult();
            cage.operator = (char) results[0];
            cage.result = results[1];
            cage.drawCageBorders();
        }

        MathdokuModel.solved = false;
        clearRequest();
        if(MathdokuView.checkUniqueBox.isSelected()) {
            if (gridSize == 2 && MathdokuModel.cageArrayList.get(0).cageList.size() == 2) {
                createGame(2);
                return;
            } else {
                solutionFlag = false;
                if(backtrackingMultipleSolutions(0)){
                    clearRequest();
                    createGame(gridSize);
                    return;
                }
            }
        }
        clearRequest();

    }

    static void solve(){
        MathdokuView.gameTab.setDisable(true);

        MathdokuModel.solution = new int[(int) Math.pow(MathdokuView.GRID_SIZE, 2)];
        MathdokuView.showMistakes.setSelected(false);
        MathdokuView.showMistakes.setDisable(true);
        for(int i=0; i<Math.pow(MathdokuView.GRID_SIZE, 2); i++){
            MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i);
            tempCell.textField.setDisable(true);
            tempCell.textField.setStyle(tempCell.textFieldBasicStyle + "-fx-text-inner-color: gray;");
            tempCell.textField.clear();
        }
        clearRequest();
        if(backtracking(0)){
            for(int i=0; i<Math.pow(MathdokuView.GRID_SIZE, 2); i++){
                MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i);
                tempCell.textField.setDisable(false);
                tempCell.textField.setStyle(tempCell.textFieldBasicStyle);
                MathdokuModel.solution[i] = Integer.parseInt(tempCell.textField.getText());
                tempCell.textField.clear();
            }
        } else {
            for(int i=0; i<Math.pow(MathdokuView.GRID_SIZE, 2); i++) {
                MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(i);
                tempCell.textField.setDisable(false);
                tempCell.textField.setStyle(tempCell.textFieldBasicStyle);
            }
            MathdokuView.showNoSolutionPopup();
        }
        MathdokuView.showMistakes.setDisable(false);
        MathdokuView.gameTab.setDisable(false);
    }

    static boolean backtracking(int startIndex){
        MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(startIndex);
        int nextIndex = startIndex + 1;
        for (int i=1; i<=MathdokuView.GRID_SIZE; i++){
            tempCell.textField.setText(String.valueOf(i));
            if(checkRows(true) && checkColumns(true) && checkCages()){
                if(nextIndex == Math.pow(MathdokuView.GRID_SIZE, 2) || backtracking(nextIndex)){
                    return true;
                }
            }
            tempCell.textField.clear();
        }
        return false;
    }

    static boolean solutionFlag;

    static boolean backtrackingMultipleSolutions(int startIndex) {
        if(startIndex == Math.pow(MathdokuView.GRID_SIZE, 2)){
            /* DEBUG PRINTOUT
            System.out.println("Solution Found");
            System.out.println(" ");
            */
            //Branch taken if end is reached
            if (solutionFlag) {
                //Branch taken after first solution
                return true;
            } else {
                //Branch taken during first solution
                ((MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(startIndex-1)).textField.clear();
                solutionFlag = true;
                return false;
            }
        } else {
            //Branch taken if end isn't reached
            MathdokuView.MathdokuCell tempCell = (MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(startIndex);
            for (int i = 1; i <= MathdokuView.GRID_SIZE; i++) {
                /* DEBUG PRINTOUT
                for(int j=0; j<MathdokuView.GRID_SIZE; j++){
                    for(int k = 0; k< MathdokuView.GRID_SIZE; k++){
                        String temp = ((MathdokuView.MathdokuCell) MathdokuView.mathdokuGridPane.getChildren().get(k+(MathdokuView.GRID_SIZE * j))).textField.getText();
                        if(temp == null){
                            temp = "0";
                        }
                        System.out.print(temp + "\t");
                        temp = "";
                    }
                    System.out.println(" ");
                }
                if(!checkRows(true)){
                    System.out.println("Backtracking: Row Error");
                }
                if(!checkColumns(true)){
                    System.out.println("Backtracking: Column Error");
                }
                if(!checkCages()){
                    System.out.println("Backtracking: Cage Error");
                }
                System.out.println(" ");
                */
                //Checks every number until no errors
                tempCell.textField.setText(String.valueOf(i));
                if(checkRows(true) && checkColumns(true) && checkCages() && backtrackingMultipleSolutions(startIndex + 1)){
                    //No errors & Recursive step
                    return true;
                }
            }
            /* DEBUG PRINTOUT
            System.out.println("Backtracking");
            System.out.println(" ");
            */
            //Branch taken if there is no possible solution to the cell based on the other cells
            tempCell.textField.clear();
            return false;
        }
    }
}
