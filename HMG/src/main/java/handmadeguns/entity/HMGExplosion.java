package handmadeguns.entity;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import javax.vecmath.Vector3d;
import java.util.*;

import static java.lang.Math.abs;

public class HMGExplosion extends Explosion {
	//todo hardpatch for MCH vehicles
	private static final Random explosionRNG = new Random();
	private static final boolean DEBUG_HMG_BLAST_EXPOSURE = false;
	private float explosionDamage = -1.0F;

	public HMGExplosion(World p_i1948_1_, Entity p_i1948_2_, double p_i1948_3_, double p_i1948_5_, double p_i1948_7_, float p_i1948_9_) {
		this(p_i1948_1_, p_i1948_2_, p_i1948_3_, p_i1948_5_, p_i1948_7_, p_i1948_9_, -1.0F);
	}

	public HMGExplosion(World p_i1948_1_, Entity p_i1948_2_, double p_i1948_3_, double p_i1948_5_, double p_i1948_7_, float p_i1948_9_, float explosionDamage) {
		super(p_i1948_1_, p_i1948_2_, p_i1948_3_, p_i1948_5_, p_i1948_7_, p_i1948_9_);
		this.worldObj = p_i1948_1_;
		this.explosionDamage = explosionDamage;
	}

	private World worldObj;

	private Map field_77288_k = new HashMap();

