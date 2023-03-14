//******************************************************************************
// Copyright (C) 2016-2023 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Fri Mar 10 18:48:57 2023 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160209 [weaver]:	Original file.
// 20190203 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
// 20190227 [weaver]:	Updated to use model and asynchronous event handling.
// 20190318 [weaver]:	Modified for homework04.
// 20210320 [weaver]:	Added basic keyboard hints to drawMode().
// 20220311 [weaver]:	Improved hint wording in updatePointWithReflection().
// 20230310 [weaver]:	Improved TODO guidance especially for members to add.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.assignment.homework04;

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

	public static final int			MIN_SIDES = 3;
	public static final int			MAX_SIDES = 12;

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
	private final MouseHandler			mouseHandler;

	private final Deque<Point2D.Double>			special;
	private final ArrayList<Deque<Point2D.Double>>	regions;

	// Reference Vector
	// TODO: PUT MEMBERS FOR THE REFERENCE VECTOR HERE
	Point2D.Double rv;

	// Tracer and Bounces
	// TODO: PUT MEMBERS FOR THE TRACER AND BOUNCES HERE
	private Deque<Point2D.Double>	traces;
	private Deque<Point2D.Double>	bounces;



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

		// Initialize container polygons
		special = createSpecialPolygon();					// For N = 2
		regions = new ArrayList<Deque<Point2D.Double>>();	// For MIN to MAX

		for (int i=MIN_SIDES; i<=MAX_SIDES; i++)
			regions.add(createPolygon(i));

		// Initialize reference vector
		// TODO: INITIALIZE MEMBERS FOR THE REFERENCE VECTOR HERE
		rv = new Point2D.Double(model.getFactor(), 0);

		// Initialize tracer and bounces
		traces = new ArrayDeque<Point2D.Double>();
		bounces = new ArrayDeque<Point2D.Double>(); 

		// Initialize controller (interaction handlers)
		keyHandler = new KeyHandler(this, model);
		mouseHandler = new MouseHandler(this, model);

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
	// Public Methods
	//**********************************************************************

	public void	clearAllTrace()
	{
		// Remove all trajectory and bounce points

		// TODO: YOUR CODE HERE
		traces = new ArrayDeque<Point2D.Double>();
		bounces = new ArrayDeque<Point2D.Double>(); 
	}

	public int getMaxSides() {
		return MAX_SIDES;
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

		Deque<Point2D.Double>	polygon = getCurrentPolygon();
		Point2D.Double			q = model.getObject();

		updatePointWithReflection(polygon, q);
		model.setObjectInSceneCoordinatesAlt(new Point2D.Double(q.x, q.y));

		// Remove old (>1 second) trajectory and bounce points
		if (counter % 60 < 1) {
			if(!traces.isEmpty() && !bounces.isEmpty()) {
				traces.removeLast();
				bounces.removeLast();
			}
		}					
			
	}

	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
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
		glu.gluOrtho2D(-1.2, 1.2, -1.2, 1.2);		// 2D translate and scale
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	private void	drawMode(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();

		renderer.beginRendering(w, h);

		// Draw all text in light gray
		renderer.setColor(0.75f, 0.75f, 0.75f, 1.0f);

		Point2D.Double	cursor = model.getCursor();

		if (cursor != null)
		{
			String		sx = FORMAT.format(new Double(cursor.x));
			String		sy = FORMAT.format(new Double(cursor.y));
			String		s = "Pointer at (" + sx + "," + sy + ")";

			renderer.draw(s, 2, 2);
		}
		else
		{
			renderer.draw("No Pointer", 2, 2);
		}

		String		sn = ("[q|w] Number = " + model.getNumber());
		String		sf = ("[a|s] Factor = " + FORMAT.format(model.getFactor()));
		String		sc = ("[c]   Center moving object in polygon");

		renderer.draw(sn, 2, 16);
		renderer.draw(sf, 2, 30);
		renderer.draw(sc, 2, 44);

		renderer.endRendering();
	}

	private void	drawMain(GL2 gl)
	{
		drawAxes(gl);						// X and Y axes
		drawContainer(gl);					// Container polygon
		drawTracing(gl);					// Object trajectory
		drawBounces(gl);					// Reflection points
		drawObject(gl);					// The moving object
		drawCursor(gl);					// Cursor around the mouse point
	}

	// Draw horizontal (y==0) and vertical (x==0) axes
	private void	drawAxes(GL2 gl)
	{
		gl.glColor3f(0.25f, 0.25f, 0.25f);			// Dark gray

		gl.glBegin(GL.GL_LINES);

		gl.glVertex2d(-10.0, 0.0);
		gl.glVertex2d(10.0, 0.0);

		gl.glVertex2d(0.0, -10.0);
		gl.glVertex2d(0.0, 10.0);

		gl.glEnd();
	}

	// Fills and edges the polygon that is surrounding the moving object.
	private void	drawContainer(GL2 gl)
	{
		Deque<Point2D.Double>	polygon = getCurrentPolygon();
		//System.out.println(polygon);

		gl.glColor3f(.5f, .5f, .5f);			// White
		edgePolygon(gl, polygon);

		gl.glColor3f(0.15f, 0.15f, 0.15f);			// Very dark gray
		fillPolygon(gl, polygon);


	}

	// If the cursor point is not null, draw something helpful around it.
	private void	drawCursor(GL2 gl)
	{
		Point2D.Double	cursor = model.getCursor();

		if (cursor == null)
			return;

		gl.glBegin(GL.GL_LINE_LOOP);
		gl.glColor3f(0.5f, 0.5f, 0.5f);

		for (int i=0; i<32; i++)
		{
			double	theta = (2.0 * Math.PI) * (i / 32.0);

			gl.glVertex2d(cursor.x + 0.01 * Math.cos(theta),
						  cursor.y + 0.01 * Math.sin(theta));
		}

		gl.glEnd();
	}

	// Draw the moving object, which in this assignment is a single point.
	private void	drawObject(GL2 gl)
	{
		Point2D.Double	object = model.getObject();
		gl.glBegin(GL2.GL_POINTS);
		gl.glColor3f(1f, 1f, (float)(51/255));			// bright yellow
		gl.glVertex2d(object.x, object.y);

		gl.glEnd();	
	}

	// Draw the object trajectory in the polygon.
	private void	drawTracing(GL2 gl)
	{
		if(!traces.isEmpty()) {
			gl.glBegin(GL2.GL_LINES);//static field
			Point2D.Double object = model.getObject();
			Point2D.Double dest = traces.getFirst();
			gl.glColor3f(0, 0, (float)(51/255));			// blue?
			gl.glVertex2d(object.x,object.y);
			gl.glVertex2d(dest.x,dest.y);

			gl.glEnd();
		}
	}

	// Draw the reflection points on the polygon.
	private void	drawBounces(GL2 gl)
	{
		if(!bounces.isEmpty()) {
			gl.glBegin(GL2.GL_POINTS);
			gl.glColor3f(1f, 0f, 0f);			// red
			Iterator<Point2D.Double> bIterator = bounces.iterator();
			while (bIterator.hasNext()) {
				Point2D.Double bounce = bIterator.next();
				gl.glVertex2d(bounce.x, bounce.y);
			}

			gl.glEnd();	
		}
	}


	//**********************************************************************
	// Private Methods (Polygons)
	//**********************************************************************

	// Custom polygon for the sides=2 case. Irregular but convex.
	private Deque<Point2D.Double>	createSpecialPolygon()
	{
		Deque<Point2D.Double>	polygon = new ArrayDeque<Point2D.Double>(10);

		polygon.add(new Point2D.Double( 1.00, -0.86));
		polygon.add(new Point2D.Double( 1.00, -0.24));
		polygon.add(new Point2D.Double( 0.48,  0.90));
		polygon.add(new Point2D.Double( 0.05,  1.00));
		polygon.add(new Point2D.Double(-0.34,  0.87));

		polygon.add(new Point2D.Double(-0.86,  0.40));
		polygon.add(new Point2D.Double(-1.00,  0.04));
		polygon.add(new Point2D.Double(-0.93, -0.42));
		polygon.add(new Point2D.Double(-0.53, -0.84));
		polygon.add(new Point2D.Double( 0.71, -1.00));
		polygon.add(new Point2D.Double( 1.00, -0.86));

		return polygon;
	}

	// Creates a regular N-gon with points stored in counterclockwise order.
	// The polygon is centered at the origin with first vertex at (1.0, 0.0).
	private Deque<Point2D.Double> createPolygon(int sides) {
		Deque<Point2D.Double> polygon = new ArrayDeque<>(sides);
		double theta = 0.0;
		double delta = 2.0 * Math.PI / sides;
		double radius = .70 / Math.cos(delta/2.0); // compute the radius to make sure the polygon is centered at (0,0) and the first point is (1,0)
		if(sides == 3) {
			radius = .50 / Math.cos(delta/2.0); // triangle size is too big at .7 but other polygons are good size
		}
		for (int i = 0; i < sides; i++) {
			double x = radius * Math.cos(theta);
			double y = radius * Math.sin(theta);
			polygon.add(new Point2D.Double(x, y));
			theta += delta;
		}
		return polygon;
	}

	private void edgePolygon(GL2 gl, Deque<Point2D.Double> polygon) {
		Iterator<Point2D.Double> pIterator = polygon.iterator();
		gl.glBegin(GL.GL_LINE_LOOP);
		while (pIterator.hasNext()) {
			Point2D.Double vertex = pIterator.next();
			gl.glVertex2d(vertex.x, vertex.y);
		}
		gl.glEnd();

	}

	// Draws the interior of the specified polygon.
	private void	fillPolygon(GL2 gl, Deque<Point2D.Double> polygon)
	{
		Iterator<Point2D.Double> pIterator = polygon.iterator();
 
		gl.glBegin(GL2.GL_POLYGON);
		while (pIterator.hasNext()) {
			Point2D.Double vertex = pIterator.next();
			gl.glVertex2d(vertex.getX(), vertex.getY());
		}
		gl.glEnd();
	}

	// Get the polygon that is currently containing the moving object.
	private Deque<Point2D.Double>	getCurrentPolygon()
	{
		int	sides = model.getNumber();

		if (sides == 2)
			return special;
		else if ((MIN_SIDES <= sides) && (sides <= MAX_SIDES+1))
			return regions.get(sides - MIN_SIDES);
		else
			return null;
	}

	// Special method for privileged use by the Model class ONLY.
	public boolean	currentPolygonContains(Point2D.Double q)
	{
		return contains(getCurrentPolygon(), q);
	}

	//**********************************************************************
	// Private Methods (Reflection)
	//**********************************************************************

	// Updates the x and y coordinates of point q. Adds a vector to the provided
	// point, reflecting as needed off the sides of the provided polygon to
	// determine the new coordinates. The new coordinates are "returned" in q.
	public void	updatePointWithReflection(Deque<Point2D.Double> polygon,
											  Point2D.Double q)
	{
		// TODO: YOUR CODE HERE. Hints for how to approach it follow.
		
		// Use the reference vector to remember the current direction of
		// movement with a magnitude equal to the default distance (factor=1.0).

		Point2D.Double v = new Point2D.Double(rv.x * model.getFactor(), rv.y * model.getFactor());

		// For each update, copy the reference vector and scale it by the
		// current speed factor...

		// ...then loop to consume the scaled vector until nothing is left.
		while (v.distance(0,0) > 0)
		{
			double minDist = Double.MAX_VALUE;
			Point2D.Double closestIntersection = null;
			Point2D.Double p1 = polygon.getLast();
			for(Point2D.Double p2 : polygon) {
				Point2D.Double intersection = hmmm;
			}
			// 1. Calculate which side the point will reach first.

			//    Loop the polygon counterclockwise, taking vertices pairwise.

			//    For each side, see "Intersection of a Line through a Line".

			//    Important: Check for edge cases (pun?) in which q is parallel
			//    to the side or slightly outside it (due to roundoff error).
			//    See Figure 4.37 and the dot products below it on page 176.

			//    Always remember to check for divide-by-zero!

			// 2. If the point WON'T reach the closest side in this update,
			//    simply add the vector to it, and break out of the loop.
			break;
			//    Or if it WILL reach the side:

			//    Move the point to the hit point on the closest side.
			//    Calculate the reflected vector. Reduce it by the amount
			//    already moved, and use it as the new scaled vector.

			// After bounces, remember to update the direction of the reference
			// vector for the next time updatePointWithReflection() is called!
		}
	}
	//**********************************************************************
	// Private Methods (Vectors)
	//**********************************************************************

	// This might be a method to calculate a dot product. Sure seems like it.
	private double		dot(double vx, double vy, double vz,
							double wx, double wy, double wz)
	{
		return (vx*wx)+(vy*wy)+(vz*wz);
	}

	// Determines if point q is to the left of line p1->p2. If strict is false,
	// points exactly on the line are considered to be left of it.
	private boolean	isLeft(Point2D.Double p1, Point2D.Double p2,
							   Point2D.Double q, boolean strict)
	{
		//getting vector p1->p2
		double vx = p2.x - p1.x;
		double vy = p2.y - p1.y;
		//getting vector p1->q
		double wx = q.x - p1.x;
		double wy = q.y - p1.y;

		//calculate dot prod
		double dp = dot(vx, vy, 0, wx, wy, 0);

		//return result
		if(strict) return dp > 0;
		else return dp >= 0;
	}

	// Determines if point q is inside a polygon. The polygon must be convex
	// with points stored in counterclockwise order. Points exactly on any side
	// of the polygon are considered to be outside of it.
	private boolean	contains(Deque<Point2D.Double> polygon,
								 Point2D.Double q)
	{
		boolean contained = true;
		Iterator<Point2D.Double> pIterator = polygon.iterator();
		Point2D.Double p1 = pIterator.next();
		Point2D.Double p2;
		while (pIterator.hasNext()) {
			p2 = pIterator.next();
			if(!isLeft(p1,p2,q,false)) {
				contained = false;
				break;
			}
			p1 = p2;
		}
		// Hint: Use isLeft(). See the slide on "Testing Containment in 2D".

		return contained && isLeft(p1, polygon.getFirst(), q, false);
	}

}

//******************************************************************************
