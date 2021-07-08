package com.seibel.lod.util;

import net.minecraft.block.Blocks;
import net.minecraft.world.biome.*;

import java.awt.*;

public class BiomeColorsUtils {
    //public static OverworldBiomeSource overworldBiomeSource = new OverworldBiomeSource(MCVersion.v1_16_4, 64971835648254);

    public static Color getColorFromBiome(Biome biome,double x, double y){
        int color = 0;
        switch(biome.getBiomeCategory()) {
            case BEACH:
            case DESERT:
                color = Blocks.SAND.defaultMaterialColor().col;
                break;
            case EXTREME_HILLS:
                color = Blocks.SNOW.defaultMaterialColor().col;
                break;
            case NONE:
                break;
            case FOREST:
            case JUNGLE:
            case TAIGA:
                color = biome.getFoliageColor();
                break;
            case MUSHROOM:
                color = Blocks.MYCELIUM.defaultMaterialColor().col;
                break;
            case PLAINS:
            case SAVANNA:
                color = biome.getGrassColor(x,y);
                break;
            case OCEAN:
            case RIVER:
            case SWAMP:
                color = biome.getWaterColor();
                break;
            case ICY:
                color = Blocks.PACKED_ICE.defaultMaterialColor().col;
                break;
            case THEEND:
                color = Blocks.END_STONE.defaultMaterialColor().col;
                break;
            case NETHER:
                color = Blocks.NETHERRACK.defaultMaterialColor().col;
                break;
            case MESA:
                color = Blocks.RED_SAND.defaultMaterialColor().col;
                break;
            default:
                color = 0;
        }
        return new Color(color);
    }

    public static Color getColorFromBiome(Biome biome){
        int color = 0;
        switch(biome.getBiomeCategory()) {
            case BEACH:
            case DESERT:
                color = Blocks.SAND.defaultMaterialColor().col;
                break;
            case EXTREME_HILLS:
                color = Blocks.SNOW.defaultMaterialColor().col;
                break;
            case FOREST:
            case SAVANNA:
            case JUNGLE:
            case TAIGA:
                color = biome.getFoliageColor();
                break;
            case MUSHROOM:
                color = Blocks.MYCELIUM.defaultMaterialColor().col;
                break;
            case PLAINS:
                color = Blocks.GRASS_BLOCK.defaultMaterialColor().col;
                break;
            case OCEAN:
            case RIVER:
            case SWAMP:
                color = biome.getWaterColor();
                break;
            case ICY:
                color = Blocks.PACKED_ICE.defaultMaterialColor().col;
                break;
            case THEEND:
                color = Blocks.END_STONE.defaultMaterialColor().col;
                break;
            case NETHER:
                color = Blocks.NETHERRACK.defaultMaterialColor().col;
                break;
            case MESA:
                color = Blocks.RED_SAND.defaultMaterialColor().col;
                break;
            case NONE:
            default:
                color = 0;
        }
        return new Color(color);
    }

