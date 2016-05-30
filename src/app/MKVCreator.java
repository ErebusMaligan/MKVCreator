package app;

import gui.entry.CheckEntry;
import gui.entry.DirectoryEntry;
import gui.entry.Entry;
import gui.entry.FileEntry;
import gui.props.UIEntryProps;
import gui.props.variable.BooleanVariable;
import gui.props.variable.IntVariable;
import gui.props.variable.StringVariable;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import process.io.ProcessStreamSiphon;
import statics.GUIUtils;
import ui.log.LogDialog;

/**
 * @author Daniel J. Rivers
 *         2013
 *
 * Created: Aug 24, 2013, 6:14:01 PM
 */
public class MKVCreator extends JFrame {

	private static final long serialVersionUID = 1L;

	private UIEntryProps props = new UIEntryProps();

	public MKVCreator() {
		ImageIcon icon = new ImageIcon( getClass().getResource( "MKV.png" ) );
		this.setTitle( "MKV Creator" );
		this.setIconImage( icon.getImage() );
		this.setSize( new Dimension( 640, 235 ) );
		this.setDefaultCloseOperation( EXIT_ON_CLOSE );
		this.setLayout( new BorderLayout() );
		setupProps();
		this.add( getTabbedPane(), BorderLayout.CENTER );
		JButton b = new JButton( "Run MKV Rip" );
		b.addActionListener( e -> {
			String pName = "MKVCREATE";
			LogDialog ld = new LogDialog( MKVCreator.this, pName, false );
			ld.getLogPanel().setSkimReplace( new ProcessStreamSiphon() {

				public void skimMessage( String name, String s ) {
					boolean sameLine = false;
					String out = s;
					if ( s.endsWith( ProcessWrapper.SAME_LINE ) ) {
						sameLine = true;
						out = s.substring( 0, s.length() - ProcessWrapper.SAME_LINE.length() );
					}

					if ( !sameLine ) {
						ld.getLogPanel().appendWithTime( out );
						ld.getLogPanel().addBlankLine();
					} else {
						ld.getLogPanel().append( out );
					}
				}
				
				public void notifyProcessEnded( String arg0 ) {}

				public void notifyProcessStarted( String arg0 ) {}
			});
			new ProcessWrapper( pName, props ).execute();
		} );
		this.add( b, BorderLayout.SOUTH );
		this.setVisible( true );
	}

	private void setupProps() {
		props.addVariable( "prefix", new StringVariable() );
		props.addVariable( "startSeason", new IntVariable( 1 ) );
		props.addVariable( "minTime", new IntVariable( 17 ) );
		props.addVariable( "endSeason", new IntVariable( 1 ) );
		props.addVariable( "maxTime", new IntVariable( 99 ) );
		props.addVariable( "usePrefix", new BooleanVariable( true ) );
		props.addVariable( "singleSeason", new BooleanVariable( false ) );
		props.addVariable( "perDisc", new BooleanVariable( true ) );
		
		props.addVariable( "startingFile", new StringVariable() );
		props.addVariable( "startingEp", new IntVariable( 1 ) );
		props.addVariable( "singleFile", new BooleanVariable( false ) );
		
		props.addVariable( "inputDir", new StringVariable( "D:/RIPS/ISO" ) );
		props.addVariable( "outputDir", new StringVariable( "D:/RIPS/MKV" ) );
		
		props.addVariable( "makeMKV", new StringVariable( "C:/Program Files (x86)/MakeMKV/makemkvcon64.exe" ) );
		props.addVariable( "MKVMerge", new StringVariable( "C:/Program Files (x86)/MKVtoolnix/mkvmerge.exe" ) );
		props.addVariable( "DVDFab8QT", new StringVariable( "C:/Program Files (x86)/DVDFab 8 qt/dvdfab.exe" ) );
		props.addVariable( "Handbrake", new StringVariable( "C:/Program Files/Handbrake/HandBrakeCLI.exe" ) );
		
		props.addVariable( "useHandbrake", new BooleanVariable( false ) );
	}

