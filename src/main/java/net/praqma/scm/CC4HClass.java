package net.praqma.scm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;

import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.PollingResult;
import hudson.scm.SCMRevisionState;

import hudson.util.Digester2;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import net.praqma.clearcase.objects.Baseline;
import net.praqma.clearcase.objects.Component;
import net.praqma.clearcase.objects.Stream;
import net.praqma.debug.Debug;
import net.praqma.clearcase.objects.ClearBase;
import net.praqma.scm.SCMRevisionStateImpl;

/**
 * CC4HClass is responsible for everything regarding Hudsons connection to ClearCase.
 * This class defines all the files required by the user. The information can be entered on the config page.
 * 
 * @author Troels Selch S�rensen
 * @author Margit
 *
 */
public class CC4HClass extends SCM {
	
	private String levelToPoll;
	private String loadModule;
	private String component;
	private String stream;
	private boolean newest;
	private boolean newerThanRecommended;
	
	private Baseline baseline;
	private List<String> levels = null;
	private List<String> loadModules = null;
	
	protected static Debug logger = Debug.GetLogger();

	/**
	 * The constructor is used by Hudson to create the instance of the plugin needed for a connection to ClearCase.
	 * It is annotated with <code>@DataBoundConstructor</code> to tell Hudson where to put the information retrieved from the configuration page in the WebUI.
	 * 
	 * @param component This string defines the component needed to find baselines.
	 * @param levelToPoll This string defines the level to poll ClearCase for.
	 * @param loadModule This string tells if we should load all modules or only the ones that are modifiable.
	 * @param stream This string defines the stream needed to find baselines.
	 * @param newest This boolean tells if we should build only the newest baseline.
	 * @param newerThanRecommended This boolean tells if we only should care about baselines that are newer than the recommended baseline. 
	 */
	@DataBoundConstructor
	public CC4HClass(String component, String levelToPoll, String loadModule,
			String stream, boolean newest, boolean newerThanRecommended) {
		
		logger.trace_function();
		this.component = component;
		this.levelToPoll = levelToPoll;
		this.loadModule = loadModule;
		this.stream = stream;
		this.newest = newest;
		this.newerThanRecommended = newerThanRecommended;
	}
	
