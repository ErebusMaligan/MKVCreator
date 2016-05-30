package app;

import gui.props.UIEntryProps;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import process.NonStandardProcess;
import statics.StringUtils;
import ui.log.LogFileSiphon;

/**
 * @author Daniel J. Rivers
 *         2013
 *
 * Created: Aug 26, 2013, 12:53:49 AM 
 */
public class ProcessWrapper extends NonStandardProcess {
	
	private UIEntryProps props;
	private int episodeCounter = 0;
	
	public static String SAME_LINE = "$SL";
	
	public ProcessWrapper( String name, UIEntryProps props ) {
		super( name );
		this.props = props;
		new File( props.getString( "outputDir" ) ).mkdir();
		new LogFileSiphon( name, props.getString( "outputDir" ) + "/" + props.getString( "prefix" ) + ".log" ) {
			public void skimMessage( String name, String s ) {
				try {
					boolean sameLine = false;
					String out = s;
					if ( s.endsWith( SAME_LINE ) ) {
						sameLine = true;
						out = s.substring( 0, s.length() - SAME_LINE.length() );
					}
					if ( !sameLine ) {
						fstream.write( "[" + sdf.format( new Date( System.currentTimeMillis() ) ) + "]:  " + out );
						fstream.newLine();
					} else {
						fstream.write( out );
					}
					fstream.flush();
				} catch ( IOException e ) {
					e.printStackTrace();
				}
			}
		};
	}
	
	public void execute() {
		new Thread() {
			public void run() {
				String finalInput = props.getString( "inputDir" ) + ( Boolean.parseBoolean( props.getString( "usePrefix" ) ) ? "/" + props.getString( "prefix" ) : "" );
				String finalOutput = props.getString( "outputDir" ) + ( Boolean.parseBoolean( props.getString( "usePrefix" ) ) ? "/" + props.getString( "prefix" ) : "" );
				String startFile = props.getString( "startingFile" ).equals( "" ) ? null : props.getString( "startingFile" );
				boolean singleDone = false;
				File dir = new File( finalInput );
				sendMessage( "Converting major directory: " + dir.getAbsolutePath() + "\n" );
				File[] sub = dir.listFiles();
				//For all subdirectories (non recursive) in the given main directory...
				for ( int seasonNumber = Integer.parseInt( props.getString( "startSeason" ) ) - 1; ( seasonNumber < sub.length || Boolean.parseBoolean( props.getString( "singleSeason" ) ) ) && seasonNumber < Integer.parseInt( props.getString( "endSeason" ) ); seasonNumber++ ) {
					File seasonDirectory = !Boolean.parseBoolean( props.getString( "singleSeason" ) ) ? seasonDirectory = sub[ seasonNumber ] : sub[ 0 ];
					if ( seasonDirectory.isDirectory() ) {
						sendMessage( "Searching in Directory: " + seasonDirectory.getName() + "\n" );
						File destDir = new File( finalOutput + "/" + seasonDirectory.getName() );
						episodeCounter = Integer.parseInt( props.getString( "startingEp" ) );
						for ( File isoFile : seasonDirectory.listFiles() ) {
							if ( isoFile.getName().endsWith( ".ISO" ) || isoFile.getName().endsWith( ".iso" ) ) {
								sendMessage( "Analyzing File: " + isoFile.getName() + "\n" );
								if ( startFile == null || isoFile.getName().equals( startFile ) ) {
										startFile = null;
									if ( !Boolean.parseBoolean( props.getString( "singleFile" ) ) || ( Boolean.parseBoolean( props.getString( "singleFile" ) ) && !singleDone ) ) {
										try {
											//Info Process
											Process p = new ProcessBuilder( props.getString( "makeMKV" ), "info", "iso:" + isoFile.getAbsolutePath() ).start();
											BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
											String line;
											while ( ( line = br.readLine() ) != null ) {
												sendMessage( line + "\n" );
												
												//If it's a title line...
												if ( line.startsWith( "Title #" ) && !( line.contains( "skipped" ) ) ) {
													int trackNumber = -1;
													try {
//														trackNumber = Integer.parseInt( line.substring( line.indexOf( '#' ) + 1, line.indexOf( 'w' ) - 1 ) ) + 1;  //not sure why this was set to + 1... keep an eye on it
														trackNumber = Integer.parseInt( line.substring( line.indexOf( '#' ) + 1, line.indexOf( 'w' ) - 1 ) );														
													} catch ( NumberFormatException e ) {
														continue;
													}
													String time = line.substring( line.indexOf( ", " ) + 1, line.lastIndexOf( ":" ) );
													int hours = Integer.parseInt( time.substring( 1, time.indexOf( ':' ) ) );
													int minutes = Integer.parseInt( time.substring( time.indexOf( ":" ) + 1 ) );
													
													//If it's longer than the minimum time allowed go ahead and process it
													if ( ( minutes >= Integer.parseInt( props.getString( "minTime" ) ) || hours > 0 ) && minutes < Integer.parseInt( props.getString( "maxTime" ) ) ) {
														//Make Season directory if needed
														if ( !destDir.exists() ) {
															destDir.mkdir();
														}
														sendMessage( "VOB EXTRACTING:\n" );
														extract( "\"DVDVOB\"", isoFile, destDir, trackNumber );
														sendMessage( "\n" );
														try {
															Thread.sleep( 4000 ); //was 10000 previously in case a problem with file locks not being released comes up again... previous value always worked.
														} catch ( InterruptedException e ) {
															e.printStackTrace();
														}
														sendMessage( "RENAMING:\n" );
														File[] files = rename( hours, destDir, seasonNumber, trackNumber );
														
														try {
															Thread.sleep( 1000 ); //was 10000 previously in case a problem with file locks not being released comes up again... previous value always worked.
														} catch ( InterruptedException e ) {
															e.printStackTrace();
														}
														
														File dest = null;
														if ( !Boolean.parseBoolean( props.getString( "useHandbrake" ) ) )  {
															sendMessage( "\nMKV MERGING:\n" );
															dest = merge( files );
														} else {
															sendMessage( "\nENCODING WITH HANDBRAKE:\n" );
															dest = handbrakeEncode( files );
														}
														
														
														sendMessage( "\nMOVING SOURCE FILES:\n" );
														moveFiles( destDir, files );
														if ( Boolean.parseBoolean( props.getString( "perDisc" ) ) ) {
															moveResult( dest, new File( destDir.getAbsolutePath() + "/" + isoFile.getName().substring( 0, isoFile.getName().length() - 4 ) ) );
														}
													}
												}
											}
										} catch ( IOException e ) {
											sendMessage( e.getMessage() );
										}
										singleDone = true;
									}
								}
							}
						}
					}
				}	
			}
		}.start();
	}
	
