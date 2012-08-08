package edu.washington.cs.oneswarm.f2f.sharedservices;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import edu.washington.cs.oneswarm.f2f.servicesharing.XML;

public class ExitNodeDB {
    private static final String DATABASE_FILE = "/home/nick/knownExitNodes.xml";
    // The max age that a ExitNode registration may have befire it is deleted.
    private static final int MAX_AGE = 60 * 60 * 1000;
    private static final int GRACE_PERIOD = 60 * 1000;
    private final Map<Long, ExitNodeRecord> registeredKeys;
    private PriorityQueue<ExitNodeRecord> readableExitNodeList;
    private final PriorityQueue<ExitNodeRecord> mutableExitNodeList;

    public ExitNodeDB() throws ParserConfigurationException, SAXException, IOException {
        mutableExitNodeList = new PriorityQueue<ExitNodeRecord>();
        registeredKeys = new HashMap<Long, ExitNodeRecord>();
        try {
            File dbFile = new File(DATABASE_FILE);
            if (!dbFile.exists()) {
                dbFile.createNewFile();
                FileWriter file = new FileWriter(dbFile);
                file.write(XML.HEADER + XML.tag(XML.EXIT_NODE_LIST, ""));
                file.flush();
                file.close();
            }
            readFromFile(dbFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Thread dbClean = new Thread(new DBCleaner());
        dbClean.setDaemon(true);
        dbClean.start();
    }

    public void add(ExitNodeRecord node) {
        String errors = node.checkForErrors(true);
        if (!errors.equals("")) {
            throw new IllegalArgumentException(errors);
        }

        if (registeredKeys.containsKey(node.getId())) {
            ExitNodeRecord oldNode = registeredKeys.get(node.getId());
            if (!oldNode.getPublicKey().equals(node.getPublicKey())) {
                throw new IllegalArgumentException("Duplicate Key Used.");
            }
            remove(oldNode);
        }
        synchronized (mutableExitNodeList) {
            mutableExitNodeList.add(node);
        }
        registeredKeys.put(node.getId(), node);
    }

    void remove(ExitNodeRecord node) {
        synchronized (mutableExitNodeList) {
            mutableExitNodeList.remove(node);
        }

        registeredKeys.remove(node.getId());
    }

    public void checkIn(ExitNodeRecord node) {
        if (!registeredKeys.containsKey(node.getId())) {
            throw new IllegalArgumentException("Service ID is not registered.");
        }

        String errors = node.checkForErrors(false);
        if (!errors.equals("")) {
            throw new IllegalArgumentException("Invalid Node: \n" + errors);
        }

        ExitNodeRecord oldNode = registeredKeys.get(node.getId());
        if (node.signature.equals(oldNode.signature)
                && oldNode.getPublicKey().equals(node.getPublicKey())) {
            registeredKeys.get(node.getId()).checkIn();
        } else {
            throw new IllegalArgumentException(
                    "Public Key or Signature does not match existing registration.");
        }
    }

    void saveEdits() {
        PriorityQueue<ExitNodeRecord> newReadable = new PriorityQueue<ExitNodeRecord>();
        synchronized (mutableExitNodeList) {
            for (ExitNodeRecord node : mutableExitNodeList) {
                newReadable.add(node);
            }
        }
        readableExitNodeList = newReadable;
        saveToFile(new File(DATABASE_FILE));
    }

    public String getUpdatesSince(long lastUpdateTime) {
        StringBuilder sb = new StringBuilder();
        for (ExitNodeRecord node : readableExitNodeList) {
            if (node.createdTime > lastUpdateTime) {
                sb.append(node);
            } else {
                break;
            }
        }
        return sb.toString();
    }

    @Override
    @Deprecated
    public String toString() {
        return getUpdatesSince(0l);
    }

    private void saveToFile(File file) {
        try {
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), "UTF8");
            out.write(XML.HEADER + XML.tag(XML.EXIT_NODE_LIST, getUpdatesSince(0)));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readFromFile(File file) throws ParserConfigurationException, SAXException,
            IOException {
        FileInputStream in = new FileInputStream(file);
        List<ExitNodeRecord> savedNodes = new Parser(in).parseAsExitNodeList(0);
        for (ExitNodeRecord node : savedNodes) {
            add(node);
        }
        saveEdits();
        in.close();
    }

    private class DBCleaner implements Runnable {

        @Override
        public void run() {
            long lastRun = 0l;
            while (true) {
                synchronized (mutableExitNodeList) {
                    lastRun = System.currentTimeMillis();
                    Iterator<ExitNodeRecord> nodes = mutableExitNodeList.iterator();
                    while (nodes.hasNext()) {
                        ExitNodeRecord node = nodes.next();
                        // Check if the Node is too old with a grace period
                        if (lastRun > node.lastCheckinTime + MAX_AGE + GRACE_PERIOD) {
                            nodes.remove();
                        }
                    }
                }
                saveEdits();
                while (System.currentTimeMillis() < lastRun + (MAX_AGE / 2)) {
                    try {
                        Thread.sleep(10 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}