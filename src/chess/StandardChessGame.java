package chess;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JOptionPane;

//TODO:
//Add functionality to make testing more automated. Should be able to interpret move lists, and step forward, maybe even step back
//Checkmate-checking: Is there any way better than brute-forcing all moves to see if legal or not?.

@SuppressWarnings("serial")
public class StandardChessGame extends Game implements Runnable
{
	private Thread m_thread;
	private StandardChessBoard m_game_board;
	private StandardChessGameGraphics m_graphics;
	private StandardChessGameAnimation m_animation;

	public void init()
	{
		m_game_board = new StandardChessBoard();
		m_graphics = new StandardChessGameGraphics();
		m_animation = new StandardChessGameAnimation(m_graphics);

		Definitions.makeInitB();
		Definitions.makeMaskB();
		Definitions.makeInitR();
		Definitions.makeMaskR();
		Definitions.makeRankR();

		//String testFEN = "8/8/7P/8/8/8/8/k5K1 b - - 0 37";
		//String testFEN = "6k1/8/5r2/6K1/8/8/8/5q2 w - - 0 37";
		//String testFEN = "1k6/2q2ppr/7p/2p5/3p2K1/2r5/8/8 w - - 0 37";
		//String testFEN = "1K6/2q6/k7/8/8/8/8/8 w - - 0 37";
		//String testFEN = "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2";
		//m_game_board.FENtoPosition(testFEN);

		setupStandard();

		//p1 = new HumanPlayer("Human WHITE", Definitions.Color.WHITE, this);
		//p2 = new HumanPlayer("Human BLACK", Definitions.Color.BLACK, this);
		p1 = new ComputerPlayer("CPU WHITE", Definitions.Color.WHITE, this);
		p2 = new ComputerPlayer("CPU BLACK", Definitions.Color.BLACK, this);

		m_thread = new Thread(this);
		m_thread.start();

		if (m_game_board.whoseTurn() == Definitions.Color.WHITE)
			p1.promptMove();
		else
			p2.promptMove();
	}

	public void setupStandard()
	{
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

		m_game_board.setTurn(Definitions.Color.WHITE);
	}

	public void run()
	{
		Definitions.State state = m_game_board.getState();
		while (state == Definitions.State.NORMAL && m_game_board.getFiftymoverulecount() < 100) //50 moves for each side
		{
			Player cur = (m_game_board.whoseTurn() == Definitions.Color.WHITE ? p1 : p2);
			if (cur.getColor() == Definitions.Color.WHITE)
				m_game_board.incrementTurncount();
			if (cur.isDone()) 
			{
				Move m = cur.getMove();
				if (m == null)
					break;

				processMove(m);
				flipTurn();
				state = m_game_board.getState();
				/*
				System.out.println(Long.toHexString(m_game_board.m_white));
				System.out.println(Long.toHexString(m_game_board.m_black));
				System.out.println(Long.toHexString(m_game_board.m_pawns));
				System.out.println(Long.toHexString(m_game_board.m_knights));
				System.out.println(Long.toHexString(m_game_board.m_bishops));
				System.out.println(Long.toHexString(m_game_board.m_rooks));
				System.out.println(Long.toHexString(m_game_board.m_queens));
				System.out.println(Long.toHexString(m_game_board.m_kings));
				System.out.println();*/
			}
			try { Thread.sleep(30); }
			catch (InterruptedException e) {}
			repaint();
		}
		String reason = "";
		Definitions.Color winner = null; //indicating stalemate by default
		if (state == Definitions.State.CHECKMATE)
		{
			winner = Definitions.flip(m_game_board.whoseTurn());
		}
		else if (state == Definitions.State.STALEMATE)
		{
			reason = "Stalemate";
		}
		else if (m_game_board.getFiftymoverulecount() >= 100)
		{
			reason = "50-move rule";
		}
		m_graphics.drawEndMessage(getGraphics(), winner, reason);
		System.out.println("The game has ended.");
	}

