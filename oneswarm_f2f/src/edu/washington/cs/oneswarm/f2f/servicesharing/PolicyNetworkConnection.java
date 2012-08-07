package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import org.apache.commons.lang.NotImplementedException;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.TransportBase;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;

import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageDecoder;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageEncoder;

public class PolicyNetworkConnection implements NetworkConnection {
    public final static Logger logger = Logger.getLogger(PolicyNetworkConnection.class.getName());
    /**
     * Read an initial header from an incoming stream before creating an actual
     * network connection. Header looks like: port * 2, address length, address.
     */
    private NetworkConnection connection;
    private ConnectionListener pendingListener;
    private final PolicyHeader request;
    private final long serviceId;
    private static final byte SS = 0x43;

    private class PolicyHeader {
        public int port = 0;
        public String host = null;
        public int toRead = 3;
        public boolean lengthRead = false;
    }

    // TODO: construct with policy.
    public PolicyNetworkConnection(long serviceId) {
        request = new PolicyHeader();
        this.serviceId = serviceId;
    }

    protected void completeHeader(Message message, boolean manual_listener_notify) {
        logger.info("Policy network connection header completed.");
        if (ExitNodeList.getInstance().allowLocalExitConnection(serviceId, request.host,
                request.port)) {
            logger.info("Connection allowed to " + request.host + ":" + request.port);
            InetSocketAddress address = new InetSocketAddress(request.host, request.port);
            ConnectionEndpoint target = new ConnectionEndpoint(address);
            target.addProtocol(new ProtocolEndpointTCP(address));
            this.connection = NetworkManager.getSingleton().createConnection(target,
                    new RawMessageEncoder(), new RawMessageDecoder(), false, false, new byte[0][0]);
            if (this.pendingListener != null) {
                this.connection.connect(true, this.pendingListener);
            }
            if (message.getData().length > 0) {
                this.connection.getOutgoingMessageQueue().addMessage(message, manual_listener_notify);
            }
        } else {
            logger.info("Connection denied to " + request.host + ":" + request.port);
            if (this.pendingListener != null) {
                this.pendingListener.connectFailure(new Throwable("Connection Denied by policy"));
            }
        }
    }

    @Override
    public OutgoingMessageQueue getOutgoingMessageQueue() {
        if (connection == null) {
            return new OutgoingMessageQueue() {
                @Override
                public void setTransport(Transport _transport) {
                    throw new NotImplementedException();
                }

                @Override
                public void setTrace(boolean on) {
                    throw new NotImplementedException();
                }

                @Override
                public void setEncoder(MessageStreamEncoder stream_encoder) {
                    throw new NotImplementedException();
                }

                @Override
                public void removeMessagesOfType(Message[] message_types,
                        boolean manual_listener_notify) {
                    throw new NotImplementedException();
                }

                @Override
                public boolean removeMessage(Message message, boolean manual_listener_notify) {
                    throw new NotImplementedException();
                }

                @Override
                public void registerQueueListener(MessageQueueListener listener) {
                    throw new NotImplementedException();
                }

                @Override
                public void notifyOfExternallySentMessage(Message message) {
                    throw new NotImplementedException();
                }

                @Override
                public boolean isDestroyed() {
                    throw new NotImplementedException();
                }

                @Override
                public boolean hasUrgentMessage() {
                    throw new NotImplementedException();
                }

                @Override
                public int getTotalSize() {
                    throw new NotImplementedException();
                }

                @Override
                public String getQueueTrace() {
                    throw new NotImplementedException();
                }

                @Override
                public int getPercentDoneOfCurrentMessage() {
                    throw new NotImplementedException();
                }

                @Override
                public int getMssSize() {
                    throw new NotImplementedException();
                }

                @Override
                public MessageStreamEncoder getEncoder() {
                    throw new NotImplementedException();
                }

                @Override
                public void flush() {
                    throw new NotImplementedException();
                }

                @Override
                public void doListenerNotifications() {
                    throw new NotImplementedException();
                }

                @Override
                public void destroy() {
                    throw new NotImplementedException();
                }

                @Override
                public int deliverToTransport(int max_bytes, boolean manual_listener_notify)
                        throws IOException {
                    throw new NotImplementedException();
                }

                @Override
                public void cancelQueueListener(MessageQueueListener listener) {
                    throw new NotImplementedException();
                }

                private Message normalize(Message m) {
                    DirectByteBuffer[] data = m.getData();
                    int totalLength = 0;
                    for (DirectByteBuffer d : data) {
                        totalLength += d.remaining(SS);
                    }
                    DirectByteBuffer concatination = DirectByteBufferPool.getBuffer(SS, totalLength);
                    for (DirectByteBuffer d : data) {
                        concatination.put(SS, d);
                    }
                    concatination.flip(SS);
                    return new DataMessage(concatination);
                }

                /**
                 * Construct network connection from an initial message.
                 */
                @Override
                public void addMessage(Message message, boolean manual_listener_notify) {
                    if (connection != null) {
                        logger.warning("PNC outgoing message queue in use after handoff.");
                        connection.getOutgoingMessageQueue().addMessage(message, manual_listener_notify);
                        return;
                    }
                    Message newMessage = normalize(message);
                    DirectByteBuffer[] data = newMessage.getData();
                    if (data.length == 0) {
                        return;
                    }
                    
                    DirectByteBuffer top = data[0];
                    if (top.remaining(SS) == 0) {
                        return;
                    }
                    
                    int remaining = PolicyNetworkConnection.this.request.toRead;
                    if (top.remaining(SS) < remaining) {
                        throw new NotImplementedException();
                    }
                    if (!PolicyNetworkConnection.this.request.lengthRead) {
                        int port = top.getShort(SS);
                        logger.finer("Port read as " + port);
                        PolicyNetworkConnection.this.request.port = port;
                        PolicyNetworkConnection.this.request.toRead = top.get(SS);
                        PolicyNetworkConnection.this.request.lengthRead = true;
                    }

                    remaining = PolicyNetworkConnection.this.request.toRead;
                    if (top.remaining(SS) == 0) {
                        return;
                    } else if (top.remaining(SS) < remaining) {
                        throw new NotImplementedException();
                    }
                    
                    byte[] addr = new byte[remaining];
                    top.get(SS, addr);
                    PolicyNetworkConnection.this.request.host = new String(addr);
                    logger.finer("Host read as " + new String(addr));
                    PolicyNetworkConnection.this.completeHeader(newMessage, manual_listener_notify);
                }
            };
        } else {
            return connection.getOutgoingMessageQueue();
        }
    }

