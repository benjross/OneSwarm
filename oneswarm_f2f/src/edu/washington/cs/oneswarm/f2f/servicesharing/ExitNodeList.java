package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.gudy.azureus2.core3.config.COConfigurationManager;

public class ExitNodeList {
    // Singleton Pattern.
    private final static ExitNodeList instance = new ExitNodeList();
    private static final String LOCAL_SERVICE_KEY_CONFIG_KEY = "DISTINGUISHED_SHARED_SERVICE_KEY";
    private static final long KEEPALIVE_INTERVAL = 55 * 60 * 1000;
    // TODO (nick) decide where this should be saved / edited by user... Azureus
    // Conf with UI in Ben's setting panel?
    // TODO (nick) should it publish to one, or a list of servers?
    private static String ExitNodeDirectoryUrl;

    private final List<ExitNodeInfo> exitNodeList;
    private final Map<Long, ExitNodeInfo> localSharedExitServices;

    private ExitNodeList() {
        ExitNodeDirectoryUrl = "http://127.0.0.1:7888/";
        // TODO (nick) Uncomment to enable regular updates once debugging is
        // done.

        // Timer keepAliveRegistrations = new Timer();
        // keepAliveRegistrations.schedule(new TimerTask() {
        // @Override
        // public void run() {
        // ExitNodeList.this.registerExitNodes();
        // // Check the response for errors.
        // }
        // }, 0, KEEPALIVE_INTERVAL);
        this.exitNodeList = new LinkedList<ExitNodeInfo>();
        this.localSharedExitServices = new HashMap<Long, ExitNodeInfo>();
    }

    public static ExitNodeList getInstance() {
        return instance;
    }

    public void addNodes(ExitNodeInfo[] exitNodes) {
        for (ExitNodeInfo server : exitNodes) {
            this.exitNodeList.add(server);
        }
        sortAndSave();
    }

    public void addNode(ExitNodeInfo exitNode) {
        exitNodeList.add(exitNode);
        sortAndSave();
    }

    private void sortAndSave() {
        Collections.sort(exitNodeList);
        // TODO (nick) serialize and save list to disk
    }

    public ExitNodeInfo pickServer(String url, int port) {
        for (ExitNodeInfo server : exitNodeList) {
            if (server.allowsConnectionTo(url, port)) {
                return server;
            }
        }
        return null;
    }

    /**
     * Get (and generate if it does not yet exist) a distinguished key for this
     * machine's locally shared service.
     * 
     * @return The local shared service key.
     */
    public long getLocalServiceKey() {
        long serviceKey = COConfigurationManager.getLongParameter(LOCAL_SERVICE_KEY_CONFIG_KEY, 0L);
        if (serviceKey == 0) {
            Random r = new Random();
            serviceKey = r.nextLong();
            serviceKey = Math.abs(serviceKey);
            COConfigurationManager.setParameter(LOCAL_SERVICE_KEY_CONFIG_KEY, serviceKey);
        }
        return serviceKey;
    }

    /**
     * Reset the locally shared service key, in case the node wishes to change
     * identity.
     */
    public void resetLocalServiceKey() {
        COConfigurationManager.setParameter(LOCAL_SERVICE_KEY_CONFIG_KEY, 0L);
    }

    public void setExitNodeSharedService(ExitNodeInfo exitNode) {
        localSharedExitServices.put(exitNode.getId(), exitNode);
    }

    public void removeExitNodeSharedService(long serviceId) {
        if (isExitNodeSharedService(serviceId)) {
            localSharedExitServices.remove(serviceId);
        }
    }

    public boolean isExitNodeSharedService(long serviceId) {
        return localSharedExitServices.containsKey(serviceId);
    }

    public boolean allowLocalExitConnection(long serviceId, String address, int port) {
        if (isExitNodeSharedService(serviceId)) {
            return localSharedExitServices.get(serviceId).allowsConnectionTo(address, port);
        } else {
            return false;
        }
    }

    public void registerExitNodes() throws IOException {

        // StringBuilder output = new StringBuilder();
        // for (ExitNodeInfo node : localSharedExitServices.values()) {
        // // TODO (nick) Remove newline
        // output.append(node.fullXML() + "\n");
        // }
        // output = XMLConstants.tag(XMLConstants.EXIT_NODE_LIST,
        // output).insert(0, XMLConstants.HEADER);
        //
        // StringBuilder response = sendToServer(ExitNodeDirectoryUrl +
        // "?action=register", output);
        //
        // //TODO (nick) Debug only
        // System.out.println(response.toString());
        //
        // //TODO (nick) un psuedo
        // output = new StringBuilder();
        // for(resp that contains error){
        //
        // }
    }

    private InputStream sendToServer(String serverUrl, StringBuilder body) throws IOException {
        StringBuilder response = new StringBuilder();
        URL server = new URL(serverUrl);
        HttpURLConnection req = (HttpURLConnection) server.openConnection();
        req.setDoInput(true);
        req.setDoOutput(true);
        req.setUseCaches(false);
        req.setRequestProperty("Content-Type", "text/xml");

        OutputStream out = req.getOutputStream();

        try {
            out.write(body.toString().getBytes(XMLConstants.ENCODING));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        out.flush();

        return req.getInputStream();
    }
}
