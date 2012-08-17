package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;


import java.util.LinkedList;
import java.util.ListIterator;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;

public class ExitPolicyPanel extends SettingsPanel implements ClickHandler {
    TextBox searchBox = new TextBox();
    RadioButton everythingButton = new RadioButton("group","everything");
    RadioButton localButton = new RadioButton("group","local");
    RadioButton safeButton = new RadioButton("group","safe");
    RadioButton customButton = new RadioButton("group","custom");
    TextArea exitNodes = new TextArea();
//    ExitNodeInfo exits;
    
    public ExitPolicyPanel(){
        
        HorizontalPanel buttons = new HorizontalPanel();
        
        everythingButton.addClickHandler(this);
        localButton.addClickHandler(this);
        safeButton.addClickHandler(this);
        customButton.addClickHandler(this);
        
        buttons.add(everythingButton);
        buttons.add(localButton);
        buttons.add(safeButton);
        buttons.add(customButton);
        
        HorizontalPanel addNodes = new HorizontalPanel();
        
        addNodes.add(exitNodes);
        
        HorizontalPanel search = new HorizontalPanel();
        
        searchBox.setText("...");
        Button searchButton = new Button("search"); //TODO(ben) msg.button_search() 
        searchButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
        
        Label searchLabel = new Label("search"); // TODO(ben) msg.settings_admin_search_nodes()
        
        search.add(searchLabel);
        search.add(searchBox);
        search.add(searchButton);

        OneSwarmRPCClient.getService().getExitPolicyStrings(
                new AsyncCallback<LinkedList<String>>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(LinkedList<String> result) {
                        nodeInput(result);
                        
                        loadNotify();
                    }
                });
        
        super.add(buttons);
        super.add(addNodes);
        super.add(search);
    }
    
    private void nodeInput(LinkedList<String> result) {
        //nodes.setExitPolicy(string.split("\\r?\\n"));
        exitNodes.setText("");
        StringBuilder exitString = new StringBuilder();
        ListIterator<String> itr = result.listIterator();
        while(itr.hasNext()) {
            exitString.append(itr.next() + ""); //TODO(ben) add new line
        }
        exitNodes.setText(exitString.toString());
    }
    
    private void cleanUp() {
        
    }
    
    @Override
    public void sync() {
        cleanUp();
        OneSwarmRPCClient.getService().setExitNodeSharedService(exitNodes.getText(), 
        new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
                caught.printStackTrace();
            }

            @Override
            public void onSuccess(Void result) {
                System.out.println("saved exit policy successfully");   
            }
        });
    }

    @Override
    public String validData() {
        return null;
    }
    
    public void onClick(ClickEvent event) {
        Object sender = event.getSource();
        if (sender.equals(customButton)) {
            exitNodes.setEnabled(true);
        } else {
            exitNodes.setEnabled(false);
        }
        
        OneSwarmRPCClient.getService().getPresetPolicy(
                ((CheckBox) sender).getText(), 
                new AsyncCallback<LinkedList<String>>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(LinkedList<String> result) {
                        nodeInput(result);
                        System.out.println("exit policy loaded successfully"); 
                    }
                });

    }
}
