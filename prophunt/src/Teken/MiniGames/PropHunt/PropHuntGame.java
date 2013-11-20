package Teken.MiniGames.PropHunt;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import net.minecraft.server.v1_6_R2.EntityHuman;
import net.minecraft.server.v1_6_R2.Packet20NamedEntitySpawn;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import pgDev.bukkit.DisguiseCraft.api.DisguiseCraftAPI;
import pgDev.bukkit.DisguiseCraft.disguise.Disguise;

public class PropHuntGame implements Listener {
	List<PropHuntPlayer> players = new ArrayList<PropHuntPlayer>();
	boolean gameStarted = false;
	boolean cancelled = false;
	GameSettings gs;
	Timer countDownTimer;
	Timer hidingTimer;
	Timer gameTimer;
	int playerJoinTimeCountdown;
	int hidingTimeCountdown;
	int gameLenghtCountdown;
	DisguiseCraftAPI dcAPI;
	int currentPlayerCount = 0;

	Plugin plugin;

	public PropHuntGame(Plugin plugin,DisguiseCraftAPI api,GameSettings g){
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		dcAPI = api;
		gs = g;
		this.plugin = plugin;
		playerJoinTimeCountdown = gs.playerJoinTime;
		hidingTimeCountdown = gs.hidingTime;
		gameLenghtCountdown = gs.gameLenght;
	}

	@EventHandler public void onPlayerJoin(PlayerJoinEvent event){
		if(gameStarted){
			event.getPlayer().kickPlayer("A Game Has Already Started");
		}else if(currentPlayerCount < gs.maxPlayerCount){
			addPlayer(event.getPlayer());
			if(currentPlayerCount >= gs.maxPlayerCount-1){
				gameStart();
			}
		}
	}

	@EventHandler public void onPlayerQuit(PlayerQuitEvent event){
		for(PropHuntPlayer p: players){
			if(p.username.equals(event.getPlayer().getName())){
				players.remove(p);
				return;
			}
		}
	}

	@EventHandler public void onPlayerHurtByEntity(EntityDamageByEntityEvent event){
		if(event.getDamager() instanceof Player){
			Player d = (Player)event.getDamager();
			Player p = (Player)event.getEntity();
			PropHuntPlayer pd = getPropHuntPlayer(d.getName());
			PropHuntPlayer pp = getPropHuntPlayer(p.getName());
			if(pd != null && pp != null){
				if(pd.state == Status.FINDING && pp.state == Status.HIDING){
					p.sendMessage("[Tekens Prop Hunt] You have been caught");
					d.sendMessage("[Tekens Prop Hunt] You have caught "+p.getDisplayName());
					addScore(d);
					playerCaught(pp);
				}
			}
		}
	}

