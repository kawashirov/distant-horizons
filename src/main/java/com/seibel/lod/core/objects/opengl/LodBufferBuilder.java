/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.objects.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * A (almost) exact copy of Minecraft's
 * BufferBuilder object. <br>
 * Which allows for creating and filling
 * OpenGL buffers.
 * 
 * @author James Seibel
 * @version 11-13-2021
 */
public class LodBufferBuilder
{
	private static final Logger LOGGER = LogManager.getLogger();
	public ByteBuffer buffer;
	
	private final List<LodBufferBuilder.DrawState> vertexCounts = Lists.newArrayList();
	private int lastRenderedCountIndex = 0;
	private int totalRenderedBytes = 0;
	private int nextElementByte = 0;
	private int totalUploadedBytes = 0;
	private int vertices;
	private LodVertexFormatElement currentElement;
	private int elementIndex;
	private int mode;
	private LodVertexFormat format;
	private boolean building;
	
	
	
	
	public LodBufferBuilder(int bufferSizeInBytes)
	{
		this.buffer = allocateByteBuffer(bufferSizeInBytes * 4);
	}
	
	
	
	/** originally from MC's GLAllocation class */
	private ByteBuffer allocateByteBuffer(int bufferSizeInBytes)
	{
		return ByteBuffer.allocateDirect(bufferSizeInBytes).order(ByteOrder.nativeOrder());
	}
	/** originally from MC's GLAllocation class */
	@SuppressWarnings("unused")
	private FloatBuffer allocateFloatBuffer(int bufferSizeInBytes)
	{
		return allocateByteBuffer(bufferSizeInBytes).asFloatBuffer();
	}
	
	
	
	/** make sure the buffer doesn't overflow when inserting new elements */
	private void ensureVertexCapacity()
	{
		this.ensureCapacity(this.format.getVertexSize());
	}
	private void ensureCapacity(int vertexSizeInBytes)
	{
		if (this.nextElementByte + vertexSizeInBytes > this.buffer.capacity())
		{
			int i = this.buffer.capacity();
			int j = i + roundUp(vertexSizeInBytes);
			//LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", i, j);
			ByteBuffer bytebuffer = allocateByteBuffer(j);
			this.buffer.position(0);
			bytebuffer.put(this.buffer);
			bytebuffer.rewind();
			this.buffer = bytebuffer;
		}
	}
	private static int roundUp(int vertexSizeInBytes)
	{
		int i = 2097152; // 2 ^ 21
		if (vertexSizeInBytes == 0)
		{
			return i;
		}
		else
		{
			if (vertexSizeInBytes < 0)
			{
				i *= -1;
			}
			
			int j = vertexSizeInBytes % i;
			return j == 0 ? vertexSizeInBytes : vertexSizeInBytes + i - j;
		}
	}
	
	
	/* not currently needed sortQuads()
	// the x,y,z location is a blockPos
	public void sortQuads(float x, float y, float z)
	{
		((Buffer) this.buffer).clear();
		FloatBuffer floatbuffer = this.buffer.asFloatBuffer();
		int i = this.vertices / 4;
		float[] afloat = new float[i];
		
		for (int j = 0; j < i; ++j)
		{
			afloat[j] = getQuadDistanceFromPlayer(floatbuffer, x, y, z, this.format.getIntegerSize(), this.totalRenderedBytes / 4 + j * this.format.getVertexSize());
		}
		
		int[] aint = new int[i];
		
		for (int k = 0; k < aint.length; aint[k] = k++)
		{
		}
		
		IntArrays.mergeSort(aint, (p_227830_1_, p_227830_2_) ->
		{
			return Floats.compare(afloat[p_227830_2_], afloat[p_227830_1_]);
		});
		BitSet bitset = new BitSet();
		FloatBuffer floatbuffer1 = allocateFloatBuffer(this.format.getIntegerSize() * 4);
		
		for (int l = bitset.nextClearBit(0); l < aint.length; l = bitset.nextClearBit(l + 1))
		{
			int i1 = aint[l];
			if (i1 != l)
			{
				this.limitToVertex(floatbuffer, i1);
				((Buffer) floatbuffer1).clear();
				floatbuffer1.put(floatbuffer);
				int j1 = i1;
				
				for (int k1 = aint[i1]; j1 != l; k1 = aint[k1])
				{
					this.limitToVertex(floatbuffer, k1);
					FloatBuffer floatbuffer2 = floatbuffer.slice();
					this.limitToVertex(floatbuffer, j1);
					floatbuffer.put(floatbuffer2);
					bitset.set(j1);
					j1 = k1;
				}
				
				this.limitToVertex(floatbuffer, l);
				((Buffer) floatbuffer1).flip();
				floatbuffer.put(floatbuffer1);
			}
			
			bitset.set(l);
		}
	}
	
	
	private void limitToVertex(FloatBuffer p_227829_1_, int p_227829_2_)
	{
		int i = this.format.getIntegerSize() * 4;
		((Buffer) p_227829_1_).limit(this.totalRenderedBytes / 4 + (p_227829_2_ + 1) * i);
		((Buffer) p_227829_1_).position(this.totalRenderedBytes / 4 + p_227829_2_ * i);
	}
	*/
	
