/**
 * Copyright (c) 2005, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

// layout and containers
import java.awt.Dimension;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

// event listeners
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

// other stuff
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.LicenseDisplay;
import net.java.dev.spellcast.utilities.ActionVerifyPanel;

/**
 * An extended <code>JFrame</code> which provides all the frames in
 * KoLmafia the ability to update their displays, given some integer
 * value and the message to use for updating.
 */

public abstract class KoLFrame extends javax.swing.JFrame
{
	public static final int NOCHANGE_STATE = 0;
	public static final int ENABLED_STATE  = 1;
	public static final int DISABLED_STATE = 2;

	protected KoLmafia client;
	protected KoLPanel contentPanel;

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title,
	 * to be associated with the given client.
	 */

	protected KoLFrame( String title, KoLmafia client )
	{
		super( title );
		this.client = client;
	}

	/**
	 * Updates the display to reflect the given display state and
	 * to contain the given message.  Note that if there is no
	 * content panel, this method does nothing.
	 */

	public void updateDisplay( int displayState, String message )
	{
		if ( contentPanel != null && client != null )
		{
			client.getLogStream().println( message );
			(new DisplayStatus( displayState, message )).run();
		}
	}

	/**
	 * Utility method used to give the content panel for this
	 * <code>KoLFrame</code> focus.  Note that if the content
	 * panel is <code>null</code>< this method does nothing.
	 */

	public void requestFocus()
	{
		super.requestFocus();
		if ( contentPanel != null )
			contentPanel.requestFocus();
	}

	/**
	 * Utility method used to add the default <code>KoLmafia</code>
	 * configuration menu to the given menu bar.  The default menu
	 * contains the ability to customize preferences (global if it
	 * is invoked before login, character-specific if after) and
	 * initialize the debugger.
	 *
	 * @param	menuBar	The <code>JMenuBar</code> to which the configuration menu will be attached
	 */

	protected final void addConfigureMenu( JMenuBar menuBar )
	{
		JMenu configureMenu = new JMenu("Configure");
		configureMenu.setMnemonic( KeyEvent.VK_C );
		menuBar.add( configureMenu );

		JMenuItem settingsItem = new JMenuItem( "Preferences...", KeyEvent.VK_P );
		settingsItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				OptionsFrame oframe = new OptionsFrame( client );
				oframe.pack();  oframe.setVisible( true );
				oframe.requestFocus();
			}
		});

		configureMenu.add( settingsItem );

		final JMenuItem loggerItem = new JMenuItem( "Initialize Debugger", KeyEvent.VK_D );
		loggerItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				client.initializeLogStream();
				loggerItem.setEnabled( false );
			}
		});

		configureMenu.add( loggerItem );
	}

	/**
	 * Utility method used to add the default <code>KoLmafia</code> Help
	 * menu to the given menu bar.  The default Help menu contains the
	 * copyright statement for <code>KoLmafia</code>.
	 *
	 * @param	menuBar	The <code>JMenuBar</code> to which the Help menu will be attached
	 */

	protected final void addHelpMenu( JMenuBar menuBar )
	{
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic( KeyEvent.VK_H );
		menuBar.add( helpMenu );

		JMenuItem aboutItem = new JMenuItem( "About KoLmafia", KeyEvent.VK_C );
		aboutItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{	(new LicenseDisplay( "KoLmafia: Copyright Notice" )).requestFocus();
			}
		});

		helpMenu.add( aboutItem );
	}

	/**
	 * An internal class which allows focus to be returned to the
	 * client's active frame when auxiliary windows are closed.
	 */

	protected class ReturnFocusAdapter extends WindowAdapter
	{
		public void windowClosed( WindowEvent e )
		{
			if ( client != null )
				client.getActiveFrame().requestFocus();
		}
	}

	/**
	 * An internal class used as the basis for content panels.  This
	 * class builds upon the <code>ActionVerifyPanel</code> by adding
	 * <code>setStatusMessage()</code> and <code>clear()</code> methods
	 * as well as a method which allows GUIs to make sure that all
	 * status-message updating occurs within the AWT thread.
	 */

	protected abstract class KoLPanel extends ActionVerifyPanel
	{
		protected KoLPanel( String confirmedText, String cancelledText )
		{
			super( confirmedText, cancelledText );
		}

		protected KoLPanel( String confirmedText, String cancelledText, Dimension labelSize, Dimension fieldSize )
		{
			super( confirmedText, cancelledText, labelSize, fieldSize );
		}

		public abstract void clear();
		public abstract void setStatusMessage( String s );

		protected final class StatusMessageChanger implements Runnable
		{
			private String status;

			public StatusMessageChanger( String status )
			{	this.status = status;
			}

			public void run()
			{
				if ( !SwingUtilities.isEventDispatchThread() )
				{
					SwingUtilities.invokeLater( this );
					return;
				}

				setStatusMessage( status );
			}
		}

	}

	/**
	 * A <code>Runnable</code> object which can be placed inside of
	 * a call to <code>javax.swing.SwingUtilities.invokeLater()</code>
	 * to ensure that the GUI is only modified inside of the AWT thread.
	 */

	protected class DisplayStatus implements Runnable
	{
		private int displayState;
		private String status;

		public DisplayStatus( int displayState, String status )
		{
			this.displayState = displayState;
			this.status = status;
		}

		public void run()
		{
			if ( !SwingUtilities.isEventDispatchThread() )
			{
				SwingUtilities.invokeLater( this );
				return;
			}

			contentPanel.setStatusMessage( status );

			switch ( displayState )
			{
				case ENABLED_STATE:
					contentPanel.setEnabled( true );
					contentPanel.clear();
					break;

				case DISABLED_STATE:
					contentPanel.setEnabled( false );
					break;
			}
		}
	}
}