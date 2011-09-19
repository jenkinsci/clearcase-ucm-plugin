package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.clearcase.ucm.utils.BuildNumber;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.appenders.StreamAppender;

import hudson.FilePath.FileCallable;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;

public class GetClearCaseVersion implements FileCallable<String> {

	private static final long serialVersionUID = -8984877325832486334L;

	private Project project;
	private Pipe pipe;
	
	public GetClearCaseVersion( Project project, Pipe pipe ) {
		this.project = project;
		this.pipe = pipe;
    }
    
    @Override
    public String invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
        
    	StreamAppender app = null;
    	if( pipe != null ) {
	    	PrintStream toMaster = new PrintStream( pipe.getOut() );
	    	app = new StreamAppender( toMaster );
	    	Logger.addAppender( app );
    	}
        
    	String version = "";
    	
    	try {
			version = BuildNumber.getBuildNumber(project);
		} catch (UCMException e) {
        	Logger.removeAppender( app );
        	throw new IOException( "Unable to load " + project.getShortname() + ":" + e.getMessage() );
		}

    	Logger.removeAppender( app );

    	return version;
    }

}