	public void extract( String mode, File f, File destDir, int trackNumber ) throws IOException {
		Process p = new ProcessBuilder( props.getString( "DVDFab8QT" ), "/MODE " + mode + " " + "/SRC \"" + f.getAbsolutePath() + "\"" + " " + "/DEST \"" + destDir.getAbsolutePath() + "\\" + "\"" + " " + "/PROFILE \"vob.passthrough\"" + " " + "/TITLE " + "\"" + trackNumber + "\"" + " " + "/CLOSE" ).start();
		BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String line;
		while ( ( line = br.readLine() ) != null ) {
			sendMessage( line + "\n" );
		}
		sendMessage( "Extracting: " + trackNumber + " from " + f.getAbsolutePath() + " to " + destDir.getAbsolutePath() + "\n" );
	}
	
	public File merge( File[] files ) throws IOException {
		String vobPath = files[ 0 ].getAbsolutePath();
		sendMessage( "VOB PATH: " + vobPath + "\n" );
		String newFile = vobPath.substring( 0, vobPath.length() - 4 ) + ".mkv";
		sendMessage( "NAME: " + "\"" + newFile + "\"\n" );
		sendMessage( "COMMAND: mkvmerge " + "-o \"" + newFile + "\" " + "\"" + vobPath + "\"\n" );
		Process p = new ProcessBuilder( props.getString( "MKVMerge" ), "-o", "\"" + newFile + "\"", "\"" + vobPath + "\"" ).start();
		BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String line;
		while ( ( line = br.readLine() ) != null ) {
			if ( !line.startsWith( "Progress" ) ) {
				sendMessage( line + "\n" );
			}
		}
		return new File( vobPath.substring( 0, vobPath.length() - 4 ) + ".mkv" );
	}
	
