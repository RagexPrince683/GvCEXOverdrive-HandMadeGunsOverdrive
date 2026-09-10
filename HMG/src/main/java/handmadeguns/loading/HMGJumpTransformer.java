package handmadeguns.loading;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Entry rejection, after Forge's deobfuscation transformer (MCP or SRG names). */
public final class HMGJumpTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.entity.EntityLivingBase".equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        for (Object value : node.methods) {
            MethodNode method = (MethodNode) value;
            if (!"()V".equals(method.desc)
                    || !("jump".equals(method.name) || "func_70664_aZ".equals(method.name))) continue;
            InsnList hook = new InsnList();
            LabelNode vanilla = new LabelNode();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "handmadeguns/event/HMGJumpHandler", "rejectFallbackJump",
                    "(Lnet/minecraft/entity/EntityLivingBase;)Z", false));
            hook.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
            hook.add(new InsnNode(Opcodes.RETURN));
            hook.add(vanilla);
            // Empty stack and unchanged locals; preserve the original method frames.
            hook.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
            method.instructions.insert(hook);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        }
        throw new IllegalStateException("HMG could not find EntityLivingBase.jump()V");
    }
}
