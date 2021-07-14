package com.seibel.lod.objects;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.util.BiomeColorsUtils;
import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.mcutils.version.MCVersion;

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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

@SuppressWarnings("serial")
public class QuadTreeImage extends JPanel {
    private static final int PREF_W = 1536;
    private static final int PREF_H = PREF_W;
    private List<MyDrawable> drawables = new ArrayList<>();

    public QuadTreeImage() {
        setBackground(Color.white);
    }

    public void addMyDrawable(MyDrawable myDrawable) {
        drawables.add(myDrawable);
        repaint();
    }

    @Override
    // make it bigger
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return new Dimension(PREF_W, PREF_H);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        for (MyDrawable myDrawable : drawables) {
            myDrawable.draw(g2);
        }
    }

    public void clearAll() {
        drawables.clear();
        repaint();
    }

    private static void createAndShowGui() {

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
        int sizeOfTheWorld = 64;

        LodQuadTreeDimension dim = new LodQuadTreeDimension(null, null, sizeOfTheWorld);

        //SIMULATING A PLAYER MOVING,
        int[] playerXs = {0, 100, 200, 300, 400, 500};
        int[] playerZs = {0, 100, 200, 300, 400, 500};
        for (int pos = 0; pos < 1; pos++) {
            int playerX = playerXs[pos];
            int playerZ = playerZs[pos];

            //int sizeOfTheWorld=512; //TRY THIS TO SEE A 250'000 BLOCK RENDER DISTANCE
            dim.move(Math.floorDiv(playerX, 512), Math.floorDiv(playerZ, 512));
/*
            System.out.println(dim.getRegion(0, 0));
            System.out.println(dim.getCenterX());
            System.out.println(dim.getCenterZ());
            System.out.println(dim.getWidth());

            System.out.println("GETTING LOD FROM COORDINATE BEFORE GENERETION");
            System.out.println(dim.getLodFromCoordinates(-6, -6));
*/

            int[] distances = {100000, 8000, 4000, 2000, 1000, 500, 250, 100, 50, 25};
            for (int i = 0; i <= (9 - 2); i++) {
                List<LodQuadTree> levelToGenerate = dim.getNodeToGenerate(playerX, playerZ, (byte) (9 - i), DistanceGenerationMode.SERVER, distances[i], 0);
                //System.out.println(levelToGenerate);
                for (LodQuadTree level : levelToGenerate) {
                    Color color;
                    int startX = level.getLodNodeData().startX;
                    int startZ = level.getLodNodeData().startZ;
                    int endX = level.getLodNodeData().endX;
                    int endZ = level.getLodNodeData().endZ;
                    int centerX = level.getLodNodeData().centerX;
                    int centerZ = level.getLodNodeData().centerZ;
                    int width = level.getLodNodeData().width;
                    byte otherLevel = LodQuadTreeNode.BLOCK_LEVEL;
                    int otherWidth = LodQuadTreeNode.BLOCK_WIDTH;

                    List<Integer> posXs = new ArrayList<>();
                    List<Integer> posZs = new ArrayList<>();
                    if (level.getLodNodeData().level == 0) {
                        posXs.add(Math.floorDiv(startX, otherWidth));
                        posZs.add(Math.floorDiv(startZ, otherWidth));
                    } else {
                        posXs.add(Math.floorDiv(startX, otherWidth));
                        posXs.add(Math.floorDiv(centerX + 1, otherWidth));
                        posZs.add(Math.floorDiv(startZ, otherWidth));
                        posZs.add(Math.floorDiv(centerZ + 1, otherWidth));
                    }

                    //System.out.println(posXs);
                    //System.out.println(posZs);

                    for (Integer posXI : posXs) {
                        for (Integer posZI : posZs) {
                            int posX = posXI.intValue();
                            int posZ = posZI.intValue();
                            //System.out.println(posX + " " + posZ);
                            color = BiomeColorsUtils.getColorFromBiomeManual(biomeSource.getBiome(posX, 0, posZ));
                            //color = BiomeColorsUtils.getColorFromIdCB(biomeSource.getBiome(posZ, 0, posX).getId());
                            LodQuadTreeNode node = new LodQuadTreeNode(otherLevel, posX, posZ, new LodDataPoint(0, 0, color), DistanceGenerationMode.SERVER);
                            if (dim.addNode(node)) {
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


            }
            Set<DistanceGenerationMode> complexityMask = LodQuadTreeDimension.FULL_COMPLEXITY_MASK;

            List<LodQuadTreeNode> lodList = new ArrayList<>();
            //The min and max distances should increase quadratically

            lodList.addAll(dim.getNodeToRender(playerX, playerZ, (byte) 3, complexityMask, 250, 0));
            lodList.addAll(dim.getNodeToRender(playerX, playerZ, (byte) 4, complexityMask, 500, 250));
            lodList.addAll(dim.getNodeToRender(playerX, playerZ, (byte) 5, complexityMask, 1000, 500));
            lodList.addAll(dim.getNodeToRender(playerX, playerZ, (byte) 6, complexityMask, 2000, 1000));
            lodList.addAll(dim.getNodeToRender(playerX, playerZ, (byte) 7, complexityMask, 4000, 2000));
            lodList.addAll(dim.getNodeToRender(playerX, playerZ, (byte) 8, complexityMask, 8000, 4000));
            lodList.addAll(dim.getNodeToRender(playerX, playerZ, (byte) 9, complexityMask, 10000000, 8000));
            System.out.println(lodList.size());


            //     List<LodQuadTreeNode> lodList = dim.getNodes(complexityMask, false, false); //USE THIS TO SEE AL THE LODS
            listOfList.add(lodList);
        }
        System.out.println("GETTING LOD FROM COORDINATE AFTER GENERETION");
        System.out.println(dim.getLodFromCoordinates(0, 100, (byte) 1));
        //FROM THIS POINT ON THE CODE JUST CREATE THE IMAGE

        int timerDelay = 200;
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
                    double amp = ((double) 3) / ((double) maxSize);
                    Collection<LodQuadTreeNode> lodList = listOfList.get(drawCount);
                    for (LodQuadTreeNode data : lodList) {
                        myDrawables.add(new MyDrawable(new Rectangle2D.Double(
                                ((data.startX - xOffset) * amp),
                                ((data.startZ - zOffset) * amp),
                                data.width * amp,
                                data.width * amp),
                                data.lodDataPoint.color, new BasicStroke(1)));
                    }
                    /*
                    myDrawables.add(new MyDrawable(new Rectangle2D.Double(
                            (playerX - 10 - xOffset) * amp,
                            (playerZ - 10 - zOffset) * amp,
                            20* amp,
                            20* amp),
                            Color.yellow, new BasicStroke(1)));

                     */
                    for (int k = 0; k < myDrawables.size(); k++) {
                        quadTreeImage.addMyDrawable(myDrawables.get(k));
                    }
                    BufferedImage img = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = img.createGraphics();
                    frame.printAll(g2d);
                    g2d.dispose();
                    try {
                        ImageIO.write(img, "png", new File("ImgEnd" + drawCount + ".png"));
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    drawCount++;
                }
            }
        }).start();
    }

    public static void main(String[] args) {

        /*
        LodQuadTreeDimension dim2 = new LodQuadTreeDimension(null, null, 8);
        List<LodQuadTree> levelToGenerate = dim2.getNodeToGenerate(0, 0, (byte) 0, (int) 10000, 0);
        System.out.println(levelToGenerate);
        dim2.addNode(new LodNodeData((byte) 0,0,0,-1,-1, new Color(100,100,100),true));
        dim2.addNode(new LodNodeData((byte) 0,256,0,-1,-1, new Color(100,100,100),true));
        dim2.addNode(new LodNodeData((byte) 0,0,256,-1,-1, new Color(100,100,100),true));
        dim2.addNode(new LodNodeData((byte) 0,256,256,-1,-1, new Color(100,100,100),true));
        levelToGenerate = dim2.getNodeToGenerate(0, 0,  (byte) 0, (int) 10000, 0);
        System.out.println(levelToGenerate);

         */

        SwingUtilities.invokeLater(new Runnable() {
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
