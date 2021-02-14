package com.backsun.lod.renderer;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Callable;

import com.backsun.lod.util.enums.FogDistance;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

/**
 * 
 * 
 * @author James Seibel
 * @version 02-13-2021
 */
public class BuildBufferThread implements Callable<NearFarBuffer>
{
	public ByteBuffer nearBuffer;
	public ByteBuffer farBuffer;
	public FogDistance distanceMode;
	public AxisAlignedBB[][] lods;
	public Color[][] colors;
	
	private int lodStartX = 0;
	private int lodStartZ = 0;
	private int start = 0;
	private int end = -1;
	
	private int vertexCount = 0;
	private VertexFormat vertexFormat = null;
	private int vertexFormatIndex = 0;
	private VertexFormatElement vertexFormatElement = null; 
	
	
	
	BuildBufferThread()
	{
		vertexCount = 0;
		vertexFormat = DefaultVertexFormats.POSITION_COLOR;
		vertexFormatIndex = 0;
		vertexFormatElement = vertexFormat.getElement(vertexFormatIndex); 
	}
	
	BuildBufferThread(ByteBuffer newNearByteBuffer, ByteBuffer newFarByteBuffer, AxisAlignedBB[][] newLods, Color[][] newColors, FogDistance newDistanceMode, Vec3i newLodStartCoordinate, int threadNumber, int totalThreads)
	{
		setNewData(newNearByteBuffer, newFarByteBuffer, distanceMode, newLodStartCoordinate, newLods, newColors, threadNumber, totalThreads);
		
		vertexCount = 0;
		vertexFormat = DefaultVertexFormats.POSITION_COLOR;
		vertexFormatIndex = 0;
		vertexFormatElement = vertexFormat.getElement(vertexFormatIndex); 
	}
	
	public void setNewData(ByteBuffer newNearByteBuffer, ByteBuffer newFarByteBuffer, FogDistance newDistanceMode, Vec3i newlodStartCoordinate, AxisAlignedBB[][] newLods, Color[][] newColors, int threadNumber, int totalThreads)
	{
		vertexCount = 0;
        vertexFormatIndex = 0;
		
		nearBuffer = newNearByteBuffer;
		farBuffer = newFarByteBuffer;
		distanceMode = newDistanceMode;
		lods = newLods;
		colors = newColors;
		
		int numbChunksWide = lods.length;
		int rowsToRender = numbChunksWide / totalThreads;
		start = threadNumber * rowsToRender;
		end = (threadNumber + 1) * rowsToRender;
		
		lodStartX = newlodStartCoordinate.getX();
		lodStartZ = newlodStartCoordinate.getZ();
	}
	
	@Override
	public NearFarBuffer call()
	{
		int numbChunksWide = lods.length;
		
		ByteBuffer currentBuffer;
		AxisAlignedBB bb;
		int red;
		int green;
		int blue;
		int alpha;
		
		int chunkX;
		int chunkZ;
		
		// x axis
		for (int i = start; i < end; i++)
		{
			// z axis
			for (int j = 0; j < numbChunksWide; j++)
			{
				if (lods[i][j] == null || colors[i][j] == null)
					continue;
				
				bb = lods[i][j];
				
				// get the color of this LOD object
				red = colors[i][j].getRed();
				green = colors[i][j].getGreen();
				blue = colors[i][j].getBlue();
				alpha = colors[i][j].getAlpha();
				
				// choose which buffer to add these LODs too
				if (distanceMode == FogDistance.BOTH)
				{
					if (RenderUtil.isCoordinateInNearFogArea(i, j, numbChunksWide / 2))
						currentBuffer = nearBuffer;
					else
						currentBuffer = farBuffer;
				}
				else if (distanceMode == FogDistance.NEAR)
				{
					currentBuffer = nearBuffer;
				}
				else // if (distanceMode == FogDistance.FAR)
				{
					currentBuffer = farBuffer;
				}
				
				
				if (bb.minY != bb.maxY)
				{
					// top (facing up)
					addPosAndColor(currentBuffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
					// bottom (facing down)
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);

					// south (facing -Z) 
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
					// north (facing +Z)
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);

					// west (facing -X)
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
					// east (facing +X)
					addPosAndColor(currentBuffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
				}
				else
				{
					// render this LOD as one block thick
					
					// top (facing up)
					addPosAndColor(currentBuffer, bb.minX, bb.minY+1, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY+1, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY+1, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY+1, bb.minZ, red, green, blue, alpha);
					// bottom (facing down)
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);

					// south (facing -Z) 
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY+1, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY+1, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
					// north (facing +Z)
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY+1, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY+1, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);

					// west (facing -X)
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY+1, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.minX, bb.minY+1, bb.minZ, red, green, blue, alpha);
					// east (facing +X)
					addPosAndColor(currentBuffer, bb.maxX, bb.minY+1, bb.minZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY+1, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
					addPosAndColor(currentBuffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
				}
				
			} // z axis
		} // x axis
		
