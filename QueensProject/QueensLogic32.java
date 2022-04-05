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

        //BDDExamples.main(new String[0]);

        int nVars = size * size;
        fact.setVarNum(nVars);
        
        rules = composeRules();

        System.out.println("Is Zero: " + rules.isZero());
        System.out.println("Is One: " + rules.isOne());

        updateBoard(rules);
    }

    public int[][] getBoard() {
        return board;
    }

    public void insertQueen(int column, int row) {
        if (board[column][row] != 0) return; // Guards against invalid moves

        var queen = fact.ithVar(posToVarId(column, row));
        var newRules = rules.restrict(queen);
        System.out.println("Has Changed: " + rules.equals(newRules));
        rules = newRules;
        updateBoard(rules);
        board[column][row] = 1;
    }
    // === End of IQueensLogic === //

    /**
     * Composes all rules into a single BDD.
     */
    private BDD composeRules() {
        // Jules are combined using conjunction, so the base case is true.
        var rul = fact.one();

        rul = rul.and(queensMustNotCaptureHorizontally());
        rul = rul.and(queensMustNotCaptureVertically());
        //rul = rul.and(queensMustNotCaptureDiagonally());

        return rul;
    }

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

    private BDD queensMustNotCaptureHorizontally() {
        var rul = fact.one();
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                var placedQueen = posToVar(column, row);
                var notAllowed = fact.zero();
                for (int i = 0; i < size; i++) {
                    if (i != column) {
                        notAllowed = notAllowed.or(posToVar(i, row));
                    }
                }
                rul = rul.and(placedQueen.imp(notAllowed.not()));
            }
        }
        return rul;
    }

    private BDD queensMustNotCaptureVertically() {
        var rul = fact.one();
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                var placedQueen = posToVar(column, row);
                var notAllowed = fact.zero();
                for (int i = 0; i < size; i++) {
                    if (i != row) {
                        notAllowed = notAllowed.or(posToVar(column, i));
                    }
                }
                rul = rul.and(placedQueen.imp(notAllowed.not()));
            }
        }
        return rul;
    }
/*
    private BDD queensMustNotCaptureDiagonally() {
        var rul = fact.one();
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                var placedQueen = posToVar(column, row);
                var notAllowed = fact.zero();
                for (int c = 0; c < size; c++) {
                    for (int r = 0; r < size; r++) {
                        if (c != column && r != row && overlapDiagonal(column, row, c, r)) {
                            notAllowed = notAllowed.or(posToVar(c, r));
                        }
                    }
                }
                rul = rul.and(placedQueen.imp(notAllowed.not()));
            }
        }
        return rul;
    }

    private boolean overlapDiagonal(int col1, int row1, int col2, int row2) {
        
    }
*/
    /**
     * Assign an incrementing variable to each position on the board
     * Ie. for a 5x5 board:
     *  0  1  2  3  4 /n
     *  5  6  7  8  9 /n
     * 10 11 12 13 14 /n
     * 15 16 17 18 19 /n
     * 20 21 22 23 24
     * @param column
     * @param row
     * @return
     */
    private int posToVarId(int column, int row) {
        return column + row * size;
    }

    private BDD posToVar(int column, int row) {
        return fact.ithVar(posToVarId(column, row));
    }

    /**
     * Returns rul with the restriction that a queen must be placed at
     * (column, row).
     */
    private BDD withQueen(BDD rul, int column, int row) {
        var queen = fact.ithVar(posToVarId(column, row));
        return rul.and(queen);
    }

    /**
     * Returns rul with the restriction that a queen cannot be placed at
     * (column, row).,
     */
    private BDD withOutQueen(BDD rul, int column, int row) {
        var queen = fact.nithVar(posToVarId(column, row));
        return rul.and(queen);
    }

    /**
     * Check if the a queen can be placed at (column, row) within the current rules/state,
     * returns 1 if a queen must be placed, returns -1 if a queen can't be placed
     * and returns 0 if it might be placed.
     */
    private int validDomain(BDD rul, int column, int row) {
        // We must place queen here
        if (withOutQueen(rul, column, row).isZero())
            return 1;
            
        // We cannot place queen here
        if (withQueen(rul, column, row).isZero())
            return -1;
        
        // We *may* place queen here
        return 0;
    }

    /**
     * Update the board to represent the given rules (including restrictions
     * for queen placement).
     * @param rul
     */
    private void updateBoard(BDD rul) {
        for (int column = 0; column < size; column++) {
            for (int row = 0; row < size; row++) {
                board[column][row] = validDomain(rul, column, row);
            }
        }
    }
}