	public void doExplosionA()
	{
		float f = this.explosionSize;
		HashSet hashset = new HashSet();
		int i;
		int j;
		int k;
		double d5;
		double d6;
		double d7;

		int field_77289_h = 16;
		for (i = 0; i < field_77289_h; ++i)
		{
			for (j = 0; j < field_77289_h; ++j)
			{
				for (k = 0; k < field_77289_h; ++k)
				{
					if (i == 0 || i == field_77289_h - 1 || j == 0 || j == field_77289_h - 1 || k == 0 || k == field_77289_h - 1)
					{
						double d0 = (double)((float)i / ((float) field_77289_h - 1.0F) * 2.0F - 1.0F);
						double d1 = (double)((float)j / ((float) field_77289_h - 1.0F) * 2.0F - 1.0F);
						double d2 = (double)((float)k / ((float) field_77289_h - 1.0F) * 2.0F - 1.0F);
						double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
						d0 /= d3;
						d1 /= d3;
						d2 /= d3;
						float f1 = this.explosionSize * (0.7F + this.worldObj.rand.nextFloat() * 0.6F);
						d5 = this.explosionX;
						d6 = this.explosionY;
						d7 = this.explosionZ;

						for (float f2 = 0.3F; f1 > 0.0F; f1 -= f2 * 0.75F)
						{
							int j1 = MathHelper.floor_double(d5);
							int k1 = MathHelper.floor_double(d6);
							int l1 = MathHelper.floor_double(d7);
							Block block = this.worldObj.getBlock(j1, k1, l1);

							if (block.getMaterial() != Material.air)
							{
								float f3 = block.getExplosionResistance(this.exploder, worldObj, j1, k1, l1, explosionX, explosionY, explosionZ);
								f1 -= (f3 + 0.3F) * f2;
							}

							if (f1 > 0.0F)
							{
								hashset.add(new ChunkPosition(j1, k1, l1));
							}
							if (block.getMaterial() != Material.air)
							{
								float f3 = block.getExplosionResistance(this.exploder, worldObj, j1, k1, l1, explosionX, explosionY, explosionZ);
								f1 -= (f3 + 0.3F) * f2;
							}

							d5 += d0 * (double)f2;
							d6 += d1 * (double)f2;
							d7 += d2 * (double)f2;
						}
					}
				}
			}
		}

		this.affectedBlockPositions.addAll(hashset);
		this.explosionSize *= 2.0F;
		i = MathHelper.floor_double(this.explosionX - (double)this.explosionSize - 1.0D);
		j = MathHelper.floor_double(this.explosionX + (double)this.explosionSize + 1.0D);
		k = MathHelper.floor_double(this.explosionY - (double)this.explosionSize - 1.0D);
		int i2 = MathHelper.floor_double(this.explosionY + (double)this.explosionSize + 1.0D);
		int l = MathHelper.floor_double(this.explosionZ - (double)this.explosionSize - 1.0D);
		int j2 = MathHelper.floor_double(this.explosionZ + (double)this.explosionSize + 1.0D);
		List list = this.worldObj.getEntitiesWithinAABB( Entity.class,AxisAlignedBB.getBoundingBox((double)i, (double)k, (double)l, (double)j, (double)i2, (double)j2));
		net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(this.worldObj, this, list, this.explosionSize);
		Vec3 vec3 = Vec3.createVectorHelper(this.explosionX, this.explosionY, this.explosionZ);

		for (int i1 = 0; i1 < list.size(); ++i1)
		{
			Entity entity = (Entity)list.get(i1);
			double d4 = entity.getDistance(this.explosionX, this.explosionY, this.explosionZ) / (double)this.explosionSize;
			double horizontalX = entity.posX - this.explosionX;
			double horizontalZ = entity.posZ - this.explosionZ;
			double horizontalDistance = Math.sqrt(horizontalX * horizontalX + horizontalZ * horizontalZ);
			// Configured damage uses a cylinder; legacy damage and knockback retain their sphere.
			boolean inDamageRange = this.explosionDamage >= 0.0F
					? abs(entity.posY - this.explosionY) <= f && horizontalDistance < this.explosionSize
					: d4 <= 1.0D;

			if (inDamageRange || d4 <= 1.0D)
			{
				d5 = entity.posX - this.explosionX;
				d6 = entity.posY + (double)entity.getEyeHeight() - this.explosionY;
				d7 = entity.posZ - this.explosionZ;
				double d9 = (double)MathHelper.sqrt_double(d5 * d5 + d6 * d6 + d7 * d7);

				// Damage eligibility is independent of the eye-based knockback direction.
				double d10 = (double)this.getHMGBlockDensity(vec3, entity.boundingBox);
				double d11 = (1.0D - d4) * d10;
				if (inDamageRange && (this.explosionDamage >= 0.0F || d9 != 0.0D))
				{
					entity.attackEntityFrom(DamageSource.setExplosionSource(this), getExplosionDamage(horizontalDistance, d10, d11, f));
				}

				if (d4 <= 1.0D && d9 != 0.0D)
				{
					d5 /= d9;
					d6 /= d9;
					d7 /= d9;
					//we're gonna be buffing this by a multiple of 3 so mcheli fucks off with it's retarded damg calc
					//old method:
					//entity.attackEntityFrom(DamageSource.setExplosionSource(this), (float)((int)((d11 * d11 + d11) / 2.0D * 8.0D * (double)this.explosionSize + 1.0D)));
					//float damage = (float)((int)((var41 * var41 + var41) / 1.2D * (double)super.explosionSize));
					//hopefully parity with MCH so we aren't completely busting our balls trying to compat ts into oblivion
					double d8 = EnchantmentProtection.func_92092_a(entity, d11);
					if(entity instanceof HMGEntityFallingBlockModified || entity instanceof EntityFallingBlock) {
						entity.addVelocity(d5 * d8/100,
								d6 * d8/100,
								d7 * d8/100);
					}else {
						entity.addVelocity(d5 * d8,
								d6 * d8,
								d7 * d8);
					}

					if (entity instanceof EntityPlayer)
					{
						this.field_77288_k.put((EntityPlayer)entity, Vec3.createVectorHelper(d5 * d11, d6 * d11, d7 * d11));
					}
				}
			}
		}

		this.explosionSize = f;
	}


	private float getHMGBlockDensity(Vec3 explosionPos, AxisAlignedBB entityBB)
	{
		double sampleStepX = 1.0D / ((entityBB.maxX - entityBB.minX) * 2.0D + 1.0D);
		double sampleStepY = 1.0D / ((entityBB.maxY - entityBB.minY) * 2.0D + 1.0D);
		double sampleStepZ = 1.0D / ((entityBB.maxZ - entityBB.minZ) * 2.0D + 1.0D);

		if (sampleStepX <= 0.0D || sampleStepY <= 0.0D || sampleStepZ <= 0.0D)
		{
			return 0.0F;
		}

		int unblockedSamples = 0;
		int totalSamples = 0;
		double xOffset = (1.0D - Math.floor(1.0D / sampleStepX) * sampleStepX) / 2.0D;
		double zOffset = (1.0D - Math.floor(1.0D / sampleStepZ) * sampleStepZ) / 2.0D;

		for (double sampleX = 0.0D; sampleX <= 1.0D; sampleX += sampleStepX)
		{
			for (double sampleY = 0.0D; sampleY <= 1.0D; sampleY += sampleStepY)
			{
				for (double sampleZ = 0.0D; sampleZ <= 1.0D; sampleZ += sampleStepZ)
				{
					double x = entityBB.minX + (entityBB.maxX - entityBB.minX) * sampleX;
					double y = entityBB.minY + (entityBB.maxY - entityBB.minY) * sampleY;
					double z = entityBB.minZ + (entityBB.maxZ - entityBB.minZ) * sampleZ;

					if (!this.isHMGBlastRayBlocked(Vec3.createVectorHelper(x + xOffset, y, z + zOffset), explosionPos))
					{
						++unblockedSamples;
					}

					++totalSamples;
				}
			}
		}

		return (float)unblockedSamples / (float)totalSamples;
	}

