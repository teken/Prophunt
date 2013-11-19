package Teken.MiniGames.PropHunt;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import pgDev.bukkit.DisguiseCraft.DisguiseCraft;
import pgDev.bukkit.DisguiseCraft.api.DisguiseCraftAPI;

public class main extends JavaPlugin{
	static final String name = "Tekens Prop Hunt";
	static final String textName = "["+name+"] ";
	static DisguiseCraftAPI dcAPI;
	public PropHuntGame currentGame;
	GameSettings gs;

	@Override public void onEnable(){
		getLogger().info(name+" has been enabled");
		setupDisguiseCraft();
		this.getCommand("prophunt").setExecutor(this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		gs = new GameSettings();
	}

	@Override public void onDisable(){
		getLogger().info(name+" has been disabled");
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		Player p = (Player)sender;
		if(!p.isOp() || p.getName().equals("iMrExoticDK"))return true;
		if (cmd.getName().equalsIgnoreCase("prophunt")){
			if(args[0].equalsIgnoreCase("start")){
				if(currentGame != null)currentGame.endGame();
				startNewGame();
				return true;
			}else if(args[0].equalsIgnoreCase("stop")){
				if(currentGame != null)currentGame.endGame();
				return true;
			}else if(args[0].equalsIgnoreCase("disguises")){
				String message = textName+"Possible Disguises:";
				for(PropDisguise pd:gs.possibleDisguises){
					message += " "+pd.name+" ";
				}
				p.sendMessage(message);
				return true;
			}else{
				p.sendMessage(textName+"Fuck Off!");
				return true;
			}
		}
		return true;
	}

	public void setupDisguiseCraft() {
		dcAPI = DisguiseCraft.getAPI();
	}

	public void startNewGame(){
		getLogger().info(textName+"New prop hunt game instance created");
		PropHuntGame game = new PropHuntGame(this,dcAPI,gs);
		if(game != null) currentGame = game;
		getLogger().info(textName+"Game instance set");
		game.start();
	}
}
