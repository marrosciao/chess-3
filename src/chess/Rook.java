package chess;
import java.util.ArrayList;


public class Rook extends Piece
{
	public Rook(int row, int col, Definitions.Color color)
	{
		super(row, col, color, "R");
	}
	
	public Rook(Rook other)
	{
		super(other.row(), other.col(), other.color(), "R");
	}
	
	public ArrayList<Move> getMoves()
	{
		return getOrthogonals();
	}
	
	public ArrayList<Move> getThreats()
	{
		return getMoves();
	}
	
	public Piece clone()
	{
		return new Rook(this);
	}
}