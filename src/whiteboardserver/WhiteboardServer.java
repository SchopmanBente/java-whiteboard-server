package whiteboardserver;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Random;

import shared.messages.client.DrawingMessage;
import shared.messages.server.ServerMessage;
import shared.messages.server.UsersMessage;
import shared.messages.server.WhiteboardMessage;
import shared.model.ColorChange;
import shared.model.Line;
import shared.model.Stamp;
import shared.model.Ring;
import shared.model.TextDrawing;
import shared.model.User;

public class WhiteboardServer extends Observable {
	private List<ClientHandler> clients = new ArrayList<>();
	private BufferedImage whiteboard;
	private ServerSocket serverSocket = null;

	/*
	 * De main-methode die afhankelijk van de args-length een WhiteboardServer
	 * genereert die een socket opzet op port 2500 of een port naar keuze.
	 * 
	 * @param: String[] args
	 * 
	 * @return: Een WhiteboardServer
	 */
	public static void main(String[] args) {
		if (args.length == 1) {
			int port = Integer.parseInt(args[0]);
			new WhiteboardServer(port);
		} else {
			new WhiteboardServer(2500);
		}

	}

	/*
	 * Constructor WhiteboardServer
	 * 
	 * @param int port
	 */
	public WhiteboardServer(int port) {
		// BufferedImage om eenvoudig tekeningen te kunnen toevoegen
		whiteboard = new BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB);
		// Achtergrondkleur voor whiteboard maken
		Graphics2D graphics = whiteboard.createGraphics();
		graphics.setBackground(new Color(255, 255, 255));
		graphics.fillRect(0, 0, whiteboard.getWidth(), whiteboard.getHeight());

		try {
			serverSocket = new ServerSocket(port);

			System.out.println("Server started op port " + port);

			while (true) {
				Socket clientSocket = serverSocket.accept();
				clients.add(new ClientHandler(clientSocket, this));
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (serverSocket != null)
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}

	/*
	 * Methode waardoor naar elke actieve client een ServerMessage wordt
	 * verstuurd
	 * 
	 * @param ServerMessage message
	 */
	public void tellEveryone(ServerMessage message) {
		System.out.println("Let's tell everyone!");

		for (ClientHandler client : clients) {
			client.send(message);
		}
	}

	/*
	 * Methode om het whiteboard te verzenden naar clients
	 * 
	 * @param void
	 */
	public void sendWhiteboardImage() {
		System.out.println("Server is sending a WhiteboardMessage");
		WhiteboardMessage message = new WhiteboardMessage(whiteboard);
		tellEveryone(message);
	}

	/*
	 * Methode om de lijst met users te verzenden naar de clients
	 * 
	 * @param void
	 */
	public void sendUsersMessage() {
		System.out.println("Server is sending a UsersMessage");
		List<User> users = new ArrayList<User>();

		for (ClientHandler client : this.clients) {
			users.add(client.getUser());
		}

		UsersMessage message = new UsersMessage(users);
		tellEveryone(message);
	}
	

	/*
	 * Methode om een tekst op het whiteboard te plaatsen
	 * 
	 * @param DrawingMessage message
	 */
	public void drawText(DrawingMessage drawing) {
		String stroke = ((TextDrawing) drawing.getDrawing()).getText();
		System.out.println(stroke);
		int x = drawing.getDrawing().getLocation().x;
		int y = drawing.getDrawing().getLocation().y;

		Graphics2D graphics = whiteboard.createGraphics();
		graphics.setColor(drawing.getSender().getColor());
		graphics.drawString(stroke, x, y);
		graphics.dispose();

		sendWhiteboardImage();

	}

	/*
	 * Methode om een client te verwijderen uit de lijst van clients
	 * 
	 * @param ClientHandler clientHandler
	 */
	public void removeClient(ClientHandler clientHandler) {
		System.out.println("Removing client:" + clientHandler.getUser().getName());
		clients.remove(clientHandler);
	}

	/*
	 * Methode om een lijn op het whiteboard te tekenen
	 * 
	 * @param DrawingMessage message
	 */
	public void drawLine(DrawingMessage paintMessage) {
		Line line = (Line) paintMessage.getDrawing();
		Point start = (Point) line.getLocation();
		Point end = (Point) line.getEnd();
		Color kleur = paintMessage.getSender().getColor();
		System.out.println(kleur);
		Graphics2D graphics = whiteboard.createGraphics();
		graphics.setColor(kleur);
		graphics.drawLine(start.x, start.y, end.x, end.y);
		graphics.dispose();

		sendWhiteboardImage();
	}

	/*
	 * Methode om een stamp op het whiteboard te tekenen door hier een
	 * BufferedImage van te maken
	 * 
	 * @param DrawingMessage paintMessage
	 */
	public void drawStamp(DrawingMessage paintMessage) {
		Stamp stamp = (Stamp) paintMessage.getDrawing();
		Color userColor = paintMessage.getSender().getColor();

		Graphics2D graphics = whiteboard.createGraphics();

		// BufferedImage maken voor stempel
		BufferedImage image = new BufferedImage(stamp.getStamp().length, stamp.getStamp()[0].length,
				BufferedImage.TYPE_INT_ARGB);

		for (int column = 0; column < image.getWidth(); column++) {
			for (int row = 0; row < image.getHeight(); row++) {
				Color color;
				if (stamp.getStamp()[row][column]) {
					color = userColor;
				} else {
					color = new Color(255, 255, 255, 0);
				}

				image.setRGB(column, row, color.getRGB());
			}
		}

		graphics.drawImage(image, stamp.getLocation().x, stamp.getLocation().y, null);
		graphics.dispose();

		sendWhiteboardImage();

	}
	
	/*
	 * Methode om een ring op het whiteboard te tekenen door hier een
	 * BufferedImage van te maken
	 * 
	 * @param DrawingMessage paintMessage
	 */
	public void drawRing(DrawingMessage paintMessage) {
		Ring ring = (Ring) paintMessage.getDrawing();
		Point point = paintMessage.getDrawing().getLocation();
		Graphics2D graphics = whiteboard.createGraphics();
		Random r = new Random();
		int width = r.nextInt(200 - 50) + 50;
		int height = width;
		graphics.setColor(paintMessage.getSender().getColor());
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setStroke(new BasicStroke(1));
		graphics.drawArc(point.x, point.y, width, height, 0, 360);
		graphics.dispose();
		
		sendWhiteboardImage();
	}

	public void changeUserColorDrawings(Color color, ColorChange colorChange) {
	 Graphics2D graphics = whiteboard.createGraphics();
		BufferedImage image = new BufferedImage(whiteboard.getWidth(), whiteboard.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		for (int column = 0; column < image.getWidth(); column++) {
			for (int row = 0; row < image.getHeight(); row++) {
				Color newColor;
				System.out.println(whiteboard.getRGB(column, row) == color.getRGB());
				if (whiteboard.getRGB(row, column) == color.getRGB()) {
					newColor = colorChange.getKleur();
				}else{
					newColor = new Color(whiteboard.getRGB(column, row));
				}
				image.setRGB(column, row, newColor.getRGB());
			}
		}
		System.out.println("Ook hier kom ik");
		graphics.drawImage(image,0,0, null);
		graphics.dispose(); 
		sendWhiteboardImage();
		
	}

	public void updateClienthandler(ClientHandler clientHandler) {
		int index = clients.indexOf(clientHandler);
		clients.set(index, clientHandler);
		
	}

}
