package whiteboardserver;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import shared.messages.Message;
import shared.messages.client.DrawingMessage;
import shared.messages.client.InitialMessage;
import shared.messages.client.StopMessage;
import shared.messages.client.UserMessage;
import shared.model.Line;
import shared.model.Ring;
import shared.model.Stamp;
import shared.model.TextDrawing;
import shared.model.User;

public class ClientHandler implements Runnable {
	private Socket socket;
	private ObjectInputStream reader;
	private ObjectOutputStream writer;
	private WhiteboardServer server;
	private User user;

	/*
	 * Constructor voor de ClientHandler
	 * 
	 * @param Socket socket
	 * 
	 * @param WhiteboardServer server
	 */
	public ClientHandler(Socket socket, WhiteboardServer server) {
		this.socket = socket;
		this.server = server;
		try {
			reader = new ObjectInputStream(socket.getInputStream());
			writer = new ObjectOutputStream(socket.getOutputStream());

			Thread t = new Thread(this);
			t.start();

			System.out.println("new client connected");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/*
	 * Methode om InitialMessage, StopMessge en DrawingMessage te verzenden
	 */
	@Override
	public void run() {
		Object message;
		try {
			while ((message = reader.readObject()) != null) {
				System.out.println("read - " + message);
				if (message instanceof InitialMessage) {
					System.out.print("InitialMessage found");
					sendInitialMessage(message);

				} else if (message instanceof StopMessage) {
					System.out.print("Stopmessage found");
					sendStopMessage(message);
				} else if(message instanceof UserMessage){
					System.out.println("UserMessage found!");
					sendUserMessage(message);
				} else {
					System.out.println("DrawingMessage found");
					sendDrawingMessage(message);
				}
			}
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}

	private void sendUserMessage(Object message) {
		System.out.println("Going to the usermessage~!");
		UserMessage userMessage = (UserMessage) message;
		Color old = this.user.getColor();
		this.user = new User(userMessage.getSender().getName(), userMessage.getColorChange().getKleur());
		server.updateClienthandler(this);
		server.changeUserColorDrawings(old, userMessage.getColorChange());
		server.sendUsersMessage();
		
		
	}

	/*
	 * Methode om StopMessage te laten versturen
	 * 
	 * @param Object Message
	 */
	private void sendStopMessage(Object message) {
		server.removeClient(this);
		server.sendUsersMessage();

	}

	/*
	 * Methode om een DrawingMessage te versturen
	 * 
	 * @param Object message
	 */
	private void sendDrawingMessage(Object message) {
		DrawingMessage paintMessage = (DrawingMessage) message;
		if (paintMessage.getDrawing() instanceof TextDrawing) {
			System.out.print("TextDrawing message found!");
			server.drawText(paintMessage);
		} else if (paintMessage.getDrawing() instanceof Line) {
			System.out.println("LineDrawing found");
			server.drawLine(paintMessage);
		} else if (paintMessage.getDrawing() instanceof Stamp) {
			System.out.println("StampDrawing found");
			server.drawStamp(paintMessage);
		} else if(paintMessage.getDrawing() instanceof Ring){
			System.out.println("RingDrawing found");
			server.drawRing(paintMessage);
		} else {
			System.out.print("This DrawingMessage doesn't exist ");
		}

	}

	/*
	 * Methode om een InitialMessage te verzenden
	 * 
	 * @param Object message
	 */
	private void sendInitialMessage(Object message) {
		this.setUser(((InitialMessage) message).getSender());
		System.out.println(this.getUser().getColor() + " " + this.getUser().getColor());
		server.sendWhiteboardImage();
		server.sendUsersMessage();

	}

	/*
	 * Methode om een message te versturen naar de client Deze is synchronized
	 * om ervoor te zorgen dat de lock pas vrijgegeven wordt als de message
	 * verwerkt is.
	 * 
	 * @param Message message
	 */
	public void send(Message message) {
		try {
			synchronized (writer) {
				writer.writeObject(message);
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

}
