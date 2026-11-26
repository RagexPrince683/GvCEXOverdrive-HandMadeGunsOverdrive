package hmggvcmob.entity.util;

import net.minecraft.entity.EntityLiving;

import javax.vecmath.Vector3d;

public class EntityAndPos {
    public final EntityLiving entity;
    private final Vector3d position = new Vector3d();

    public EntityAndPos(EntityLiving entity) {
        this.entity = entity;
        position.set(entity.posX, entity.boundingBox.minY, entity.posZ);
    }

    public Vector3d getPos() {
        return position;
    }

    public double[] getArrayPos() {
        return new double[]{position.x, position.y, position.z};
    }

    public void set(double x, double y, double z) {
        position.set(x, y, z);
    }
}