	public void paint(Graphics g)
	{
		//Painting with a backbuffer reduces flickering
		Image backbuffer = createImage(g.getClipBounds().width, g.getClipBounds().height);
		Graphics backg = backbuffer.getGraphics();

		m_graphics.drawBoard(backg);
		if (p1 instanceof HumanPlayer)
		{
			int sq = ((HumanPlayer)p1).getSelected();
			m_graphics.drawMovable(backg, m_game_board.allMovesPiece(7 - (sq / 8), 7 - (sq % 8)));
			m_graphics.drawSelected(backg, ((HumanPlayer)p1).getSelected());
		}
		if (p2 instanceof HumanPlayer)
		{
			int sq = ((HumanPlayer)p2).getSelected();
			m_graphics.drawMovable(backg, m_game_board.allMovesPiece(7 - (sq / 8), 7 - (sq % 8)));
			m_graphics.drawSelected(backg, ((HumanPlayer)p2).getSelected());
		}
		m_graphics.drawBorders(backg);
		m_graphics.drawMarkers(backg);
		m_graphics.drawNames(backg, p1, p2, m_game_board.whoseTurn());
		m_graphics.drawPieces(backg, m_game_board);

		g.drawImage(backbuffer, 0, 0, this);
	}

	public StandardChessBoard getBoard()
	{
		return m_game_board;
	}

	private void flipTurn() //not sure who should flip the board: game or board
	{
		//m_game_board.setTurn(Definitions.flip(m_game_board.whoseTurn()));
		Player next = (m_game_board.whoseTurn() == Definitions.Color.WHITE ? p1 : p2);
		next.promptMove();
	}

	public void promotePawn(int r, int c)
	{
		Player cur = (m_game_board.whoseTurn() == Definitions.Color.WHITE ? p1 : p2);

		if (cur instanceof HumanPlayer)
		{
			String[] param = { "Queen", "Rook", "Knight", "Bishop" };
			String input = (String) JOptionPane.showInputDialog(null, "Which piece do you want to promote to?", "Pawn Promotion", JOptionPane.QUESTION_MESSAGE, null, param, param[0]);

			int sq = (7-r)*8 + (7-c);
			m_game_board.m_pawns &= ~(1L << sq);
			if (input == "Queen")
			{
				m_game_board.m_queens |= (1L << sq);
			}
			else if (input == "Rook")
			{
				m_game_board.m_rooks |= (1L << sq);
			}
			else if (input == "Knight")
			{
				m_game_board.m_knights |= (1L << sq);		
			}
			else //Bishop
			{
				m_game_board.m_bishops |= (1L << sq);
			}
		}
		else //AI chooses queen for now
		{
			int sq = (7-r)*8 + (7-c);
			m_game_board.m_pawns &= ~(1L << sq);
			m_game_board.m_queens |= (1L << sq);
		}
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

	public void processMove(Move newMove) //don't like the ugly structure of this code
	{
		Move correspondingRookMove = m_game_board.processMove(newMove);
		m_animation.animateMove(getGraphics(), newMove, m_game_board);
		m_game_board.move(newMove); //has to be down here for time being because en passant needs to know dest sq is empty; fix if you can

		if (correspondingRookMove != null)
		{
			m_animation.animateMove(getGraphics(), correspondingRookMove, m_game_board);
			m_game_board.setTurn(Definitions.flip(m_game_board.whoseTurn())); //to undo double flipping of moving king and then rook
			m_game_board.move(correspondingRookMove);
		}

		Piece movedPiece = m_game_board.getPiece(newMove.rf, newMove.cf);
		if (movedPiece instanceof Pawn)
		{
			if (((m_game_board.whoseTurn() == Definitions.Color.BLACK) && (newMove.rf == 0)) 
					|| ((m_game_board.whoseTurn() == Definitions.Color.WHITE) && (newMove.rf == 7))) //flipped by earlier move
			{
				promotePawn(newMove.rf, newMove.cf);
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
