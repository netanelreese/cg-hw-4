//******************************************************************************
// Copyright (C) 2023 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Tue Feb 14 18:17:37 2023 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20230214 [weaver]:	Original file.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.application.circle;

//import java.lang.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import edu.ou.cs.cg.utilities.Utilities;

//******************************************************************************

/**
 * The <CODE>View</CODE> class.<P>
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class View
	implements GLEventListener
{
	//**********************************************************************
	// Private Class Members
	//**********************************************************************

	private static final int			DEFAULT_FRAMES_PER_SECOND = 60;
	private static final DecimalFormat	FORMAT = new DecimalFormat("0.000");

	//**********************************************************************
	// Public Class Members
	//**********************************************************************

	public static final GLUT			MYGLUT = new GLUT();
	public static final Random			RANDOM = new Random();

	public static final int			MIN_SIDES = 3;
	public static final int			MAX_SIDES = 128;

	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private final GLJPanel				canvas;
	private int						w;			// Canvas width
	private int						h;			// Canvas height

	private TextRenderer				renderer;

	private final FPSAnimator			animator;
	private int						counter;	// Frame counter

	private final Model				model;

	private final KeyHandler			keyHandler;
	//private final MouseHandler			mouseHandler;

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public View(GLJPanel canvas)
	{
		this.canvas = canvas;

		// Initialize rendering
		counter = 0;
		canvas.addGLEventListener(this);

		// Initialize model (scene data and parameter manager)
		model = new Model(this);

		// Initialize controller (interaction handlers)
		keyHandler = new KeyHandler(this, model);
		//mouseHandler = new MouseHandler(this, model);

		// Initialize animation
		animator = new FPSAnimator(canvas, DEFAULT_FRAMES_PER_SECOND);
		animator.start();
	}

	//**********************************************************************
	// Getters and Setters
	//**********************************************************************

	public GLJPanel	getCanvas()
	{
		return canvas;
	}

	public int	getWidth()
	{
		return w;
	}

	public int	getHeight()
	{
		return h;
	}

	//**********************************************************************
	// Override Methods (GLEventListener)
	//**********************************************************************

	public void	init(GLAutoDrawable drawable)
	{
		w = drawable.getSurfaceWidth();
		h = drawable.getSurfaceHeight();

		renderer = new TextRenderer(new Font("Monospaced", Font.PLAIN, 12),
									true, true);

		initPipeline(drawable);
	}

	public void	dispose(GLAutoDrawable drawable)
	{
		renderer = null;
	}

	public void	display(GLAutoDrawable drawable)
	{
		updatePipeline(drawable);

		update(drawable);
		render(drawable);
	}

	public void	reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		this.w = w;
		this.h = h;
	}

	//**********************************************************************
	// Private Methods (Rendering)
	//**********************************************************************

	private void	update(GLAutoDrawable drawable)
	{
		counter++;									// Advance animation counter
	}

	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);		// Clear the buffer

		// Draw the scene
		drawMain(gl);								// Draw main content
		drawMode(drawable);						// Draw mode text

		gl.glFlush();								// Finish and display
	}

	//**********************************************************************
	// Private Methods (Pipeline)
	//**********************************************************************

	private void	initPipeline(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);	// Black background

		// Make points easier to see on Hi-DPI displays
		gl.glEnable(GL2.GL_POINT_SMOOTH);	// Turn on point anti-aliasing
	}

	private void	updatePipeline(GLAutoDrawable drawable)
	{
		GL2			gl = drawable.getGL().getGL2();
		GLU			glu = GLU.createGLU();

		gl.glMatrixMode(GL2.GL_PROJECTION);		// Prepare for matrix xform
		gl.glLoadIdentity();						// Set to identity matrix
		glu.gluOrtho2D(-1.1, 1.1, -1.1, 1.1);	// 2D translate and scale
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	private void	drawMode(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();

		renderer.beginRendering(w, h);

		// Draw all text in medium gray
		renderer.setColor(0.75f, 0.75f, 0.75f, 1.0f);

		String		sn = ("[q|w] Sides =      " + model.getSides());
		String		sr = ("[a|s] Radius =     " + model.getRadius());
		String		sf = ("[f]   Fill shape = " + model.getFill());
		String		se = ("[e]   Edge shape = " + model.getEdge());
		String		sb = ("[b]   Background = " + model.getBack());

		renderer.draw(sn, 2, 2);
		renderer.draw(sr, 2, 16);
		renderer.draw(sf, 2, 30);
		renderer.draw(se, 2, 44);
		renderer.draw(sb, 2, 58);

		renderer.endRendering();
	}

	private void	drawMain(GL2 gl)
	{
		if (model.getBack())
			backCircle(gl);						// Fill the reference circle

		if (model.getFill())
			fillCircle(gl);						// Fill the circle

		if (model.getEdge())
			edgeCircle(gl);						// Edge the circle
	}

	private void	backCircle(GL2 gl)
	{
		int		sides = MAX_SIDES;
		double	radius = model.getRadius();

		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(1.0f, 1.0f, 1.0f);		// White

		for (int i=0; i<sides; i++)
		{
			double	theta = (2.0 * Math.PI) * (i / (double)sides);

			gl.glVertex2d(radius * Math.cos(theta),
						  radius * Math.sin(theta));
		}

		gl.glEnd();
	}

	private void	fillCircle(GL2 gl)
	{
		int		sides = model.getSides();
		double	radius = model.getRadius();

		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(1.0f, 1.0f, 0.0f);		// Yellow

		for (int i=0; i<sides; i++)
		{
			double	theta = (2.0 * Math.PI) * (i / (double)sides);

			gl.glVertex2d(radius * Math.cos(theta),
						  radius * Math.sin(theta));
		}

		gl.glEnd();
	}

	private void	edgeCircle(GL2 gl)
	{
		int		sides = model.getSides();
		double	radius = model.getRadius();

		gl.glBegin(GL.GL_LINE_LOOP);
		gl.glColor3f(1.0f, 0.0f, 0.0f);		// Red

		for (int i=0; i<sides; i++)
		{
			double	theta = (2.0 * Math.PI) * (i / (double)sides);

			gl.glVertex2d(radius * Math.cos(theta),
						  radius * Math.sin(theta));
		}

		gl.glEnd();
	}
}

//******************************************************************************
