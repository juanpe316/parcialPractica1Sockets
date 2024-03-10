package es.ubu.lsi.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Clase ChatClientImpl, implementa la estructura del cliente para conectarse al
 * servidor. Recibiendo y enviando mensajes.
 */
public class ChatClientImpl implements ChatClient {

	/**
	 * Servidor a conectar.
	 */
	private String server;
	/**
	 * Nombre del cliente.
	 */
	private String username;
	/**
	 * Puerto a conectar del cliente.
	 */
	private int port;
	/**
	 * Cliente conectado.
	 */
	private boolean carryOn = true;
	/**
	 * Id del cliente.
	 */
	private int id;
	/**
	 * Flujo de salida.
	 */
	private ObjectOutputStream salida;
	/**
	 * Flujo de entrada.
	 */
	private ObjectInputStream entrada;
	/**
	 * Variable estática para mantener el próximo ID disponible.
	 */
	private static int nextId = 0;

	/**
	 * Constructor de la clase.
	 * 
	 * @param server   Servidor a conectar.
	 * @param port     Puerto a conectar.
	 * @param username Nombre del usuario.
	 */
	public ChatClientImpl(String server, int port, String username) {
		this.server = server;
		this.port = port;
		this.username = username;
		this.id = getNextId();
	}

	
	
	
	public String getUsername() {
		return username;
	}


	public int getId() {
		return id;
	}



	/**
	 * Devuelve el valor del id correspondiente.
	 * 
	 * @return El próximo valor del id.
	 */
	private static synchronized int getNextId() {
		return ++nextId;
	}

	/**
	 * Arrancamos el servicio.
	 */
	@SuppressWarnings("resource")
	@Override
	public boolean start() {

		try {
			Socket socket = new Socket(server, port);
			System.out.println("Conexión establecida con el servidor en " + server + ":" + port);

			entrada = new ObjectInputStream(socket.getInputStream());
			salida = new ObjectOutputStream(socket.getOutputStream());

			// Enviar mensaje inicial al servidor
			ChatMessage mensajeBienvinida = new ChatMessage(id, MessageType.MESSAGE,
					"Hola, mi nombre de usuario es " + username);
			salida.writeObject(mensajeBienvinida);
			salida.flush();

			// Iniciar hilo para escuchar mensajes del servidor
			ChatClientListener escucha = new ChatClientListener();

			Thread listenerThread = new Thread(escucha);
			listenerThread.start();

			// Leer mensajes desde consola y enviarlos al servidor
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String linea;
			while ((linea = br.readLine()) != null) {
				// Si se escribe "logout", desconectar
				if (linea.equalsIgnoreCase("logout")) {
					disconect();
					break;
				}
				if (linea.startsWith("ban ")) {
					 String usuarioABanear = linea.substring(4); // Extrae el nombre del usuario a banear
					   sendMessage(new ChatMessage(id, MessageType.MESSAGE, "ban " + usuarioABanear));
				}

				if (linea.startsWith("unban ")) {
					String usuarioADesbanear = linea.substring(6); // Extrae el nombre del usuario a desbanear
				    sendMessage(new ChatMessage(id, MessageType.MESSAGE, "unban " + usuarioADesbanear));
				}
				sendMessage(new ChatMessage(id, MessageType.MESSAGE, linea));
			}

			return true;

		} catch (IOException e) {
			System.err.println("Error al conectar con el servidor: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

	}

	/**
	 * Envía un mensaje al servidor.
	 */
	@Override
	public void sendMessage(ChatMessage msg) {
		try {
			salida.writeObject(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Se desconecta del servidor.
	 */
	@Override
	public void disconect() {

		try {
			// Crea un mensaje de tipo logout
			ChatMessage mensajeDesconexion = new ChatMessage(this.id, MessageType.LOGOUT,
					"Adiós, me estoy desconectando.");
			sendMessage(mensajeDesconexion);

			// Cierra la conexión
			if (salida != null)
				salida.close();
			if (entrada != null)
				entrada.close();
			System.out.println("Conexión cerrada.");
			carryOn = false;
		} catch (IOException e) {
			System.err.println("Error al intentar cerrar la conexión: " + e.getMessage());
			e.printStackTrace();
		}

	}

	/**
	 * Método main.
	 * 
	 * @param args Recibidos como parámetros.
	 */
	public static void main(String[] args) {
		String server = null;
		int puerto = 1500;
		String username = null;

		if (args.length < 2) {
			server = "localhost";
			username = args[0];
		} else {
			server = args[0];
			username = args[2];
		}

		ChatClientImpl nuevoCliente = new ChatClientImpl(server, puerto, username);
		nuevoCliente.start();
	}

	/**
	 * Clase interna de escucha.
	 */
	public class ChatClientListener implements Runnable {

		/**
		 * Escucha de los mensajes.
		 */
		@Override
		public void run() {
			while (carryOn) {
				try {
					ChatMessage mensaje = (ChatMessage) entrada.readObject();
					System.out.println(mensaje.getMessage());

				} catch (IOException e) {
					System.err.println("Error al leer el mensaje del servidor: " + e.getMessage());
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					System.err.println("Error al leer el mensaje del servidor: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}

	}

}
