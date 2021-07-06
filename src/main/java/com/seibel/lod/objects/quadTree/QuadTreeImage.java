package com.seibel.lod.objects.quadTree;

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
        for(int i = 0; i<9; i++){
            List<AbstractMap.SimpleEntry<LodQuadTree, Integer>> levelToGenerate= lodQuadTree.getLevelToGenerate(0,0,(byte) (9-i),1000,0);
            boolean bw= true;
            System.out.println(levelToGenerate);
            for(AbstractMap.SimpleEntry<LodQuadTree, Integer> levelDist : levelToGenerate){
                LodQuadTree level = levelDist.getKey();
                Color color ;
                if(bw){
                    color = Color.red;
                    bw = false;
                }else{
                    color = Color.blue;
                    bw = true;
                }

                int posZ = level.getLodNodeData().startX/LodNodeData.BLOCK_WIDTH;
                int posX = level.getLodNodeData().startZ/LodNodeData.BLOCK_WIDTH;
                System.out.println(posX + " " + posZ);
                lodQuadTree.setNodeAtLowerLevel(new LodNodeData(LodNodeData.BLOCK_LEVEL, posX, posZ, 0, 0, color,true),true);

                posZ = level.getLodNodeData().endX/LodNodeData.BLOCK_WIDTH;
                posX = level.getLodNodeData().startZ/LodNodeData.BLOCK_WIDTH;
                System.out.println(posX + " " + posZ);
                lodQuadTree.setNodeAtLowerLevel(new LodNodeData(LodNodeData.BLOCK_LEVEL, posX, posZ, 0, 0, color,true),true);

                posZ = level.getLodNodeData().startX/LodNodeData.BLOCK_WIDTH;
                posX = level.getLodNodeData().endX/LodNodeData.BLOCK_WIDTH;
                System.out.println(posX + " " + posZ);
                lodQuadTree.setNodeAtLowerLevel(new LodNodeData(LodNodeData.BLOCK_LEVEL, posX, posZ, 0, 0, color,true),true);

                posZ = level.getLodNodeData().endX/LodNodeData.BLOCK_WIDTH;
                posX = level.getLodNodeData().endZ/LodNodeData.BLOCK_WIDTH;
                System.out.println(posX + " " + posZ);
                lodQuadTree.setNodeAtLowerLevel(new LodNodeData(LodNodeData.BLOCK_LEVEL, posX, posZ, 0, 0, color,true),true);
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

        int timerDelay = 1;
        new Timer(timerDelay, new ActionListener() {
            private int drawCount = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (drawCount >= myDrawables.size()) {
                    drawCount = 0;
                    quadTreeImage.clearAll();
                } else {
                    quadTreeImage.addMyDrawable(myDrawables.get(drawCount));
                    drawCount++;
                }
            }
        }).start();
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
        g2.setStroke(stroke);
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
