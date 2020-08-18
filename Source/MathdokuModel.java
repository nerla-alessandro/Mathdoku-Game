import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

public class MathdokuModel {

    static class Cage {

        ArrayList<MathdokuView.MathdokuCell> cageList;
        int result;
        char operator;

        Cage(char operator, int result){
            cageList = new ArrayList<>();
            this.operator = operator;
            this.result = result;
        }

        Cage(ArrayList<MathdokuView.MathdokuCell> cageList){
            this.cageList = cageList;
            for (MathdokuView.MathdokuCell cell : this.cageList ) {
                cell.parentCage = this;
            }
        }

        void addCell(MathdokuView.MathdokuCell cell){
            cageList.add(cell);
            cell.parentCage = this;
        }

        boolean checkAdjacency(){
            boolean notAdjacent;
            if(cageList.size() == 1){
                return true;
            }
            for (MathdokuView.MathdokuCell cell : cageList) {
                notAdjacent = true;
                for (MathdokuView.MathdokuCell cellCompare : cageList){
                    if (!cell.equals(cellCompare) && cell.isAdjacent(cellCompare)) {
                        notAdjacent = false;
                        break;
                    }
                }
                if(notAdjacent){
                    return false;
                }
            }
            return true;
        }

        static boolean checkUniqueness(ArrayList<Cage> cages){
            int counter = 0;
            HashSet<MathdokuView.MathdokuCell> cellSet = new HashSet<>();
            for(Cage cage : cages) {
                for (MathdokuView.MathdokuCell cell : cage.cageList) {
                    counter++;
                    cellSet.add(cell);
                }
            }
            return cellSet.size() == counter;
        }

        boolean isElementOfCage(MathdokuView.MathdokuCell cell){
            if(cell == null){
                return false;
            }
            return this.cageList.contains(cell);
        }

        @SuppressWarnings("StringConcatenationInLoop")
        void drawCageBorders(){
            MathdokuView.MathdokuCell labelCell = null;
            for (MathdokuView.MathdokuCell cell : this.cageList) {
                if(labelCell == null || cell.gridPos < labelCell.gridPos){
                    labelCell = cell;
                }
                cell.borderStyle = " -fx-border-width: ";
                if(this.isElementOfCage(cell.getTopCell())){
                    cell.borderStyle += " 0.2";
                } else {
                    cell.borderStyle += " 2";
                }
                if(this.isElementOfCage(cell.getRightCell())){
                    cell.borderStyle += " 0.2";
                } else {
                    cell.borderStyle += " 2";
                }
                if(this.isElementOfCage(cell.getBottomCell())){
                    cell.borderStyle += " 0.2";
                } else {
                    cell.borderStyle += " 2";
                }
                if(this.isElementOfCage(cell.getLeftCell())){
                    cell.borderStyle += " 0.2";
                } else {
                    cell.borderStyle += " 2";
                }
                cell.borderStyle += ";";
                cell.setStyle(cell.basicStyle + cell.borderStyle + " -fx-background-color: #ffffff;");
            }
            if (labelCell != null) {
                labelCell.topLabel.setText(" " + this.result + "" + this.operator);
            }
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder(result + "" + operator + " ");
            for (MathdokuView.MathdokuCell cell: cageList) {
                s.append(cell).append(",");
            }
            return s.toString();
        }

        public int[] getRandomResult() {
            int[] cageResult = new int[2];
            MathdokuView.MathdokuCell maxValueCell;
            boolean resultError;
            if(cageList.size() == 1){
                cageResult[0] = ' ';
                cageResult[1] = Integer.parseInt(cageList.get(0).textField.getText());
                return cageResult;
            }
            do {
                resultError = false;
                cageResult[1] = 0;
                switch (MathdokuController.getRandomInt(20, 20, 30, 30)) {
                    case 1:
                        cageResult[0] = '+';
                        for (MathdokuView.MathdokuCell cell : this.cageList) {
                            cageResult[1] = cageResult[1] + Integer.parseInt(cell.textField.getText());
                        }
                        break;
                    case 2:
                        cageResult[0] = 'x';
                        cageResult[1] = 1;
                        for (MathdokuView.MathdokuCell cell : this.cageList) {
                            cageResult[1] *= Integer.parseInt(cell.textField.getText());
                        }
                        break;
                    case 3:
                        cageResult[0] = '-';
                        maxValueCell = this.cageList.get(0);
                        for (MathdokuView.MathdokuCell cell : this.cageList) {
                            if (Integer.parseInt(cell.textField.getText()) > Integer.parseInt(maxValueCell.textField.getText())) {
                                maxValueCell = cell;
                            }
                        }
                        cageResult[1] = Integer.parseInt(maxValueCell.textField.getText());
                        for (MathdokuView.MathdokuCell cell : this.cageList) {
                            if (cell != maxValueCell) {
                                cageResult[1] -= Integer.parseInt(cell.textField.getText());
                            }
                            if(cageResult[1] < 0){
                                resultError = true;
                            }
                        }
                        break;
                    case 4:
                        cageResult[0] = '÷';
                        maxValueCell = this.cageList.get(0);
                        for (MathdokuView.MathdokuCell cell : this.cageList) {
                            if (Integer.parseInt(cell.textField.getText()) > Integer.parseInt(maxValueCell.textField.getText())) {
                                maxValueCell = cell;
                            }
                        }
                        cageResult[1] = Integer.parseInt(maxValueCell.textField.getText());
                        for (MathdokuView.MathdokuCell cell : this.cageList) {
                            if (cell != maxValueCell) {
                                if(cageResult[1] % Integer.parseInt(cell.textField.getText()) == 0){
                                    cageResult[1] /= Integer.parseInt(cell.textField.getText());
                                } else {
                                    resultError = true;
                                }
                            }
                        }
                        break;
                    default:
                        cageResult[0] = '+';
                }
            }while(resultError);
            return cageResult;
        }
    }

    static class Move {
        MathdokuView.MathdokuCell cell;
        int value;
        int prev_value;

        public Move(MathdokuView.MathdokuCell cell, int value) {
            this.cell = cell;
            this.value = value;
            prev_value = this.cell.lastValue;
            this.cell.lastValue = value;
        }

        @Override
        public String toString() {
            return "Move{" + cell + ", " + value + "}";
        }
    }

    static Stack<Move> moveStack =  new Stack<>();
    static Stack<Move> redoStack =  new Stack<>();
    static ArrayList<Cage> cageArrayList = new ArrayList<>();

    static boolean solved = false;
    static int[] solution;


}
