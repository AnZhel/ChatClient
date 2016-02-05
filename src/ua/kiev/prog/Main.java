package ua.kiev.prog;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


class GetThread extends Thread {
	private int n;

	@Override
	public void run() {
		try {
			while (!isInterrupted()) {
				URL url = new URL("http://localhost:8080/get?from=" + n);
				HttpURLConnection http = (HttpURLConnection) url.openConnection();

				InputStream is = http.getInputStream();
				try {
					int sz = is.available();
					if (sz > 0) {
						byte[] buf = new byte[is.available()];
						is.read(buf);

						Gson gson = new GsonBuilder().create();
						Message[] list = gson.fromJson(new String(buf), Message[].class);

						for (Message m : list) {
							System.out.println(m);
							n++;
						}
					}
				} finally {
					is.close();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
	}
}

public class Main {
	static String login;
	static void showHelp(){
		System.out.println("Here commands of chat are:");
		System.out.println("$to 'nick_name' - send a private message");
		System.out.println("$users - show list of members");
		System.out.println("$room 'name_room' - change q chat room");
		System.out.println("$status 'nick_name' - check members status");
		System.out.println("$help - show this message");
		System.out.println("$exit - leave the chat");

	}

	static void disconnect() throws IOException{
		URL url = new URL("http://localhost:8080/logout?login=" + login);
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		int response = http.getResponseCode();
	}

	static boolean register(Scanner scanner) throws IOException{
		while (true) {
			System.out.println("Enter login (blank to exit): ");
			login = scanner.nextLine();
			if (login.isEmpty()) return false;
			System.out.println("Enter password: ");
			String password = scanner.nextLine();
			URL url = new URL("http://localhost:8080/register?login=" + login + "&password=" + password);
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			int response = http.getResponseCode();
			if (response == 200) return true;
			else {
				System.out.println("The login is already used!");
			};
		}
	}

	static boolean authorize(Scanner scanner) throws IOException{
		while (true){
			System.out.println("Enter login: ");
			login = scanner.nextLine();
			System.out.println("Enter password: ");
			String password = scanner.nextLine();
			URL url = new URL("http://localhost:8080/login?login=" + login+"&password="+password);
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			int response = http.getResponseCode();
			if (response==200) return true;
			else {
				System.out.println("Wrong login or password!");
				System.out.println("Type 'register' to register, 'exit' to leave the chat, other for another try to login");
				String s = scanner.nextLine();
				if (s.equals("register")) return register(scanner);
				else if (s.equals("exit"))return false;
			}
		}
	}

	static int sendMessage(String text, String to) throws IOException{
		Message m = new Message();
		m.setText(text);
		m.setFrom(login);
		m.setTo(to);
		return m.send("http://localhost:8080/add");

	}

	public static void main(String[] args) throws IOException{
		Scanner scanner = new Scanner(System.in);
		try {
			if (!authorize(scanner)) {disconnect();return;}
			System.out.println("Hello, "+ login);
			showHelp();
			GetThread th = new GetThread();
			th.setDaemon(true);
			th.start();

			while (true) {
				String text = scanner.nextLine();
				String[] words = text.split(" |:");
				String command = words[0];
				switch (command) {
					case "$help":
						showHelp();
						break;
					case "$exit":
						disconnect();
						return;
					case "$to":
						text = text.replaceFirst("\\$to","");
						text = text.replaceFirst(words[1],"").trim();
						int result = sendMessage(text,words[1]);
						if (result!=200) {disconnect(); return;}
						break;
					default:
					  result = sendMessage(text, null);
						if (result!=200) {disconnect(); return;}
				}
			}
		} finally {
			scanner.close();
		}
	}
}
