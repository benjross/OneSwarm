package edu.washington.cs.oneswarm.test.integration;

import static org.testng.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.washington.cs.oneswarm.f2f.ExperimentalHarnessManager;
import edu.washington.cs.oneswarm.f2f.datagram.DatagramConnection;
import edu.washington.cs.oneswarm.f2f.servicesharing.ProxyServer;
import edu.washington.cs.oneswarm.f2f.socks.OSSocksServer;
import edu.washington.cs.oneswarm.test.util.TestUtils;
import edu.washington.cs.oneswarm.test.util.TwoProcessTestBase;

public class TwoProcessClientSocksPageProxyTest extends TwoProcessTestBase {
    private static final int SEARCH_KEY = ServiceSharingTeardownTest.SEARCH_KEY;
    private final static int SERVER_PORT = ServiceSharingTeardownTest.SERVER_PORT;
    private final static int CLIENT_PORT = ServiceSharingTeardownTest.CLIENT_PORT;
    private final static String LOCALHOST = ServiceSharingTeardownTest.LOCALHOST;

    private static Logger logger = Logger.getLogger(TwoProcessClientSocksPageProxyTest.class.getName());

    @BeforeClass
    public static void setUpClass() throws Exception {
        TwoProcessTestBase.startSelenium = false;
        TwoProcessTestBase.setUpClass();
    }

    @Before
    public void setupLogging() {
        logFinest(logger);
        logFinest(ProxyServer.logger);
        logFinest(DatagramConnection.logger);
        logFinest(OSSocksServer.logger);
    }

    @Test
    public void testProxyFunctionality() throws InterruptedException {
        /*
         * Test plan:
         * * Start OneSwarm (done in setupClass())
         * * Start a remote copy of oneswarm with this one as a friend
         * * Create a proxy server as a shared service in the local instance.
         * * Expose a client on the remote instance linked to the local proxy.
         * * Load page in the local instance using the remote client as a proxy.
         * * Verify connectivity.
         */

        try {
            tellLocalToShareService(SEARCH_KEY, SERVER_PORT);
            // Register the client service
            localOneSwarm.getCoordinator().addCommand(
                    "inject edu.washington.cs.oneswarm.test.integration.ServiceSharingExperiment");
            localOneSwarm.getCoordinator().addCommand(
                    "expose_socks " + SEARCH_KEY + " " + CLIENT_PORT);
            Thread.sleep(5000);
            doLoadPage(CLIENT_PORT);
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.toString());
            fail();
        } finally {
            logger.info("End PageProxyTest()");
        }
    }

    private void tellLocalToShareService(long searchKey, int port) throws InterruptedException {
        ExperimentalHarnessManager.get().enqueue(new String[] {
                "inject edu.washington.cs.oneswarm.test.integration.ServiceSharingExperiment",
                "share_socks socks " + searchKey + " 127.0.0.1 " + port
        });
    }

    protected void doLoadPage(int port) throws ClientProtocolException, IOException {
        URL target = new URL("http://www.google.com");
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(LOCALHOST, CLIENT_PORT));
        URLConnection conn = target.openConnection(proxy);
        InputStreamReader isr = new InputStreamReader(conn.getInputStream());
        BufferedReader br = new BufferedReader(isr);

        Boolean valid = false;
        String line = br.readLine();
        while (line != null) {
            if (line.indexOf("google") > -1) {
                valid = true;
                break;
            }
            line = br.readLine();
        }
        assert (valid == true);
    }

    /** Boilerplate code for running as executable. */
    public static void main(String[] args) throws Exception {
        TestUtils.swtCompatibleTestRunner(TwoProcessClientSocksPageProxyTest.class);
    }
}
