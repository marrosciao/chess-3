package chess;

import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;

import javax.swing.JOptionPane;

//TODO:
//Add functionality to make testing more automated. Should be able to interpret move lists, and step forward, maybe even step back
//Checkmate-checking: Is there any way better than brute-forcing all moves to see if legal or not?
//
//Right now, the promptMove part for Players is a bit circular. Need to think about how to restructure program.

@SuppressWarnings("serial")
public class StandardChessGame extends Game implements Runnable
{
	private Thread m_thread;
	private Board m_game_board;
	private StandardChessGameGraphics m_graphics;
	private StandardChessGameAnimation m_animation;
	
	private int enpassantCol; //the column (0-7) of the pawn to move two spaces last turn, -1 if no pawn moved two spaces
	private boolean whiteCanCastleKingside; //false if king's rook or king have moved
	private boolean whiteCanCastleQueenside; //false if queen's rook or king have moved
	private boolean blackCanCastleKingside;
	private boolean blackCanCastleQueenside;

	public void init()
	{
		m_game_board = new Board();
		m_graphics = new StandardChessGameGraphics();
		m_animation = new StandardChessGameAnimation(m_graphics);

		whiteCanCastleKingside = true;
		whiteCanCastleQueenside = true;		
		blackCanCastleKingside = true;
		blackCanCastleQueenside = true;

		m_game_board.placePiece(new Rook(0, 0, Definitions.Color.BLACK), 0, 0);
		m_game_board.placePiece(new Knight(0, 1, Definitions.Color.BLACK), 0, 1);
		m_game_board.placePiece(new Bishop(0, 2, Definitions.Color.BLACK), 0, 2);
		m_game_board.placePiece(new Queen(0, 3, Definitions.Color.BLACK), 0, 3);
		m_game_board.placePiece(new King(0, 4, Definitions.Color.BLACK), 0, 4);
		m_game_board.placePiece(new Bishop(0, 5, Definitions.Color.BLACK), 0, 5);
		m_game_board.placePiece(new Knight(0, 6, Definitions.Color.BLACK), 0, 6);
		m_game_board.placePiece(new Rook(0, 7, Definitions.Color.BLACK), 0, 7);
		for (int c = 0; c < 8; c++) {
			m_game_board.placePiece(new Pawn(1, c, Definitions.Color.BLACK), 1, c); 
		}
		m_game_board.placePiece(new Rook(7, 0, Definitions.Color.WHITE), 7, 0);
		m_game_board.placePiece(new Knight(7, 1, Definitions.Color.WHITE), 7, 1);
		m_game_board.placePiece(new Bishop(7, 2, Definitions.Color.WHITE), 7, 2);
		m_game_board.placePiece(new Queen(7, 3, Definitions.Color.WHITE), 7, 3);
		m_game_board.placePiece(new King(7, 4, Definitions.Color.WHITE), 7, 4);
		m_game_board.placePiece(new Bishop(7, 5, Definitions.Color.WHITE), 7, 5);
		m_game_board.placePiece(new Knight(7, 6, Definitions.Color.WHITE), 7, 6);
		m_game_board.placePiece(new Rook(7, 7, Definitions.Color.WHITE), 7, 7);
		for (int c = 0; c < 8; c++) {
			m_game_board.placePiece(new Pawn(6, c, Definitions.Color.WHITE), 6, c); 
		}

		p1 = new HumanPlayer("Human WHITE", Definitions.Color.WHITE, this);
		p2 = new ComputerPlayer("CPU BLACK", Definitions.Color.BLACK, this);

		setTurn(Definitions.Color.WHITE);
		p1.promptMove();
		
		m_thread = new Thread(this);
		m_thread.start();
	}
	
	public void run()
	{
		while (true) {
			Player cur = (whoseTurn() == Definitions.Color.WHITE ? p1 : p2);
			if (cur.isDone()) {
				processMove(cur.getMove());
				flipTurn();
				Definitions.State state = getState(whoseTurn(), m_game_board);				
				if (state != Definitions.State.NORMAL) {
					break;
				}
			}
			try { Thread.sleep(30); }
			catch (InterruptedException e) {}
			repaint();
		}
		System.out.println("The game has ended.");
	}
	
	public void paint(Graphics g)
	{
		//Painting with a backbuffer reduces flickering
		Image backbuffer = createImage(g.getClipBounds().width, g.getClipBounds().height);
		Graphics backg = backbuffer.getGraphics();

		m_graphics.drawBoard(backg);
		m_graphics.drawMovable(backg, allMovesPiece(((HumanPlayer)p1).getSelected(), m_game_board));
		m_graphics.drawSelected(backg, ((HumanPlayer)p1).getSelected());
		m_graphics.drawBorders(backg);
		m_graphics.drawMarkers(backg);
		m_graphics.drawNames(backg, p1, p2, whoseTurn());
		m_graphics.drawPieces(backg, m_game_board);

		g.drawImage(backbuffer, 0, 0, this);
	}