    /**
     * methods that gives the ChunkBase color of biomes
     * @param biome to check
     * @return color of the biome
     */
    public static Color getColorFromIdRealistic(kaptainwutax.biomeutils.biome.Biome biome){
        Biome.Builder builder = new Biome.Builder();
        int color = 0;
        switch(biome.getCategory()) {
            case BEACH:
            case DESERT:
                color = Blocks.SAND.defaultMaterialColor().col;
                break;
            case EXTREME_HILLS:
                color = Blocks.SNOW.defaultMaterialColor().col;
                break;
            case FOREST:
                builder.biomeCategory(Biome.Category.FOREST);
                color = builder.build().getFoliageColor();
                break;
            case SAVANNA:
                builder.biomeCategory(Biome.Category.SAVANNA);
                color = builder.build().getFoliageColor();
                break;
            case JUNGLE:
                builder.biomeCategory(Biome.Category.JUNGLE);
                color = builder.build().getFoliageColor();
                break;
            case TAIGA:
                builder.biomeCategory(Biome.Category.TAIGA);
                color = builder.build().getFoliageColor();
                break;
            case MUSHROOM:
                color = Blocks.MYCELIUM.defaultMaterialColor().col;
                break;
            case PLAINS:
                color = Blocks.GRASS_BLOCK.defaultMaterialColor().col;
                break;
            case OCEAN:
                builder.biomeCategory(Biome.Category.OCEAN);
                color = builder.build().getWaterColor();
                break;
            case RIVER:
                builder.biomeCategory(Biome.Category.RIVER);
                color = builder.build().getWaterColor();
                break;
            case SWAMP:
                builder.biomeCategory(Biome.Category.SWAMP);
                color = builder.build().getWaterColor();
                break;
            case ICY:
                color = Blocks.PACKED_ICE.defaultMaterialColor().col;
                break;
            case THE_END:
                color = Blocks.END_STONE.defaultMaterialColor().col;
                break;
            case NETHER:
                color = Blocks.NETHERRACK.defaultMaterialColor().col;
                break;
            case BADLANDS_PLATEAU:
            case MESA:
                color = Blocks.RED_SAND.defaultMaterialColor().col;
                break;
            case NONE:
            default:
                color = 0;
        }
        return new Color(color);
    }

    /**
     * methods that gives the ChunkBase color of biomes
     * @param biome to check
     * @return color of the biome
     */
    public static Color getColorFromBiomeBlock(kaptainwutax.biomeutils.biome.Biome biome){
        int color = 0;
        switch(biome.getCategory()) {
            case BEACH:
            case DESERT:
                color = Blocks.SAND.defaultMaterialColor().col;
                break;
            case EXTREME_HILLS:
                color = Blocks.SNOW.defaultMaterialColor().col;
                break;
            case FOREST:
                color = Blocks.OAK_LEAVES.defaultMaterialColor().col;
                break;
            case SAVANNA:
                color = Blocks.ACACIA_LEAVES.defaultMaterialColor().col;
                break;
            case JUNGLE:
                color = Blocks.JUNGLE_LEAVES.defaultMaterialColor().col;
                break;
            case TAIGA:
                color = Blocks.SPRUCE_LEAVES.defaultMaterialColor().col;
                break;
            case MUSHROOM:
                color = Blocks.MYCELIUM.defaultMaterialColor().col;
                break;
            case PLAINS:
                color = Blocks.GRASS_BLOCK.defaultMaterialColor().col;
                break;
            case OCEAN:
            case RIVER:
                color = Blocks.WATER.defaultMaterialColor().col;
            case SWAMP:
                color = Blocks.LILY_PAD.defaultMaterialColor().col;
                break;
            case ICY:
                color = Blocks.PACKED_ICE.defaultMaterialColor().col;
                break;
            case THE_END:
                color = Blocks.END_STONE.defaultMaterialColor().col;
                break;
            case NETHER:
                color = Blocks.NETHERRACK.defaultMaterialColor().col;
                break;
            case BADLANDS_PLATEAU:
            case MESA:
                color = Blocks.RED_SAND.defaultMaterialColor().col;
                break;
            case NONE:
            default:
                color = 0;
        }
        return new Color(color);
    }

