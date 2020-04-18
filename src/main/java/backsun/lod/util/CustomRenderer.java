package backsun.lod.util;

import org.lwjgl.opengl.GL11;

public class CustomRenderer
{
	public static void drawTest(double x, double y, double z)
	{
		// renders a red x at the coordinates 9 9 9
        double doubleX = x;//mc.player.posX - 0.5;
        double doubleY = y;//mc.player.posY + 0.1;
        double doubleZ = z;//mc.player.posZ - 0.5;

        GL11.glPushMatrix();
        GL11.glTranslated(-doubleX, -doubleY, -doubleZ);
        GL11.glColor3ub((byte)255,(byte)0,(byte)0);
        float mx = 9;
        float my = 9;
        float mz = 9;
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3f(mx+0.4f,my,mz+0.4f);
        GL11.glVertex3f(mx-0.4f,my,mz-0.4f);
        GL11.glVertex3f(mx+0.4f,my,mz-0.4f);
        GL11.glVertex3f(mx-0.4f,my,mz+0.4f);
        GL11.glEnd();
        GL11.glPopMatrix();
	}
}
