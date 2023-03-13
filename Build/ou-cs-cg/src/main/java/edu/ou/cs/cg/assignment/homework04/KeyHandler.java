//******************************************************************************
// Copyright (C) 2016-2019 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Tue Mar 19 00:37:52 2019 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160225 [weaver]:	Original file.
// 20190227 [weaver]:	Updated to use model and asynchronous event handling.
// 20190318 [weaver]:	Modified for homework04.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.assignment.homework04;

//import java.lang.*;
import java.awt.Component;
import java.awt.event.*;
import java.awt.geom.Point2D;
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

	public void	keyPressed(KeyEvent e)
	{
		boolean	b = Utilities.isShiftDown(e);
		int		number = model.getNumber();
		double		factor = model.getFactor();

		switch (e.getKeyCode())
		{
			case KeyEvent.VK_C:	// Move object to center
				// TODO: YOUR CODE HERE
				return;

			case KeyEvent.VK_Q:	// Decrement sides, in [2, MAX], 2=special
				// TODO: YOUR CODE HERE
				break;

			case KeyEvent.VK_W:	// Increment sides, in [2, MAX], 2=special
				// TODO: YOUR CODE HERE
				break;

			case KeyEvent.VK_A:	// Decrease speed by 1.1x, 2x if shift down
				// TODO: YOUR CODE HERE
				break;

			case KeyEvent.VK_S:	// Increase speed by 1.1x, 2x if shift down
				// TODO: YOUR CODE HERE
				break;
		}
	}
}

//******************************************************************************
