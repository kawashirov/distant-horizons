package backsun.lod.util;

import java.nio.FloatBuffer;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LodClippingHelper extends ClippingHelper
{
    private static final LodClippingHelper instance = new LodClippingHelper();
    private final FloatBuffer projectionMatrixBuffer = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer modelviewMatrixBuffer = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer floatBuffer16 = GLAllocation.createDirectFloatBuffer(16);

    /**
     * Initializes the ClippingHelper object then returns an instance of it.
     */
    public static ClippingHelper getInstance()
    {
        instance.init();
        return instance;
    }

    private void normalize(float[] vec4)
    {
        float f = MathHelper.sqrt(vec4[0] * vec4[0] + vec4[1] * vec4[1] + vec4[2] * vec4[2]);
        vec4[0] /= f;
        vec4[1] /= f;
        vec4[2] /= f;
        vec4[3] /= f;
    }

    public void init()
    {
        projectionMatrixBuffer.clear();
        modelviewMatrixBuffer.clear();
        floatBuffer16.clear();
        
        
        GlStateManager.getFloat(2982, modelviewMatrixBuffer); // 2982 = model view matrix location
        float[] proj = projectionMatrix;
        float[] mvm = modelviewMatrix;
        projectionMatrixBuffer.flip().limit(16);
        projectionMatrixBuffer.get(proj);
        modelviewMatrixBuffer.flip().limit(16);
        modelviewMatrixBuffer.get(mvm);
        clippingMatrix[0] = mvm[0] * proj[0] + mvm[1] * proj[4] + mvm[2] * proj[8] + mvm[3] * proj[12];
        clippingMatrix[1] = mvm[0] * proj[1] + mvm[1] * proj[5] + mvm[2] * proj[9] + mvm[3] * proj[13];
        clippingMatrix[2] = mvm[0] * proj[2] + mvm[1] * proj[6] + mvm[2] * proj[10] + mvm[3] * proj[14];
        clippingMatrix[3] = mvm[0] * proj[3] + mvm[1] * proj[7] + mvm[2] * proj[11] + mvm[3] * proj[15];
        clippingMatrix[4] = mvm[4] * proj[0] + mvm[5] * proj[4] + mvm[6] * proj[8] + mvm[7] * proj[12];
        clippingMatrix[5] = mvm[4] * proj[1] + mvm[5] * proj[5] + mvm[6] * proj[9] + mvm[7] * proj[13];
        clippingMatrix[6] = mvm[4] * proj[2] + mvm[5] * proj[6] + mvm[6] * proj[10] + mvm[7] * proj[14];
        clippingMatrix[7] = mvm[4] * proj[3] + mvm[5] * proj[7] + mvm[6] * proj[11] + mvm[7] * proj[15];
        clippingMatrix[8] = mvm[8] * proj[0] + mvm[9] * proj[4] + mvm[10] * proj[8] + mvm[11] * proj[12];
        clippingMatrix[9] = mvm[8] * proj[1] + mvm[9] * proj[5] + mvm[10] * proj[9] + mvm[11] * proj[13];
        clippingMatrix[10] = mvm[8] * proj[2] + mvm[9] * proj[6] + mvm[10] * proj[10] + mvm[11] * proj[14];
        clippingMatrix[11] = mvm[8] * proj[3] + mvm[9] * proj[7] + mvm[10] * proj[11] + mvm[11] * proj[15];
        clippingMatrix[12] = mvm[12] * proj[0] + mvm[13] * proj[4] + mvm[14] * proj[8] + mvm[15] * proj[12];
        clippingMatrix[13] = mvm[12] * proj[1] + mvm[13] * proj[5] + mvm[14] * proj[9] + mvm[15] * proj[13];
        clippingMatrix[14] = mvm[12] * proj[2] + mvm[13] * proj[6] + mvm[14] * proj[10] + mvm[15] * proj[14];
        clippingMatrix[15] = mvm[12] * proj[3] + mvm[13] * proj[7] + mvm[14] * proj[11] + mvm[15] * proj[15];
        
        System.out.println(proj[0] + "\t" +proj[1] + "\t" +proj[2] + "\t" +proj[3]);
        System.out.println(proj[4] + "\t" +proj[5] + "\t" +proj[6] + "\t" +proj[7]);
        System.out.println(proj[8] + "\t" +proj[9] + "\t" +proj[10] + "\t" +proj[11]);
        System.out.println(proj[12] + "\t" +proj[13] + "\t" +proj[14] + "\t" +proj[15]);
        System.out.println();
        
        float[] afloat2 = frustum[0];
        afloat2[0] = clippingMatrix[3] - clippingMatrix[0];
        afloat2[1] = clippingMatrix[7] - clippingMatrix[4];
        afloat2[2] = clippingMatrix[11] - clippingMatrix[8];
        afloat2[3] = clippingMatrix[15] - clippingMatrix[12];
        normalize(afloat2);
        float[] afloat3 = frustum[1];
        afloat3[0] = clippingMatrix[3] + clippingMatrix[0];
        afloat3[1] = clippingMatrix[7] + clippingMatrix[4];
        afloat3[2] = clippingMatrix[11] + clippingMatrix[8];
        afloat3[3] = clippingMatrix[15] + clippingMatrix[12];
        normalize(afloat3);
        float[] afloat4 = frustum[2];
        afloat4[0] = clippingMatrix[3] + clippingMatrix[1];
        afloat4[1] = clippingMatrix[7] + clippingMatrix[5];
        afloat4[2] = clippingMatrix[11] + clippingMatrix[9];
        afloat4[3] = clippingMatrix[15] + clippingMatrix[13];
        normalize(afloat4);
        float[] afloat5 = frustum[3];
        afloat5[0] = clippingMatrix[3] - clippingMatrix[1];
        afloat5[1] = clippingMatrix[7] - clippingMatrix[5];
        afloat5[2] = clippingMatrix[11] - clippingMatrix[9];
        afloat5[3] = clippingMatrix[15] - clippingMatrix[13];
        normalize(afloat5);
        float[] afloat6 = frustum[4];
        afloat6[0] = clippingMatrix[3] - clippingMatrix[2];
        afloat6[1] = clippingMatrix[7] - clippingMatrix[6];
        afloat6[2] = clippingMatrix[11] - clippingMatrix[10];
        afloat6[3] = clippingMatrix[15] - clippingMatrix[14];
        normalize(afloat6);
        float[] afloat7 = frustum[5];
        afloat7[0] = clippingMatrix[3] + clippingMatrix[2];
        afloat7[1] = clippingMatrix[7] + clippingMatrix[6];
        afloat7[2] = clippingMatrix[11] + clippingMatrix[10];
        afloat7[3] = clippingMatrix[15] + clippingMatrix[14];
        normalize(afloat7);
    }
}