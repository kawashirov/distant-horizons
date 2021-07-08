package com.seibel.lod.objects.quadTree;

import com.seibel.lod.util.BiomeColorsUtils;
import kaptainwutax.biomeutils.biome.Biome;
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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

@SuppressWarnings("serial")
public class QuadTreeImage extends JPanel {
    private static final int PREF_W = 1024;
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
        LodQuadTree lodQuadTree = new LodQuadTree(0, 0);
        final QuadTreeImage quadTreeImage = new QuadTreeImage();

        JFrame frame = new JFrame("DrawChit");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(quadTreeImage);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        System.out.println(lodQuadTree.getLodNodeData().endX);
        int playerX = 150;
        int playerZ = 260;
        List<Collection<LodNodeData>> listOfList = new ArrayList<>();
        OverworldBiomeSource biomeSource = new OverworldBiomeSource(MCVersion.v1_16_5, 0);
        for (int i = 0; i <= (9 - 2); i++) {
            for (int j = 0; j < 1; j++) {
                int dist;
                if (i == 9) {
                    dist = 500;
                } else {
                    dist = 500;
                }
                List<AbstractMap.SimpleEntry<LodQuadTree, Integer>> levelToGenerate = lodQuadTree.getLevelToGenerate(playerX, playerZ, (byte) (9 - i), (int) dist * (9 - i + 1), 0);
                System.out.println(levelToGenerate);
                for (AbstractMap.SimpleEntry<LodQuadTree, Integer> levelDist : levelToGenerate) {
                    LodQuadTree level = levelDist.getKey();
                    Color color;
                    int startX = level.getLodNodeData().startX;
                    int startZ = level.getLodNodeData().startZ;
                    int endX = level.getLodNodeData().endX;
                    int endZ = level.getLodNodeData().endZ;
                    int centerX = level.getLodNodeData().centerX;
                    int centerZ = level.getLodNodeData().centerZ;
                    int width = level.getLodNodeData().width;
                    byte otherLevel = LodNodeData.BLOCK_LEVEL;
                    int otherWidth = LodNodeData.BLOCK_WIDTH;

                    List<Integer> posXs = new ArrayList<>();
                    List<Integer> posZs = new ArrayList<>();
                    if(level.getLodNodeData().level == 0){
                        posXs.add(startX / otherWidth);
                        posZs.add(startZ / otherWidth);
                    }else if(level.getLodNodeData().level == 2){
                        posXs.add(startX / otherWidth);
                        posXs.add(endX / otherWidth);
                        posZs.add(startZ / otherWidth);
                        posZs.add(endZ / otherWidth);
                    }else{
                        posXs.add(startX / otherWidth);
                        posXs.add((centerX / otherWidth)-1);
                        posXs.add(centerX / otherWidth);
                        posXs.add(endX / otherWidth);
                        posZs.add(startZ / otherWidth);
                        posZs.add((centerZ / otherWidth)-1);
                        posZs.add(centerZ / otherWidth);
                        posZs.add(endZ / otherWidth);
                    }

                    for(Integer posXI : posXs){
                        for(Integer posZI : posZs){
                            int posX = posXI.intValue();
                            int posZ = posZI.intValue();
                            //color = BiomeColorsUtils.getColorFromBiomeManual(biomeSource.getBiome(posZ, 0, posX));
                            color = BiomeColorsUtils.getColorFromIdCB(biomeSource.getBiome(posZ, 0, posX).getId());
                            lodQuadTree.setNodeAtLowerLevel(new LodNodeData(otherLevel, posX, posZ, 0, 0, color, true), true);
                        }
                    }
                }
            }

            Collection<LodNodeData> lodList = lodQuadTree.getNodeToRender(playerX, playerZ, (byte) 0, 10000, 0);

            //lodList = lodQuadTree.getNodeList(false,false,false);
            listOfList.add(lodList);

            final List<MyDrawable> myDrawables = new ArrayList<>();
            int amp = 2;
            for (LodNodeData data : lodList) {
                myDrawables.add(new MyDrawable(new Rectangle2D.Double(data.startX * amp, data.startZ * amp, data.width * amp, data.width * amp),
                        data.color, new BasicStroke(1)));
            }
            myDrawables.add(new MyDrawable(new Rectangle2D.Double(playerZ * amp - 10, playerX * amp - 10, 20, 20),
                    Color.yellow, new BasicStroke(1)));
            for (int k = 0; k < myDrawables.size(); k++) {
                quadTreeImage.addMyDrawable(myDrawables.get(k));
            }
        }

        /*
        int timerDelay = 100;
        new Timer(timerDelay, new ActionListener() {
            private int drawCount = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (drawCount >= listOfList.size()) {
                    drawCount = 0;
                    quadTreeImage.clearAll();
                } else {
                    final List<MyDrawable> myDrawables = new ArrayList<>();
                    int amp = 2;
                    Collection<LodNodeData> lodList = listOfList.get(drawCount);
                    for (LodNodeData data : lodList) {
                        myDrawables.add(new MyDrawable(new Rectangle2D.Double(data.startX * amp, data.startZ * amp, data.width * amp, data.width * amp),
                                data.color, new BasicStroke(1)));
                    }
                    myDrawables.add(new MyDrawable(new Rectangle2D.Double(playerZ * amp - 10, playerX * amp - 10, 20, 20),
                            Color.yellow, new BasicStroke(1)));
                    for (int k = 0; k < myDrawables.size(); k++) {
                        quadTreeImage.addMyDrawable(myDrawables.get(k));
                    }
                    drawCount++;
                }
            }
        }).start();
         */
    }

    public static void main(String[] args) {
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