	public Board getBoard()
	{
		return m_game_board;
	}

	public boolean isLegalMove(Move m, Board b, Definitions.Color color)
	{		
		//generate moves and use board to determine legality (is there a piece in the way? etc.)

		Piece p = b.getPiece(m.r0, m.c0);

		if ((p == null) || (p.color() != color))
		{
			return false; //source square has no piece, or selected piece is opponent's piece
		}

		Piece destination = b.getPiece(m.rf, m.cf);
		boolean occupiedDest = (destination != null);
		ArrayList<Move> moves = p.getMoves();

		if (occupiedDest && (destination.color() == color)) //case of trying to move to source square is handled here
		{
			return false; //can't move to square occupied by same color piece
		}

		if (moves.contains(m))
		{
			Board tempBoard = b.clone(); //clone board
			tempBoard.move(m);

			if (!(p instanceof Knight) && (hasPieceInWay(m, b))) 
				//if it's not a knight, it can't jump
			{
				return false;
			}

			if (inCheck(color, tempBoard))
			{
				return false; //illegal move because you will be in check after move
			}

			if (p instanceof Knight) //Update: king moves no longer automatically legal
			{
				return true; //all possible moves are automatically legal because we already checked earlier
				//that the destination square does not contain a piece of same color
			}
			if (p instanceof King)
			{
				//Is there a better way to do this? Seems ugly
				boolean canCastleKingside;
				boolean canCastleQueenside;
				if (color == Definitions.Color.WHITE)
				{
					canCastleKingside = whiteCanCastleKingside;
					canCastleQueenside = whiteCanCastleQueenside;
				}
				else
				{
					canCastleKingside = blackCanCastleKingside;
					canCastleQueenside = blackCanCastleQueenside;
				}

				if (canCastleKingside && m.cf - m.c0 > 1) //kingside castle attempt
				{
					Board temp = new Board(b);
					Move intermediate = new Move(m.r0, m.c0, m.r0, m.c0 + 1);
					temp.move(intermediate);
					return (canCastleKingside && !inCheck(color, temp) && !inCheck(color, b)); //can't be in check
				}
				if (canCastleQueenside && m.cf - m.c0 < -1) //queenside castle attempt
				{
					Board temp = new Board(b);
					Move intermediate = new Move(m.r0, m.c0, m.r0, m.c0 - 1);
					temp.move(intermediate);
					return (canCastleQueenside && !inCheck(color, temp) && !inCheck(color, b)); //can't be in check
				}
				if (Math.abs(m.cf - m.c0) <= 1)
				{
					return true; //one square moves are legal
				}
				else return false; //can't castle, so >1 square moves illegal
			}
			if (p instanceof Pawn) //must split into cases
			{
				if (m.cf != m.c0) //changed columns; must be capture
				{
					boolean validCapture = (b.getPiece(m.rf, m.cf) != null); //can't be our own piece because of earlier check
					boolean enpassant;
					if (color == Definitions.Color.WHITE)
						enpassant = ((m.cf == enpassantCol) && (m.r0 == 3));
					else
						enpassant = ((m.cf == enpassantCol) && (m.r0 == 4));
					
					return (validCapture || enpassant);
				}
				else
				{
					if (m.rf - m.r0 == 2) //push two squares
					{
						return (b.getPiece((m.rf + m.r0) / 2, m.cf) == null)
								&& (b.getPiece(m.rf, m.cf) == null); //both squares empty
					}
					else //push one square
					{
						return (b.getPiece(m.rf, m.cf) == null); //square empty
					}
				}
			}
			return true;
		}
		return false; //move is not in our move list
	}

	public ArrayList<Move> allMovesPiece(Piece p, Board b)
	{
		if (p == null) return null;
		ArrayList<Move> legalMoves = new ArrayList<Move>();
		ArrayList<Move> temp = p.getMoves();
		for (Move m : temp)
		{
			if (this.isLegalMove(m, b, p.color()))
			{
				legalMoves.add(m);
			}
		}
		return legalMoves;
	}
	