    /**
     * methods that gives the ChunkBase color of biomes
     * @param biome to check
     * @return color of the biome
     */
    public static Color getColorFromBiomeManual(kaptainwutax.biomeutils.biome.Biome biome){
        Color color;
        switch(biome.getCategory()) {
            case BEACH:
            case DESERT:
                color = new Color();
                break;
            case EXTREME_HILLS:
                color = Blocks.SNOW.defaultMaterialColor().col;
                break;
            case FOREST:
                color = Blocks.OAK_LEAVES.defaultMaterialColor().col;
                break;
            case SAVANNA:
                color = Blocks.ACACIA_LEAVES.defaultMaterialColor().col;
                break;
            case JUNGLE:
                color = Blocks.JUNGLE_LEAVES.defaultMaterialColor().col;
                break;
            case TAIGA:
                color = Blocks.SPRUCE_LEAVES.defaultMaterialColor().col;
                break;
            case MUSHROOM:
                color = Blocks.MYCELIUM.defaultMaterialColor().col;
                break;
            case PLAINS:
                color = Blocks.GRASS_BLOCK.defaultMaterialColor().col;
                break;
            case OCEAN:
            case RIVER:
                color = Blocks.WATER.defaultMaterialColor().col;
            case SWAMP:
                color = Blocks.LILY_PAD.defaultMaterialColor().col;
                break;
            case ICY:
                color = Blocks.PACKED_ICE.defaultMaterialColor().col;
                break;
            case THE_END:
                color = Blocks.END_STONE.defaultMaterialColor().col;
                break;
            case NETHER:
                color = Blocks.NETHERRACK.defaultMaterialColor().col;
                break;
            case BADLANDS_PLATEAU:
            case MESA:
                color = Blocks.RED_SAND.defaultMaterialColor().col;
                break;
            case NONE:
            default:
                color = 0;
        }
        return color;
    }

