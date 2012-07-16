package edu.washington.cs.oneswarm.ui.gwt.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ClientServiceInfo implements IsSerializable{
    
    public String serviceName;
    public long serviceID;
    
    //Used only by GWT for serialization
    @Deprecated
    public ClientServiceInfo(){
    }
    
    public ClientServiceInfo(long id, String name) {
        this.serviceID = id;
        this.serviceName = name;
    }
}
