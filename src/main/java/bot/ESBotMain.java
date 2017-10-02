package bot;

import java.io.FileReader;
import java.util.Scanner;

public class ESBotMain {

	public static void main(String[] args){
		new ESBotMain();
	}

	public ESBotMain(){
		System.out.println("Welcome to ESBot-Command Line Interface");
		final String commmandList = "\nAvailable Commands:"
				+ "\n\thelp: Displays these commands"
				+ "\n\treadToken: reads the necessary token for the bot from token.txt"
				+ "\n\tinputToken: reads next input as token"
				+ "\n\tdisplayToken: displays the current token"
				+ "\n\tstart: starts the bot with the given token"
				+ "\n\tstop: stops the bot"
				+ "\n\texit: stops the bot and exits ESBot-Command Line Interface"
				+ "\nNote:\n\tWhile the bot is running, the token can not be changed.";
		System.out.println(commmandList);
		boolean running = true;
		boolean botRunning = false;

		ESBot esBot = null;
		final String tokenDefault = "no token given";
		String token = tokenDefault;

		Scanner keyboard = new Scanner(System.in);
		String input = "";

		// Start bot on launch.
		try{
			Scanner in = new Scanner(new FileReader("token.txt"));
			token = in.nextLine();
			in.close();
			System.out.println("\nToken read successfully, autostarting ESBot instance...\n");
			esBot = new ESBot(token);
			botRunning = true;
		}
		catch(Exception e){
			System.out.println("\nReading failed. Manual token entry required.");
			e.printStackTrace();
		}

		while(running){
			input = keyboard.nextLine();
			if(!botRunning){
				switch(input){
					case "readToken":
						try{
							Scanner in = new Scanner(new FileReader("token.txt"));
							token = in.nextLine();
							in.close();
							System.out.println("Token read successfully.");
						}
						catch(Exception e){
							System.out.println("Read failed.");
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
						if(token.equals(tokenDefault))
							System.out.println("Token needed.");
						else{
							esBot = new ESBot(token);
							botRunning = true;
						}
						break;
					case "help":
						System.out.println(commmandList);
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
			}
			else{
				switch(input){
					case "help":
						System.out.println(commmandList);
						break;
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