    /**
     * methods that gives the ChunkBase color of biomes
     * @param biomeId id of the biome
     * @return color of the biome
     */
    public static Color getColorFromIdCB(int biomeId){
        int red=0;
        int green=0;
        int blue=0;
        switch(biomeId) {
            case 0:
                red = 0;
                green = 0;
                blue = 112;
                break;
            case 1:
                red = 141;
                green = 179;
                blue = 96;
                break;
            case 2:
                red = 250;
                green = 148;
                blue = 24;
                break;
            case 3:
                red = 96;
                green = 96;
                blue = 96;
                break;
            case 4:
                red = 5;
                green = 102;
                blue = 33;
                break;
            case 5:
                red = 11;
                green = 2;
                blue = 89;
                break;
            case 6:
                red = 7;
                green = 249;
                blue = 178;
                break;
            case 7:
                red = 0;
                green = 0;
                blue = 255;
                break;
            case 8:
                red = 255;
                green = 0;
                blue = 0;
                break;
            case 9:
                red = 128;
                green = 128;
                blue = 255;
                break;
            case 10:
                red = 112;
                green = 112;
                blue = 214;
                break;
            case 11:
                red = 160;
                green = 160;
                blue = 255;
                break;
            case 12:
                red = 255;
                green = 255;
                blue = 255;
                break;
            case 13:
                red = 160;
                green = 160;
                blue = 160;
                break;
            case 14:
                red = 255;
                green = 0;
                blue = 255;
                break;
            case 15:
                red = 160;
                green = 0;
                blue = 255;
                break;
            case 16:
                red = 250;
                green = 222;
                blue = 85;
                break;
            case 17:
                red = 210;
                green = 95;
                blue = 18;
                break;
            case 18:
                red = 34;
                green = 85;
                blue = 28;
                break;
            case 19:
                red = 22;
                green = 57;
                blue = 51;
                break;
            case 20:
                red = 114;
                green = 120;
                blue = 154;
                break;
            case 21:
                red = 83;
                green = 123;
                blue = 9;
                break;
            case 22:
                red = 44;
                green = 66;
                blue = 5;
                break;
            case 23:
                red = 98;
                green = 139;
                blue = 23;
                break;
            case 24:
                red = 0;
                green = 0;
                blue = 48;
                break;
            case 25:
                red = 162;
                green = 162;
                blue = 132;
                break;
            case 26:
                red = 250;
                green = 240;
                blue = 192;
                break;
            case 27:
                red = 48;
                green = 116;
                blue = 68;
                break;
            case 28:
                red = 31;
                green = 5;
                blue = 50;
                break;
            case 29:
                red = 64;
                green = 81;
                blue = 26;
                break;
            case 30:
                red = 49;
                green = 85;
                blue = 74;
                break;
            case 31:
                red = 36;
                green = 63;
                blue = 54;
                break;
            case 32:
                red = 89;
                green = 102;
                blue = 81;
                break;
            case 33:
                red = 69;
                green = 7;
                blue = 62;
                break;
            case 34:
                red = 80;
                green = 112;
                blue = 80;
                break;
            case 35:
                red = 189;
                green = 18;
                blue = 95;
                break;
            case 36:
                red = 167;
                green = 157;
                blue = 100;
                break;
            case 37:
                red = 217;
                green = 69;
                blue = 21;
                break;
            case 38:
                red = 17;
                green = 151;
                blue = 101;
                break;
            case 39:
                red = 202;
                green = 140;
                blue = 101;
                break;
            case 40:
                red = 128;
                green = 128;
                blue = 255;
                break;
            case 41:
                red = 128;
                green = 128;
                blue = 255;
                break;
            case 42:
                red = 128;
                green = 128;
                blue = 255;
                break;
            case 43:
                red = 128;
                green = 128;
                blue = 255;
                break;
            case 44:
                red = 0;
                green = 0;
                blue = 172;
                break;
            case 45:
                red = 0;
                green = 0;
                blue = 144;
                break;
            case 46:
                red = 32;
                green = 32;
                blue = 112;
                break;
            case 47:
                red = 0;
                green = 0;
                blue = 80;
                break;
            case 48:
                red = 0;
                green = 0;
                blue = 64;
                break;
            case 49:
                red = 32;
                green = 32;
                blue = 56;
                break;
            case 50:
                red = 64;
                green = 64;
                blue = 144;
                break;
            case 127:
                red = 0;
                green = 0;
                blue = 0;
                break;
            case 129:
                red = 181;
                green = 219;
                blue = 136;
                break;
            case 130:
                red = 255;
                green = 188;
                blue = 64;
                break;
            case 131:
                red = 136;
                green = 136;
                blue = 136;
                break;
            case 132:
                red = 45;
                green = 142;
                blue = 73;
                break;
            case 133:
                red = 51;
                green = 142;
                blue = 19;
                break;
            case 134:
                red = 47;
                green = 255;
                blue = 18;
                break;
            case 140:
                red = 180;
                green = 20;
                blue = 220;
                break;
            case 149:
                red = 123;
                green = 13;
                blue = 49;
                break;
            case 151:
                red = 138;
                green = 179;
                blue = 63;
                break;
            case 155:
                red = 88;
                green = 156;
                blue = 108;
                break;
            case 156:
                red = 71;
                green = 15;
                blue = 90;
                break;
            case 157:
                red = 104;
                green = 121;
                blue = 66;
                break;
            case 158:
                red = 89;
                green = 125;
                blue = 114;
                break;
            case 160:
                red = 129;
                green = 142;
                blue = 121;
                break;
            case 161:
                red = 109;
                green = 119;
                blue = 102;
                break;
            case 162:
                red = 120;
                green = 52;
                blue = 120;
                break;
            case 163:
                red = 229;
                green = 218;
                blue = 135;
                break;
            case 164:
                red = 207;
                green = 197;
                blue = 140;
                break;
            case 165:
                red = 255;
                green = 109;
                blue = 61;
                break;
            case 166:
                red = 216;
                green = 191;
                blue = 141;
                break;
            case 167:
                red = 242;
                green = 180;
                blue = 141;
                break;
            case 168:
                red = 118;
                green = 142;
                blue = 20;
                break;
            case 169:
                red = 59;
                green = 71;
                blue = 10;
                break;
            case 170:
                red = 82;
                green = 41;
                blue = 33;
                break;
            case 171:
                red = 221;
                green = 8;
                blue = 8;
                break;
            case 172:
                red = 73;
                green = 144;
                blue = 123;
                break;
            default:
                red = 255;
                green = 0;
                blue = 0;
        }
        return new Color(red, green, blue);
    }
}