	public void start(){
		countDownTimer = new Timer();
		countDownTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				playerJoinTimeCountdown--;
				setAllPlayersXPTimer(playerJoinTimeCountdown,gs.playerJoinTime);
				if(playerJoinTimeCountdown <= 0){
					gameStart();
				}
				if(gameStarted || cancelled){
					this.cancel();
					return;
				}
			}
		}, 1000, 1000);
		plugin.getLogger().info("[Tekens Prop Hunt] Prop hunt game started and is open for players to join");
		setOnlinePlayersToNormal();
	}

	public void gameStart(){
		gameStarted = true;
		chooseHunters();
		setNormalPlayerToHiding();
		teleportAllHidingPlayerToStartingLocation();
		randomlyDisguiseHidingPlayers();
		hidingTimer = new Timer();
		hidingTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				hidingTimeCountdown--;
				setAllPlayersXPTimer(hidingTimeCountdown,gs.hidingTime);
				if(hidingTimeCountdown <= 0){
					gameRelease();
					this.cancel();
				}
				if(!gameStarted || cancelled){
					this.cancel();
					return;
				}
			}
		}, 1000, 1000);
		plugin.getLogger().info("[Tekens Prop Hunt] Prop hunt game has released players");
	}

	public void gameRelease(){
		teleportAllHuntingPlayerToStartingLocation();
		gameTimer = new Timer();
		gameTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				gameLenghtCountdown--;
				setAllPlayersXPTimer(gameLenghtCountdown,gs.gameLenght);
				if(gameLenghtCountdown <= 0 || allHiderOut()){
					endGame();
					this.cancel();
				}
				if(!gameStarted || cancelled){
					this.cancel();
					return;
				}
			}
		}, 1000, 1000);
		plugin.getLogger().info("[Tekens Prop Hunt] Hunter released");
		sendMessageToPlayerState("[Tekens Prop Hunt] Hunter released", Status.HIDING);
	}

	public void endGame(){
		undisguiseAll();
		plugin.getLogger().info("[Tekens Prop Hunt] Games has ended");
		for(PropHuntPlayer p: players){
			try{
				if(p.state == Status.HIDING){
					addScore(Bukkit.getPlayerExact(p.username));
				}
				ByteArrayOutputStream b = new ByteArrayOutputStream();
				DataOutputStream out = new DataOutputStream(b);
				try {
					out.writeUTF("Connect");
					out.writeUTF(gs.endGameServer);
				} catch (IOException eee) {
					Bukkit.getLogger().info("[Tekens Prop Hunt] You'll never see me!");
				}
				Bukkit.getPlayerExact(p.username).sendPluginMessage(this.plugin, "BungeeCord", b.toByteArray());
			}catch(Exception e){
				plugin.getLogger().info("[Tekens Prop Hunt] Bungee was not found");
				plugin.getLogger().info(gs.endGameServer);
				e.printStackTrace();
			}
		}
		plugin.getLogger().info("[Tekens Prop Hunt] All players have teleported away");
		this.gameStarted = false;
	}

	public void chooseHunters(){
		for(int x = 0;x < gs.numberOfStartingHiders; x++){
			int index = new Random().nextInt(players.size());
			PropHuntPlayer player = players.get(index);
			player.state = Status.FINDING;
			Bukkit.getPlayerExact(player.username).sendMessage("[Tekens Prop Hunt] You are now a hunter");
		}
		plugin.getLogger().info("[Tekens Prop Hunt] Hunters choosen");
	}

	public void addPlayer(Player p){
		players.add(new PropHuntPlayer(p.getName(), Status.WAITING));

		String prevName = p.getName();
		EntityHuman ep = ((CraftPlayer)p).getHandle();
		Packet20NamedEntitySpawn packet = new Packet20NamedEntitySpawn(ep); 
		for(Player pl: plugin.getServer().getOnlinePlayers()){
			if(pl != p)((CraftPlayer)pl).getHandle().playerConnection.sendPacket(packet);
		}
		p.teleport(gs.waitingLoc);
		p.sendMessage("[Tekens Prop Hunt] You are now playing prop hunt");
		currentPlayerCount++;
	}

	public void setNormalPlayerToHiding(){
		for(PropHuntPlayer p: players){
			if(p.state == Status.WAITING){
				p.state = Status.HIDING;
				Bukkit.getPlayerExact("[Tekens Prop Hunt] Fly, you fools");
			}
		}
		plugin.getLogger().info("[Tekens Prop Hunt] Normal players are now set to hiding");
	}

	public void setOnlinePlayersToNormal(){
		for(Player p: Bukkit.getOnlinePlayers()){
			addPlayer(p);
		}
		plugin.getLogger().info("[Tekens Prop Hunt] Online players are now normal players");
	}

	public void teleportAllHidingPlayerToStartingLocation(){
		for(PropHuntPlayer p: players){
			if(p.state == Status.HIDING){
				Bukkit.getPlayerExact(p.username).teleport(gs.startingLoc);
			}
		}
		plugin.getLogger().info("[Tekens Prop Hunt] Hiding players teleported to starting location");
	}

	public void teleportAllHuntingPlayerToStartingLocation(){
		for(PropHuntPlayer p: players){
			if(p.state == Status.FINDING){
				Bukkit.getPlayerExact(p.username).teleport(gs.startingLoc);
			}
		}
		plugin.getLogger().info("[Tekens Prop Hunt] Hunting players teleported to starting location");
	}

	public void teleportAPlayerToStartingLocation(Player p){
		p.teleport(gs.startingLoc);
		plugin.getLogger().info("[Tekens Prop Hunt] Hiding players teleported to starting location");
	}

	public void teleportAnOutPlayerToSpectatingLocation(Player p){
		p.teleport(gs.specLoc);
		plugin.getLogger().info("[Tekens Prop Hunt] Out player has been teleported to spectating location");
	}

	public PropHuntPlayer getPropHuntPlayer(String user){
		for(PropHuntPlayer p: players){
			if(p.username.equals(user))return p;
		}
		return null;
	}

	public void playerCaught(PropHuntPlayer pp){
		plugin.getLogger().info("[Tekens Prop Hunt] Player caught");
		if(gs.caughtPlayersHunt){
			pp.state = Status.FINDING;
			teleportAPlayerToStartingLocation(Bukkit.getPlayerExact(pp.username));
		}else{
			pp.state = Status.OUT;
			teleportAnOutPlayerToSpectatingLocation(Bukkit.getPlayerExact(pp.username));
		}
	}

	public void addScore(Player p){
		plugin.getLogger().info("[Tekens Prop Hunt] Score has been added to "+p.getName());
	}

	public void randomlyDisguiseHidingPlayers(){
		for(PropHuntPlayer p: players){
			if(p.state == Status.HIDING){
				int index = new Random().nextInt(gs.possibleDisguises.size());
				dcAPI.undisguisePlayer(Bukkit.getPlayerExact(p.username));
				dcAPI.disguisePlayer(Bukkit.getPlayerExact(p.username),getNewDisguise(index));
				Bukkit.getPlayerExact(p.username).sendMessage("[Tekens Prop Hunt] You are now disguised as a "+gs.possibleDisguises.get(index).name);
			}
		}
		plugin.getLogger().info("[Tekens Prop Hunt] Disguises set");
	}

	public Disguise getNewDisguise(int index){
		PropDisguise prop = gs.possibleDisguises.get(index);
		if(prop.distype.isBlock()){
			return new Disguise(dcAPI.newEntityID(),"blockID:"+prop.ID,prop.distype);
		}else{
			return new Disguise(dcAPI.newEntityID(), prop.distype);
		}
	}

	public void undisguiseAll(){
		for(PropHuntPlayer p: players){
			dcAPI.undisguisePlayer(Bukkit.getPlayerExact(p.username));
		}
	}

	public void sendMessageToPlayerState(String message,Status state){
		for(PropHuntPlayer p: players){
			if(p.state == state){
				Bukkit.getPlayerExact(p.username).sendMessage(message);
			}
		}
	}

	public void setAllPlayersXPTimer(float timeLeft,float total){
		float value = timeLeft/total;
		for(PropHuntPlayer p: players){
			setPlayerXPBar(Bukkit.getPlayerExact(p.username), value);
		}
	}

	public void setPlayerXPBar(Player p, float value){
		p.setExp(value);
	}

	public boolean allHiderOut(){
		for(PropHuntPlayer p: players){
			if(p.state == Status.HIDING)return false;
		}
		return true;
	}
}
