package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {

	String temp;
	String method;
	String url;
	String pram;
	int contentLength;
	String[] tokens;
	ArrayList<User> userList = new ArrayList<User>(10);
	
	
	private static final Logger log = LoggerFactory
			.getLogger(RequestHandler.class);

	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	public void run() {
		log.debug("New Client Connect! Connected IP : {}, Port : {}",
				connection.getInetAddress(), connection.getPort());

		try (InputStream in = connection.getInputStream();
			OutputStream out = connection.getOutputStream()) {
			// TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
			BufferedReader bfReader = null;
			bfReader = new BufferedReader(new InputStreamReader(in));

			String temp = bfReader.readLine();

			while (!"".equals(temp)) {
				checkHeader(temp);
				checkContentLength(temp);
				System.out.println(temp);

				temp = bfReader.readLine();
			} // while 끝
			
			DataOutputStream dos = new DataOutputStream(out);
			if (method.equals("POST")) {
				String userInfo = IOUtils.readData(bfReader, contentLength);
				userList.add(saveUserInfo(userInfo));
				System.out.println(userList.get(0).toString());
				response302Header(dos);
				
			}
			
			if(method.equals("GET")&&url.equals("/login")){
				Map<String, String> loginUserData = HttpRequestUtils.parseQueryString(pram);
				for (int i = 0; i < userList.size(); i++) {
					if(userList.get(i).equals(loginUserData.get("userId"))&&userList.get(i).equals(loginUserData.get("password"))){
						response302Login(dos);
						break;
					}
					
				}
			}
			
			byte[] body = Files.readAllBytes(new File("./webapp" + url)
					.toPath());
			System.out.println(body);
			
			response200Header(dos, body.length);
			responseBody(dos, body);

		} catch (

		IOException e)

		{
			log.error(e.getMessage());
		}

	}

	// save method type, url 
	public void checkHeader(String line) {
		tokens = line.split(" ");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals("GET") || tokens[i].equals("POST")) {
				method = tokens[i];
				url = tokens[i + 1];
				if(url.contains("?")){
					checkLogin(url);
				}
			}
		}
	}
	
	public void checkLogin(String url) {
		tokens = url.split("\\?");
		for (int i = 0; i < tokens.length; i++) {
			if(tokens[i].equals("/login")) {
				url = tokens[i];
				pram = tokens[i+1];
			}
		}
	}
	

	// check ContentLength and save contentLenghth
	public void checkContentLength(String line) {
		tokens = line.split(" ");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals("Content-Length:")) {
				contentLength = Integer.parseInt(tokens[i + 1]);
				break;
			}
		}
	}
	// account user with input data
	public User saveUserInfo(String line) {
		if (line != "") {
			Map<String, String> map = HttpRequestUtils.parseQueryString(line);
			User user = new User(map.get("userId"), map.get("password"),
					map.get("name"), map.get("email"));
			System.out.println(user.toString());
			return user;
		}
		return null;
	}

	private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 200 OK \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void response302Header(DataOutputStream dos){
		try {
			dos.writeBytes("HTTP/1.1 302 Found \r\n");
			dos.writeBytes("Location: /index.html\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void response302Login(DataOutputStream dos){
		try {
			dos.writeBytes("HTTP/1.1 302 Found \r\n");
			dos.writeBytes("Location: /sucesslogin.html\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void responseBody(DataOutputStream dos, byte[] body) {
		try {
			dos.write(body, 0, body.length);
			dos.writeBytes("\r\n");
			dos.flush();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
}
