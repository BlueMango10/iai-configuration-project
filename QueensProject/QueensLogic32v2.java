import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

public class QueensLogic32v2 implements IQueensLogic{
    private int size;		// Size of quadratic game board (i.e. size = #rows = #columns)
    private int[][] board;	// Content of the board. Possible values: 0 (empty), 1 (queen), -1 (no queen allowed)
    private BDDFactory fact = JFactory.init(2_000_000, 200_000);
    private BDD[] vars;
    private BDD zero = fact.zero();
    private BDD one = fact.one();
    private BDD rules = one;


    public void initializeBoard(int size) {
        this.size = size;
        this.board = new int[size][size];
        
        int nVars = size * size;
        fact.setVarNum(nVars);

        vars = new BDD[nVars];
        for (int i = 0; i < nVars; i++) {
            vars[i] = fact.ithVar(i);
        }

        rules = rules.and(eachColumnMustHaveAtLeastOneQueen());
        rules = rules.and(queensMustNotCaptureHorizontally());
    }
   
    public int[][] getBoard() {
        return board;
    }

    public void insertQueen(int column, int row) {
        board[column][row] = 1;
    }

    private int posToVarId(int column, int row) {
        return column + row * size;
    }

    

    private BDD posToVar(int column, int row) {
        return vars[posToVarId(column, row)];
    }

    private BDD withQueenAt(BDD rules, int column, int row) {
        return rules.restrict(posToVar(column, row));
    }

    private BDD withNotQueenAt(BDD rules, int column, int row) {
        return rules.restrict(posToVar(column, row).not());
    }

    private int[][] validValues(boolean[][] queens, BDD rules) {
        BDD queenPlacement = one;
        int[][] qP = new int[size][size];
        for (int column = 0; column < size; column++) {
            for (int row = 0; row < size; row++) {
                if (queenPlacement.and(withQueenAt(rules, column, row)).isOne()) {
                    qP[column][row] = 1;
                } else if (queenPlacement.and(withNotQueenAt(rules, column, row)).isOne()) {
                    qP[column][row] = -1;
                } else {
                    qP[column][row] = 0;
                }
            }
        }
        return qP;
    }


    // === Rules ===

    private BDD eachColumnMustHaveAtLeastOneQueen() {
        var rul = one;
        for (int column = 0; column < size; column++) {
            var colRul = zero;
            for (int row = 0; row < size; row++) {
                colRul = colRul.or(posToVar(column, row));
            }
            rul = rul.and(colRul);
        }
        return rul;
    }

    private BDD queensMustNotCaptureHorizontally() {
        var rul = one;
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                var placedQueen = posToVar(column, row);
                var notAllowed = zero;
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
        var rul = one;
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                var placedQueen = posToVar(column, row);
                var notAllowed = zero;
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
}