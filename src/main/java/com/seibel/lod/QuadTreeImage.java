/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
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
package com.seibel.lod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.objects.LodDataPoint;
import com.seibel.lod.objects.LodQuadTreeDimension;
import com.seibel.lod.objects.LodQuadTreeNode;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.util.BiomeColorsUtils;

import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.mcutils.version.MCVersion;

/**
 * 
 * @author Leonardo Amato
 *
 */
@SuppressWarnings("serial")
public class QuadTreeImage extends JPanel
{
    private static final int PREF_W = 1024;
    private static final int PREF_H = PREF_W;
    private List<MyDrawable> drawables = new ArrayList<>();

    public QuadTreeImage()
    {
        setBackground(Color.white);
    }

    public void addMyDrawable(MyDrawable myDrawable)
    {
        drawables.add(myDrawable);
        repaint();
    }

    @Override
    // make it bigger
    public Dimension getPreferredSize()
    {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return new Dimension(PREF_W, PREF_H);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        for (MyDrawable myDrawable : drawables) {
            myDrawable.draw(g2);
        }
    }

    public void clearAll()
    {
        drawables.clear();
        repaint();
    }

    private static void createAndShowGui()
    {

        final QuadTreeImage quadTreeImage = new QuadTreeImage();


        JFrame frame = new JFrame("DrawChit");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(quadTreeImage);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        List<List<LodQuadTreeNode>> listOfList = new ArrayList<>();
        OverworldBiomeSource biomeSource = new OverworldBiomeSource(MCVersion.v1_16_5, 1000);
        //EndBiomeSource biomeSource = new EndBiomeSource(MCVersion.v1_16_5, 1000);
        int sizeOfTheWorld = 32;

        LodQuadTreeDimension dim = new LodQuadTreeDimension(null, null, sizeOfTheWorld);

        //SIMULATING A PLAYER MOVING,
        int[] playerXs = {0, 100, 200, 300, 400, 1000};
        int[] playerZs = {0, 100, 200, 300, 400, 500};
        for (int pos = 0; pos < 1; pos++)
        {
            int playerX = 0 + playerXs[pos]; //2097152
            int playerZ = 0 + playerZs[pos]/2;

            //int sizeOfTheWorld=512; //TRY THIS TO SEE A 250'000 BLOCK RENDER DISTANCE
            dim.move(new RegionPos(Math.floorDiv(playerX, 512), Math.floorDiv(playerZ, 512)));
/*
            System.out.println(dim.getRegion(0, 0));
            System.out.println(dim.getCenterX());
            System.out.println(dim.getCenterZ());
            System.out.println(dim.getWidth());

            System.out.println("GETTING LOD FROM COORDINATE BEFORE GENERETION");
            System.out.println(dim.getLodFromCoordinates(-6, -6));
*/

            DistanceGenerationMode[] complexities = {DistanceGenerationMode.BIOME_ONLY, DistanceGenerationMode.BIOME_ONLY, DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT, DistanceGenerationMode.SURFACE, DistanceGenerationMode.SURFACE, DistanceGenerationMode.FEATURES, DistanceGenerationMode.FEATURES, DistanceGenerationMode.FEATURES, DistanceGenerationMode.FEATURES, DistanceGenerationMode.FEATURES};
            int[] distances = {1000000, 8000, 4000, 2000, 1000, 500, 250, 100, 50, 25};
            for (int i = 0; i <= (9-2); i++) {
                List<LodQuadTreeNode> levelToGenerate = dim.getNodesToGenerate(playerX, playerZ, (byte) (9 - i),  complexities[i], distances[i]*2, 0);
                //System.out.println(levelToGenerate);
                for (LodQuadTreeNode node : levelToGenerate) {
                    Color color;
                    int startX = node.startX;
                    int startZ = node.startZ;
                    int endX = node.endX;
                    int endZ = node.endZ;
                    int centerX = node.centerX;
                    int centerZ = node.centerZ;
                    int width = node.width;
                    byte otherLevel = LodQuadTreeNode.BLOCK_LEVEL;
                    int otherWidth = LodQuadTreeNode.BLOCK_WIDTH;

                    List<Integer> posXs = new ArrayList<>();
                    List<Integer> posZs = new ArrayList<>();
                    posXs.add(Math.floorDiv(startX, otherWidth));
                    posXs.add(Math.floorDiv(centerX, otherWidth));
                    posZs.add(Math.floorDiv(startZ, otherWidth));
                    posZs.add(Math.floorDiv(centerZ, otherWidth));

                    for (Integer posXI : posXs) {
                        for (Integer posZI : posZs) {
                            int posX = posXI.intValue();
                            int posZ = posZI.intValue();
                            color = BiomeColorsUtils.getColorFromBiomeManual(biomeSource.getBiome(posX, 0, posZ));
                            LodQuadTreeNode newNode = new LodQuadTreeNode(otherLevel, posX, posZ, new LodDataPoint(0, 0, color), complexities[i]);
                            if (dim.addNode(newNode)) {
                            }
                        }
                    }
                }

                //Set<DistanceGenerationMode> complexityMask = new HashSet<>();
                //complexityMask.add(DistanceGenerationMode.SERVER);
                //complexityMask.add(DistanceGenerationMode.FEATURES);
                //complexityMask.add(DistanceGenerationMode.SURFACE);
                //complexityMask.add(DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT);
                //complexityMask.add(DistanceGenerationMode.BIOME_ONLY);
                Set<DistanceGenerationMode> complexityMask = LodQuadTreeDimension.FULL_COMPLEXITY_MASK;

                List<LodQuadTreeNode> lodList = new ArrayList<>();
                //The min and max distances should increase quadratically
                //int[] distances2 = {100000, 8000, 4000, 2000, 1000, 500, 250, 0};
                int[] distances2 = {0, 250, 500, 1000, 2000, 4000, 8000, 100000};
                for (int h = 0; h <= (9 - 3); h++) {
                    lodList.addAll(dim.getNodeToRender(playerX, playerZ, (byte) (3+h), complexityMask, distances2[h+1], distances2[h]));
                }
                System.out.println("Number of node to render " + lodList.size());
                listOfList.add(lodList);
                System.out.println("Number of list " + listOfList.size());
                /*
                List<LodQuadTreeNode> lodList = dim.getNodes(complexityMask, false, false); //USE THIS TO SEE AL THE LODS
                listOfList.add(lodList);
                */

            }

        }
        System.out.println("GETTING LOD FROM COORDINATE AFTER GENERETION");
        System.out.println(dim.getLodFromCoordinates(0, 100, (byte) 1));
        //FROM THIS POINT ON THE CODE JUST CREATE THE IMAGE

        int timerDelay = 1000;
        System.out.println("STARTING");
        System.out.println(dim.getWidth());
        System.out.println(dim.getCenterX());
        int xOffset = listOfList.stream().mapToInt(x -> x.stream().mapToInt(y -> y.startX).min().getAsInt()).min().getAsInt();
        int zOffset = listOfList.stream().mapToInt(x -> x.stream().mapToInt(y -> y.startZ).min().getAsInt()).min().getAsInt();
        int maxX = listOfList.stream().mapToInt(x -> x.stream().mapToInt(y -> y.startX).max().getAsInt()).min().getAsInt();
        int maxZ = listOfList.stream().mapToInt(x -> x.stream().mapToInt(y -> y.startZ).max().getAsInt()).min().getAsInt();
        int maxSize = Math.max(maxX - xOffset, maxZ - zOffset) / 512;
        System.out.println(xOffset);
        System.out.println(zOffset);
        new Timer(timerDelay, new ActionListener() {
            private int drawCount = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (drawCount >= listOfList.size()) {
                    drawCount = 0;
                } else {
                    if (drawCount == 0) quadTreeImage.clearAll();
                    final List<MyDrawable> myDrawables = new ArrayList<>();
                    double amp = ((double) 2) / ((double) sizeOfTheWorld);
                    Collection<LodQuadTreeNode> lodList = listOfList.get(drawCount);
                    for (LodQuadTreeNode data : lodList) {
                        Color colorOfComplexity = Color.black;
                        switch (data.complexity){
                            case NONE:
                                colorOfComplexity = Color.black;
                                break;
                            case BIOME_ONLY:
                                colorOfComplexity = Color.red;
                                break;
                            case BIOME_ONLY_SIMULATE_HEIGHT:
                                colorOfComplexity = Color.yellow;
                                break;
                            case SURFACE:
                                colorOfComplexity = Color.blue;
                                break;
                            case FEATURES:
                                colorOfComplexity = Color.cyan;
                                break;
                            case SERVER:
                                colorOfComplexity = Color.green;
                                break;
                        }
                        myDrawables.add(new MyDrawable(new Rectangle2D.Double(
                                ((data.startX - xOffset) * amp),
                                ((data.startZ - zOffset) * amp),
                                data.width * amp,
                                data.width * amp),
                                data.lodDataPoint.color, new BasicStroke(1)));
                    }
                        myDrawables.add(new MyDrawable(new Rectangle2D.Double(
                                (playerXs[0] - xOffset) * amp,
                                (playerZs[0] - zOffset) * amp,
                                20,
                                20),
                                Color.yellow, new BasicStroke(1)));
                    for (int k = 0; k < myDrawables.size(); k++) {
                        quadTreeImage.addMyDrawable(myDrawables.get(k));
                    }
                    /*
                    BufferedImage img = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = img.createGraphics();
                    frame.printAll(g2d);
                    g2d.dispose();
                    try {
                        ImageIO.write(img, "png", new File("ImgEnd" + drawCount + ".png"));
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                     */
                    drawCount++;
                }
            }
        }).start();
    }

    public static void main(String[] args)
    {
        /*
        LodQuadTreeDimension dim2 = new LodQuadTreeDimension(null, null, 1);
        dim2.move(10000000,10000000);
        List<LodQuadTreeNode> levelToGenerate = dim2.getNodesToGenerate(10000000, 10000000,  (byte) 7,DistanceGenerationMode.SERVER, (int) 10000, 0);
        System.out.println(levelToGenerate);
        dim2.addNode(new LodQuadTreeNode((byte) 0,0,0,new LodDataPoint(-1,-1, new Color(100,100,100)),DistanceGenerationMode.SERVER));
        dim2.addNode(new LodQuadTreeNode((byte) 0,256,0,new LodDataPoint(-1,-1, new Color(100,100,100)),DistanceGenerationMode.SERVER));
        dim2.addNode(new LodQuadTreeNode((byte) 0,0,256,new LodDataPoint(-1,-1, new Color(100,100,100)),DistanceGenerationMode.SERVER));
        dim2.addNode(new LodQuadTreeNode((byte) 0,256,256,new LodDataPoint(-1,-1, new Color(100,100,100)),DistanceGenerationMode.SERVER));
        levelToGenerate = dim2.getNodesToGenerate(10000000, 10000000,  (byte) 7,DistanceGenerationMode.SERVER, (int) 10000, 0);
        System.out.println(levelToGenerate);

         */
        /*
        System.out.println(DistanceGenerationMode.SERVER.compareTo(DistanceGenerationMode.SERVER));
        System.out.println(DistanceGenerationMode.NONE.compareTo(DistanceGenerationMode.SERVER));
        System.out.println(DistanceGenerationMode.SERVER.compareTo(DistanceGenerationMode.NONE));
        System.out.println(DistanceGenerationMode.BIOME_ONLY.compareTo(DistanceGenerationMode.SURFACE));
        System.out.println(DistanceGenerationMode.SURFACE.compareTo(DistanceGenerationMode.BIOME_ONLY));
        System.out.println(DistanceGenerationMode.BIOME_ONLY.compareTo(DistanceGenerationMode.BIOME_ONLY));
        System.out.println(DistanceGenerationMode.BIOME_ONLY.compareTo(DistanceGenerationMode.NONE));
        System.out.println(DistanceGenerationMode.NONE.compareTo(DistanceGenerationMode.BIOME_ONLY));

         */
        SwingUtilities.invokeLater(new Runnable() {
            @Override
			public void run() {
                createAndShowGui();
            }
        });



    }
}

class MyDrawable {
    private Shape shape;
    private Color color;
    private Stroke stroke;

    public MyDrawable(Shape shape, Color color, Stroke stroke) {
        this.shape = shape;
        this.color = color;
        this.stroke = stroke;
    }

    public Shape getShape() {
        return shape;
    }

    public Color getColor() {
        return color;
    }

    public Stroke getStroke() {
        return stroke;
    }

    public void draw(Graphics2D g2) {
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();

        g2.setColor(color);
        g2.fill(shape);
        //g2.setStroke(stroke);
        g2.draw(shape);

        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    public void fill(Graphics2D g2) {
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();

        g2.setColor(color);
        g2.setStroke(stroke);
        g2.fill(shape);

        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

}
