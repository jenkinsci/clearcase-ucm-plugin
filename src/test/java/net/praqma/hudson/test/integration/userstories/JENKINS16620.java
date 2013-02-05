package net.praqma.hudson.test.integration.userstories;

import hudson.model.*;
import hudson.model.Project;
import hudson.scm.PollingResult;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.*;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.junit.DescriptionRule;
import net.praqma.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author cwolfgang
 *         Date: 04-02-13
 *         Time: 12:00
 */
public class JENKINS16620 extends BaseTestClass {

    @Rule
    public static ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-16620", "setup-JENKINS-16620.xml" );

    @Rule
    public static DescriptionRule desc = new DescriptionRule();

    @Test
    @TestDescription( title = "JENKINS-16620", text = "Changed baselines cannot be rebuild" )
    public void jenkins16620() throws Exception {
        Project project = new CCUCMRule.ProjectCreator( "JENKINS-16620", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() ).getProject();

        AbstractBuild build1 = new CCUCMRule.ProjectBuilder( project ).failBuild( true ).build();

        Baseline bl = ccenv.context.baselines.get( "model-1" ).load();

        bl.setPromotionLevel( net.praqma.clearcase.ucm.entities.Project.PromotionLevel.INITIAL );

        AbstractBuild build2 = new CCUCMRule.ProjectBuilder( project ).build();

        new SystemValidator( build2 ).validateBuild( Result.SUCCESS ).validate();
    }

    @Test
    @TestDescription( title = "JENKINS-16620", text = "Changed baselines MUST NOT ABLE TO be rebuild on ANY" )
    public void jenkins16620Any() throws Exception {
        Project project = new CCUCMRule.ProjectCreator( "JENKINS-16620", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() ).setPromotionLevel( null ).getProject();

        AbstractBuild build1 = new CCUCMRule.ProjectBuilder( project ).failBuild( true ).build();

        new SystemValidator( build1 ).validateBuild( Result.SUCCESS ).validate();

        Baseline bl = ccenv.context.baselines.get( "model-1" ).load();

        bl.setPromotionLevel( net.praqma.clearcase.ucm.entities.Project.PromotionLevel.INITIAL );

        PollingResult result = project.poll( jenkins.createTaskListener() );

        assertTrue( result.hasChanges() );
    }

}