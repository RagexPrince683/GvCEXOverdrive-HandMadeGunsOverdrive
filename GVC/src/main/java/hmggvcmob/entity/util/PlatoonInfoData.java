package hmggvcmob.entity.util;

import handmadevehicle.entity.parts.Modes;

import static handmadevehicle.entity.parts.Modes.Wait;

public class PlatoonInfoData {
	public boolean isLeader = false;
	public boolean isOnPlatoon = false;
	public double[] target = new double[3];
	public Modes mode = Wait;
	public PlatoonOrder order;
	public String platoonName = null;
}
