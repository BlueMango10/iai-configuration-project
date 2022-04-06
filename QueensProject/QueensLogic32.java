import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

public class QueensLogic32 implements IQueensLogic {
    private int size;
    private int[][] board; // 1 = Must have queen, 0 = May have queen, -1 = Cannot have queen

    private BDDFactory fact = JFactory.init(2_000_000, 200_000);
    private BDD rules;

    // === Start of IQueensLogic === //
    public void initializeBoard(int size) {
        this.size = size;
        this.board = new int[size][size];

        int nVars = size * size;
        fact.setVarNum(nVars);
        
        rules = composeRules();

        // Find the initial valid domains (due to the board size)
        updateBoard(rules);
    }

    public int[][] getBoard() {
        return board;
    }

    public void insertQueen(int column, int row) {
        if (board[column][row] != 0) return; // Guards against invalid moves

        // Get the queen we just placed as a BDD
        var queen = fact.ithVar(posToVarId(column, row));

        // Add a rule that a queen must be placed in the chosen position for a
        // solution to be valid.
        // (It may have been more appropriate to use BDD.restrict, but we could
        //  not get that approach to work the way we expected it to)
        rules = rules.and(queen);
        updateBoard(rules);
    }
    // === End of IQueensLogic === //

    /**
     * Composes all rules into a single BDD.
     */
    private BDD composeRules() {
        // Jules are combined using conjunction, so the base case is true.
        var rul = fact.one();

        rul = rul.and(eachColumnMustHaveAtLeastOneQueen());
        rul = rul.and(queensMustNotCaptureHorizontally());
        rul = rul.and(queensMustNotCaptureVertically());
        rul = rul.and(queensMustNotCaptureDiagonally());

        return rul;
    }

    // === Start of Rules === //

    /*
     * The horizonal, vertical and diagonal rules could easily have been one
     * function by merging the inner loops, but we kept them seperate for
     * clarity.
     */

    /**
     * A BDD representing the rule that each column must have at least one queen.
     */
    private BDD eachColumnMustHaveAtLeastOneQueen() {
        var rul = fact.one();
        for (int column = 0; column < size; column++) {
            var colRul = fact.zero();
            for (int row = 0; row < size; row++) {
                colRul = colRul.or(posToVar(column, row));
            }
            rul = rul.and(colRul);
        }
        return rul;
    }

    /**
     * A BDD representing the rule that queens must not be able to capture any other
     * queen horizontally.
     */
    private BDD queensMustNotCaptureHorizontally() {
        var rul = fact.one();
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                var placedQueen = posToVar(column, row);
                var notAllowed = fact.zero();
                // Inner rule creation
                for (int i = 0; i < size; i++) {
                    if (i != column) {
                        notAllowed = notAllowed.or(posToVar(i, row));
                    }
                }
                // End of inner rule creation
                rul = rul.and(placedQueen.imp(notAllowed.not()));
            }
        }
        return rul;
    }

    /**
     * A BDD representing the rule that queens must not be able to capture any other
     * queen vertically.
     */
    private BDD queensMustNotCaptureVertically() {
        var rul = fact.one();
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                var placedQueen = posToVar(column, row);
                var notAllowed = fact.zero();
                // Inner rule creation
                for (int i = 0; i < size; i++) {
                    if (i != row) {
                        notAllowed = notAllowed.or(posToVar(column, i));
                    }
                }
                // End of inner rule creation
                rul = rul.and(placedQueen.imp(notAllowed.not()));
            }
        }
        return rul;
    }

    /**
     * A BDD representing the rule that queens must not be able to capture any other
     * queen diagonally.
     */
    private BDD queensMustNotCaptureDiagonally() {
        var rul = fact.one();
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                var placedQueen = posToVar(column, row);
                var notAllowed = fact.zero();
                // Inner rule creation
                for (int c = 0; c < size; c++) {
                    for (int r = 0; r < size; r++) {
                        if (c != column && r != row && alignDiagonal(column, row, c, r)) {
                            notAllowed = notAllowed.or(posToVar(c, r));
                        }
                    }
                }
                // End of inner rule creation
                rul = rul.and(placedQueen.imp(notAllowed.not()));
            }
        }
        return rul;
    }
    
    /** Check if two positions align diagonally. */
    private boolean alignDiagonal(int col1, int row1, int col2, int row2) {
        return Math.abs(col1 - col2) == Math.abs(row1 - row2);
    }

    // === End ofRules === //

    /**
     * Assign an incrementing variable to each position on the board.
     * Ie. for a 5x5 board:
     * <pre>
     *00 01 02 03 04
     *05 06 07 08 09
     *10 11 12 13 14
     *15 16 17 18 19
     *20 21 22 23 24
     * </pre>
     */
    private int posToVarId(int column, int row) {
        return column + row * size;
    }

    /**
     * Retrieves the BDD corrosponding to the variable at a position on the board.
     */
    private BDD posToVar(int column, int row) {
        return fact.ithVar(posToVarId(column, row));
    }

    /**
     * Returns rul with the restriction that a queen must be placed at
     * (column, row). (technically a rule)
     */
    private BDD withQueen(BDD rul, int column, int row) {
        var queen = fact.ithVar(posToVarId(column, row));
        return rul.and(queen);
    }

    /**
     * Returns rul with the restriction that a queen cannot be placed at
     * (column, row). (technically a rule)
     */
    private BDD withOutQueen(BDD rul, int column, int row) {
        var queen = fact.nithVar(posToVarId(column, row));
        return rul.and(queen);
    }

    /**
     * Check if the a queen can be placed at (column, row) within the current rules/state of the board,
     * returns 1 if a queen <i>must</i> be placed, returns -1 if a queen <i>can't</i> be placed
     * and returns 0 if it <i>might</i> be placed.
     */
    private int validDomain(BDD rul, int column, int row) {
        // We must place queen here
        // (if not doing so would make the rules a contradiction)
        if (withOutQueen(rul, column, row).isZero())
            return 1;
            
        // We cannot place queen here
        // (if doing so would make the rules a contradiction)
        if (withQueen(rul, column, row).isZero())
            return -1;
        
        // We *may* place queen here
        return 0;
    }

    /**
     * Update the board to represent the given rules (including restrictions
     * for queen placement).
     */
    private void updateBoard(BDD rul) {
        for (int column = 0; column < size; column++) {
            for (int row = 0; row < size; row++) {
                board[column][row] = validDomain(rul, column, row);
            }
        }
    }
}
