package org.awillock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class ConwaysGame {

    public static final String NEXT = "n";
    public static final String QUIT = "q";

    public Grid gameGrid;

    public ConwaysGame(int xGridSize, int yGridSize) {
        List<String> initList =
                Grid.generateInitList(
                        Grid.getValidGridSize(xGridSize),
                        Grid.getValidGridSize(yGridSize));
        gameGrid = new Grid(xGridSize, yGridSize, initList);
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            ConwaysGame game = initializeGame(scanner);
            processGameInput(game, scanner);
        } catch (NumberFormatException | NoSuchElementException | IllegalStateException e) {
            System.err.println("Unexpected exception: " + e);
            e.printStackTrace();
        }
    }

    private static ConwaysGame initializeGame(Scanner scanner)
            throws NumberFormatException {
        System.out.print("\nPlease enter the number of columns (X-axis) for the grid: ");
        int xInput = scanner.nextInt();
        System.out.print("Please enter the number of rows (Y-axis) for the grid: ");
        int yInput = scanner.nextInt();

        ConwaysGame game = new ConwaysGame(xInput, yInput);
        System.out.println("\nSuccessfully initialized the starting grid:\n");
        game.gameGrid.print();
        return game;
    }

    private static void processGameInput(ConwaysGame game, Scanner scanner)
            throws NoSuchElementException, IllegalStateException {
        while (true) {
            System.out.printf("Please type '%s' to go to the next stage, or type '%s' to exit: ", NEXT, QUIT);
            String result = scanner.next();
            if (result.equalsIgnoreCase(QUIT)) {
                System.exit(0);
            }

            if (!result.equalsIgnoreCase(NEXT)) {
                System.out.printf("\nInvalid command '%s' - must be one of '%s' or '%s'\n", result, NEXT, QUIT);
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
    }

    static class Grid {
        public static final int MINIMUM_GRID_SIZE = 3;
        public static final int DEFAULT_GRID_SIZE = 5;
        public static final int MAXIMUM_GRID_SIZE = 20;
        public static final String SEPARATOR = " ";

        private final int xSize;
        private final int ySize;

        private int stage = 0;

        // The naive initial solution to this problem of course uses a matrix directly, but using a (Hash)Map allows us
        // to get O(1) insertion and lookup performance - so long as we can avoid ever iterating through the map's keys
        // or values. The key to avoiding that iteration is recognizing that the relationships between a cell and its
        // neighbours are fixed - and can thus also be looked up in constant time upon each iteration.
        //
        // Therefore, the overall performance of this solution is O(N), where N is the _number_ of cells in the grid
        // (i.e. xGridSize * yGridSize), as you need to iterate over each cell to update its state at each stage.
        private final Map<GridCoordinates, Cell> cellMap = new HashMap<>();

        // The matrix is still useful for displaying the grid contents though, so we populate it as well.
        private final Cell[][] cellMatrix;

        public Grid(int xSize, int ySize, List<String> inputList) {
            this.xSize = Grid.getValidGridSize(xSize);
            this.ySize = Grid.getValidGridSize(ySize);
            cellMatrix = new Cell[this.xSize][this.ySize];

            int yLineCount = 0;
            for (String line : inputList) {
                try {
                    String[] cellValues = line.split(SEPARATOR);
                    for (int xIndex = 0; xIndex < cellValues.length; xIndex++) {
                        int value = Integer.parseInt(cellValues[xIndex]);
                        Cell cell = new Cell(xIndex, yLineCount, value);
                        cellMatrix[xIndex][yLineCount] = cell;
                        cellMap.put(cell.coordinates, cell);
                    }
                } catch (NumberFormatException e) {
                    // Do nothing - the inputList was auto-generated. If Random isn't generating valid integers, we've
                    // got bigger problems :-)
                }
                yLineCount++;
            }
            buildAdjacencies();
        }

        public void print() {
            StringBuilder view = new StringBuilder();
            for (int yPosition = 0; yPosition < ySize; yPosition++) {
                for (int xPosition = 0; xPosition < xSize; xPosition++) {
                    view.append(cellMatrix[xPosition][yPosition].state).append(xPosition < xSize - 1 ? " " : "\n");
                }
            }
            System.out.println("Stage is: " + stage);
            System.out.println(view);
        }

        public void buildAdjacencies() {
            for (Cell cell : cellMap.values()) {
                cell.findAndRegisterNeighbours(cellMap, xSize, ySize);
            }
        }

        public boolean allCellsAreDead() {
            for (Cell cell : cellMap.values()) {
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

        public void updateCellStates() {
            for (Cell currentCell : cellMap.values()) {
                currentCell.advanceState();
            }

            for (Cell currentCell : cellMap.values()) {
                currentCell.liveOrDie();
            }
        }

        public static List<String> generateInitList(int xGridSize, int yGridSize) {
            xGridSize = Grid.getValidGridSize(xGridSize);
            yGridSize = Grid.getValidGridSize(yGridSize);

            Random random = new Random();
            List<String> initList = new ArrayList<>();
            StringBuilder output = new StringBuilder();
            for (int yPosition = 0; yPosition < yGridSize; yPosition++) {
                for (int xPosition = 0; xPosition < xGridSize; xPosition++) {
                    int cellState = random.nextInt(2);
                    output.append(cellState).append(xPosition < (xGridSize - 1) ? " " : "");
                }
                String line = output.toString();
                initList.add(line);
                output.setLength(0);
            }

            return initList;
        }

        public static int getValidGridSize(int gridSize) {
            return gridSize >= MINIMUM_GRID_SIZE && gridSize <= MAXIMUM_GRID_SIZE ? gridSize : DEFAULT_GRID_SIZE;
        }
    }

    static class Cell {
        public static final int DEAD = 0;
        public static final int ALIVE = 1;

        private final GridCoordinates coordinates;
        private final Set<Cell> neighbours = new LinkedHashSet<>();

        private int state;
        private int previousState;

        public Cell(int x, int y, int state) {
            coordinates = new GridCoordinates(x, y);
            this.state = state;
            this.previousState = state;
        }

        public void findAndRegisterNeighbours(Map<GridCoordinates, Cell> coordinatesMap, int gridXSize, int gridYSize) {
            Cell neighbour;
            for (GridCoordinates neighbourCoordinates : coordinates.getValidNeighbours(gridXSize, gridYSize)) {
                neighbour = coordinatesMap.get(neighbourCoordinates);
                if (neighbour != null) {
                    neighbours.add(neighbour);
                }
            }
        }

        public void advanceState() {
            previousState = state;
        }

        // Reads the previous state value for all neighbours, which are only updated when their state is advanced by the
        // controller (i.e. Grid).
        public void liveOrDie() {
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
        }
    }

    // NOTE: Pulling the X and Y coordinate fields back into the Cell class, and using a string tuple as the lookup key
    //       into the cellMap instead, would be a meaningful memory optimization for large values of N (i.e. xGridSize
    //       * yGridSize). This implementation separates concerns better though (aside from the annoying need to pass
    //       xGridSize and yGridSize via the Cell's interface down to its contained GridCoordinates).
    static class GridCoordinates {
        private final int xPosition;
        private final int yPosition;

        public GridCoordinates(int xPos, int yPos) {
            xPosition = xPos;
            yPosition = yPos;
        }

        // This method is one-half of the key to this entire implementation. If we tell a GridCoordinates object the
        // boundaries of the grid that it's a part of, then it can always construct the set of valid neighbour
        // coordinates in constant time, as there are always no fewer than three neighbours (for corner coordinates),
        // and no more than eight (for center coordinates).
        public Set<GridCoordinates> getValidNeighbours(int gridXSize, int gridYSize) {
            Set<GridCoordinates> neighbourList = new HashSet<>();
            for (int x = xPosition - 1; x < xPosition + 2; x++) {
                for (int y = yPosition - 1; y < yPosition + 2; y++) {
                    if (x == xPosition && y == yPosition) {
                        continue;
                    }
                    if (x < 0 || y < 0 || x > gridXSize - 1 || y > gridYSize - 1) {
                        continue;
                    }
                    neighbourList.add(new GridCoordinates(x, y));
                }
            }
            return neighbourList;
        }

        @Override
        public String toString() {
            return "X coordinate: " + xPosition + ", Y coordinate: " + yPosition;
        }

        // Since this class is being used as a key to a map, we really to implement equals() and hashCode()
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }

            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            GridCoordinates that = (GridCoordinates) object;
            return xPosition == that.xPosition && yPosition == that.yPosition;
        }

        @Override
        public int hashCode() {
            return Objects.hash(xPosition, yPosition);
        }
    }
}