	public File handbrakeEncode( File[] files ) throws IOException {
		String vobPath = files[ 0 ].getAbsolutePath();
		sendMessage( "VOB PATH: " + vobPath + "\n" );
		String newFile = vobPath.substring( 0, vobPath.length() - 4 ) + ".mkv";
		sendMessage( "NAME: " + "\"" + newFile + "\"\n" );
		sendMessage( "COMMAND: HandbrakeCLI " + "-i \"" + vobPath + "\" " + "-o " + "\"" + newFile + "\"" + " -e x264 -q 12 -B 160" + "\n" );
		//if redirectErrorStream( true ) is not called, the process will always hang and print out 0.00% done until the java program is exited, then it will magically unblock and start running
		//redirecting fixes the problem completely... though still not sure why
		Process p = new ProcessBuilder( props.getString( "Handbrake" ), "-i", "\"" + vobPath + "\"", "-o", "\"" + newFile + "\"", "-e", "x264", "-q", "12", "-B", "160" ).redirectErrorStream( true ).start();
		BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String line;
		while ( ( line = br.readLine() ) != null ) {
			if ( !line.startsWith( "Encoding" ) && !line.contains( "->" ) && !line.trim().isEmpty() ) {
				sendMessage( line + "\n" );
			} else if ( line.startsWith( "Encoding" ) && line.contains( "," ) && line.contains( "%" ) ) {
				sendMessage( line.substring( line.indexOf( "," ) + 1, line.indexOf( "%" ) + 1 ).trim() + " " + SAME_LINE );
			}
		}
		return new File( vobPath.substring( 0, vobPath.length() - 4 ) + ".mkv" );	
	}
	
	public File[] rename( int hours, File destDir, int seasonNumber, int trackNumber ) {
		File[] ret = new File[ 3 ];
		//Rename file to work with XBMC
		//first check if it's a 2 part episode
		String extra = "";
		if ( hours > 0 ) {
			extra = "e" + StringUtils.addZeroes( episodeCounter + 1 );
		}
		String[] ext = new String[] { "vob", "idx", "sub" };
		for ( int i = 0; i < ext.length; i++ ) {
			String e = ext[ i ];
			File z = null;
			for ( File f : destDir.listFiles() ) {
				if ( f.getName().endsWith( e ) ) {
					z = f;
					break;
				}
			}
			if ( z != null ) {
	//			File z = new File( destDir.getAbsolutePath() + "\\" + isoFile.getName().substring( 0, isoFile.getName().lastIndexOf( "." ) ) + "." + "Title" + trackNumber + "." + e );
				String newFileName = props.getString( "prefix" ) + "_s" + StringUtils.addZeroes( seasonNumber + 1 ) + "e" + StringUtils.addZeroes( episodeCounter ) + extra + "." + e;
				sendMessage( "Renaming: " + z.getName() + " to: " + newFileName + "\n" );
				File n = new File( destDir.getAbsolutePath() + "\\" + newFileName );
				z.renameTo( n );
				ret[ i ] = n;
			}
		}
		episodeCounter++;
		if ( hours > 0 ) {
			episodeCounter++;
		}
		sendMessage( "\n\n" );
		return ret;
	}
	
	public void moveFiles( File destDir, File[] files ) {
		File dir = new File( destDir.getAbsolutePath() + "/SourceFiles" );
		dir.mkdir();
		for ( File f : files ) {
			if ( f != null ) {
				File n = new File( destDir.getAbsolutePath() + "/SourceFiles/" + f.getName() );
				sendMessage( f.getAbsolutePath() + " moved to " + n.getAbsolutePath() + "\n" );
				f.renameTo( n );
			}
		}
	}
	
	public void moveResult( final File result, final File dir ) {
		dir.mkdir();
		new Thread() {
			public void run() {
				boolean success = false;
				while ( !success ) {
					try {
						result.renameTo( new File( dir.getAbsolutePath() + "/" + result.getName() ) );
						success = true;
					} catch ( Exception e ) {
					}
					try {
						Thread.sleep( 1000 );
					} catch ( InterruptedException e ) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
}