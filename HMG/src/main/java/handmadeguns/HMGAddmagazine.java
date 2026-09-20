package handmadeguns;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import handmadeguns.items.HMGItemBullet_with_Internal_Bullet;
import handmadeguns.client.render.HMGRenderItemCustom;
import handmadeguns.pack.HMGPackAssetResolver;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.IModelCustom;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class HMGAddmagazine {
    public static List Magazines = new ArrayList();
    public static void load(boolean isClient, File file1) throws IOException {
        HMGPackAssetResolver resolver = new HMGPackAssetResolver(file1.getParentFile().getParentFile());
        String Name = "";
        String UIName = "";
        int stacksize = 0;
        String texture = "";
        boolean canobj = false;
        String  objmodel = "";
        String objtexture = "";
        String str;
        int type = 0;
        int damage = 1;
        float explosion = 1;
        boolean destroyBlock = false;
        float resistance = 0.99f;
        float acceleration = 0f;
        float gravity_acceleration = 0.029f;
        boolean canbounce   = false;
        float 	 bouncerate  = 0;
        float 	 bouncelimit = 0;
        String modelname = "default";
        int round = 30;
        if(checkBeforeReadfile(file1)) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file1),"Shift-JIS"))) {
                while ((str = br.readLine()) != null) {  // 1�s���ǂݍ���
                String[] data = HMGGunMaker.splitComma(str);
                if(data.length>0){
                    if(data[0].equals("StackSize")){
                        stacksize = Integer.parseInt(data[1]);
                    }
                    if(data[0].equals("Type")){
                        type = Integer.parseInt(data[1]);
                    }
                    if(data[0].equals("Damage")){
                        damage = Integer.parseInt(data[1]);
                    }
                    if (data[0].equals("Explosion")) {
                        explosion = Float.parseFloat(data[1]);
                    }
                    if(data[0].equals("Destroy_Block")){
                        destroyBlock = Boolean.parseBoolean(data[1]);
                    }
                    if (data[0].equals("Resistance")) {
                        resistance = Float.parseFloat(data[1]);
                    }
                    if (data[0].equals("Acceleration")) {
                        acceleration = Float.parseFloat(data[1]);
                    }
                    if (data[0].equals("Gravity_acceleration")) {
                        gravity_acceleration = Float.parseFloat(data[1]);
                    }
                    if(data[0].equals("Canbounce")){
                        canbounce = Boolean.parseBoolean(data[1]);
                    }
                    if (data[0].equals("Bouncerate")) {
                        bouncerate = Float.parseFloat(data[1]);
                    }
                    if (data[0].equals("Bouncelimit")) {
                        bouncelimit = Float.parseFloat(data[1]);
                    }
                    if (data[0].equals("Bullet_model_name")){
                        modelname = data[1];
                    }
                    if(data[0].equals("Round")){
                        round = Integer.parseInt(data[1]);
                    }


                    if(data[0].equals("canobj")){
                        canobj = Boolean.parseBoolean(data[1]);
                    }
                    if(data[0].equals("Objmodel")){
                        objmodel =data[1];
                    }
                    if (data[0].equals("ObjTexture") || data[0].equals("ModelTexture")) {
                        objtexture = data[1];
                    }

                    if(data[0].equals("System_Name")){
                        Name = data[1];
                    }
                    if(data[0].equals("UI_Name")){
                        UIName = data[1];
                    }
                    if(data[0].equals("Texture")){
                        try { texture = resolver.itemTextureName(data[1]); }
                        catch (FileNotFoundException missing) { texture = data[1]; }
                    }
                    if(data[0].equals("END")){
                        HMGItemBullet_with_Internal_Bullet newmagazine = new HMGItemBullet_with_Internal_Bullet();
						Item registered = GameRegistry.findItem("HandmadeGuns", Name);
						if (registered != null) {
							if (!(registered instanceof HMGItemBullet_with_Internal_Bullet)) {
								System.err.println("[HMG] Ignoring incompatible magazine override for " + Name);
								continue;
							}
							newmagazine = (HMGItemBullet_with_Internal_Bullet) registered;
						}
                        newmagazine.setMaxStackSize(stacksize);
                        newmagazine.setMaxDamage(round);
                        newmagazine.setTextureName("handmadeguns:"+texture);
                        newmagazine.setCreativeTab(HandmadeGunsCore.tabhmg);
                        newmagazine.type                     = type;
                        newmagazine.damage                   = damage;
                        newmagazine.gravity_acceleration     = gravity_acceleration;
                        newmagazine.resistance               = resistance;
                        newmagazine.acceleration             = acceleration;
                        newmagazine.explosion                = explosion;
                        newmagazine.destroyBlock             = destroyBlock;
                        newmagazine.canbounce                = canbounce;
                        newmagazine.bouncerate               = bouncerate;
                        newmagazine.bouncelimit              = bouncelimit;
                        newmagazine.modelname                = modelname;
                        if(UIName != null){
                            LanguageRegistry.instance().addNameForObject(newmagazine, "jp_JP", UIName);
                            LanguageRegistry.instance().addNameForObject(newmagazine, "en_US", Name);
                        }else{
                            LanguageRegistry.instance().addNameForObject(newmagazine, "en_US", Name);
                        }
                        System.out.println("" + Name);
                        if(canobj && isClient) {
                            IModelCustom attach = HMGGunMaker.getCachedModel(resolver.resourceLocation(HMGPackAssetResolver.Type.MODEL, objmodel));
                            ResourceLocation attachtexture = HMGGunMaker.getCachedResourceLocation(resolver.resourceLocation(HMGPackAssetResolver.Type.MODEL_TEXTURE, objtexture));
                            MinecraftForgeClient.registerItemRenderer(newmagazine, new HMGRenderItemCustom(attach, attachtexture));
                        }
						if (registered == null) GameRegistry.registerItem(newmagazine, Name);
                    }
                }
                }
            }
        }
    }
    private static boolean checkBeforeReadfile(File file){
        if (file.exists()){
            if (file.isFile() && file.canRead()){
                return true;
            }
        }

        return false;
    }
}
