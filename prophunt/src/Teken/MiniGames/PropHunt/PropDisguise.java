package Teken.MiniGames.PropHunt;

import org.bukkit.Material;

import pgDev.bukkit.DisguiseCraft.disguise.DisguiseType;

public class PropDisguise {
	public DisguiseType distype;
	public int ID;
	public String name;
	public PropDisguise(DisguiseType distype, int ID) {
		this.distype = distype;
		this.ID = ID;
		setName();
	}
	public PropDisguise(DisguiseType distype) {
		this.distype = distype;
		this.ID = 0;
		setName();
	}
	
	public void setName(){
		if(distype.isBlock()){
			name = "block of "+Material.getMaterial(ID).name().toLowerCase();
		}else{
			name = distype.name();
		}
	}
}
