import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

public class QueensLogic32 implements IQueensLogic {
    private int size;
    private int[][] board; // 1 = Must have queen, 0 = May have queen, -1 = Cannot have queen

    private BDDFactory fact = JFactory.init(2_000_000, 200_000);
    private BDD rules = fact.one();

    // === Start of IQueensLogic === //
    public void initializeBoard(int size) {
        this.size = size;
        this.board = new int[size][size];

        int nVars = size * size;
        fact.setVarNum(nVars);

        
        // Add the rules relevant for each queen position to the global rules.
        for (int column = 0; column < size; column++) {
            for (int row = 0; row < size; row++) {
                rules = rules.or(rulesForPos(column, row));
            }
        }
        

        updateBoard(rules);
    }

    public int[][] getBoard() {
        return board;
    }

    public void insertQueen(int column, int row) {
        if (board[column][row] != 0) return; // Guards against invalid moves
        
        var newRules = withQueen(rules, column, row);
        System.out.println("Has Changed: " + rules.equals(newRules));
        System.out.println("Is Zero: " + newRules.isZero());
        System.out.println("Is One: " + newRules.isOne());
        rules = newRules;
        updateBoard(rules);
    }
    // === End of IQueensLogic === //

    /**
     * Returns a BDD representing the rules which are relevant when a queen is
     * placed at (column, row).
     */
    private BDD rulesForPos(int column, int row) {
        var rul = fact.one();

        // One queen per column
        for (int i = 0; i < size; i++) {
            var variable = posToVarId(column, i);
            if (i == row) {
                rul = rul.or(fact.ithVar(variable));
            } else {
                rul = rul.or(fact.nithVar(variable));
            }
        }

        /*
        // One queen per row
        for (int i = 0; i < size; i++) {
            var variable = posToVarId(i, row);
            if (i == column)
                rul = rul.or(fact.ithVar(variable));
            else
                rul = rul.or(fact.nithVar(variable));
        }
        */

        return rul;
    }

    /**
     * Assign an incrementing variable to each position on the board
     * Ie. for a 5x5 board:
     *  0  1  2  3  4
     *  5  6  7  8  9
     * 10 11 12 13 14
     * 15 16 17 18 19
     * 20 21 22 23 24
     * @param column
     * @param row
     * @return
     */
    private int posToVarId(int column, int row) {
        return column + row * size;
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
        if (withOutQueen(rul, column, row).isOne())
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
                System.out.println("What is validDomain() on column: " + column + ", row: " +
                row + "? " + validDomain(rul, column, row));
                board[column][row] = validDomain(rul, column, row);
            }
        }
    }
}
