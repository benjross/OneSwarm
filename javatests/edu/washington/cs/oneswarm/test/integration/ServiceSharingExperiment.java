package edu.washington.cs.oneswarm.test.integration;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import edu.washington.cs.oneswarm.f2f.ExperimentInterface;
import edu.washington.cs.oneswarm.f2f.servicesharing.ExitNodeInfo;
import edu.washington.cs.oneswarm.f2f.servicesharing.ExitNodeList;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;
import edu.washington.cs.oneswarm.f2f.socks.OSSocksServer;
import edu.washington.cs.oneswarm.f2f.socks.SocksCommandHandler;

public class ServiceSharingExperiment implements ExperimentInterface {
    private static Logger logger = Logger.getLogger(ServiceSharingExperiment.class.getName());
    private static Thread oss = null;

    @Override
    public String[] getKeys() {
        return new String[] { "share_service", "expose_client", "clean_services", "expose_socks", "share_socks" };
    }

    @Override
    public void execute(String command) {
        logger.info("sse asked to execute " + command);
        String[] toks = command.toLowerCase().split("\\s+");
        if (toks[0].equals("share_service")) {
            String name = toks[1];
            long searchKey = Long.parseLong(toks[2]);
            String address = toks[3];
            int port = Integer.parseInt(toks[4]);
            // final OSF2FMain f2fMain = OSF2FMain.getSingelton();
            ServiceSharingManager.getInstance().registerSharedService(searchKey, name,
                    new InetSocketAddress(address, port));
            logger.info("adding service: "
                    + ServiceSharingManager.getInstance().getSharedService(searchKey));
            logger.warning("SERVICE ADDED for " + name + " (" + searchKey + ") " + " "
                    + address + ":" + port);
        } else if (toks[0].equals("expose_client")) {
            String name = toks[1];
            long key = Long.parseLong(toks[2]);
            int port = Integer.parseInt(toks[3]);
            ServiceSharingManager.getInstance().registerClientService(name, port, key);
            logger.info("adding client: "
                    + ServiceSharingManager.getInstance().getClientService(key));
        } else if (toks[0].equals("expose_socks")) {
            long key = Long.parseLong(toks[1]);
            int port = Integer.parseInt(toks[2]);

            // Set up policy.
            String[] exitPolicy = new String[] {"accept *:*"};
            ExitNodeInfo info = new ExitNodeInfo("socks_server", key, 0, exitPolicy, null, null);
            info.setIpAddr(new InetSocketAddress("localhost", 0).getAddress().getAddress());
            ExitNodeList.getInstance().addNode(info);
            // Start the client listener.
            SocksCommandHandler.Interface handler = new SocksCommandHandler.HandoffToOneSwarm();
            OSSocksServer server = new OSSocksServer(port, handler);
            oss = new Thread(server);
            oss.setDaemon(true);
            oss.setName("socks client listener");
            oss.start();
        } else if (toks[0].equals("share_socks")) {
            String name = toks[1];
            long key = Long.parseLong(toks[2]);
            String address = toks[3];
            int port = Integer.parseInt(toks[4]);
            String[] exitPolicy = new String[] {"accept *:*"};
            ExitNodeInfo info = new ExitNodeInfo(name, key, 0, exitPolicy, null, null);
            ServiceSharingManager.getInstance().registerExitNodeSharedService(key, name, 
                    new InetSocketAddress(address, port), info);
        } else if (toks[0].equals("clean_services")) {
            if (oss != null) {
                oss.stop();
                oss = null;
            }
            ServiceSharingManager.getInstance().clearLocalServices();
        } else {
            logger.warning("Unknown Service command: " + toks[0]);
            return;
        }
    }

    @Override
    public void load() {
        return;
    }

}
