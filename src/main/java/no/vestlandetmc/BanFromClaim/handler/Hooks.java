package no.vestlandetmc.BanFromClaim.handler;

public class Hooks {

	private static boolean griefPrevention = false;
	private static boolean griefDefender = false;

	private static boolean gSit = false;

	public static boolean gpEnabled() {
		return griefPrevention;
	}

	public static void setGP() {
		griefPrevention = true;
	}

	public static void setGSIT() {
		gSit = true;
	}

	public static boolean gSitEnabled() {
		return gSit;
	}


}