	private boolean isHMGBlastRayBlocked(Vec3 start, Vec3 end)
	{
		double deltaX = end.xCoord - start.xCoord;
		double deltaY = end.yCoord - start.yCoord;
		double deltaZ = end.zCoord - start.zCoord;
		double distance = MathHelper.sqrt_double(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
		int steps = Math.max(1, MathHelper.ceiling_double_int(distance / 0.25D));

		for (int step = 0; step <= steps; ++step)
		{
			double progress = (double)step / (double)steps;
			int x = MathHelper.floor_double(start.xCoord + deltaX * progress);
			int y = MathHelper.floor_double(start.yCoord + deltaY * progress);
			int z = MathHelper.floor_double(start.zCoord + deltaZ * progress);

			if (this.hmgBlocksExplosionDamage(this.worldObj, x, y, z))
			{
				if (DEBUG_HMG_BLAST_EXPOSURE)
				{
					System.out.println("HMG blast exposure blocked by " + this.worldObj.getBlock(x, y, z) + " at " + x + "," + y + "," + z);
				}

				return true;
			}
		}

		return false;
	}

	private boolean hmgBlocksExplosionDamage(World world, int x, int y, int z)
	{
		Block block = world.getBlock(x, y, z);

		if (block == null || block.isAir(world, x, y, z))
		{
			return false;
		}

		Material material = block.getMaterial();

		// Use physical properties, not opacity/render type: glass, doors, fences,
		// and slabs can provide cover. Webs slow entities but are not blast cover.
		if (material == Material.web || !material.blocksMovement() || block.isReplaceable(world, x, y, z))
		{
			return false;
		}

		// Preserve HMG's existing treatment of foliage as non-protective.
		if (material == Material.leaves)
		{
			return false;
		}

		block.setBlockBoundsBasedOnState(world, x, y, z);

		if (block.getCollisionBoundingBoxFromPool(world, x, y, z) == null)
		{
			return false;
		}

		return true;
	}

	private float getExplosionDamage(double horizontalDistance, double blockDensity, double vanillaExposure, float fullDamageRadius)
	{
		if (this.explosionDamage < 0.0F)
		{
			return (float)((int)((vanillaExposure * vanillaExposure + vanillaExposure) / 1.2D * (double)this.explosionSize * 3));
		}

		double falloff = 1.0D;
		double falloffRange = (double)this.explosionSize - (double)fullDamageRadius;

		if (falloffRange > 0.0D && horizontalDistance > (double)fullDamageRadius)
		{
			falloff = 1.0D - ((horizontalDistance - (double)fullDamageRadius) / falloffRange);
		}

		if (falloff <= 0.0D || blockDensity <= 0.0D)
		{
			return 0.0F;
		}

		return (float)((double)this.explosionDamage * falloff * blockDensity);
	}

	/**
	 * Does the second part of the explosion (sound, particles, drop spawn)
	 */
	public void doExplosionB(boolean p_77279_1_)
	{
		this.worldObj.playSoundEffect(this.explosionX, this.explosionY, this.explosionZ, "random.explode", 4.0F, (1.0F + (this.worldObj.rand.nextFloat() - this.worldObj.rand.nextFloat()) * 0.2F) * 0.7F);

		if (this.explosionSize >= 2.0F && this.isSmoking)
		{
			this.worldObj.spawnParticle("hugeexplosion", this.explosionX, this.explosionY, this.explosionZ, 1.0D, 0.0D, 0.0D);
		}
		else
		{
			this.worldObj.spawnParticle("largeexplode", this.explosionX, this.explosionY, this.explosionZ, 1.0D, 0.0D, 0.0D);
		}

		Iterator iterator;
		ChunkPosition chunkposition;
		int i;
		int j;
		int k;
		Block block;

		if (this.isSmoking)
		{
			iterator = this.affectedBlockPositions.iterator();

			while (iterator.hasNext())
			{
				chunkposition = (ChunkPosition)iterator.next();
				i = chunkposition.chunkPosX;
				j = chunkposition.chunkPosY;
				k = chunkposition.chunkPosZ;
				block = this.worldObj.getBlock(i, j, k);

				if (p_77279_1_)
				{
					double d0 = (double)((float)i + this.worldObj.rand.nextFloat());
					double d1 = (double)((float)j + this.worldObj.rand.nextFloat());
					double d2 = (double)((float)k + this.worldObj.rand.nextFloat());
					double d3 = d0 - this.explosionX;
					double d4 = d1 - this.explosionY;
					double d5 = d2 - this.explosionZ;
					double d6 = (double)MathHelper.sqrt_double(d3 * d3 + d4 * d4 + d5 * d5);
					d3 /= d6;
					d4 /= d6;
					d5 /= d6;
					double d7 = 0.5D / (d6 / (double)this.explosionSize + 0.1D);
					d7 *= (double)(this.worldObj.rand.nextFloat() * this.worldObj.rand.nextFloat() + 0.3F);
					d3 *= d7;
					d4 *= d7;
					d5 *= d7;
					this.worldObj.spawnParticle("explode", (d0 + this.explosionX * 1.0D) / 2.0D, (d1 + this.explosionY * 1.0D) / 2.0D, (d2 + this.explosionZ * 1.0D) / 2.0D, d3, d4, d5);
					this.worldObj.spawnParticle("smoke", d0, d1, d2, d3, d4, d5);
				}

				if (block.getMaterial() != Material.air)
				{
					Vector3d zeroToBlockVec = new Vector3d(i+0.5 - this.explosionX,
							j+0.5 - this.explosionY,
							k+0.5 - this.explosionZ);
					if(zeroToBlockVec.lengthSquared() < explosionSize/10 || block != Blocks.grass && block != Blocks.dirt && block != Blocks.sand && block != Blocks.cobblestone){
						if (block.canDropFromExplosion(this))
						{
							block.dropBlockAsItemWithChance(this.worldObj, i, j, k, this.worldObj.getBlockMetadata(i, j, k), 1.0F / this.explosionSize, 0);
						}


						block.onBlockExploded(this.worldObj, i, j, k, this);
					}else {
						HMGEntityFallingBlockModified entityFallingBlock = new HMGEntityFallingBlockModified(worldObj,i + 0.5, j + 0.5, k + 0.5,block,worldObj.getBlockMetadata(i,j,k));
						double distFromGroundZero = zeroToBlockVec.lengthSquared();
						zeroToBlockVec.scale(0.1 * explosionSize/(1+zeroToBlockVec.lengthSquared()));

						entityFallingBlock.motionX = zeroToBlockVec.x;
						entityFallingBlock.motionY = 0.3/(1+distFromGroundZero) + zeroToBlockVec.y;
						entityFallingBlock.motionZ = zeroToBlockVec.z;
						worldObj.spawnEntityInWorld(entityFallingBlock);
						worldObj.setBlockToAir(i, j, k);
					}
				}
			}
		}

		if (this.isFlaming)
		{
			iterator = this.affectedBlockPositions.iterator();

			while (iterator.hasNext())
			{
				chunkposition = (ChunkPosition)iterator.next();
				i = chunkposition.chunkPosX;
				j = chunkposition.chunkPosY;
				k = chunkposition.chunkPosZ;
				block = this.worldObj.getBlock(i, j, k);
				Block block1 = this.worldObj.getBlock(i, j - 1, k);

				if (block.getMaterial() == Material.air && block1.func_149730_j() && this.explosionRNG.nextInt(3) == 0)
				{
					this.worldObj.setBlock(i, j, k, Blocks.fire);
				}
			}
		}
	}
	public Map func_77277_b()
	{
		return this.field_77288_k;
	}
}
