package org.awillock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class ConwaysGame {

    private int gridSize;
    public Grid gameGrid;

    public ConwaysGame(int gridSize) {
        this.gridSize = gridSize;
        List<String> initList = Grid.generateInitList(gridSize);
        gameGrid = new Grid(gridSize, initList);
    }

    public static void main(String[] args) {

        ConwaysGame game = new ConwaysGame(5);

        System.out.println("\nWelcome to Conway's Game. Here is the starting grid:\n");
        game.gameGrid.print();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Please type 'next' to go to the next stage, or type 'quit' or 'exit' to quit: ");

                String result = scanner.next();
                if (result.equalsIgnoreCase("quit") || result.equalsIgnoreCase("exit")) {
                    System.exit(0);
                }

                if (!result.equalsIgnoreCase("next")) {
                    System.out.print(
                            String.format(
                                    "\nInvalid command '%s' - must be one of 'next', 'quit' or 'exit'\n", result));
                    continue;
                }

                game.gameGrid.incrementStage();
                System.out.println("Here is the new grid state:\n");
                game.gameGrid.print();

                if (game.gameGrid.allCellsAreDead()) {
                    System.out.println("All cells have died. The game is over.");
                    System.exit(0);
                }

            }
        } catch (NoSuchElementException | IllegalStateException e) {
            // Do nothing
        }
    }

    class Grid {
        public static int MINIMUM_GRID_SIZE = 3;
        public static int DEFAULT_GRID_SIZE = 4;
        public static int MAXIMUM_GRID_SIZE = 10;

        private int size;
        private int stage = 0;
        private Cell[][] cellMatrix;
        private Map<GridCoordinate, Cell> cellDictionary = new HashMap<>();

        private static String SEPARATOR = " ";

        public Grid(int size, List<String> inputList) {
            this.size = size;
            cellMatrix = new Cell[size][size];

            int lineCount = 0;
            System.out.println("Number of lines is: " + inputList.size());
            for (String line : inputList) {
                try {
                    String[] cellValues = line.split(SEPARATOR);
                    for (int index = 0; index < cellValues.length; index++) {
                        int value = Integer.parseInt(cellValues[index]);
                        Cell cell = new Cell(index, lineCount, value);
                        cellMatrix[index][lineCount] = cell;
                        cellDictionary.put(cell.coordinate, cell);
                    }
                } catch (NumberFormatException e) {
                    // Do nothing
                }
                lineCount++;
            }
            buildAdjacencies();
        }

        public void print() {
            StringBuilder output = new StringBuilder();
            for (int yPosition = 0; yPosition < size; yPosition++) {
                for (int xPosition = 0; xPosition < size; xPosition++) {
                    output.append(cellMatrix[xPosition][yPosition].state + ((xPosition < size - 1) ? " " : "\n"));
                }
            }
            System.out.println("Stage is: " + stage);
            System.out.println(output.toString());
        }

        public boolean allCellsAreDead() {
            for (Cell cell : cellDictionary.values()) {
                if (cell.state == Cell.ALIVE) {
                    return false;
                }
            }
            return true;
        }

        public void incrementStage() {
            stage++;
            updateCellStates();
        }

        public void buildAdjacencies() {
            for (Cell cell : cellDictionary.values()) {
                cell.findAndRegisterNeighbours(cellDictionary, size);
            }
        }

        public void updateCellStates() {
            for (Cell currentCell : cellDictionary.values()) {
                currentCell.advanceState();
            }

            for (Cell currentCell : cellDictionary.values()) {
                currentCell.liveOrDie();
            }
        }

        public static List<String> generateInitList(int gridSize) {
            if (gridSize < MINIMUM_GRID_SIZE || gridSize > MAXIMUM_GRID_SIZE) {
                gridSize = DEFAULT_GRID_SIZE;
            }

            Random random = new Random();
            List<String> initList = new ArrayList<>();
            StringBuilder output = new StringBuilder();
            for (int yPosition = 0; yPosition < gridSize; yPosition++) {
                for (int xPosition = 0; xPosition < gridSize; xPosition++) {
                    int cellState = random.nextInt(2);
                    output.append(cellState + ((xPosition < gridSize - 1) ? " " : ""));
                }
                String line = output.toString();
                initList.add(line);
                output.setLength(0);
            }

            return initList;
        }
    }

    class Cell {
        public static int DEAD = 0;
        public static int ALIVE = 1;

        public int state;
        public int previousState;
        public GridCoordinate coordinate;
        private Set<Cell> neighbours = new LinkedHashSet<>();

        public Cell(int x, int y, int state) {
            coordinate = new GridCoordinate(x, y);
            this.state = state;
            this.previousState = state;
        }

        public void findAndRegisterNeighbours(Map<GridCoordinate, Cell> dictionary, int gridSize) {
            Cell neighbour;
            for (GridCoordinate neighbourCoordinate : coordinate.getValidNeighbours(gridSize)) {
                neighbour = dictionary.get(neighbourCoordinate);
                if (neighbour != null) {
                    neighbours.add(neighbour);
                }
            }
        }

        public int advanceState() {
            int oldPreviousState = previousState;
            previousState = state;
            return oldPreviousState;
        }

        // Reads the previous state value for all neighbours, which are only updated when their state is advanced by the
        // controller (i.e. Grid).
        public int liveOrDie() {
            int liveNeighbours = 0;
            for (Cell neighbour : neighbours) {
                if (neighbour.previousState == ALIVE) {
                    liveNeighbours++;
                }
            }

            if (liveNeighbours < 2) {
                state = DEAD;
            } else if (liveNeighbours > 3) {
                state = DEAD;
            } else if (liveNeighbours == 3 && state == DEAD) {
                state = ALIVE;
            }
            return state;
        }
    }

    public class GridCoordinate {
        int xPosition;
        int yPosition;

        public GridCoordinate(int xPos, int yPos) {
            xPosition = xPos;
            yPosition = yPos;
        }

        public boolean equals(GridCoordinate otherCoordinate) {
            if (otherCoordinate == null) {
                return false;
            }
            if (otherCoordinate == this) {
                return true;
            }
            if (this.xPosition == otherCoordinate.xPosition && this.yPosition == otherCoordinate.yPosition) {
                return true;
            }
            return false;
        }

        public boolean isNeighbour(GridCoordinate coordinate) {
            if (coordinate == null) {
                return false;
            }
            if (coordinate.equals(this)) {
                return false;
            }
            if ((coordinate.xPosition - this.xPosition < 2 && coordinate.xPosition - this.xPosition > -2) &&
                    (coordinate.yPosition - this.xPosition < 2 && coordinate.yPosition - this.yPosition > -2)) {
                return true;
            }
            return false;
        }

        public Set<GridCoordinate> getValidNeighbours(int gridSize) {
            Set<GridCoordinate> neighbourList = new HashSet<>();
            for (int x = xPosition - 1; x < xPosition + 2; x++) {
                for (int y = yPosition - 1; y < yPosition + 2; y++) {
                    if (x == xPosition && y == yPosition) {
                        continue;
                    }
                    if (x < 0 || y < 0 || x > gridSize - 1 || y > gridSize - 1) {
                        continue;
                    }
                    neighbourList.add(new GridCoordinate(x, y));
                }
            }
            return neighbourList;
        }
    }
}