	/* not curerntly needed getState()
	public LodBufferBuilder.State getState()
	{
		((Buffer) this.buffer).limit(this.nextElementByte);
		((Buffer) this.buffer).position(this.totalRenderedBytes);
		ByteBuffer bytebuffer = ByteBuffer.allocate(this.vertices * this.format.getVertexSize());
		bytebuffer.put(this.buffer);
		((Buffer) this.buffer).clear();
		return new LodBufferBuilder.State(bytebuffer, this.format);
	}
	*/
	
	/* not currently needed getQuadDistanceFromPlayer()
	private static float getQuadDistanceFromPlayer(FloatBuffer floatBuffer, float x, float y, float z, int p_181665_4_, int p_181665_5_)
	{
		float f = floatBuffer.get(p_181665_5_ + p_181665_4_ * 0 + 0);
		float f1 = floatBuffer.get(p_181665_5_ + p_181665_4_ * 0 + 1);
		float f2 = floatBuffer.get(p_181665_5_ + p_181665_4_ * 0 + 2);
		float f3 = floatBuffer.get(p_181665_5_ + p_181665_4_ * 1 + 0);
		float f4 = floatBuffer.get(p_181665_5_ + p_181665_4_ * 1 + 1);
		float f5 = floatBuffer.get(p_181665_5_ + p_181665_4_ * 1 + 2);
		float f6 = floatBuffer.get(p_181665_5_ + p_181665_4_ * 2 + 0);
		float f7 = floatBuffer.get(p_181665_5_ + p_181665_4_ * 2 + 1);
		float f8 = floatBuffer.get(p_181665_5_ + p_181665_4_ * 2 + 2);
		float f9 = floatBuffer.get(p_181665_5_ + p_181665_4_ * 3 + 0);
		float f10 = floatBuffer.get(p_181665_5_ + p_181665_4_ * 3 + 1);
		float f11 = floatBuffer.get(p_181665_5_ + p_181665_4_ * 3 + 2);
		float f12 = (f + f3 + f6 + f9) * 0.25F - x;
		float f13 = (f1 + f4 + f7 + f10) * 0.25F - y;
		float f14 = (f2 + f5 + f8 + f11) * 0.25F - z;
		return f12 * f12 + f13 * f13 + f14 * f14;
	}
	*/
	
	/* not currently needed restoreState()
	public void restoreState(LodBufferBuilder.State bufferState)
	{
		((Buffer) bufferState.data).clear();
		int i = bufferState.data.capacity();
		this.ensureCapacity(i);
		((Buffer) this.buffer).limit(this.buffer.capacity());
		((Buffer) this.buffer).position(this.totalRenderedBytes);
		this.buffer.put(bufferState.data);
		((Buffer) this.buffer).clear();
		LodVertexFormat LodVertexFormat = bufferState.format;
		this.switchFormat(LodVertexFormat);
		this.vertices = i / LodVertexFormat.getVertexSize();
		this.nextElementByte = this.totalRenderedBytes + this.vertices * LodVertexFormat.getVertexSize();
	}
	*/
	
	
	
	

	private void switchFormat(LodVertexFormat newFormat)
	{
		format = newFormat;
	}
	
	
	
	
	//========================================//
	// methods for actually building a buffer //
	//========================================//
	
