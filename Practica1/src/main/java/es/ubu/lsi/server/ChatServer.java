package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;

public interface ChatServer {
	public void startup();

	public void shutdown();

	public void broadcast(ChatMessage msg);

	public void remove(int id);
}
