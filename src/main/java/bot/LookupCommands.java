package bot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;

import javax.imageio.ImageIO;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;

public class LookupCommands 
implements CommandExecutor{

	private String data;

	public LookupCommands() {
		data = readData();
	}

	public String readData(){
		String data = "";
		try {
			LinkedList<URL> dataFiles = new LinkedList<>();
			try(BufferedReader br = new BufferedReader(new InputStreamReader(new URL("https://raw.githubusercontent.com/MCOfficer/EndlessSky-Discord-Bot/master/data/dataFileNames.txt").openStream()))) {
				String line = br.readLine();

				while (line != null) {
					dataFiles.add(new URL("https://raw.githubusercontent.com/endless-sky/endless-sky/master/data/" + line + ".txt"));
					line = br.readLine();
				}
			}			
			for(URL url : dataFiles){
				try(BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
					StringBuilder sb = new StringBuilder();
					String line = br.readLine();

					while (line != null) {
						sb.append(line);
						sb.append(System.lineSeparator());
						line = br.readLine();
					}
					data += sb.toString();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
	@Command(aliases = {"-issue"}, description = "Provide link for \"X\" Endless Sky issue.", usage = "-issue X", privateMessages = true)
	public void onIssueCommand(MessageChannel channel, String[] args){
		if(args.length>=1){
			String request = args[0];
			String path = "https://github.com/endless-sky/endless-sky/issues/" + request;
			channel.sendMessage(path).queue();
		}
	}
	
	@Command(aliases = {"-pull"}, description = "Provide link for \"X\" Endless Sky pull request.", usage = "-pull X", privateMessages = true)
	public void onPullCommand(MessageChannel channel, String[] args){
		if(args.length>=1){
			String request = args[0];
			String path = "https://github.com/endless-sky/endless-sky/pull/" + request;
			channel.sendMessage(path).queue();
		}
	}
	
	@Command(aliases = {"-commit"}, description = "Provide link for \"X\" Endless Sky commit.", usage = "-commit X", privateMessages = true)
	public void onCommitCommand(MessageChannel channel, String[] args){
		if(args.length>=1){
			String request = args[0];
			String path = "https://github.com/endless-sky/endless-sky/commit/" + request;
			channel.sendMessage(path).queue();
		}
	}
	
	@Command(aliases = {"-lookup"}, description = "Shows image and description of X.", usage = "-lookup X", privateMessages = true)
	public void onLookupCommand(MessageChannel channel, String[] args){
		if(args.length>=1){
			String request = args[0];
			for( int i = 1; i< args.length;i++){
				request += " " + args[i];
			}
			String output = lookupData(request);
			if(output.contains("sprite")||output.contains("thumbnail")){
				int start = 0,end = 0;
				if(output.contains("thumbnail")){
					start = output.indexOf("thumbnail") + 10;
					end = output.indexOf('\n', start)-1;
				}else if(output.contains("sprite")){
					start = output.indexOf("sprite")+7;
					end = output.indexOf('\n', start)-1;
				}
				String path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + ".png?raw=true";
				if(isImage(path)){
					EmbedBuilder eb = new EmbedBuilder();
					eb.setImage(path);
					channel.sendMessage(eb.build()).queue(x -> {
						if(output.contains("description"))
							OutputHelper(channel, output.substring(output.indexOf("description")).replaceAll("description", ""));
					});
				}else{
					path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + "-0.png?raw=true";
					if(isImage(path)){
						EmbedBuilder eb = new EmbedBuilder();
						eb.setImage(path);
						channel.sendMessage(eb.build()).queue(x -> {
							if(output.contains("description"))
								OutputHelper(channel, output.substring(output.indexOf("description")).replaceAll("description", ""));
						});
					}else{
						path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + "+0.png?raw=true";
						if(isImage(path)){
							EmbedBuilder eb = new EmbedBuilder();
							eb.setImage(path);
							channel.sendMessage(eb.build()).queue(x -> {
								if(output.contains("description"))
									OutputHelper(channel, output.substring(output.indexOf("description")).replaceAll("description", ""));
							});
						}else{
							path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + "~0.png?raw=true";
							if(isImage(path)){
								EmbedBuilder eb = new EmbedBuilder();
								eb.setImage(path);
								channel.sendMessage(eb.build()).queue(x -> {
									if(output.contains("description"))
										OutputHelper(channel, output.substring(output.indexOf("description")).replaceAll("description", ""));
								});
							}else{
								path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + "=0.png?raw=true";
								if(isImage(path)){
									EmbedBuilder eb = new EmbedBuilder();
									eb.setImage(path);
									channel.sendMessage(eb.build()).queue(x -> {
										if(output.contains("description"))
											OutputHelper(channel, output.substring(output.indexOf("description")).replaceAll("description", ""));
									});
								}
							}
						}
					}
				}
			}else{
				if(output.contains("description"))
					OutputHelper(channel, output.substring(output.indexOf("description")));
			}
		}

	}

	@Command(aliases = {"-show"}, description = "Shows image and data of X.", usage = "-show X", privateMessages = true)
	public void onShowCommand(MessageChannel channel, String[] args){
		if(args.length>=1){
			String request = args[0];
			for( int i = 1; i< args.length;i++){
				request += " " + args[i];
			}
			String output = lookupData(request);
			if(output.contains("sprite")||output.contains("thumbnail")){
				int start = 0,end = 0;
				if(output.contains("thumbnail")){
					start = output.indexOf("thumbnail") + 10;
					end = output.indexOf('\n', start)-1;
				}else if(output.contains("sprite")){
					start = output.indexOf("sprite")+7;
					end = output.indexOf('\n', start)-1;
				}
				String path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + ".png?raw=true";
				if(isImage(path)){
					EmbedBuilder eb = new EmbedBuilder();
					eb.setImage(path);
					channel.sendMessage(eb.build()).queue(x -> {
						OutputHelper(channel,output);
					});
				}else{
					path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + "-0.png?raw=true";
					if(isImage(path)){
						EmbedBuilder eb = new EmbedBuilder();
						eb.setImage(path);
						channel.sendMessage(eb.build()).queue(x -> {
							OutputHelper(channel,output);
						});
					}else{
						path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + "+0.png?raw=true";
						if(isImage(path)){
							EmbedBuilder eb = new EmbedBuilder();
							eb.setImage(path);
							channel.sendMessage(eb.build()).queue(x -> {
								OutputHelper(channel,output);
							});
						}else{
							path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + "~0.png?raw=true";
							if(isImage(path)){
								EmbedBuilder eb = new EmbedBuilder();
								eb.setImage(path);
								channel.sendMessage(eb.build()).queue(x -> {
									OutputHelper(channel,output);
								});
							}else{
								path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + "=0.png?raw=true";
								if(isImage(path)){
									EmbedBuilder eb = new EmbedBuilder();
									eb.setImage(path);
									channel.sendMessage(eb.build()).queue(x -> {
										OutputHelper(channel,output);
									});
								}
							}
						}
					}
				}
			}else{
				OutputHelper(channel,output);
			}
		}

	}

	@Command(aliases = {"-showimage"}, description = "Shows image of X.", usage = "-showimage X", privateMessages = true)
	public void onShowimageCommand(MessageChannel channel, String[] args){
		if(args.length>=1){
			String request = args[0];
			for( int i = 1; i< args.length;i++){
				request += " " + args[i];
			}
			String output = lookupData(request);
			if(output.contains("sprite")||output.contains("thumbnail")){
				int start = 0,end = 0;
				if(output.contains("thumbnail")){
					start = output.indexOf("thumbnail") + 10;
					end = output.indexOf('\n', start)-1;
				}else if(output.contains("sprite")){
					start = output.indexOf("sprite")+7;
					end = output.indexOf('\n', start)-1;
				}
				String path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + ".png?raw=true";
				if(isImage(path)){
					EmbedBuilder eb = new EmbedBuilder();
					eb.setImage(path);
					channel.sendMessage(eb.build()).queue();
				}else{
					path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + "-0.png?raw=true";
					if(isImage(path)){
						EmbedBuilder eb = new EmbedBuilder();
						eb.setImage(path);
						channel.sendMessage(eb.build()).queue();
					}else{
						path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + "+0.png?raw=true";
						if(isImage(path)){
							EmbedBuilder eb = new EmbedBuilder();
							eb.setImage(path);
							channel.sendMessage(eb.build()).queue();
						}else{
							path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + "~0.png?raw=true";
							if(isImage(path)){
								EmbedBuilder eb = new EmbedBuilder();
								eb.setImage(path);
								channel.sendMessage(eb.build()).queue();
							}else{
								path = "https://github.com/endless-sky/endless-sky/raw/master/images/" + output.substring(start, end).replace("\"","") + "=0.png?raw=true";
								if(isImage(path)){
									EmbedBuilder eb = new EmbedBuilder();
									eb.setImage(path);
									channel.sendMessage(eb.build()).queue();
								}
							}
						}
					}
				}
			}
		}
	}

	@Command(aliases = {"-showdata"}, description = "Shows data of X.", usage = "-showdata X", privateMessages = true)
	public void onShowdataCommand(MessageChannel channel, String[] args){
		if(args.length>=1){
			String request = args[0];
			for( int i = 1; i< args.length;i++){
				request += " " + args[i];
			}
			String output = lookupData(request);
			OutputHelper(channel, output);
		}
	}
	
	public String lookupData(String lookup){
		lookup = checkLookup(lookup,true);
		if(lookup.length()>0){
			int start = data.indexOf(lookup);
			int end = start+lookup.length();
			do{
				end = data.indexOf('\n', end+1);
			}while(data.charAt(end+1)=='\t' || data.charAt(end+1)=='\n'|| data.charAt(end+1)=='#');
			return data.substring(start,end);}
		else
			return "Nothing found!";
	}
	
	public String checkLookup(String lookup, boolean helper){
		if(data.contains("\nship \"" + lookup + "\"")){
			return "\nship \"" + lookup + "\"";
		}
		else if(data.contains("\noutfit \"" + lookup + "\"")){
			return "\noutfit \"" + lookup + "\"";
		}
		else if(data.contains("\nmission \"" + lookup + "\"")){
			return "\nmission \"" + lookup + "\"";
		}
		else if(data.contains("\nsystem \"" + lookup + "\"")){
			return "\nsystem \"" + lookup + "\"";
		}
		else if(data.contains("\neffect \"" + lookup + "\"")){
			return "\neffect \"" + lookup + "\"";
		}
		else if(data.contains("\nship " + lookup)){
			return"\nship " + lookup;
		}
		else if(data.contains("\noutfit " + lookup)){
			return "\noutfit " + lookup;
		}
		else if(data.contains("\nmission " + lookup)){
			return "\nmission " + lookup;
		}
		else if(data.contains("\nsystem " + lookup)){
			return "\nsystem " + lookup;
		}
		else if(data.contains("\neffect " + lookup)){
			return "\neffect " + lookup;
		}
		else if(data.contains("\n"+lookup)){
			return "\n"+lookup;
		}
		else if(helper){
			lookup = Character.toUpperCase(lookup.charAt(0)) + lookup.toLowerCase().substring(1);
			return checkLookup(lookup,false);
		}
		else
			return "";
	}

	public void OutputHelper(MessageChannel channel,String output){
		if(output.length()<1993){
			channel.sendMessage(":\n```" + output + "```").queue();
		}
		else{
			int cut = output.lastIndexOf('\n', 0+1992);
			String o = output.substring(0, cut);			
			channel.sendMessage(":\n```" + o + "```").queue(x -> {
				OutputHelper(channel, output.substring(cut+1));
			});
		}
	}

	public boolean isImage(String url){
		try{
			URL u = new URL(url);
			return ImageIO.read(u) != null;
		}
		catch (Exception e){
			return false;
		}
	}

}