	/**
	 * @param openGlLodVertexFormat GL11.GL_QUADS, GL11.GL_TRIANGLES, etc.
	 * @param LodVertexFormat
	 */
	public void begin(int openGlLodVertexFormat, LodVertexFormat LodVertexFormat)
	{
		if (this.building)
		{
			throw new IllegalStateException("Already building!");
		}
		else
		{
			this.building = true;
			this.mode = openGlLodVertexFormat;
			this.switchFormat(LodVertexFormat);
			this.currentElement = LodVertexFormat.getElements().get(0);
			this.elementIndex = 0;
			this.buffer.clear();
		}
	}
	
	public void end()
	{
		if (!this.building)
		{
			throw new IllegalStateException("Not building!");
		}
		else
		{
			this.building = false;
			this.vertexCounts.add(new LodBufferBuilder.DrawState(this.format, this.vertices, this.mode));
			this.totalRenderedBytes += this.vertices * this.format.getVertexSize();
			this.vertices = 0;
			this.currentElement = null;
			this.elementIndex = 0;
		}
	}
	
	public void putByte(int index, byte newByte)
	{
		this.buffer.put(this.nextElementByte + index, newByte);
	}
	
	public void putShort(int index, short newShort)
	{
		this.buffer.putShort(this.nextElementByte + index, newShort);
	}
	
	public void putFloat(int index, float newFloat)
	{
		this.buffer.putFloat(this.nextElementByte + index, newFloat);
	}
	
	public void endVertex()
	{
		if (this.elementIndex != 0)
		{
			throw new IllegalStateException("Not filled all elements of the vertex");
		}
		else
		{
			++this.vertices;
			this.ensureVertexCapacity();
		}
	}
	
	public void nextElement()
	{
		ImmutableList<LodVertexFormatElement> immutablelist = this.format.getElements();
		this.elementIndex = (this.elementIndex + 1) % immutablelist.size();
		this.nextElementByte += this.currentElement.getByteSize();
		this.currentElement = immutablelist.get(this.elementIndex);
//		if (LodVertexFormatelement.getUsage() == LodVertexFormatElement.Usage.PADDING)
//		{
//			this.nextElement();
//		}
		
//		if (this.defaultColorSet && this.currentElement.getUsage() == LodVertexFormatElement.Usage.COLOR)
//		{
//			color(this.defaultR, this.defaultG, this.defaultB, this.defaultA);
//		}
		
	}
	
	public LodBufferBuilder color(int red, int green, int blue, int alpha)
	{
		LodVertexFormatElement LodVertexFormatelement = this.currentElement();
		if (LodVertexFormatelement.getType() != LodVertexFormatElement.DataType.UBYTE)
		{
			throw new IllegalStateException("Color must be stored as a UBYTE");
		}
		else
		{
			this.putByte(0, (byte) red);
			this.putByte(1, (byte) green);
			this.putByte(2, (byte) blue);
			this.putByte(3, (byte) alpha);
			this.nextElement();
			return this;
		}
	}
	
	public LodBufferBuilder vertex(float x, float y, float z)
	{
		if (this.currentElement().getType() != LodVertexFormatElement.DataType.FLOAT)
		{
			throw new IllegalStateException("Position verticies must be stored as a FLOAT");
		}
		else
		{
			this.putFloat(0, x);
			this.putFloat(4, y);
			this.putFloat(8, z);
			this.nextElement();
			return this;
		}
	}
	
	
	
	
	
	
	/* not currently needed fullVertex()
	 * TODO James isn't sure about these names
	public void vertex(float blockPosX, float blockPosY, float blockPosZ, 
			float red, float green, float blue, float alpha, 
			float textureU, float textureV, 
			int p_225588_10_, int p_225588_11_, 
			float p_225588_12_, float p_225588_13_, float p_225588_14_)
	{
		if (this.defaultColorSet)
		{
			throw new IllegalStateException();
		}
		else if (this.fastFormat)
		{
			this.putFloat(0, blockPosX);
			this.putFloat(4, blockPosY);
			this.putFloat(8, blockPosZ);
			this.putByte(12, (byte) ((int) (red * 255.0F)));
			this.putByte(13, (byte) ((int) (green * 255.0F)));
			this.putByte(14, (byte) ((int) (blue * 255.0F)));
			this.putByte(15, (byte) ((int) (alpha * 255.0F)));
			this.putFloat(16, textureU);
			this.putFloat(20, textureV);
			int i;
			if (this.fullFormat)
			{
				this.putShort(24, (short) (p_225588_10_ & '\uffff'));
				this.putShort(26, (short) (p_225588_10_ >> 16 & '\uffff'));
				i = 28;
			}
			else
			{
				i = 24;
			}
			
			this.putShort(i + 0, (short) (p_225588_11_ & '\uffff'));
			this.putShort(i + 2, (short) (p_225588_11_ >> 16 & '\uffff'));
			this.putByte(i + 4, IVertexConsumer.normalIntValue(p_225588_12_));
			this.putByte(i + 5, IVertexConsumer.normalIntValue(p_225588_13_));
			this.putByte(i + 6, IVertexConsumer.normalIntValue(p_225588_14_));
			this.nextElementByte += i + 8;
			this.endVertex();
		}
		else
		{
			super.vertex(blockPosX, blockPosY, blockPosZ, red, green, blue, alpha, textureU, textureV, p_225588_10_, p_225588_11_, p_225588_12_, p_225588_13_, p_225588_14_);
		}
	}
	*/
	
