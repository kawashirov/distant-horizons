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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
@SuppressWarnings("serial")
public class QuadTreeImage extends JPanel {
    private static final int PREF_W = 600;
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

    private static void createAndShowGui( ) {
        LodQuadTree lodQuadTree = new LodQuadTree(0,0);
        OverworldBiomeSource biomeSource = new OverworldBiomeSource(MCVersion.v1_16_5, 0);
        for(int i = 0; i<9; i++){
            for(int j = 0; j<8; j++) {
                List<AbstractMap.SimpleEntry<LodQuadTree, Integer>> levelToGenerate = lodQuadTree.getLevelToGenerate(150, 260, (byte)  (9-i), (int) 50 * (9 - i), 0);
                boolean bw = true;
                //System.out.println(levelToGenerate);
                for (AbstractMap.SimpleEntry<LodQuadTree, Integer> levelDist : levelToGenerate) {
                    LodQuadTree level = levelDist.getKey();
                    Color color;
                    int startX = level.getLodNodeData().startX;
                    int startZ = level.getLodNodeData().startZ;
                    int endX = level.getLodNodeData().endX;
                    int endZ = level.getLodNodeData().endZ;
                    int width = level.getLodNodeData().width;
                    byte otherLevel = LodNodeData.BLOCK_LEVEL;
                    int otherWidth = LodNodeData.BLOCK_WIDTH;
                    int posX = 2 * startX / otherWidth;
                    int posZ = 2 * startZ / otherWidth;
                    color = BiomeColorsUtils.getColorFromIdCB(biomeSource.getBiome(posZ,0,posX).getId());
                    lodQuadTree.setNodeAtLowerLevel(new LodNodeData(otherLevel, posX, posZ, 0, 0, color, true), true);

                    posX = 2 * endX / otherWidth;
                    posZ = 2 * startZ / otherWidth;
                    color = BiomeColorsUtils.getColorFromIdCB(biomeSource.getBiome(posZ,0,posX).getId());
                    lodQuadTree.setNodeAtLowerLevel(new LodNodeData(otherLevel, posX, posZ, 0, 0, color, true), true);

                    posX = 2 * startX / otherWidth;
                    posZ = 2 * endZ / otherWidth;
                    color = BiomeColorsUtils.getColorFromIdCB(biomeSource.getBiome(posZ,0,posX).getId());
                    lodQuadTree.setNodeAtLowerLevel(new LodNodeData(otherLevel, posX, posZ, 0, 0, color, true), true);

                    posX = 2 * endX / otherWidth;
                    posZ = 2 * endZ / otherWidth;
                    color = BiomeColorsUtils.getColorFromIdCB(biomeSource.getBiome(posZ,0,posX).getId());
                    lodQuadTree.setNodeAtLowerLevel(new LodNodeData(otherLevel, posX, posZ, 0, 0, color, true), true);
                }
            }
        }
        System.out.println(lodQuadTree.getNodeList(false,false,false));

        Collection<LodNodeData> lodList = lodQuadTree.getNodeList(false,false,false);

        final List<MyDrawable> myDrawables = new ArrayList<>();
        for(LodNodeData data : lodList) {
            myDrawables.add(new MyDrawable(new Rectangle2D.Double(data.startX+100, data.startZ+100, data.width, data.width),
                    data.color, new BasicStroke(1)));
        }

        final QuadTreeImage quadTreeImage = new QuadTreeImage();

        JFrame frame = new JFrame("DrawChit");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(quadTreeImage);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);

        for(int i=0; i<myDrawables.size(); i++){
            quadTreeImage.addMyDrawable(myDrawables.get(i));
        }
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
