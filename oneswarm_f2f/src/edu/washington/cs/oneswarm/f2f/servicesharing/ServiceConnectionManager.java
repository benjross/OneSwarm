package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection.OverlayRegistrationError;

/**
 * This class manages active service connections (ServiceChannelEndpoints.)
 * Each channel can be used for multiple connections, such that a client can
 * initiate follow-on connections to a service without re-executing a search.
 * 
 * @author willscott
 * 
 */
public class ServiceConnectionManager implements ServiceChannelEndpointDelegate {
    // Singleton Pattern.
    private final static ServiceConnectionManager instance = new ServiceConnectionManager();
    public final static Logger logger = Logger.getLogger(ServiceConnectionManager.class.getName());

    private ServiceConnectionManager() {
    }

    public static ServiceConnectionManager getInstance() {
        return instance;
    }

    // Both lists are keyed by service id. One ServiceChannelEndpoint may exist
    // for each friend connection, one ServiceConnection should exist for each
    // active connection (client or sever).
    private final HashMap<Long, List<ServiceChannelEndpoint>> connections = new HashMap<Long, List<ServiceChannelEndpoint>>();
    private final HashMap<Long, List<ServiceConnection>> services = new HashMap<Long, List<ServiceConnection>>();

    /**
     * Create the ServiceChanelEndpoint associated with a given friend
     * connection.
     * 
     * @param nextHop
     *            The friend connection.
     * @param search
     *            The service Key.
     * @param response
     *            The remote response received through the given friend
     *            connection.
     * @param outgoing
     *            True if this is a client connection.
     * @return The service endpoint for communicating service data to this
     *         friend.
     */
    public ServiceChannelEndpoint createChannel(FriendConnection nextHop, OSF2FHashSearch search,
            OSF2FHashSearchResp response, boolean outgoing) {
        ServiceChannelEndpoint channel = new ServiceChannelEndpoint(nextHop, search,
                response, outgoing);
        try {
            nextHop.registerOverlayTransport(channel);
        } catch (OverlayRegistrationError e) {
            logger.warning("got an error when registering outgoing transport: " + e.getMessage());
            return channel;
        }

        this.addChannel(channel);
        return channel;
    }

    /**
     * Begin tracking a ServiceChannelEndpoint for connection attempts.
     * 
     * @param channel
     *            The service channel endpoint to track.
     */
    private void addChannel(ServiceChannelEndpoint channel) {
        logger.fine("Network Channel registered with Connection Manager");
        Long key = channel.getServiceKey();
        if (!this.connections.containsKey(key)) {
            registerKey(key);
        }
        if (this.connections.get(key).contains(channel)) {
            logger.info("Attempting to register existing channel:" + channel);
            return;
        }
        this.connections.get(key).add(channel);
        channel.addDelegate(this, (short) -1);
        if (this.services.get(key).size() > 0) {
            for (ServiceConnection service : this.services.get(key)) {
                if (service.getSearchKey() == channel.getSearchKey()) {
                    logger.finest("Channel added to existing service: " + service.getDescription());
                    service.addChannel(channel);
                }
            }
        }
    }

    private void registerKey(Long key) {
        List<ServiceChannelEndpoint> channel = new ArrayList<ServiceChannelEndpoint>();
        this.connections.put(key, channel);
        List<ServiceConnection> service = new ArrayList<ServiceConnection>();
        this.services.put(key, service);
    }

    /**
     * Get the active oneswarm connections associated with a given service.
     * 
     * @param key
     *            The service key.
     * @return A collection of endpoints connected to the service identified by
     *         key.
     */
    public Collection<ServiceChannelEndpoint> getChannelsForService(long key) {
        return this.connections.get(Long.valueOf(key));
    }

    /* ServiceChannelEndpointDelegate implementation. */
    @Override
    public void channelDidClose(ServiceChannelEndpoint sender) {
        Long key = sender.getServiceKey();
        if (!this.connections.containsKey(key)) {
            logger.info("Attempting to deregister channel for unknown service.");
            return;
        }
        synchronized (this.connections) {
            List<ServiceChannelEndpoint> list = this.connections.get(key);
            if (list == null) {
                return;
            }
            list.remove(sender);
            if (list.size() == 0) {
                logger.fine("All service connections closed for key " + key);
                this.connections.remove(key);
            }
        }
    }

    @Override
    public void channelDidConnect(ServiceChannelEndpoint sender) {
    }

    @Override
    public void channelIsReady(ServiceChannelEndpoint sender) {
    }

    @Override
    public boolean channelGotMessage(ServiceChannelEndpoint sender, OSF2FServiceDataMsg msg) {
        // Alert the service manager when a new flow is established.
        if (msg.isSyn()) {
            logger.fine("New Flow Established over " + sender.getChannelId());
            long serviceKey = sender.getServiceKey();
            SharedService ss = ServiceSharingManager.getInstance().getSharedService(serviceKey);
            if (ss == null) {
                return false;
            }
            List<ServiceConnection> existing = services.get(serviceKey);
            short subchannel = 0;
            if (existing == null) {
                services.put(serviceKey, new ArrayList<ServiceConnection>());
            } else {
                for (ServiceConnection c : existing) {
                    if (c.subchannelId == msg.getSubchannel()
                            && c.getSearchKey() == sender.getSearchKey()) {
                        // Ignore duplicate syn messages - the connection will
                        // handle it directly.
                        return false;
                    }
                }
                subchannel = msg.getSubchannel();
            }
            NetworkConnection outgoingConnection = ss.createConnection();
            ServiceConnection c = new ServiceConnection(false, subchannel, sender.getSearchKey(),
                    outgoingConnection);
            this.services.get(serviceKey).add(c);
            for (ServiceChannelEndpoint channel : this.getChannelsForService(serviceKey)) {
                if (channel.getSearchKey() == sender.getSearchKey()) {
                    c.addChannel(channel);
                }
            }
            c.channelGotMessage(sender, msg);
            return true;
        }
        if (msg.isRst()) {
            logger.fine("RST message received.");
            List<ServiceConnection> existing = services.get(sender.getServiceKey());
            if (existing != null) {
                for (ServiceConnection c : existing) {
                    // TODO(willscott): Also need check here to differentiate
                    // distinct clients.
                    if (c.subchannelId == msg.getSubchannel()
                            && c.getSearchKey() == sender.getSearchKey()) {
                        c.closeUponReading(msg.getSequenceNumber());
                        break;
                    }
                }
            }
            return true;
        }
        return false;
    }

    public boolean requestService(NetworkConnection incomingConnection, long serverSearchKey) {
        // Create a new sub flow if channels exist, or note the request for when
        // one does.
        Collection<ServiceChannelEndpoint> channels = this.getChannelsForService(serverSearchKey);
        if (channels != null && channels.size() > 0) {
            short subchannel = (short) services.get(serverSearchKey).size();
            long searchKey = channels.iterator().next().getSearchKey();
            ServiceConnection c = new ServiceConnection(true, subchannel, searchKey,
                    incomingConnection);
            for (ServiceChannelEndpoint channel : channels) {
                c.addChannel(channel);
            }
            services.get(serverSearchKey).add(c);
            logger.fine("Service requested - existing channel found. Accepted.");
            return true;
        } else {
            logger.fine("Service requested - existing channel not present. Search Needed.");
            return false;
        }
    }

    @Override
    public boolean writesMessages() {
        return false;
    }

}