	/** 
	 * James isn't sure what the difference between 
	 * using this method and just directly getting the buffer would be.
	 * But this was what was being used before, so it will stay for now.
	 * 
	 * If anyone figures out what is special about this, please replace this comment.
	 */
	public ByteBuffer getCleanedByteBuffer()
	{
		LodBufferBuilder.DrawState bufferbuilder$drawstate = this.vertexCounts.get(this.lastRenderedCountIndex++);
		this.buffer.position(this.totalUploadedBytes);
		this.totalUploadedBytes += bufferbuilder$drawstate.vertexCount() * bufferbuilder$drawstate.format().getVertexSize();
		this.buffer.limit(this.totalUploadedBytes);
		if (this.lastRenderedCountIndex == this.vertexCounts.size() && this.vertices == 0)
		{
			this.clear();
		}
		
		ByteBuffer bytebuffer = this.buffer.slice();
		bytebuffer.order(this.buffer.order()); // FORGE: Fix incorrect byte order
		this.buffer.clear();
		return bytebuffer; // the original method also returned bufferbuilder$drawstate
	}
	
	
	public void clear()
	{
		if (this.totalRenderedBytes != this.totalUploadedBytes)
		{
			LOGGER.warn("Bytes mismatch " + this.totalRenderedBytes + " " + this.totalUploadedBytes);
		}
		
		this.discard();
	}
	
	public void discard()
	{
		this.totalRenderedBytes = 0;
		this.totalUploadedBytes = 0;
		this.nextElementByte = 0;
		this.vertexCounts.clear();
		this.lastRenderedCountIndex = 0;
	}
	
	public LodVertexFormatElement currentElement()
	{
		if (this.currentElement == null)
		{
			throw new IllegalStateException("BufferBuilder not started");
		}
		else
		{
			return this.currentElement;
		}
	}
	
	public boolean building()
	{
		return this.building;
	}
	
	
	
	
	
	//==================//
	// internal classes //
	//==================//
	
	
	public static final class DrawState
	{
		private final LodVertexFormat format;
		private final int vertexCount;
		private final int mode;
		
		private DrawState(LodVertexFormat p_i225905_1_, int p_i225905_2_, int p_i225905_3_)
		{
			this.format = p_i225905_1_;
			this.vertexCount = p_i225905_2_;
			this.mode = p_i225905_3_;
		}
		
		public LodVertexFormat format()
		{
			return this.format;
		}
		
		public int vertexCount()
		{
			return this.vertexCount;
		}
		
		public int mode()
		{
			return this.mode;
		}
	}
	
	
	
	// Forge added methods
	public void putBulkData(ByteBuffer buffer)
	{
		ensureCapacity(buffer.limit() + this.format.getVertexSize());
		this.buffer.position(this.vertices * this.format.getVertexSize());
		this.buffer.put(buffer);
		this.vertices += buffer.limit() / this.format.getVertexSize();
		this.nextElementByte += buffer.limit();
	}
	
	public LodVertexFormat getLodVertexFormat()
	{
		return this.format;
	}
}