		return new NearFarBuffer(nearBuffer, farBuffer);
	}
	
	private void addPosAndColor(ByteBuffer buffer, double x, double y, double z, int red, int green, int blue, int alpha)
	{
		addPos(buffer, x, y, z);
		addColor(buffer, red, green, blue, alpha);
		endVertex();
	}

	private void addPos(ByteBuffer byteBuffer, double x, double y, double z)
	{
		int i = this.vertexCount * this.vertexFormat.getNextOffset() + this.vertexFormat.getOffset(this.vertexFormatIndex);

        switch (this.vertexFormatElement.getType())
        {
            case FLOAT: // This is the one currently used
                byteBuffer.putFloat(i, (float)(x));
                byteBuffer.putFloat(i + 4, (float)(y));
                byteBuffer.putFloat(i + 8, (float)(z));
                break;
            case UINT:
            case INT:
                byteBuffer.putInt(i, Float.floatToRawIntBits((float)(x)));
                byteBuffer.putInt(i + 4, Float.floatToRawIntBits((float)(y)));
                byteBuffer.putInt(i + 8, Float.floatToRawIntBits((float)(z)));
                break;
            case USHORT:
            case SHORT:
                byteBuffer.putShort(i, (short)((int)(x)));
                byteBuffer.putShort(i + 2, (short)((int)(y)));
                byteBuffer.putShort(i + 4, (short)((int)(z)));
                break;
            case UBYTE:
            case BYTE:
                byteBuffer.put(i, (byte)((int)(x)));
                byteBuffer.put(i + 1, (byte)((int)(y)));
                byteBuffer.put(i + 2, (byte)((int)(z)));
        }
        
        nextVertexFormatIndex();
	}
	
	private void addColor(ByteBuffer byteBuffer, int red, int green, int blue, int alpha)
    {
        int i = this.vertexCount * this.vertexFormat.getNextOffset() + this.vertexFormat.getOffset(this.vertexFormatIndex);

        switch (this.vertexFormatElement.getType())
        {
            case FLOAT:
            	byteBuffer.putFloat(i, red / 255.0F);
            	byteBuffer.putFloat(i + 4, green / 255.0F);
            	byteBuffer.putFloat(i + 8, blue / 255.0F);
            	byteBuffer.putFloat(i + 12, alpha / 255.0F);
                break;
            case UINT:
            case INT:
            	byteBuffer.putFloat(i, red);
            	byteBuffer.putFloat(i + 4, green);
            	byteBuffer.putFloat(i + 8, blue);
            	byteBuffer.putFloat(i + 12, alpha);
                break;
            case USHORT:
            case SHORT:
            	byteBuffer.putShort(i, (short)red);
            	byteBuffer.putShort(i + 2, (short)green);
            	byteBuffer.putShort(i + 4, (short)blue);
            	byteBuffer.putShort(i + 6, (short)alpha);
                break;
            case UBYTE:
            case BYTE:
            	
                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
                {
                	// this is the one used currently
                    byteBuffer.put(i, (byte)red);
                    byteBuffer.put(i + 1, (byte)green);
                    byteBuffer.put(i + 2, (byte)blue);
                    byteBuffer.put(i + 3, (byte)alpha);
                }
                else
                {
                    byteBuffer.put(i, (byte)alpha);
                    byteBuffer.put(i + 1, (byte)blue);
                    byteBuffer.put(i + 2, (byte)green);
                    byteBuffer.put(i + 3, (byte)red);
                }
        }

        nextVertexFormatIndex();
    }
	
	private void nextVertexFormatIndex()
    {
        ++this.vertexFormatIndex;
        this.vertexFormatIndex %= this.vertexFormat.getElementCount();
        this.vertexFormatElement = this.vertexFormat.getElement(this.vertexFormatIndex);

        if (this.vertexFormatElement.getUsage() == VertexFormatElement.EnumUsage.PADDING)
        {
            this.nextVertexFormatIndex();
        }
    }
	
	private void endVertex()
    {
        ++this.vertexCount;
        growBuffer(this.vertexFormat.getNextOffset());
    }
	
	private void growBuffer(int p_181670_1_)
    {
        //if (MathHelper.roundUp(p_181670_1_, 4) / 4 > this.rawIntBuffer.remaining() || this.vertexCount * this.vertexFormat.getNextOffset() + p_181670_1_ > this.byteBuffer.capacity())
		if (this.vertexCount * this.vertexFormat.getNextOffset() + p_181670_1_ > nearBuffer.capacity())
        {
            int i = nearBuffer.capacity();
            int j = i + MathHelper.roundUp(p_181670_1_, 2097152);
//	            int k = this.rawIntBuffer.position();
            ByteBuffer directBytebuffer = GLAllocation.createDirectByteBuffer(j);
            nearBuffer.position(0);
            directBytebuffer.put(nearBuffer);
            directBytebuffer.rewind();
            nearBuffer = directBytebuffer;
//	            this.rawFloatBuffer = buffer.asFloatBuffer().asReadOnlyBuffer();
//	            this.rawIntBuffer = buffer.asIntBuffer();
//	            this.rawIntBuffer.position(k);
//	            this.rawShortBuffer = buffer.asShortBuffer();
//	            this.rawShortBuffer.position(k << 1);
        }
    }
	
	
	
}