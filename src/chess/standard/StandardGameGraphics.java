package chess.standard;

import java.awt.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import chess.Board;
import chess.Definitions;
import chess.EasyButton;
import chess.Game;
import chess.GameApplet;
import chess.GameGraphics;
import chess.Move;
import chess.Player;

@SuppressWarnings("serial")
public class StandardGameGraphics extends GameGraphics
{
	private StandardGame m_game;
	private int m_boardOffsetX, m_boardOffsetY, m_blockSize; //for determining pixel coordinates of squares
	private Map<String, BufferedImage> m_gPieces; //mapping of piece type to picture in img folder
	private BufferedImage m_gMovable, m_gSelected;
	private BufferedImage[] m_gBlocks = new BufferedImage[2];
	private long m_movableBlocks, m_selectedBlocks; //bitmap of which squares are movable or have been selected
	private Thread m_moveAnimator;
	private MoveAnimation m_moveAnimation;
	
	public StandardGameGraphics(GameApplet applet)
	{
		m_applet = applet;
	}
	
	public void init(Game game)
	{
		m_game = (StandardGame)game;
		m_boardOffsetY = Definitions.HEIGHT/8;
		m_boardOffsetX = Definitions.HEIGHT/8;
		m_blockSize = Definitions.HEIGHT*3/4 / Definitions.NUMROWS;
		
		m_gPieces = new HashMap<String, BufferedImage>();
		try //set up our images
		{
			for (int i = 0; i < Definitions.NUMPIECES; i++) //create mapping between pieces and images of pieces
				m_gPieces.put(Definitions.PIECENAMES[i], ImageIO.read(getClass().getResourceAsStream("/Images/piece" + Definitions.PIECENAMES[i] + ".png")));
			m_gBlocks[0] = ImageIO.read(getClass().getResourceAsStream("/Images/blockW.png")); //image of light square
			m_gBlocks[1] = ImageIO.read(getClass().getResourceAsStream("/Images/blockB.png")); //image of dark square
			m_gMovable = ImageIO.read(getClass().getResourceAsStream("/Images/blockMovable.png"));
			m_gSelected = ImageIO.read(getClass().getResourceAsStream("/Images/blockSelected.png"));
		}
		catch (Exception ex)
		{
			System.out.println("Error loading images!");
		}
		
		try //set up our fonts
		{
		    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		    ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/Fonts/LBRITE.TTF")));
		    ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/Fonts/LBRITED.TTF")));
		    ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/Fonts/LBRITEDI.TTF")));
		    ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/Fonts/LBRITEI.TTF")));
		}
		catch (Exception ex)
		{
			System.out.println("Error loading fonts!");
		}
	}
	
    public Dimension getPreferredSize()
    {
        return new Dimension(640, 480); //TODO: maybe move this to Definitions
    }
    
    //For human players, we need to highlight the movable squares and the selected piece
    public void updateGameState()
    {
    	m_movableBlocks = 0;
    	m_selectedBlocks = 0;
    	StandardBoard b = m_game.getBoard();
		if (m_game.getP1() instanceof StandardHumanPlayer)
		{
			int sq = ((StandardHumanPlayer)m_game.getP1()).getSelected();
			updateMovable(b.allMovesPiece(b.toRow(sq), b.toCol(sq)));
			updateSelected(sq);
		}
		if (m_game.getP2() instanceof StandardHumanPlayer)
		{
			int sq = ((StandardHumanPlayer)m_game.getP2()).getSelected();
			updateMovable(b.allMovesPiece(b.toRow(sq), b.toCol(sq)));
			updateSelected(sq);
		}
    }
	
	private void updateMovable(ArrayList<Move> moves)
	{
		for (Move m : moves) //highlight all possible destination squares
		{
			int sq = m_game.getBoard().toSq(m.rf, m.cf);
			m_movableBlocks |= (1L << sq);
		}
	}
	
	private void updateSelected(int sq)
	{
		if (sq >= 0) //if -1, we don't want to highlight anything
			m_selectedBlocks |= (1L << sq);
	}
	
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Image backbuffer = new BufferedImage(g.getClipBounds().width, g.getClipBounds().height, BufferedImage.TYPE_INT_ARGB);
		Graphics backg = backbuffer.getGraphics();
		
		drawBackground(backg);
		drawBoard(backg);
		drawBorders(backg);
		drawMarkers(backg);
		drawNames(backg, m_game.getP1(), m_game.getP2(), m_game.getBoard().whoseTurn());
		drawPieces(backg, m_game.getBoard());
		drawGUI(backg);	
		
		if (isAnimating())
			backg.drawImage(m_moveAnimation.getFrame(), 0, 0, this);
		
