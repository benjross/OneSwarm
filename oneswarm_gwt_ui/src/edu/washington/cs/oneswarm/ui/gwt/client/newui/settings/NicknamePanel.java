package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;

class NicknamePanel  extends SettingsPanel {
    TextBox nickname_box = new TextBox();
    
    public NicknamePanel() {
        super();

        HorizontalPanel h = new HorizontalPanel();

        nickname_box.setText("...");
        
        Label l = new Label(msg.settings_exitpolicy_nickname_label());  // TODO(ben) msg.settings_admin_set_name()
        h.add(l);
        h.add(nickname_box);
        nickname_box.setWidth("55px");
        
        nickname_box.setWidth("55px");
        super.add(h);
        //this.add(warning);
        //super.setWidth("100%");
        
        OneSwarmRPCClient.getService().getNickname( 
                new AsyncCallback<String>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(String result) {
                        nickname_box.setText(result);
                    }
                });
    }

    @Override
    public void sync() {
        OneSwarmRPCClient.getService().setNickname(nickname_box.getText(), new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
                caught.printStackTrace();
            }
            @Override
            public void onSuccess(Void result) {
                System.out.println("new nickname set succesfully");   
            }
        });  
    }

    @Override
    public
    String validData() {
        return null;
    }
}
