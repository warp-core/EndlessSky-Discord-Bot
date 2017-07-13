package bot;

import java.io.FileReader;
import java.util.Scanner;

public class ESBotMain {

	public static void main(String[] args) {
		new ESBotMain();
	}
	
	public ESBotMain(){
		System.out.println("Welcome to ESBot-Command Line Interface"
				+ "\nAvailable Commands:"
				+ "\n\treadToken: reads the necessary token for the bot from token.txt"
				+ "\n\tinputToken: reads next input as token"
				+ "\n\tdisplayToken: displays the current token"
				+ "\n\tstart: starts the bot with the given token"
				+ "\n\tstart: stops the bot"
				+ "\n\texit: stops the bot and exits ESBot-Command Line Interface"
				+ "\nwhile the bot is running, the token can not be changed");
		
		boolean running = true;
		boolean botRunning = false;
		
		ESBot esBot = null;
		String token = "no token given";
		
		Scanner keyboard = new Scanner(System.in);
		String input = "";
		
		// Start bot on launch.
		try{
			Scanner in = new Scanner(new FileReader("token.txt"));
			token = in.nextLine();
			in.close();
			System.out.println("Reading successfull");
		}catch(Exception e){
			System.out.println("Reading failed");
			e.printStackTrace();
		}
		botRunning = true;
		esBot = new ESBot(token);
		
		while(running){
			input = keyboard.nextLine();
			if(!botRunning){
				switch(input){
				case "readToken":
					try{
						Scanner in = new Scanner(new FileReader("token.txt"));
						token = in.nextLine();
						in.close();
						System.out.println("Reading successfull");
					}catch(Exception e){
						System.out.println("Reading failed");
						e.printStackTrace();
					}
					break;
				case "inputToken":
					token = keyboard.nextLine();
					break;
				case "displayToken":
					System.out.println("Current Token: " + token);
					break;
				case "start":
					botRunning = true;
					esBot = new ESBot(token);
					break;
				case "stop":
					System.out.println("ESBot not running!");
					break;
				case "exit":
					System.out.println("Thanks for using ESBot!");
					System.exit(0);
					return;
				default:
					break;
				}
			}else{
				switch(input){
				case "stop":
					esBot.disconnect();
					botRunning = false;
					System.out.println("Stopped ESBot!");
					break;
				case "exit":
					esBot.shutdown();
					botRunning = false;
					System.out.println("Thanks for using ESBot!");
					System.exit(0);
					return;
				default:
					break;
				}
			}
		}
		keyboard.close();
		System.exit(0);
	}

}
