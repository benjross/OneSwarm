package edu.washington.cs.oneswarm.f2f.sharedservices;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.thread.QueuedThreadPool;
import org.xml.sax.SAXParseException;

import edu.washington.cs.oneswarm.f2f.servicesharing.XML;

/**
 * Directory Server This class maintains a Database of ExitNodeInfo and serves
 * it to OneSwarm instances that need
 * 
 * @author nick
 */
public class CCServer extends Thread {
    private static final String PARAM_ACTION = "action";
    private static final String CHECK_IN = "checkin";
    private static final String REGISTER = "register";
    private static final String LIST_NODES = "list";
    // Add incremental update functionality and make a spec for Nodes deleted
    // since last update
    private static final String LAST_UPDATE = "lastUpdate";

    final Server s = new Server();
    ExitNodeDB db;

    private CCServer(int port) {
        try {
            db = new ExitNodeDB();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setName("Exit Node Directory Web Server");
        setDaemon(true);

        /* Define thread pool for the web server. */
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(2);
        threadPool.setMaxThreads(250);
        threadPool.setName("Jetty thread pool");
        threadPool.setDaemon(true);
        s.setThreadPool(threadPool);

        /* Define connection statistics for the web server. */
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setMaxIdleTime(10000);
        connector.setLowResourceMaxIdleTime(5000);
        connector.setAcceptQueueSize(100);
        connector.setHost(null);
        connector.setPort(port);
        s.addConnector(connector);

        /* Define handlers for the web server. */
        s.addHandler(new AbstractHandler() {
            @Override
            public void handle(String target, HttpServletRequest req, HttpServletResponse resp,
                    int dispatch) throws IOException, ServletException {

                Request request = (req instanceof Request) ? (Request) req : HttpConnection
                        .getCurrentConnection().getRequest();

                // Map of GET method parameters
                Map<String, String> parameters = new HashMap<String, String>();
                for (Object keyObj : request.getParameterMap().keySet()) {
                    String key = (keyObj instanceof String) ? (String) keyObj : "";
                    String value = request.getParameter(key);
                    if (key != null && value != null) {
                        parameters.put(key, value);
                    }
                }

                // Check for the action parameter and do action
                if (parameters.containsKey(PARAM_ACTION)) {
                    String action = parameters.get(PARAM_ACTION);
                    String response = "";
                    if (action.equals(CHECK_IN) || action.equals(REGISTER)) {
                        try {
                            List<ExitNodeRecord> newNodes = new Parser(request.getInputStream())
                                    .parseAsExitNodeList(0);
                            boolean justCheckIn = action.equals(CHECK_IN);
                            for (ExitNodeRecord node : newNodes) {
                                try {
                                    if (justCheckIn) {
                                        db.checkIn(node);
                                    } else {
                                        db.add(node);
                                    }
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                    // These are our checks for correctness such
                                    // as "Duplicate Key Used"
                                    response = XML.tag(
                                            XML.EXIT_NODE,
                                            XML.tag(XML.SERVICE_ID, "" + node.getId())
                                                    + XML.tag(XML.NODE_ERROR, e.getMessage()));
                                }
                            }
                            db.saveEdits();
                        } catch (SAXParseException e) {
                            e.printStackTrace();
                            // These are XML errors such as
                            // "Unexpected End of File"
                            response = XML.tag(
                                    XML.GENERAL_ERROR,
                                    "Error on line " + e.getLineNumber() + ", column "
                                            + e.getColumnNumber() + ": " + e.getMessage());
                        } catch (Exception e) {
                            e.printStackTrace();
                            response = XML.tag(XML.GENERAL_ERROR, e.getMessage());
                        }
                    } else if (action.equals(LIST_NODES)) {
                        long lastUpdate = parameters.containsKey(LAST_UPDATE) ? Long
                                .parseLong(parameters.get(LAST_UPDATE)) : 0l;
                        response = db.getUpdatesSince(lastUpdate);
                    } else {
                        response = XML.tag(XML.GENERAL_ERROR, "Invalid Operation");
                    }

                    request.setHandled(true);
                    response = XML.HEADER + XML.tag(XML.EXIT_NODE_LIST, response);
                    resp.getOutputStream().write(response.getBytes());
                }
            }
        });

        // TODO (nick) Remove console
        new Thread(new TestConsole()).start();
    }

    @Override
    public void run() {
        try {
            s.start();
            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            s.destroy();
        }
    }

    public static void main(String[] args) {
        try {
            int port = 7888;
            new CCServer(port).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Server getServer() {
        return s;
    }

    // TODO (nick) Remove Console
    private class TestConsole implements Runnable {
        @Override
        public void run() {
            // TESTING CONSOLE
            // TODO (nick) remove
            Scanner in = new Scanner(System.in);
            while (true) {
                System.out.print(">>> ");
                String[] input = in.nextLine().split(" ");

                if (input[0].equalsIgnoreCase("add")) {
                    try {
                        FileInputStream file = new FileInputStream(input[1]);
                        List<ExitNodeRecord> newNodes = new Parser(file).parseAsExitNodeList(1);
                        in.close();
                        for (ExitNodeRecord node : newNodes) {
                            db.add(node);
                        }
                        db.saveEdits();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (input[0].equalsIgnoreCase("print")) {
                    System.out.println(XML.tag(XML.EXIT_NODE_LIST, db.getUpdatesSince(0)));
                } else if (input[0].equalsIgnoreCase("clear")) {
                    new File("/home/nick/knownExitNodes.xml").delete();
                    try {
                        db = new ExitNodeDB();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("Cleared.");
                } else if (input[0].equalsIgnoreCase("exit")) {
                    break;
                }
            }
            // END TEST CONSOLE
        }
    }
}
