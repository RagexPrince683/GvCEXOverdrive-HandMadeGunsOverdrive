package handmadeguns.command;

import handmadeguns.HMGGunMaker;
import handmadeguns.HMGAddAttachment;
import handmadeguns.HandmadeGunsCore;
import handmadeguns.HMGPacketHandler;
import handmadeguns.network.PacketReloadparm;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.model.ModelFormatException;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import static handmadeguns.HandmadeGunsCore.HMG_proxy;

public class HMG_CommandReloadparm extends CommandBase implements ICommand{

    @Override
    public String getCommandName() {
        return "reloadSettings";
    }
    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }
    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return null;
    }

    @Override
    public void processCommand(ICommandSender var1, String[] var2) {
        System.out.println(""+var1);
		// Invalidate before parsing settings: existing item renderers are then
		// updated by HMGGunMaker with models read from the current pack files.
		HMG_proxy.invalidateReloadableModels();
		reloadPackSettings();
    }

	/**
	 * Re-read the registered guns' pack settings and replace their render settings.
	 * Model cache lifetime is deliberately controlled by the calling command.
	 */
	protected static void reloadPackSettings() {
//        EntityPlayer entityPlayer = getPlayer(var1,var2[0]);
//        if(entityPlayer.getHeldItem().getItem() instanceof HMGItemGunBase){
//            String str = var2[1];
//            String[] key = str.split(",");
//            ((HMGItemGunBase)((entityPlayer).getHeldItem().getItem())).reloadanimation.add(Integer.parseInt(key[0]),new Float[]{
//                            Float.parseFloat(key[1]),
//                            Float.parseFloat(key[2]),
//                            Float.parseFloat(key[3]),Float.parseFloat(key[4]),Float.parseFloat(key[5]),
//                            Float.parseFloat(key[6]),Float.parseFloat(key[7]),Float.parseFloat(key[8]),
//
//                            Float.parseFloat(key[9]),
//
//                            Float.parseFloat(key[10]),Float.parseFloat(key[11]),Float.parseFloat(key[12]),
//                            Float.parseFloat(key[13]),Float.parseFloat(key[14]),Float.parseFloat(key[15]),
//
//                            Float.parseFloat(key[16]),Float.parseFloat(key[17]),Float.parseFloat(key[18]),
//                            Float.parseFloat(key[19]),Float.parseFloat(key[20]),Float.parseFloat(key[21]),
//
//                            Float.parseFloat(key[22]),
//                            Float.parseFloat(key[23]),Float.parseFloat(key[24]),Float.parseFloat(key[25]),
//                            Float.parseFloat(key[26]),Float.parseFloat(key[27]),Float.parseFloat(key[28]),
//                            Float.parseFloat(key[29]),Float.parseFloat(key[30]),Float.parseFloat(key[31])
//                    }
//            );
//            ((HMGItemGunBase)((entityPlayer).getHeldItem().getItem())).reloadanim = true;
//        }
        File packdir = new File(HMG_proxy.ProxyFile(), "handmadeguns_Packs");
        packdir.mkdirs();

        File[] packlist = packdir.listFiles(File::isDirectory);
        if (packlist == null) return;

        Arrays.sort(packlist, Comparator.comparing(File::getName)); // optional

        HMGGunMaker gunMaker = new HMGGunMaker();

        /***
         * File diregun = new File(apack, "guns");
         *             File[] filegun = diregun.listFiles();
         *             Arrays.sort(filegun, new Comparator<File>() {
         *                 public int compare(File file1, File file2) {
         *                     return file1.getName().compareTo(file2.getName());
         *                 }
         *             });
         *             for (int ii = 0; ii < filegun.length; ii++) {
         *                 if (filegun[ii].isFile()) {
         *                     try {
         *                         new HMGGunMaker().load(true, filegun[ii]);
         *                     } catch (ModelFormatException e) {
         *                         e.printStackTrace();
         *                     }
         */

        for (File apack : packlist) {
            if (!HandmadeGunsCore.isHMGPack(apack)) continue;
            // Reloaded guns must see the same isolated per-pack coefficient context as startup.
            HandmadeGunsCore.configurePackCoefficients(apack);
            File gunDir = new File(apack, "guns");
			File[] gunFiles = gunDir.listFiles(File::isFile);
			if (gunFiles != null) {
				Arrays.sort(gunFiles, Comparator.comparing(File::getName));
				for (File gunFile : gunFiles) {
					try {
						gunMaker.load(true, gunFile);
					} catch (ModelFormatException e) {
						e.printStackTrace();
					}
				}
			}
			File[] attachmentFiles = new File(apack, "attachment").listFiles(File::isFile);
			if (attachmentFiles != null) {
				Arrays.sort(attachmentFiles, Comparator.comparing(File::getName));
				for (File attachmentFile : attachmentFiles) {
					HMGAddAttachment.reloadAttachmentSettings(attachmentFile);
				}
			}
        }

        HMG_proxy.setUpModels();
        if (cpw.mods.fml.common.FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            handmadeguns.event.HMGJumpHandler.syncReloadedPolicy();
        }
    }

}
