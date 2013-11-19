package Teken.MiniGames.PropHunt;

public class Utils {
	
	public static String capFirstLetter(String s){
		String p = s.toLowerCase();
		return p.substring(0, 1).toUpperCase() + p.substring(1);
	}

}