    @Override
    public ConnectionEndpoint getEndpoint() {
        return connection.getEndpoint();
    }

    @Override
    public void notifyOfException(Throwable error) {
        connection.notifyOfException(error);
    }

    @Override
    public IncomingMessageQueue getIncomingMessageQueue() {
        return connection.getIncomingMessageQueue();
    }

    @Override
    public TransportBase getTransportBase() {
        return connection.getTransportBase();
    }

    @Override
    public int getMssSize() {
        return connection.getMssSize();
    }

    @Override
    public boolean isLANLocal() {
        return connection.isLANLocal();
    }

    @Override
    public void setUploadLimit(int limit) {
        connection.setUploadLimit(limit);
    }

    @Override
    public int getUploadLimit() {
        return connection.getUploadLimit();
    }

    @Override
    public void setDownloadLimit(int limit) {
        connection.setDownloadLimit(limit);
    }

    @Override
    public int getDownloadLimit() {
        return connection.getDownloadLimit();
    }

    @Override
    public LimitedRateGroup[] getRateLimiters(boolean upload) {
        return connection.getRateLimiters(upload);
    }

    @Override
    public void addRateLimiter(LimitedRateGroup limiter, boolean upload) {
        connection.addRateLimiter(limiter, upload);
    }

    @Override
    public void removeRateLimiter(LimitedRateGroup limiter, boolean upload) {
        connection.removeRateLimiter(limiter, upload);
    }

    @Override
    public String getString() {
        return connection.getString();
    }

    @Override
    public void connect(boolean high_priority, ConnectionListener listener) {
        if (connection == null) {
            this.pendingListener = listener;
        } else {
            connection.connect(high_priority, listener);
        }
    }

    @Override
    public void connect(ByteBuffer initial_outbound_data, boolean high_priority,
            ConnectionListener listener) {
        connection.connect(initial_outbound_data, high_priority, listener);
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    public void startMessageProcessing() {
        NetworkManager.getSingleton().startTransferProcessing( this );
        connection.startMessageProcessing();
    }

    @Override
    public void enableEnhancedMessageProcessing(boolean enable) {
        connection.enableEnhancedMessageProcessing(enable);
    }

    @Override
    public Transport detachTransport() {
        return connection.detachTransport();
    }

    @Override
    public Transport getTransport() {
        return connection.getTransport();
    }

    @Override
    public boolean isConnected() {
        if (connection == null) {
            return true;
        } else {
            return connection.isConnected();
        }
    }
}
