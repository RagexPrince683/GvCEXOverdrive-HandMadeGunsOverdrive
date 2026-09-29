package handmadeguns.client.modelLoader.emb_modelloader;

import net.minecraft.client.renderer.GLAllocation;

import java.nio.FloatBuffer;

public class MQO_Material {
	float dif;
	float amb;
	float emi;
	float spc;
	float power;
	// MQO col(R G B A) owns opacity; lighting coefficients only scale RGB.
	float alpha = 1.0F;
	FloatBuffer dif_Buf;
	FloatBuffer amb_Buf;
	FloatBuffer emi_Buf;
	FloatBuffer spc_Buf;
	FloatBuffer power_Buf;

	public void setUp(){
		dif_Buf = GLAllocation.createDirectFloatBuffer(16).put(new float[]{dif,dif,dif,alpha});
		amb_Buf = GLAllocation.createDirectFloatBuffer(16).put(new float[]{amb,amb,amb,1.0F});
		emi_Buf = GLAllocation.createDirectFloatBuffer(16).put(new float[]{emi,emi,emi,1.0F});
		spc_Buf = GLAllocation.createDirectFloatBuffer(16).put(new float[]{spc,spc,spc,1.0F});
		dif_Buf.flip();
		amb_Buf.flip();
		emi_Buf.flip();
		spc_Buf.flip();

		power_Buf = GLAllocation.createDirectFloatBuffer(16).put(new float[]{power,power,power,power});
		power_Buf.flip();
	}
	private static FloatBuffer colorBuffer = GLAllocation.createDirectFloatBuffer(16);
	public static FloatBuffer setColorBuffer(double p_74517_0_, double p_74517_2_, double p_74517_4_, double p_74517_6_)
	{
		/**
		 * Update and return colorBuffer with the RGBA values passed as arguments
		 */
		return setColorBuffer((float)p_74517_0_, (float)p_74517_2_, (float)p_74517_4_, (float)p_74517_6_);
	}
	public static FloatBuffer setColorBuffer(float p_74521_0_, float p_74521_1_, float p_74521_2_, float p_74521_3_)
	{
		colorBuffer.clear();
		colorBuffer.put(p_74521_0_).put(p_74521_1_).put(p_74521_2_).put(p_74521_3_);
		colorBuffer.flip();
		/** Float buffer used to set OpenGL material colors */
		return colorBuffer;
	}
	public static FloatBuffer setColorBuffer(float p_74521_0_)
	{
		colorBuffer.clear();
		colorBuffer.put(p_74521_0_).put(p_74521_0_).put(p_74521_0_).put(p_74521_0_);
		/** Float buffer used to set OpenGL material colors */
		return colorBuffer;
	}
}
