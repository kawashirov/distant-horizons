package backsun.lod.asm;

import java.util.Arrays;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import backsun.lod.util.RenderGlobalHook;
import net.minecraft.launchwrapper.IClassTransformer;

public class RenderGlobalClassTransformer implements IClassTransformer
{
	private static final String[] classesBeingTransformed = { "net.minecraft.client.renderer.RenderGlobal" };
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] classBeingTransformed)
	{
		int index = Arrays.asList(classesBeingTransformed).indexOf(transformedName);
		
		// do we wan't to transform this class?
		if (index != -1)
		{
			// yes, transform this class
			boolean isObfuscated = !name.equals(transformedName);
			return transformClass(index, classBeingTransformed, isObfuscated);
		}
		else
		{
			// no, just skip this class
			return classBeingTransformed;
		}
	}
	
	private static byte[] transformClass(int index, byte[] classBeingTransformed, boolean isObfuscated)
	{
		try
		{
			// convert the byte code into readable ASM code
			ClassNode classNode = new ClassNode();
			ClassReader classReader = new ClassReader(classBeingTransformed);
			classReader.accept(classNode, 0);
			
			transformRenderGlobal(classNode, isObfuscated);
			
			// convert back into byte code
			ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			classNode.accept(classWriter);
			return classWriter.toByteArray();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return classBeingTransformed;
	}
	
	
	
	private static void transformRenderGlobal(ClassNode classNode, boolean isObfuscated)
	{
		final String methodName = isObfuscated ? "a" : "renderBlockLayer";
		final String methodDesc = isObfuscated ? 
			"(Lamk;)V" : 
			"(Lnet/minecraft/util/BlockRenderLayer;)V";
		
		for (MethodNode method : classNode.methods)
		{
			if (method.name.equals(methodName) && method.desc.equals(methodDesc))
			{
				AbstractInsnNode firstLoadNode = null;
				AbstractInsnNode firstReturnNode = null;
				for (AbstractInsnNode instruction : method.instructions.toArray())
				{
					if (firstLoadNode == null && instruction.getOpcode() == Opcodes.ALOAD)
					{
						// look for the first time the RenderGlobal (self)
						// variable is loaded, IE the first line of code
						// in the unedited method
						if (((VarInsnNode) instruction).var == 0)
						{
							firstLoadNode = instruction;
						}
					}
					
					if (instruction.getOpcode() == Opcodes.RETURN)
					{
						// look for the first (and only) return statement
						// IE the last line of code in the unedited method
						firstReturnNode = instruction;
						break;
					}
				}
				
				if (firstLoadNode != null && firstReturnNode != null) 
                {
					// add the startRenderingStencil method to the beginning of the method
                    InsnList toInsert = new InsnList();
                    toInsert.add(new VarInsnNode(Opcodes.ALOAD, 1)); // BlockRenderLayer variable
                    toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(RenderGlobalHook.class), RenderGlobalHook.START_STENCIL_METHOD_NAME, isObfuscated ? "(Lamk;)V" : "(Lnet/minecraft/util/BlockRenderLayer;)V", false));
                    toInsert.add(new LabelNode());
                    method.instructions.insertBefore(firstLoadNode, toInsert);
                    
                    
                    // add the endRenderingStencil method to the end of the method
                    toInsert = new InsnList();
                    toInsert.add(new VarInsnNode(Opcodes.ALOAD, 1)); // BlockRenderLayer variable
                    toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(RenderGlobalHook.class), RenderGlobalHook.END_STENCIL_METHOD_NAME, isObfuscated ? "(Lamk;)V" : "(Lnet/minecraft/util/BlockRenderLayer;)V", false));
                    toInsert.add(new LabelNode());
                    method.instructions.insertBefore(firstReturnNode, toInsert);
                }
                else
                {
                    System.out.println("Something went wrong transforming RenderGlobal!");
                }
				
				
			}
		}
	}
}