	public ArrayList<Move> allMoves(Definitions.Color color, Board b)
	{
		ArrayList<Move> legalMoves = new ArrayList<Move>();
		
		for (int r = 0; r < 8; r++)
		{
			for (int c = 0; c < 8; c++)
			{
				Piece p = b.getPiece(r, c);
				if (p != null && p.color() == color)
				{
					legalMoves.addAll(allMovesPiece(p, b));
				}
			}
		}
		return legalMoves;
	}
	
	private boolean hasPieceInWay(Move m, Board b)
	{
		int dc = m.cf - m.c0;
		int dr = m.rf - m.r0; //remember rows are counted from the top
		int cinc; //1, 0, or -1, depending on which direction the piece is headed
		int rinc; //1, 0, or -1
		
		Piece p = b.getPiece(m.r0, m.c0);
		if (p == null || p instanceof Knight)
			return false; //vacuously false for no piece, and automatically false for knights 

		if (dc < 0)
		{
			cinc = -1;
		}
		else if (dc > 0)
		{
			cinc = 1;
		}
		else
		{
			cinc = 0;
		}

		if (dr < 0)
		{
			rinc = -1;
		}
		else if (dr > 0)
		{
			rinc = 1;
		}
		else
		{
			rinc = 0;
		}

		int r = m.r0 + rinc;
		int c = m.c0 + cinc;
		while (!((r == m.rf) && (c == m.cf)))
		{
			if (b.getPiece(r, c) != null)
			{
				//System.out.println("Piece in way. Row: " + r + ", Col: " + c); //debugging pieces
				return true; //there is a piece in our way
			}
			r = r + rinc;
			c = c + cinc;
		}
		return false; //no pieces in way
	}

	//moving to Game class from Board class, since the Board doesn't necessarily know rules of game
	//added Board argument since you might want to check temporary boards too
	public boolean inCheck(Definitions.Color color, Board b)
	{		
		int kingR; 
		int kingC;
		if (color == Definitions.Color.WHITE)
		{
			int temp = b.getWhiteKingLoc();
			kingR = temp / 10;
			kingC = temp % 10;
		}
		else
		{
			int temp = b.getBlackKingLoc();
			kingR = temp / 10;
			kingC = temp % 10;
		}

		for (int r = 0; r < Definitions.NUMROWS; r++)
		{
			for (int c = 0; c < Definitions.NUMCOLS; c++)
			{
				Piece temp = b.getPiece(r, c);
				Move m = new Move(r, c, kingR, kingC);
				if ((temp != null) && (temp.color() != color) && (temp.getThreats().contains(m)))
				{
					if (temp instanceof Knight || !hasPieceInWay(m, b))
					{
						//System.out.println("In check. Row: " + r + ", Column: " + c); //for debugging purposes
						return true;
					}
				}
			}
		}
		return false; //no pieces can capture king
	}

	public boolean isCheckmate(Definitions.Color color, Board b)
	{		
		boolean check = inCheck(color, b);
		return (check && allMoves(color, b).size() == 0);
	}
	
	public boolean isStalemate(Definitions.Color color, Board b)
	{		
		boolean check = inCheck(color, b);
		ArrayList<Move> mvs = allMoves(whoseTurn(), b);
		return (!check && mvs.size() == 0);
	}
	
	public Definitions.State getState(Definitions.Color color, Board b)
	{
		if (b.getState(color) == Definitions.State.UNCHECKED)
		{
			boolean isInCheck = inCheck(color, b);
			int moves = allMoves(color, b).size();
			
			if (moves == 0)
			{
				if (isInCheck)
				{
					b.setState(color, Definitions.State.CHECKMATE);
					return Definitions.State.CHECKMATE;
				}
				else
				{
					b.setState(color, Definitions.State.STALEMATE);
					return Definitions.State.STALEMATE;
				}
			}
			else
			{
				b.setState(color, Definitions.State.NORMAL);
				return Definitions.State.NORMAL;
			}
		}
		else
		{
			return b.getState(color);
		}
	}
	
	public void promotePawn(int r, int c, Definitions.Color color)
	{
		String[] param = { "Queen", "Rook", "Knight", "Bishop" };
		String input = (String) JOptionPane.showInputDialog(null, "Which piece do you want to promote to?", "Pawn Promotion", JOptionPane.QUESTION_MESSAGE, null, param, param[0]);
		
		if (input == "Queen")
		{
			m_game_board.placePiece(new Queen(r, c, color), r, c);
		}
		else if (input == "Rook")
		{
			m_game_board.placePiece(new Rook(r, c, color), r, c);
		}
		else if (input == "Knight")
		{
			m_game_board.placePiece(new Knight(r, c, color), r, c);			
		}
		else //Bishop
		{
			m_game_board.placePiece(new Bishop(r, c, color), r, c);
		}
	}
	
