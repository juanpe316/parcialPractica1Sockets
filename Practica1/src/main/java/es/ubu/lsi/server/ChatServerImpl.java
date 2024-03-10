package es.ubu.lsi.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.ubu.lsi.common.ChatMessage;

public class ChatServerImpl implements ChatServer {

	private int DEFAULT_PORT = 1500;
	private int clientId;
	private SimpleDateFormat sdf;
	private int port;
	private boolean alive;
	private List<ServerThreadForClient> clientes;
	private Map<String, Set<String>> baneosPorUsuario = new HashMap<>();

	public ChatServerImpl() {
		this.port = DEFAULT_PORT;
		clientes = new ArrayList<>();
		sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
	}

	public ChatServerImpl(int port) {
		this.port = port;
		clientes = new ArrayList<>();
		sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}

	@Override
	public void startup() {

		alive = true;

		try (ServerSocket serverSocket = new ServerSocket(port);) {
			while (alive) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("Nuevo Cliente conectado: " + clientSocket.getInetAddress());

				ServerThreadForClient newClient = new ServerThreadForClient(clientSocket, clientId);
				clientes.add(newClient);

				newClient.start();

			}
			serverSocket.close();

		} catch (IOException e) {
			System.out.println("No se puede escuchar por el puerto: " + port);
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void shutdown() {
		alive = false;
		// Cierra el ServerSocket y notifica a todos los clientes
		for (ServerThreadForClient clientThread : clientes) {
			clientThread.shutdown();
		}
		System.out.println("Servidor apagado.");
	}

	@Override
	public void broadcast(ChatMessage msg) {

		String time = sdf.format(System.currentTimeMillis());
		System.out.println(time + " Enviando mensaje a todos los usuarios: " + msg.getMessage());
		synchronized (clientes) {
			for (ServerThreadForClient client : clientes) {
				client.sendMessage(msg);
			}
		}
	}

	@Override
	public void remove(int id) {

		if (id >= 0 && id < clientes.size()) {
			clientes.remove(id);
		}

	}

	public static void main(String[] args) {
		int port = 1500; // Puerto por defecto para el servidor

		// Verifica si se pasa un número de puerto como argumento
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("Argumento no válido para el puerto: " + args[0]);
				System.err.println("Usando puerto por defecto: " + port);
			}
		}

		// Crea una instancia del servidor y arranca
		ChatServerImpl server = new ChatServerImpl(port);
		System.out.println("Servidor iniciando en el puerto " + port + "...");
		server.startup();
	}

	public class ServerThreadForClient extends Thread {
		private Socket socket;
		private ObjectInputStream input;
		private ObjectOutputStream output;
		private int id;
		private String username; // Nickname del usuario conectado a este hilo
		
		
		public ServerThreadForClient(Socket socket, int id) throws IOException {
			this.socket = socket;
			this.output = new ObjectOutputStream(socket.getOutputStream());
			this.output.flush();
			this.input = new ObjectInputStream(socket.getInputStream());
			this.id = id;
		}


		public void shutdown() {
			try {
				if (input != null)
					input.close();
				if (output != null)
					output.close();
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				System.err.println("Error al cerrar los recursos del cliente: " + e.getMessage());
			}
		}


		public void sendMessage(ChatMessage msg) {
			try {
				output.writeObject(msg);
				output.flush();
			} catch (IOException e) {
				System.err.println("Error al enviar mensaje al cliente: " + e.getMessage());
			}
		}

		
		public void run() {
			ChatMessage msg;
			try {
				while (!socket.isClosed()) {
					msg = (ChatMessage) input.readObject();
					switch (msg.getType()) {
					case MESSAGE:
						if (msg.getMessage().startsWith("ban ")) {
							String usuarioBanear = msg.getMessage().substring(5);
						}
						if (msg.getMessage().startsWith("unban ")) {
							String usuarioDesBanear = msg.getMessage().substring(7);
						}
						broadcast(msg);
						break;
					case LOGOUT:
						remove(id);
						break;
					case SHUTDOWN:
						shutdown();
						break;
					}
				}
			} catch (IOException e) {
				System.err.println("Error al leer el mensaje: " + e.getMessage());
			} catch (ClassNotFoundException e) {
				System.err.println("Error al leer el mensaje: " + e.getMessage());
			} finally {
				shutdown();
			}
		}
	}
}