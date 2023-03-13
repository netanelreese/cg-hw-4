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
import java.awt.Component;
import java.awt.event.*;
//import java.awt.geom.Point2D;
import edu.ou.cs.cg.utilities.Utilities;

//******************************************************************************

/**
 * The <CODE>KeyHandler</CODE> class.<P>
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class KeyHandler extends KeyAdapter
{
	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private final View		view;
	private final Model	model;

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public KeyHandler(View view, Model model)
	{
		this.view = view;
		this.model = model;

		Component	component = view.getCanvas();

		component.addKeyListener(this);
	}

	//**********************************************************************
	// Override Methods (KeyListener)
	//**********************************************************************

	public void		keyPressed(KeyEvent e)
	{
		boolean	b = Utilities.isShiftDown(e);
		int		sides = model.getSides();
		double		radius = model.getRadius();

		switch (e.getKeyCode())
		{
			case KeyEvent.VK_Q:	// Decrement sides, in [3, MAX]
				model.setSides(Math.max(sides - 1, View.MIN_SIDES));
				break;

			case KeyEvent.VK_W:	// Increment sides, in [3, MAX]
				model.setSides(Math.min(sides + 1, View.MAX_SIDES));
				break;

			case KeyEvent.VK_A:	// Decrease radius by 1.1x, 2x if shift down
				model.setRadius((b ? radius / 2.0 : radius / 1.1));
				break;

			case KeyEvent.VK_S:	// Increase radius by 1.1x, 2x if shift down
				model.setRadius((b ? radius * 2.0 : radius * 1.1));
				break;

			case KeyEvent.VK_F:
				model.toggleFill();
				return;

			case KeyEvent.VK_E:
				model.toggleEdge();
				return;

			case KeyEvent.VK_B:
				model.toggleBack();
				return;
		}
	}
}

//******************************************************************************