	private void flipTurn()
	{
		setTurn(Definitions.flip(whoseTurn()));
		Player next = (whoseTurn() == Definitions.Color.WHITE ? p1 : p2);
		next.promptMove();
	}
	
	//TODO
	public static Move algebraicToMove(Definitions.Color color, String algebraic) //STUB
	{
		return new Move(0, 0, 0, 0);
	}
	
	public void interpretMoveList(String movelist) //does not work yet
	{
		//start with naive format of "1.e4 c5 2.Nc3 Nc6 3.f4 g6 4.Bb5 Nd4", with proper spacing and all
		String[] moves = movelist.split(" ");
		
		for (int i = 0; i < moves.length; i++)
		{
			String mv = moves[i];
			if (Character.isDigit(mv.charAt(0)))
			{
				System.out.print(mv + " "); //print out moves
				m_game_board.move(algebraicToMove(Definitions.Color.WHITE, mv.split(".")[1])); //want the part after the period
			}
			else
			{
				System.out.println(mv);
				m_game_board.move(algebraicToMove(Definitions.Color.BLACK, mv));
			}
		}
	}

	public void processMove(Move newMove)
	{
		int row = newMove.r0;
		int col = newMove.c0;
		Piece movedPiece = m_game_board.getPiece(row, col);
		m_animation.animateMove(getGraphics(), newMove, m_game_board);
		m_game_board.move(newMove); //has to be down here for time being because en passant needs to know dest sq is empty; fix if you can
		
		int castlingRow;
		if (whoseTurn() == Definitions.Color.WHITE)
		{
			castlingRow = 7;
		}
		else //Black
		{
			castlingRow = 0;
		}
		
		enpassantCol = -1; //default
		if (movedPiece instanceof Pawn)
		{
			if (Math.abs(newMove.rf - newMove.r0) == 2)
			{
				enpassantCol = newMove.c0; //enpassant now available on this column
			}
			else if ((Math.abs(newMove.cf - newMove.c0) == 1) && (m_game_board.getPiece(newMove.rf, newMove.cf) == null))
				//en passant
			{
				if (whoseTurn() == Definitions.Color.WHITE)
				{
					m_game_board.removePiece(3, newMove.cf); //not sure if this is best way, but "move" call will not erase piece
				}
				else
				{
					m_game_board.removePiece(4, newMove.cf);
				}
			}
		}
		else if (movedPiece instanceof King)
		{			
			if (whoseTurn() == Definitions.Color.WHITE)
			{
				whiteCanCastleKingside = false;
				whiteCanCastleQueenside = false;
			}
			else
			{
				blackCanCastleKingside = false;
				blackCanCastleQueenside = false;
			}

			int kingMoveLength = newMove.cf - col; //should be 2 or -2, if the move was a castling move
			if (row == castlingRow)
			{
				if (kingMoveLength == 2) //kingside
				{
					Move correspondingRookMove = new Move(castlingRow, 7, castlingRow, 5);
					m_animation.animateMove(getGraphics(), correspondingRookMove, m_game_board);
					m_game_board.move(correspondingRookMove);
				}
				else if (kingMoveLength == -2) //queenside
				{
					Move correspondingRookMove = new Move(castlingRow, 0, castlingRow, 3);
					m_animation.animateMove(getGraphics(), correspondingRookMove, m_game_board);
					m_game_board.move(correspondingRookMove);
				}
			}
		}
		else if (row == castlingRow)
		{
			if (col == 0) //queen's rook
			{
				if (whoseTurn() == Definitions.Color.WHITE)
				{
					whiteCanCastleQueenside = false;
				}
				else
				{
					blackCanCastleQueenside = false;
				}
			}
			else if (col == 7) //king's rook
			{			
				if (whoseTurn() == Definitions.Color.WHITE)
				{
					whiteCanCastleKingside = false;
				}
				else
				{
					blackCanCastleKingside = false;
				}
			}
		}

		if (movedPiece instanceof Pawn)
		{
			if (((whoseTurn() == Definitions.Color.WHITE) && (newMove.rf == 0)) 
					|| ((whoseTurn() == Definitions.Color.BLACK) && (newMove.rf == 7)))
			{
				promotePawn(newMove.rf, newMove.cf, whoseTurn());
			}
		}
	}

	//Prevents flickering when repainting
	public void update(Graphics g)
	{
		paint(g);
	}

	public void stop()
	{
		if (m_thread.isAlive()) {
			m_thread.interrupt();
		}
	}
	
	//Useless for now
	public void mouseReleased(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}

}