		g.drawImage(backbuffer, 0, 0, this);
	}
	
	public void drawBackground(Graphics g)
	{
		g.setColor(new Color(238, 238, 238)); //TODO: Possibly move to Definitions
		g.fillRect(0, 0, Definitions.WIDTH, Definitions.HEIGHT);
	}

	//Write the player names on the right side; BOLD if it is that player's turn
	public void drawNames(Graphics g, Player p1, Player p2, Definitions.Color turn)
	{
		g.setColor(Color.BLACK);
		g.setFont(new Font("Lucida Bright", (turn == p1.getColor()? Font.BOLD : Font.PLAIN), 15));
		g.drawString(p1.getName(), m_boardOffsetX + m_blockSize * (Definitions.NUMCOLS+1) + 15, m_boardOffsetY + m_blockSize * Definitions.NUMROWS);
		g.setFont(new Font("Lucida Bright", (turn == p2.getColor()? Font.BOLD : Font.PLAIN), 15));
		g.drawString(p2.getName(), m_boardOffsetX + m_blockSize * (Definitions.NUMCOLS+1) + 15, m_boardOffsetY + 20);
	}
	
	//possibly remove because it is not reliable
	public void drawEndMessage(Graphics g, Definitions.Color winner, String reason)
	{
		g.setColor(Color.BLACK);
		g.setFont(new Font("Lucida Bright", Font.BOLD, 25));
		String message;
		if (winner == null) //stalemate
			message = "Draw by " + reason + "!";
		else if (winner == Definitions.Color.WHITE)
			message = "White wins!";
		else //BLACK
			message = "Black wins!";
		g.drawString(message, m_boardOffsetX + m_blockSize * (Definitions.NUMCOLS+1) + 15, m_boardOffsetY + m_blockSize * (Definitions.NUMROWS-4));
	}
	
	//Draws each square on the board, highlighting appropriately if it is selected or movable to
	public void drawBlock(Graphics g, int row, int col)
	{
		int x = getX(col);
		int y = getY(row);
		g.drawImage(m_gBlocks[(row+col)%2], x, y, m_blockSize, m_blockSize, null);
		int sq = m_game.getBoard().toSq(row, col);
		if ((m_selectedBlocks & (1L << sq)) != 0)
			g.drawImage(m_gSelected, x, y, m_blockSize, m_blockSize, null);
		else if ((m_movableBlocks & (1L << sq)) != 0)
			g.drawImage(m_gMovable, x, y, m_blockSize, m_blockSize, null);
	}
	
	//Draws the piece using the mapping we created above
	public void drawPiece(Graphics g, char p, int x, int y)
	{
		String pstr;
		if (Character.isUpperCase(p)) //White
			pstr = "W" + p;
		else
			pstr = "B" + Character.toUpperCase(p);
		g.drawImage(m_gPieces.get(pstr), x, y, m_blockSize, m_blockSize, null);
	}
	
	//Helper function that takes row-col coordinate and converts to pixel coordinate before feeding into drawPiece
	public void drawPieceInBoard(Graphics g, char p, int r, int c)
	{
		int y = getY(r);
		int x = getX(c);
		drawPiece(g, p, x, y);
	}
	
	//Draws the markers for rows (1-8) and columns (a-h) on the edges
	public void drawMarkers(Graphics g)
	{
		g.setColor(Color.BLACK);
		for (int row = 0, y = m_boardOffsetY + m_blockSize/2; row < Definitions.NUMROWS; row++, y += m_blockSize)
		{
			g.drawString(Definitions.RMARKERS[row], m_boardOffsetX - m_blockSize/2, y);
			g.drawString(Definitions.RMARKERS[row], m_boardOffsetX + m_blockSize * Definitions.NUMCOLS + m_blockSize/3, y);
		}
		for (int col = 0, x = m_boardOffsetX + m_blockSize/2; col < Definitions.NUMCOLS; col++, x += m_blockSize)
		{
			g.drawString(Definitions.CMARKERS[col], x, m_boardOffsetY - m_blockSize/3);
			g.drawString(Definitions.CMARKERS[col], x, m_boardOffsetY + m_blockSize * Definitions.NUMROWS + m_blockSize/2);
		}
	}
	
	//Draws the square and board borders
	public void drawBorders(Graphics g)
	{
		g.setColor(Color.BLACK);
		for (int line = 0, y = m_boardOffsetY; line <= Definitions.NUMROWS; line++, y += m_blockSize)
			g.drawLine(m_boardOffsetX, y, m_boardOffsetX + Definitions.NUMCOLS*m_blockSize, y);
		for (int line = 0, x = m_boardOffsetX; line <= Definitions.NUMCOLS; line++, x += m_blockSize)
			g.drawLine(x, m_boardOffsetY, x, m_boardOffsetY + Definitions.NUMROWS*m_blockSize);
	}
	
	//Draws every piece on the board
	public void drawPieces(Graphics g, Board b)
	{
		for (int r = 0, y = m_boardOffsetY; r < Definitions.NUMROWS; r++, y += m_blockSize)
		for (int c = 0, x = m_boardOffsetX; c < Definitions.NUMCOLS; c++, x += m_blockSize)
		{
			if (b.getPiece(r, c) != 0)
				drawPiece(g, b.getPiece(r, c), x, y);
		}
	}
	
	public void drawBoard(Graphics g)
	{
		for (int r = 0; r < Definitions.NUMROWS; r++)
		for (int c = 0; c < Definitions.NUMCOLS; c++)
		{
				drawBlock(g, r, c);
		}
	}
	
	//Draws any GUI components (currently only buttons)
	public void drawGUI(Graphics g)
	{
		for (EasyButton b : m_game.getGUI().getButtons())
		{
			BufferedImage img = (b.isPressed() ? b.getPressedImg() : b.getReleasedImg());
			g.drawImage(img, b.getX(), b.getY(), b.getW(), b.getH(), null);
		}
	}
	
	public int getRow(int y)
	{
		int relativeY = y - m_boardOffsetY;
		if (relativeY < 0 || relativeY >= Definitions.NUMROWS*m_blockSize)
			return -1;
		return relativeY / m_blockSize;
	}
	
	public int getCol(int x)
	{
		int relativeX = x - m_boardOffsetX;
		if (relativeX < 0 || relativeX >= Definitions.NUMCOLS*m_blockSize)
			return -1;
		return relativeX / m_blockSize;
	}
	
	public int getY(int row)
	{
		if (row < 0 || row >= Definitions.NUMROWS) return -1;
		return m_boardOffsetY + row*m_blockSize;
	}
	
	public int getX(int col)
	{
		if (col < 0 || col >= Definitions.NUMCOLS) return -1;
		return m_boardOffsetX + col*m_blockSize;
	}
	
	private class MoveAnimation implements Runnable
	{
		public char traveler, incumbent;
		public double curX, curY;
		public double dX, dY;
		public Move move;
		private BufferedImage m_frame;
		public final int NUMTICKS = 10;
		
		public MoveAnimation(Move m, Board b)
		{
			m_frame = new BufferedImage(Definitions.WIDTH, Definitions.HEIGHT, BufferedImage.TYPE_INT_ARGB);
			move = m;
			traveler = b.getPiece(m.r0, m.c0);
			incumbent = b.getPiece(m.rf, m.cf);
			curY = getY(m.r0);
			curX = getX(m.c0);
			dY = ((double)getY(m.rf) - curY) / NUMTICKS;
			dX = ((double)getX(m.cf) - curX) / NUMTICKS;
		}
		
		public BufferedImage getFrame()
		{
			synchronized(this)
			{
				return m_frame;
			}
		}

		public void run()
		{
			for (int t = 0; t < NUMTICKS; t++)
			{
				synchronized (this)
				{
					curX += dX;
					curY += dY;
					Graphics2D g = (Graphics2D)m_frame.getGraphics();
					g.setBackground(new Color(255, 255, 255, 0));
					g.clearRect(0, 0, Definitions.WIDTH, Definitions.HEIGHT);
					drawBlock(g, move.rf, move.cf);
					if (incumbent != 0) drawPieceInBoard(g, incumbent, move.rf, move.cf);
					drawBorders(g);
					drawPiece(g, traveler, (int)curX, (int)curY);
				}
		 		try { Thread.sleep(Definitions.TICK); }
		 		catch (InterruptedException ex) {}
			}
		}
	};
	
	private class CastleMoveAnimation extends MoveAnimation
	{
		MoveAnimation rook;
		
		public CastleMoveAnimation(Move kingMove, Move rookMove, Board b)
		{
			super(kingMove, b);
			rook = new MoveAnimation(rookMove, b);
		}

		public void run()
		{
			for (int t = 0; t < NUMTICKS; t++)
			{
				synchronized (this)
				{
					curX += dX;
					curY += dY;
					rook.curX += rook.dX;
					rook.curY += rook.dY;
					Graphics2D g = (Graphics2D)getFrame().getGraphics();
					g.setBackground(new Color(255, 255, 255, 0));
					g.clearRect(0, 0, Definitions.WIDTH, Definitions.HEIGHT);
					drawBlock(g, move.rf, move.cf);
					drawBlock(g, rook.move.rf, rook.move.cf);
					drawBorders(g);
					drawPiece(g, traveler, (int)curX, (int)curY);
					drawPiece(g, rook.traveler, (int)rook.curX, (int)rook.curY);
				}
		 		try { Thread.sleep(Definitions.TICK); }
		 		catch (InterruptedException ex) {}
			}
		}
	}
	
	private void startAnimation()
	{
		if (isAnimating() || m_moveAnimation == null)
			return;
		m_moveAnimator = new Thread(m_moveAnimation);
		m_moveAnimator.setPriority(Thread.MAX_PRIORITY); //to minimize lag
		m_moveAnimator.start();
	}

	public void animateMove(Move m, Board b)
	{
		if (m == null || b == null)
			return;
		m_moveAnimation = new MoveAnimation(m, b);
		startAnimation();
	}
	
	public void animateCastlingMoves(Move kingMove, Move rookMove, Board b)
	{
		if (kingMove == null || rookMove == null || b == null)
			return;
		m_moveAnimation = new CastleMoveAnimation(kingMove, rookMove, b);
		startAnimation();
	}

	public boolean isAnimating()
	{
		return m_moveAnimator != null && m_moveAnimator.isAlive();
	}
}