	private JTabbedPane getTabbedPane() {
		JTabbedPane p = new JTabbedPane();
		p.add( "Run Info", runPanel() );
		p.add( "Partial Season", partialPanel() );
		p.add( "File Paths", filePanel() );
		p.add( "Program Paths", programPanel() );
		return p;
	}
	
	private JPanel runPanel() {
		JPanel p = new JPanel();
		p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
		p.add( new Entry( "File Prefix:", props.getVariable( "prefix" ) ) );
		GUIUtils.spacer( p );
		JPanel lower = new JPanel();
		lower.setLayout( new BoxLayout( lower, BoxLayout.X_AXIS ) );
		
		JPanel west = new JPanel();
		west.setLayout( new BoxLayout( west, BoxLayout.Y_AXIS ) );
		west.add( new Entry( "Starting Season:", props.getVariable( "startSeason" ) ) );
		GUIUtils.spacer( west );
		west.add( new Entry( "Min Time:", props.getVariable( "minTime" ) ) );

		
		JPanel east = new JPanel();
		east.setLayout( new BoxLayout( east, BoxLayout.Y_AXIS ) );
		east.add( new Entry( "Ending Season:", props.getVariable( "endSeason" ) ) );
		GUIUtils.spacer( east );
		east.add( new Entry( "Max Time:", props.getVariable( "maxTime" ) ) );
		GUIUtils.spacer( east );

	
		JPanel bottom = new JPanel();
		bottom.setLayout( new BoxLayout( bottom, BoxLayout.X_AXIS ) );
		bottom.add( new CheckEntry( "Use File Prefix", props.getVariable( "usePrefix" ) ) );
		GUIUtils.spacer( bottom );
		bottom.add( new CheckEntry( "Dir Per Disc", props.getVariable( "perDisc" ) ) );
		GUIUtils.spacer( bottom );
		bottom.add( new CheckEntry( "Single Season", props.getVariable( "singleSeason" ) ) );
		GUIUtils.spacer( bottom );
		bottom.add( new CheckEntry( "Use Handbrake", props.getVariable( "useHandbrake" ) ) );
		
		lower.add( west );
		lower.add( east );
		p.add( lower );
		GUIUtils.spacer( p );
		p.add( bottom );
		return p;
	}
	
	private JPanel partialPanel() {
		JPanel p = new JPanel();
		p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
		p.add( new Entry( "Starting File:", props.getVariable( "startingFile" ) ) );
		GUIUtils.spacer( p );
		p.add( new Entry( "Starting Ep #:", props.getVariable( "startingEp" ) ) );
		GUIUtils.spacer( p );
		p.add( new CheckEntry( "Single File Only", props.getVariable( "singleFile" ) ) );
		return p;
	}

	private JPanel filePanel() {
		JPanel p = new JPanel();
		p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
		p.add( new DirectoryEntry( "Input Directory:", props.getVariable( "inputDir" ) ) );
		GUIUtils.spacer( p );
		p.add( new DirectoryEntry( "Output Directory:", props.getVariable( "outputDir" ) ) );
		GUIUtils.spacer( p );
		return p;
	}
	
	private JPanel programPanel() {
		JPanel p = new JPanel();
		p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
		p.add( new FileEntry( "MakeMKV:", props.getVariable( "makeMKV" ) ) );
		GUIUtils.spacer( p );
		p.add( new FileEntry( "MKVMerge:", props.getVariable( "MKVMerge" ) ) );
		GUIUtils.spacer( p );
		p.add( new FileEntry( "DVDFab8QT:", props.getVariable( "DVDFab8QT" ) ) );
		GUIUtils.spacer( p );
		p.add( new FileEntry( "Handbrake:", props.getVariable( "Handbrake" ) ) );
		return p;
	}

	public static void main( String args[] ) {
		try {
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		} catch ( ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e ) {
			System.err.println( "Critical JVM Failure!" );
			e.printStackTrace();
		}
		new MKVCreator();
	}
}