	/**
	 * The repository is checked for new baselines, and if any, then the oldest will be built.
	 * 
	 */
	@Override
	public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace,
			BuildListener listener, File changelogFile) throws IOException,
			InterruptedException {
		logger.trace_function();
		//component = "component:EH@\\PDS_PVOB";
		//stream = "stream:EH@\\PDS_PVOB";
		//levelToPoll = "INITIAL";
		//TODO perform actual checkout (In clearcase context this means create the workspace(=set the filepath for hudson to use))
		Component comp = Component.GetObject(component, true); // (true means that we know the component exists in PVOB)
		//FOR USE WHEN FACTORY WORKS: 
		Stream s = Stream.GetObject(stream, true);
		List<Baseline> baselines = comp.GetBlsWithPlevel(s, ClearBase.Plevel.valueOf(levelToPoll), false, false);
		baseline = baselines.get(0); //Getting the newest baseline
		
		//TODO: The two comments below seems outdated. Should they be deleted?
		//below baseline is for testpurposes - we will call the real one from Component and get a list and find the oldest from that list
		//Baseline bl = new Baseline("baseline:Remote_15-10-2010_MPSX_Fixed_NFC_timer_subscription@\\PDS_PVOB", true);
		baseline.MarkBuildInProgess(); //TODO: Here we need Tag instead, including Hudson job info
		List<String> changes = baseline.GetDiffs("list", true);

		return writeChangelog(changelogFile,changes);
	}
	
	/**
	 * This method is used by {@link <public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace,
			BuildListener listener, File changelogFile) throws IOException,
			InterruptedException> [checkout()]} to write the changelog used uses.
	 * 
	 * @param changelogFile The file given by Hudson.
	 * @param changes The list of changes to be written as XML.
	 * @return true if the changelog was persisted, false if not.
	 * @throws IOException
	 */
	private boolean writeChangelog(File changelogFile, List<String> changes) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		//Here the .hudson/jobs/[project name]/changelog.xml is written
		baos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes());
		baos.write("<changelog>".getBytes());
		String temp;
		for(String s: changes)
		{
			baos.write("<changeset>".getBytes());
			temp = "<filepath>" + s + "</filepath>";
			baos.write(temp.getBytes());
			baos.write("</changeset>".getBytes());
		}
		baos.write("</changelog>".getBytes());
		FileOutputStream fos = new FileOutputStream(changelogFile);
	    fos.write(baos.toByteArray());
	    fos.close();
	    return true;
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		logger.trace_function();
		return new ChangeLogParserImpl();
	}

	/**
	 * Currently this method returns BUILD_NOW, but later it should evaluate IF Hudson should build.
	 */
	@Override
	public PollingResult compareRemoteRevisionWith(
			AbstractProject<?, ?> project, Launcher launcher,
			FilePath workspace, TaskListener listener, SCMRevisionState baseline)
			throws IOException, InterruptedException {
		logger.trace_function();
		return PollingResult.BUILD_NOW;
	}

	@Override
	public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build,
			Launcher launcher, TaskListener listener) throws IOException,
			InterruptedException {
		logger.trace_function();
		SCMRevisionStateImpl scmRS = new SCMRevisionStateImpl();
		logger.log (" scmRS: "+scmRS.toString());
		//TODO: DET  ER HER, DER SNER - her skal returneres null (ingen nye baselines) eller en liste af baselines eller noget boolean-noget
		return scmRS;
	}

	/**
	 * This method is used by Hudson to load persisted data when users enter the job config page. 
	 * @return
	 */
	public String getLevelToPoll() {
		logger.trace_function();
		return levelToPoll;
	}

	/**
	 * This method is used by Hudson to load persisted data when users enter the job config page. 
	 * @return
	 */
	public String getComponent() {
		logger.trace_function();
		return component;
	}

	/**
	 * This method is used by Hudson to load persisted data when users enter the job config page. 
	 * @return
	 */
	public String getStream() {
		logger.trace_function();
		return stream;
	}

	/**
	 * This method is used by Hudson to load persisted data when users enter the job config page. 
	 * @return
	 */
	public String getLoadModule() {
		logger.trace_function();
		return loadModule;
	}

	public Baseline getBaseline(){
		return baseline;
	}

	public boolean isNewest() {
		return newest;
	}

	public boolean isNewerThanRecommended() {
		return newerThanRecommended;
	}


	@Extension
	public static class CC4HClassDescriptor extends SCMDescriptor<CC4HClass> {
		private String cleartool;
		private List<String> levels;
		private List<String> loadModules;

		public CC4HClassDescriptor() {
			super(CC4HClass.class, null);
			logger.trace_function();
			levels = getLevels();
			loadModules = getLoadModules();
			load();
		}

		/**
		 * This method is called, when the user saves the global Hudson configuration.
		 */
		@Override
		public boolean configure(org.kohsuke.stapler.StaplerRequest req,
				JSONObject json) throws FormException {
			logger.trace_function();
			cleartool = req.getParameter("cc4h.cleartool").trim();
			save();
			return true;
		}

		@Override
		public String getDisplayName() {
			logger.trace_function();
			return "Clearcase 4 Hudson";
		}

		/**
		 * This method is called by the scm/CC4HClass/global.jelly to validate-without-reload.
		 * 
		 * @param value
		 * @return
		 */
		public FormValidation doExecutableCheck(@QueryParameter String value) {
			logger.trace_function();
			return FormValidation.validateExecutable(value);
		}

		public String getCleartool() {
			logger.trace_function();
			if (cleartool == null)
				return "ct";
			return cleartool;
		}
		
		public List<String> getLevels(){
			logger.trace_function();
			levels = new ArrayList<String>();
			levels.add("INITIAL");
			levels.add("BUILT");
			levels.add("TESTED");
			levels.add("RELEASED");
			levels.add("REJECTED");
			return levels;
		}
	
		public List<String> getLoadModules() {
			logger.trace_function();
			loadModules = new ArrayList<String>();
			loadModules.add("All");
			loadModules.add("Modifiable");
			return loadModules;
		}
	}
}
