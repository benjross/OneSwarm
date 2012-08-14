package edu.washington.cs.oneswarm.f2f.servicesharing;

import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Test;

import edu.washington.cs.oneswarm.test.util.OneSwarmTestBase;

/**
 * Tests ExitNodeInfo, verifying that it correctly represents the policy of the
 * server it represents.
 * 
 * @author Nick
 * 
 */
public class ExitNodePublishingTest extends OneSwarmTestBase {

    @Test
    public void testExitNodeInfo() throws Exception {
        try {
            ExitNodeList.getInstance().setExitNodeSharedService(
                    new ExitNodeInfo("Servo the Magnificent", 123456, 250,
                            new String[] { "accept *.*" }, new Date(), "Version string 2.0"));
            ExitNodeList.getInstance().registerExitNodes();

            // TODO (nick) Verify the registration on the exit Server.
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
