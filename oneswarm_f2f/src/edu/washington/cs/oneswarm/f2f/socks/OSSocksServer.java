/**
 * SOCKS 4, 4a and 5 server implementation that accepts connections before handing the channel to OneSwarm.
 */

package edu.washington.cs.oneswarm.f2f.socks;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

public class OSSocksServer implements Runnable {
    public final static Logger logger = Logger.getLogger(OSSocksServer.class.getName());

    private static final int BUFFER_SIZE = 1024;
    private final int localPort;
    private ServerSocketChannel serverSocket;

    public OSSocksServer(int localPort) {
        this.localPort = localPort;
    }

    @Override
    public void run() {
        try {
            serverSocket = ServerSocketChannel.open();
            serverSocket.socket().bind(new InetSocketAddress("localhost", localPort));
            
            while (true) {
                try {
                    SocketChannel incomingConnection = serverSocket.accept();
                    logger.fine("Accepted connection via SOCKS from "
                            + incomingConnection.socket().getRemoteSocketAddress());
                    new Thread(new OSSocksServerThread(incomingConnection, BUFFER_SIZE)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
