package Teken.MiniGames.PropHunt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import pgDev.bukkit.DisguiseCraft.api.DisguiseCraftAPI;
import pgDev.bukkit.DisguiseCraft.disguise.DisguiseType;

public class GameSettings {
	private HashMap<String,Object> properties = new HashMap<String,Object>();
	int playerJoinTime;
	int maxPlayerCount;
	int numberOfStartingHiders;
	int hidingTime;
	int gameLenght;
	boolean caughtPlayersHunt;
	Location waitingLoc;
	Location startingLoc;
	Location specLoc;
	String endGameServer;
	List<PropDisguise> possibleDisguises;

	public GameSettings(){
		loadFromConfig();
		init();
	}

	public void init(){
		this.playerJoinTime = getIntProperty("playerjointime");
		this.maxPlayerCount = getIntProperty("maxplayercount");
		this.numberOfStartingHiders = getIntProperty("numberofstartinghiders");
		this.hidingTime = getIntProperty("hidingtime");
		this.caughtPlayersHunt = getBooleanProperty("caughtplayershunt");
		this.waitingLoc = getLocationProperty("@waitinglocation");
		this.specLoc = getLocationProperty("@spectatinglocation");
		this.startingLoc = getLocationProperty("@startinglocation");
		this.endGameServer = getStringProperty("endGameServer");
		this.possibleDisguises = getDisguises();
	}

	public Location getLocationProperty(String name){
		String[] prop = (String[])getProperty(name);
		return new Location(Bukkit.getWorld(prop[0]),Integer.parseInt(prop[1]),Integer.parseInt(prop[2]),Integer.parseInt(prop[3]));
	}

	public boolean getBooleanProperty(String name){
		return Boolean.parseBoolean((String)getProperty(name));
	}

	public int getIntProperty(String name){
		return Integer.parseInt((String)getProperty(name));
	}

	public String getStringProperty(String name){
		return (String)getProperty(name);
	}

	public Object getProperty(String name){
		return this.properties.get(name);
	}

	public void loadFromConfig(){
		File config = new File("prophunt.txt");
		if(!config.exists())createconfig();
		Scanner scanner = null;
		try {
			scanner = new Scanner(config);
			while (scanner.hasNext()){
				String input = scanner.nextLine();
				input.replaceAll(" ", "");
				String[] line = input.split(":");
				if(line[0].charAt(0) == '@'){
					String[] props = line[1].split(",");
					properties.put(line[0], props);
				}else{
					properties.put(line[0], line[1]);
				}
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void createconfig(){
		PrintWriter writer;
		try {
			writer = new PrintWriter("prophunt.txt", "UTF-8");
			writer.println("playerjointime:60");
			writer.println("maxplayercount:60");
			writer.println("numberofstartinghiders:1");
			writer.println("hidingtime:120");
			writer.println("caughtplayershunt:false");
			writer.println("@waitinglocation:world,0,0,0");
			writer.println("@spectatinglocation:world,0,0,0");
			writer.println("@startinglocation:world,0,0,0");
			writer.println("endGameServer:lobby");
			writer.println("@possibleDisguises: , , ");
			//writer.println("");
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public List<PropDisguise> getDisguises(){
		String[] base = (String[])getProperty("@possibleDisguises");
		List<PropDisguise> temp = new ArrayList<PropDisguise>();
		for(String s:base){
			DisguiseType dt = DisguiseType.fromString(s);
			if(dt != null){
				if(dt.isBlock()){
					temp.add(new PropDisguise(dt,Material.getMaterial(s).getId()));
					System.out.println("[Tekens Prop Hunt] Loaded disguise from string: "+Utils.capFirstLetter(Material.getMaterial(s).name()));
				}else{
					temp.add(new PropDisguise(dt));
					System.out.println("[Tekens Prop Hunt] Loaded disguise from string: "+dt.name());
				}
			}else{
				Material m = Material.getMaterial(Integer.parseInt(s));//getMaterial(s);
				if(m!=null){
					temp.add(new PropDisguise(DisguiseType.FallingBlock,Integer.parseInt(s)));
					System.out.println("[Tekens Prop Hunt] Loaded disguise from string: "+Utils.capFirstLetter(m.name()));
				}else{
					System.out.println("[Tekens Prop Hunt] Failed to load disguise from string: "+s);
				}
			}
		}
		return temp;
	}
}