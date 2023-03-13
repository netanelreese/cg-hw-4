//******************************************************************************
// Copyright (C) 2016-2022 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Fri Feb 26 11:51:52 2022 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160209 [weaver]:	Original file.
// 20190129 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
// 20190203 [weaver]:	Additional cleanup and more extensive comments.
// 20200121 [weaver]:	Modified to set up OpenGL and UI on the Swing thread.
// 20201215 [weaver]:	Added setIdentifyPixelScale() to canvas setup.
// 20210209 [weaver]:	Added point smoothing for Hi-DPI displays.
// 20220209 [weaver]:	Additional cleanup.
//
//******************************************************************************
// Notes:
//
// Warning! This code uses deprecated features of OpenGL, including immediate
// mode vertex attribute specification, for sake of easier classroom learning.
// See www.khronos.org/opengl/wiki/Legacy_OpenGL
//
//******************************************************************************

package edu.ou.cs.cg.assignment.solution02;

//import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;

//******************************************************************************

/**
 * The <CODE>Application</CODE> class.<P>
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class Application
	implements GLEventListener, Runnable
{
	//**********************************************************************
	// Private Class Members
	//**********************************************************************

	private static final String		DEFAULT_NAME = "Solution02";
	private static final Dimension		DEFAULT_SIZE = new Dimension(1280, 720);

	//**********************************************************************
	// Public Class Members
	//**********************************************************************

	public static final GLUT			MYGLUT = new GLUT();
	public static final Random			RANDOM = new Random();

	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private int				w;				// Canvas width
	private int				h;				// Canvas height
	private int				k = 0;			// Animation counter
	private TextRenderer		renderer;

	//**********************************************************************
	// Main
	//**********************************************************************

	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(new Application(args));
	}

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public Application(String[] args)
	{
	}

	//**********************************************************************
	// Override Methods (Runnable)
	//**********************************************************************

	public void	run()
	{
		GLProfile		profile = GLProfile.getDefault();

		System.out.println("Running on Java version " + 
			System.getProperty("java.version"));
		System.out.println("Running with OpenGL version " +
			profile.getName());

		GLCapabilities	capabilities = new GLCapabilities(profile);
		GLCanvas		canvas = new GLCanvas(capabilities);	// Single-buffer
		//GLJPanel		canvas = new GLJPanel(capabilities);	// Double-buffer
		JFrame			frame = new JFrame(DEFAULT_NAME);

		// Rectify display scaling issues when in Hi-DPI mode on macOS.
		edu.ou.cs.cg.utilities.Utilities.setIdentityPixelScale(canvas);

		// Specify the starting width and height of the canvas itself
		canvas.setPreferredSize(DEFAULT_SIZE);

		// Populate and show the frame
		frame.setBounds(50, 50, 200, 200);
		frame.getContentPane().add(canvas);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Exit when the user clicks the frame's close button
		frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					System.exit(0);
				}
			});

		// Register this class to update whenever OpenGL needs it
		canvas.addGLEventListener(this);

		// Have OpenGL call display() to update the canvas 60 times per second
		FPSAnimator	animator = new FPSAnimator(canvas, 60);

		animator.start();
	}

	//**********************************************************************
	// Override Methods (GLEventListener)
	//**********************************************************************

	// Called immediately after the GLContext of the GLCanvas is initialized.
	public void	init(GLAutoDrawable drawable)
	{
		w = drawable.getSurfaceWidth();
		h = drawable.getSurfaceHeight();

		renderer = new TextRenderer(new Font("Serif", Font.PLAIN, 18),
									true, true);

		initPipeline(drawable);
	}

	// Notification to release resources for the GLContext.
	public void	dispose(GLAutoDrawable drawable)
	{
		renderer = null;
	}

	// Called to initiate rendering of each frame into the GLCanvas.
	public void	display(GLAutoDrawable drawable)
	{
		update(drawable);
		render(drawable);
	}

	// Called during the first repaint after a resize of the GLCanvas.
	public void	reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		this.w = w;
		this.h = h;
	}

	//**********************************************************************
	// Private Methods (Rendering)
	//**********************************************************************

	// Update the scene model for the current animation frame.
	private void	update(GLAutoDrawable drawable)
	{
		k++;									// Advance animation counter
	}

	// Render the scene model and display the current animation frame.
	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);	// Clear the buffer

		// Draw the scene
		setGalaxyProjection(gl);				// Position galaxy relatively
		drawLorenzGalaxy(gl);

		setScreenProjection(gl);				// Position the rest absolutely
		drawSky(gl);
		drawGround(gl);
		drawSidewalkBase(gl);
		drawStars(gl);
		drawMoon(gl);
		drawSidewalk(gl);
		drawHopscotch(gl);
		drawHouses(gl);
		drawFence(gl);
		drawKite(gl);
	}

	//**********************************************************************
	// Private Methods (Pipeline)
	//**********************************************************************

	// www.khronos.org/registry/OpenGL-Refpages/es2.0/xhtml/glBlendFunc.xml
	private void	initPipeline(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		// Make the sky gradient easier by enabling alpha blending.
		// Translucency in 3-D is more difficult and expensive than in 2-D!

		// See com.jogamp.opengl.GL
		gl.glEnable(GL2.GL_POINT_SMOOTH);	// Turn on point anti-aliasing
		gl.glEnable(GL2.GL_LINE_SMOOTH);	// Turn on line anti-aliasing

		gl.glEnable(GL.GL_BLEND);			// Turn on color channel blending
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
	}

	// Position and orient the default camera to view in 2-D, scaled specially.
	private void	setGalaxyProjection(GL2 gl)
	{
		GLU	glu = GLU.createGLU();

		gl.glMatrixMode(GL2.GL_PROJECTION);		// Prepare for matrix xform
		gl.glLoadIdentity();						// Set to identity matrix
		glu.gluOrtho2D(-1.0f, 1.0f, -1.45f, 1.0f);	// 2D translate and scale
	}

	// Position and orient the default camera to view in 2-D, in pixel coords.
	private void	setScreenProjection(GL2 gl)
	{
		GLU	glu = GLU.createGLU();

		gl.glMatrixMode(GL2.GL_PROJECTION);		// Prepare for matrix xform
		gl.glLoadIdentity();						// Set to identity matrix
		glu.gluOrtho2D(0.0f, 1280.0f, 0.0f, 720.0f);// 2D translate and scale
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	// These pages are helpful:
	// en.wikipedia.org/wiki/Lorenz_system
	// www.algosome.com/articles/lorenz-attractor-programming-code.html
	private void	drawLorenzGalaxy(GL2 gl)
	{
		gl.glPointSize(5.0f);					// Set point size (in pixels)
		gl.glBegin(GL.GL_POINTS);				// Start specifying points

		double		dt = 0.01;					// Integration step size
		double		sigma = 10.0;				// Constant for updating x
		double		rho = 28.0;				// Constant for updating y
		double		beta = 8.0 / 3.0;			// Constant for updating z
		double		lx = 0.1;					// Initial x coordinate
		double		ly = 0.0;					// Initial y coordinate
		double		lz = 0.0;					// Initial z coordinate

		for (int i=0; i<10000; i++)
		{
			double	llx = lx + dt * sigma * (ly - lx);
			double	lly = ly + dt * (lx * (rho - lz) - ly);
			double	llz = lz + dt * (lx * ly - beta * lz);

			lx = llx;
			ly = lly;
			lz = llz;
			//System.out.println(" " + lx + " " + ly + " " + lz);

			float	cc = (float)((lz + 30.0) / 60.0);
			int	n = k % 10000 - i;
			int	cw = (n + 20) * 6 + 15;

			// Lower background star opacity to experiment with animation.
			if (Math.abs(n) <= 20)							// Animation window
				setColor(gl, 255, 255-cw, cw, 255-cw);		// Dots cycling
			else
				gl.glColor4f(cc, cc, cc, 0.05f);			// Background stars

			double	dy = 0.00005 * k * (1.5 - 0.05 * lx);	// Galaxy rise

			gl.glVertex2d(-lx / 30.0, ly / 30.0 + dy);
		}

		gl.glEnd();
		gl.glPointSize(1.0f);					// Reset point size (in pixels)
	}

	private void	drawSky(GL2 gl)
	{
		gl.glBegin(GL2.GL_QUADS);

		setColor(gl, 128, 112, 80);			// Opaque gold on horizon
		gl.glVertex2i(0, 312);
		gl.glVertex2i(1280, 312);
		setColor(gl, 32, 48, 96, 64);			// Translucent dark blue at top
		gl.glVertex2i(1280, 720);
		gl.glVertex2i(0, 720);

		gl.glEnd();
	}

	private void	drawGround(GL2 gl)
	{
		gl.glBegin(GL2.GL_QUADS);

		setColor(gl, 64, 48, 48);				// Red-purple on horizon
		gl.glVertex2i(0, 312);
		gl.glVertex2i(1280, 312);
		setColor(gl, 80, 128, 64);				// Moss green by sidewalk
		gl.glVertex2i(1280, 129);
		gl.glVertex2i(0, 129);

		gl.glEnd();
	}

	private void	drawSidewalkBase(GL2 gl)
	{
		gl.glBegin(GL2.GL_QUADS);

		setColor(gl, 255, 255, 255);			// White
		gl.glVertex2i(0, 0);
		gl.glVertex2i(1280, 0);
		gl.glVertex2i(1280, 129);
		gl.glVertex2i(0, 129);

		gl.glEnd();
	}

	private void	drawSidewalk(GL2 gl)
	{
		for (int i=-1; i<16; i++)
			drawSidewalkSlab(gl, i * 79);
	}

	private void	drawSidewalkSlab(GL2 gl, int dx)
	{
		gl.glBegin(GL2.GL_QUADS);

		setColor(gl, 128, 128, 128);			// Medium gray
		gl.glVertex2i(dx +  34,   2);
		gl.glVertex2i(dx +  57, 127);
		gl.glVertex2i(dx + 134, 127);
		gl.glVertex2i(dx + 111,   2);

		gl.glEnd();
	}

	private void	drawHopscotch(GL2 gl)
	{
		drawHopscotchSquare(gl, 673, 720-622-25);
		drawHopscotchSquare(gl, 704, 720-622-25);
		drawHopscotchSquare(gl, 736, 720-622-25);

		drawHopscotchSquare(gl, 764, 720-634-25);
		drawHopscotchSquare(gl, 770, 720-608-25);

		drawHopscotchSquare(gl, 798, 720-620-25);

		drawHopscotchSquare(gl, 826, 720-631-25);
		drawHopscotchSquare(gl, 832, 720-606-25);

		drawHopscotchSquare(gl, 861, 720-620-25);
	}

	private void	drawHopscotchSquare(GL2 gl, int dx, int dy)
	{
		setColor(gl, 255, 255, 192, 128);		// Taupe + alpha
		gl.glBegin(GL2.GL_POLYGON);
		doHopscotchLoop(gl, dx, dy);
		gl.glEnd();

		// This approach cuts off the corners
		// Could do this better by drawing four trapezoids using GL_QUADS
		setColor(gl, 229, 229, 229);			// Light gray
		gl.glLineWidth(3);
		gl.glBegin(GL2.GL_LINE_LOOP);
		doHopscotchLoop(gl, dx, dy);
		gl.glEnd();
		gl.glLineWidth(1);
	}

	private void	doHopscotchLoop(GL2 gl, int dx, int dy)
	{
		gl.glVertex2i(dx +  0, dy +  0);
		gl.glVertex2i(dx +  5, dy + 25);
		gl.glVertex2i(dx + 35, dy + 25);
		gl.glVertex2i(dx + 30, dy +  0);
	}

	private void	drawFence(GL2 gl)
	{
		drawFenceSlat(gl, false,    6, 132);
		drawFenceSlat(gl, false,   30, 132);
		drawFenceSlat(gl, false,   54, 132);
		drawFenceSlat(gl, false,   78, 132);

		drawFenceSlat(gl, false,  290, 132);
		drawFenceSlat(gl, false,  314, 132);
		drawFenceSlat(gl, false,  338, 132);
		drawFenceSlat(gl, false,  362, 132);

		drawFenceSlat(gl, false,  391, 132);
		drawFenceSlat(gl, false,  415, 132);
		drawFenceSlat(gl, false,  439, 132);
		drawFenceSlat(gl, false,  463, 132);

		drawFenceSlat(gl, false,  856, 132);
		drawFenceSlat(gl, true,   880, 132);
		drawFenceSlat(gl, false,  904, 132);
		drawFenceSlat(gl, true,   928, 132);
		drawFenceSlat(gl, false,  952, 132);
		drawFenceSlat(gl, true,   976, 132);
		drawFenceSlat(gl, false, 1000, 132);
		drawFenceSlat(gl, true,  1024, 132);

		drawFenceSlat(gl, false, 1224, 132);
		drawFenceSlat(gl, true,  1248, 132);
	}

	// Draws a single fence slat with bottom left corner at dx, dy.
	// If flip is true, the slat is higher on the left, else on the right.
	private void	drawFenceSlat(GL2 gl, boolean flip, int dx, int dy)
	{
		gl.glBegin(GL2.GL_POLYGON);			// Fill the slat, in...

		setColor(gl, 192, 192, 128);			// ...tan
		gl.glVertex2i(dx +  0, dy +   0);
		gl.glVertex2i(dx +  0, dy + (flip ? 112 : 102));
		gl.glVertex2i(dx + 24, dy + (flip ? 102 : 112));
		gl.glVertex2i(dx + 24, dy +   0);

		gl.glEnd();

		gl.glBegin(GL2.GL_LINE_LOOP);			// Edge the slat, in...

		setColor(gl, 0, 0, 0);					// ...black
		gl.glVertex2i(dx +  0, dy +   0);
		gl.glVertex2i(dx +  0, dy + (flip ? 112 : 102));
		gl.glVertex2i(dx + 24, dy + (flip ? 102 : 112));
		gl.glVertex2i(dx + 24, dy +   0);

		gl.glEnd();
	}

	private void	drawStars(GL2 gl)
	{
		drawStar(gl,  921, 720 -  29, 1.00f);
		drawStar(gl, 1052, 720 -  61, 0.90f);
		drawStar(gl, 1177, 720 -  49, 0.95f);
		drawStar(gl, 1205, 720 - 153, 0.50f);
		drawStar(gl, 1146, 720 - 254, 0.30f);
	}

	private void	drawStar(GL2 gl, int cx, int cy, float alpha)
	{
		double		theta = 0.5 * Math.PI;

		setColor(gl, 255, 255, 0, (int)(alpha * 255));	// Yellow + alpha
		gl.glBegin(GL.GL_TRIANGLE_FAN);
		gl.glVertex2d(cx, cy);
		doStarVertices(gl, cx, cy, 8, 20.0, 8.0);
		gl.glVertex2d(cx + 15 * Math.cos(theta), cy + 15 * Math.sin(theta));
		gl.glEnd();
	}

	private static final int		SIDES_MOON = 18;
	private static final double	ANGLE_MOON = 2.0 * Math.PI / SIDES_MOON;

	private void	drawMoon(GL2 gl)
	{
		double		theta = 0.20 * ANGLE_MOON;
		int		cx = 94;
		int		cy = 720 - 92;
		int		r = 59;

		// Fill the whole moon in white
		gl.glBegin(GL.GL_TRIANGLE_FAN);

		setColor(gl, 255, 255, 255);			// White
		gl.glVertex2d(cx, cy);

		for (int i=0; i<SIDES_MOON+1; i++)		// 18 sides
		{
			gl.glVertex2d(cx + r * Math.cos(theta), cy + r * Math.sin(theta));
			theta += ANGLE_MOON;
		}

		gl.glEnd();

		// Fill the outside shadow in dark bluish gray
		theta = -1.80 * ANGLE_MOON;

		gl.glBegin(GL.GL_TRIANGLE_FAN);

		setColor(gl, 64, 64, 80);
		gl.glVertex2d(cx, cy);

		for (int i=0; i<8; i++)				// 7 sides
		{
			gl.glVertex2d(cx + r * Math.cos(theta), cy + r * Math.sin(theta));
			theta += ANGLE_MOON;
		}

		gl.glEnd();

		// Fill the inside shadow in dark bluish gray
		theta = 1.50 * ANGLE_MOON;
		cx = 128;
		cy = 650;
		theta = 7.2 * ANGLE_MOON;

		gl.glBegin(GL.GL_TRIANGLE_FAN);

		setColor(gl, 64, 64, 80);
		gl.glVertex2d(cx, cy);

		for (int i=0; i<8; i++)				// 7 sides
		{
			gl.glVertex2d(cx + r * Math.cos(theta), cy + r * Math.sin(theta));
			theta += ANGLE_MOON;
		}

		gl.glEnd();
	}

	private void	drawKite(GL2 gl)
	{
		drawKiteLine(gl);
		drawKiteFans(gl);
	}

	private ArrayList<Point>	kiteline = null;

	// Keep simpler than the drawing, since vertices will be interactive in HW03
	private void	drawKiteLine(GL2 gl)
	{
		if (kiteline == null)
		{
			kiteline = new ArrayList<Point>();
			kiteline.add(new Point(1024, 244));
			kiteline.add(new Point( 964, 272));
			kiteline.add(new Point( 924, 364));
			kiteline.add(new Point( 928, 396));
			kiteline.add(new Point( 900, 428));
			kiteline.add(new Point( 912, 464));
			kiteline.add(new Point( 936, 472));
			kiteline.add(new Point( 956, 490));
		}

		setColor(gl, 128, 128, 96);
		gl.glLineWidth(2);
		gl.glBegin(GL.GL_LINE_STRIP);

		for (Point p : kiteline)
			gl.glVertex2i(p.x, p.y);

		gl.glEnd();
		gl.glLineWidth(1);
	}

	private void	drawKiteFans(GL2 gl)
	{
		int		cx = 956;
		int		cy = 490;
		int		r = 80;

		// Flap those wings!
		int		ticks = 120;
		double		phase = ((k % (2 * ticks)) - ticks) / (double)ticks;
		double		variance = ANGLE_MOON * Math.cos(2 * Math.PI * phase);

		// The min and max angles of each wing, with variance over time
		double		amin =  4.0 * ANGLE_MOON - variance;
		double		amax =  9.0 * ANGLE_MOON + variance;
		double		bmin = 13.0 * ANGLE_MOON - variance;
		double		bmax = 18.0 * ANGLE_MOON + variance;

		int		fans = 5;
		double		astep = (amax - amin) / fans;
		double		bstep = (bmax - bmin) / fans;

		for (int i=0; i<fans; i++)
		{
			double	a = amin + astep * i;
			double	b = bmin + bstep * i;

			drawKiteBlade(gl, cx, cy, r, a, a + astep);	// Upper blade
			drawKiteBlade(gl, cx, cy, r, b, b + bstep);	// Lower blade
		}
	}

	private void	drawKiteBlade(GL2 gl, int cx, int cy, int r,
								  double a1, double a2)
	{
		// Fill in the blade
		setColor(gl, 48, 80, 224);
		gl.glBegin(GL2.GL_POLYGON);
		gl.glVertex2d(cx, cy);
		gl.glVertex2d(cx + r * Math.cos(a1), cy + r * Math.sin(a1));
		gl.glVertex2d(cx + r * Math.cos(a2), cy + r * Math.sin(a2));
		gl.glEnd();

		// Draw the thin struts
		setColor(gl, 96, 96, 96);
		gl.glBegin(GL.GL_LINE_STRIP);
		gl.glVertex2d(cx, cy);
		gl.glVertex2d(cx + r * Math.cos(a1), cy + r * Math.sin(a1));
		gl.glVertex2d(cx + r * Math.cos(a2), cy + r * Math.sin(a2));
		gl.glVertex2d(cx, cy);
		gl.glEnd();

		// Draw the thick translucent edges
		setColor(gl, 128, 128, 128, 64);
		gl.glLineWidth(6);
		gl.glBegin(GL.GL_LINE_STRIP);
		gl.glVertex2d(cx, cy);
		gl.glVertex2d(cx + r * Math.cos(a1), cy + r * Math.sin(a1));
		gl.glVertex2d(cx + r * Math.cos(a2), cy + r * Math.sin(a2));
		gl.glVertex2d(cx, cy);
		gl.glEnd();
		gl.glLineWidth(1);
	}

	private static final Point[]	HOUSE_OUTLINE = new Point[]
	{
		new Point(0, 0),		// lower left corner
		new Point(0, 162),		// bottom left corner
		new Point(88, 250),	// apex
		new Point(176, 162),	// top right corner
		new Point(176, 0),		// bottom left corner
	};

	private static final Point[]	HOUSE_OUTLINE1 = new Point[]
	{
		new Point(-1, -1),		// lower left corner
		new Point(-1, 162),	// bottom left corner
		new Point(88, 251),	// apex
		new Point(177, 162),	// top right corner
		new Point(177, -1),	// bottom left corner
	};

	// Too much variation to encapsulate house drawing in a drawHouse() method
	private void	drawHouses(GL2 gl)
	{
		int		tx = 108;
		int		ty = 132;

		drawChimney(gl, tx + 114, ty + 162, true);
		drawOutline(gl, tx, ty, 0, 1);
		drawRoof(gl, tx + 88, ty + 250);
		drawWindow(gl, tx + 127, ty + 127, true);
		drawDoor(gl, tx + 39, ty);

		tx = 634;
		ty = 158;

		drawChimney(gl, tx + 30, ty + 162, false);
		drawOutline(gl, tx, ty, 1, 2);
		drawWindow(gl, tx + 98, ty + 64, false);
		drawWindow(gl, tx + 144, ty + 64, false);
		drawDoor(gl, tx + 7, ty);
		drawHouseStar(gl, tx + 88, ty + 200);

		tx = 1048;
		ty = 132;

		drawChimney(gl, tx + 30, ty + 162, false);
		drawOutline(gl, tx, ty, 2, 2);
		drawWindow(gl, tx + 98, ty + 64, false);
		drawWindow(gl, tx + 144, ty + 64, false);
		drawDoor(gl, tx + 7, ty);
		drawDoorWindow(gl, tx + 27, ty + 71);
	}

	private void	drawChimney(GL2 gl, int sx, int sy, boolean smoke)
	{
		setColor(gl, 128, 0, 0);				// Firebrick red
		fillRect(gl, sx, sy, 30, 88);

		setColor(gl, 0, 0, 0);					// Black
		drawRect(gl, sx, sy, 30, 88);

		if (smoke)
			drawSmoke(gl, sx + 3, sy + 88);
	}

	private LinkedList<Point>	smoke = new LinkedList<Point>();

	// The picture's quads are boring...let's have some fun with animation!
	private void	drawSmoke(GL2 gl, int sx, int sy)
	{
		// Random walk up to two pixels on each end of the previous smoke line
		// Each point in the list defines (xmin, xmax) for a smoke line
		Point	p = ((smoke.size() == 0) ?
					 new Point(3, 27) : smoke.getFirst());
		int	ql = Math.min(30, Math.max( 0, p.x + RANDOM.nextInt(5) - 2));
		int	qr = Math.max( 0, Math.min(30, p.y + RANDOM.nextInt(5) - 2));
		Point	q = ((ql < qr) ? new Point(ql, qr) : new Point(qr, ql));

		smoke.addFirst(q);			// Add the lowest line to beginning

		if (smoke.size() > 160)	// If it's long enough,
			smoke.removeLast();	// remove the highest (=transparent) line

		int		alpha = 0;		// For most opaque line closest to chimney

		for (Point a : smoke)		// Draw all the lines lowest to highest,
		{
			if (RANDOM.nextInt(1024) < alpha)	// simulate diffusion leftward
				a.x--;

			if (RANDOM.nextInt(1024) < alpha)	// and rightward
				a.y++;

			setColor(gl, 255, 255, 255, 160 - alpha++);	// fading on the way

			gl.glBegin(GL2.GL_QUADS);
			gl.glVertex2i(sx + a.x, sy + alpha + 1);	// as height goes up
			gl.glVertex2i(sx + a.y, sy + alpha + 1);
			gl.glVertex2i(sx + a.y, sy + alpha + 2);
			gl.glVertex2i(sx + a.x, sy + alpha + 2);
			gl.glEnd();
		}
	}

	private void	drawOutline(GL2 gl, int sx, int sy, int shade, int thickness)
	{
		if (shade == 0)
			setColor(gl, 64, 64, 0);			// Dark green
		else if (shade == 1)
			setColor(gl, 143, 82, 10);			// Medium brown
		else
			setColor(gl, 128, 64, 0);			// Medium brown

		fillPoly(gl, sx, sy, HOUSE_OUTLINE);

		setColor(gl, 0, 0, 0);					// Black
		gl.glLineWidth(thickness);
		drawPoly(gl, sx, sy, HOUSE_OUTLINE);
		gl.glLineWidth(1);
	}

	private void	drawRoof(GL2 gl, int cx, int cy)
	{
		setColor(gl, 80, 64, 32);				// Dark brown

		gl.glBegin(GL.GL_TRIANGLE_FAN);
		gl.glVertex2i(cx, cy);
		gl.glVertex2i(cx - 88, cy - 88);
		gl.glVertex2i(cx - 56, cy - 88);
		gl.glVertex2i(cx - 24, cy - 88);
		gl.glVertex2i(cx + 24, cy - 88);
		gl.glVertex2i(cx + 56, cy - 88);
		gl.glVertex2i(cx + 88, cy - 88);
		gl.glEnd();

		setColor(gl, 0, 0, 0);					// Black

		gl.glBegin(GL.GL_LINE_STRIP);			// Leftmost board
		gl.glVertex2i(cx, cy);
		gl.glVertex2i(cx - 88, cy - 88);
		gl.glVertex2i(cx - 56, cy - 88);
		gl.glVertex2i(cx, cy);
		gl.glEnd();

		gl.glBegin(GL.GL_LINE_STRIP);			// Left-center board
		gl.glVertex2i(cx, cy);
		gl.glVertex2i(cx - 56, cy - 88);
		gl.glVertex2i(cx - 24, cy - 88);
		gl.glVertex2i(cx, cy);
		gl.glEnd();

		gl.glBegin(GL.GL_LINE_STRIP);			// Center board
		gl.glVertex2i(cx, cy);
		gl.glVertex2i(cx - 24, cy - 88);
		gl.glVertex2i(cx + 24, cy - 88);
		gl.glVertex2i(cx, cy);
		gl.glEnd();

		gl.glBegin(GL.GL_LINE_STRIP);			// Right-center board
		gl.glVertex2i(cx, cy);
		gl.glVertex2i(cx + 24, cy - 88);
		gl.glVertex2i(cx + 56, cy - 88);
		gl.glVertex2i(cx, cy);
		gl.glEnd();

		gl.glBegin(GL.GL_LINE_STRIP);			// Rightmost board
		gl.glVertex2i(cx, cy);
		gl.glVertex2i(cx + 56, cy - 88);
		gl.glVertex2i(cx + 88, cy - 88);
		gl.glVertex2i(cx, cy);
		gl.glEnd();
	}

	private void	drawDoor(GL2 gl, int cx, int cy)
	{
		setColor(gl, 192, 128, 0);				// Light brown
		fillRect(gl, cx, cy, 40, 92);

		setColor(gl, 0, 0, 0);					// Black
		drawRect(gl, cx, cy, 40, 92);

		setColor(gl, 176, 192, 192);			// Light steel
		fillOval(gl, cx + 8, cy + 46, 4, 4);

		setColor(gl, 0, 0, 0);					// Black
		drawOval(gl, cx + 8, cy + 46, 4, 4);
	}

	private void	drawWindow(GL2 gl, int cx, int cy, boolean shade)
	{
		int		dx = 20;
		int		dy = 20;

		setColor(gl, 255, 255, 128);			// Light yellow
		fillRect(gl, cx - dx, cy - dy, 2 * dx, 2 * dy);

		if (shade)
			setColor(gl, 224, 224, 224);		// Light gray
		else
			setColor(gl, 224, 192, 224);		// Light pink

		gl.glBegin(GL2.GL_POLYGON);			// Left shade fill
		gl.glVertex2i(cx - dx, cy - dy);
		gl.glVertex2i(cx - dx, cy + dy);
		gl.glVertex2i(cx     , cy + dy);
		gl.glEnd();

		gl.glBegin(GL2.GL_POLYGON);			// Right shade fill
		gl.glVertex2i(cx     , cy + dy);
		gl.glVertex2i(cx + dx, cy + dy);
		gl.glVertex2i(cx + dx, cy - dy);
		gl.glEnd();

		setColor(gl, 0, 0, 0);					// Black

		gl.glBegin(GL2.GL_LINE_LOOP);			// Left shade edge
		gl.glVertex2i(cx - dx, cy - dy);
		gl.glVertex2i(cx - dx, cy + dy);
		gl.glVertex2i(cx     , cy + dy);
		gl.glEnd();

		gl.glBegin(GL2.GL_LINE_LOOP);			// Right shade edge
		gl.glVertex2i(cx     , cy + dy);
		gl.glVertex2i(cx + dx, cy + dy);
		gl.glVertex2i(cx + dx, cy - dy);
		gl.glEnd();

		setColor(gl, 0, 0, 0);					// Black

		// Window frame: bottom, middle, top
		fillRect(gl, cx - dx - 1, cy - dy - 1, 2 * dx + 3, 3);
		fillRect(gl, cx - dx - 1, cy +  0 - 1, 2 * dx + 3, 3);
		fillRect(gl, cx - dx - 1, cy + dy - 1, 2 * dx + 3, 3);

		// Window frame: left, middle, right
		fillRect(gl, cx - dx - 1, cy - dy - 1, 3, 2 * dy + 3);
		fillRect(gl, cx +  0 - 1, cy - dy - 1, 3, 2 * dy + 3);
		fillRect(gl, cx + dx - 1, cy - dy - 1, 3, 2 * dy + 3);

		// Could use LINE_STRIP for the thick window frames instead
	}

	private void	drawHouseStar(GL2 gl, int cx, int cy)
	{
		double	theta = 0.5 * Math.PI;

		setColor(gl, 255, 255, 0);
		gl.glBegin(GL.GL_TRIANGLE_FAN);
		gl.glVertex2d(cx, cy);
		doStarVertices(gl, cx, cy, 5, 20.0, 8.0);
		gl.glVertex2d(cx + 20 * Math.cos(theta), cy + 20 * Math.sin(theta));
		gl.glEnd();

		setColor(gl, 0, 0, 0);
		gl.glBegin(GL.GL_LINE_STRIP);
		doStarVertices(gl, cx, cy, 5, 20.0, 8.0);
		gl.glVertex2d(cx + 20 * Math.cos(theta), cy + 20 * Math.sin(theta));
		gl.glEnd();
	}

	private void	drawDoorWindow(GL2 gl, int cx, int cy)
	{
		double	theta = 0.5 * Math.PI;

		setColor(gl, 255, 255, 128);
		gl.glBegin(GL.GL_TRIANGLE_FAN);
		gl.glVertex2d(cx, cy);
		doStarVertices(gl, cx, cy, 4, 15.0, 13.5);
		gl.glVertex2d(cx + 15 * Math.cos(theta), cy + 15 * Math.sin(theta));
		gl.glEnd();

		setColor(gl, 0, 0, 0);
		gl.glBegin(GL.GL_LINE_STRIP);
		doStarVertices(gl, cx, cy, 4, 15.0, 13.5);
		gl.glVertex2d(cx + 15 * Math.cos(theta), cy + 15 * Math.sin(theta));
		gl.glEnd();
	}

	// Warning! Text is drawn in unprojected canvas/viewport coordinates.
	// For more on text rendering, the example on this page is long but helpful:
	// jogamp.org/jogl-demos/src/demos/j2d/FlyingText.java
	private void	drawText(GLAutoDrawable drawable)
	{
		renderer.beginRendering(w, h);
		renderer.setColor(0.75f, 0.75f, 0.75f, 1.0f);
		renderer.draw("Application", 2, h - 14);
		renderer.endRendering();
	}

	//**********************************************************************
	// Private Methods (Utility Functions)
	//**********************************************************************

	private void	setColor(GL2 gl, int r, int g, int b, int a)
	{
		gl.glColor4f(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
	}

	private void	setColor(GL2 gl, int r, int g, int b)
	{
		setColor(gl, r, g, b, 255);
	}

	private void	fillRect(GL2 gl, int x, int y, int w, int h)
	{
		gl.glBegin(GL2.GL_POLYGON);
		gl.glVertex2i(x+0, y+0);
		gl.glVertex2i(x+0, y+h);
		gl.glVertex2i(x+w, y+h);
		gl.glVertex2i(x+w, y+0);
		gl.glEnd();
	}

	private void	drawRect(GL2 gl, int x, int y, int w, int h)
	{
		gl.glBegin(GL.GL_LINE_LOOP);
		gl.glVertex2i(x+0, y+0);
		gl.glVertex2i(x+0, y+h);
		gl.glVertex2i(x+w, y+h);
		gl.glVertex2i(x+w, y+0);
		gl.glEnd();
	}

	private void	fillOval(GL2 gl, int cx, int cy, int w, int h)
	{
		gl.glBegin(GL2.GL_POLYGON);

		for (int i=0; i<32; i++)
		{
			double	a = (2.0 * Math.PI) * (i / 32.0);

			gl.glVertex2d(cx + w * Math.cos(a), cy + h * Math.sin(a));
		}

		gl.glEnd();
	}

	private void	drawOval(GL2 gl, int cx, int cy, int w, int h)
	{
		gl.glBegin(GL.GL_LINE_LOOP);

		for (int i=0; i<32; i++)
		{
			double	a = (2.0 * Math.PI) * (i / 32.0);

			gl.glVertex2d(cx + w * Math.cos(a), cy + h * Math.sin(a));
		}

		gl.glEnd();
	}

	private void	fillPoly(GL2 gl, int startx, int starty, Point[] offsets)
	{
		gl.glBegin(GL2.GL_POLYGON);

		for (int i=0; i<offsets.length; i++)
			gl.glVertex2i(startx + offsets[i].x, starty + offsets[i].y);

		gl.glEnd();
	}

	private void	drawPoly(GL2 gl, int startx, int starty, Point[] offsets)
	{
		gl.glBegin(GL2.GL_LINE_LOOP);

		for (int i=0; i<offsets.length; i++)
			gl.glVertex2i(startx + offsets[i].x, starty + offsets[i].y);

		gl.glEnd();
	}

	private void	doStarVertices(GL2 gl, int cx, int cy, int sides,
								   double r1, double r2)
	{
		double	delta = Math.PI / sides;
		double	theta = 0.5 * Math.PI;

		for (int i=0; i<sides; i++)
		{
			gl.glVertex2d(cx + r1 * Math.cos(theta), cy + r1 * Math.sin(theta));
			theta += delta;

			gl.glVertex2d(cx + r2 * Math.cos(theta), cy + r2 * Math.sin(theta));
			theta += delta;
		}
	}
}

//******************************************************************************
