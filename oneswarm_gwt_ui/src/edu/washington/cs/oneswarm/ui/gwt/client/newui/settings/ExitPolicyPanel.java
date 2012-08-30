package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;


import java.util.LinkedList;
import java.util.ListIterator;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;

public class ExitPolicyPanel extends SettingsPanel implements ClickHandler {
    RadioButton everythingButton = new RadioButton("group", msg.settings_exitpolicy_policy_everything());
    RadioButton localButton = new RadioButton("group", msg.settings_exitpolicy_policy_local());
    RadioButton safeButton = new RadioButton("group", msg.settings_exitpolicy_policy_safe());
    RadioButton customButton = new RadioButton("group", msg.settings_exitpolicy_policy_custom());
    TextArea exitNodes = new TextArea();


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

        OneSwarmRPCClient.getService().getExitPolicyStrings(
                new AsyncCallback<LinkedList<String>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    @Override
                    public void onSuccess(LinkedList<String> result) {
                        String button = result.poll();
                        if (button.equals("Safe")) {
                            safeButton.setValue(true);
                            exitNodes.setEnabled(false);
                        } else if (button.equals("Local")) {
                            localButton.setValue(true);
                            exitNodes.setEnabled(false);
                        } else if (button.equals("Everything")) {
                            everythingButton.setValue(true);
                            exitNodes.setEnabled(false);
                        } else {
                            customButton.setValue(true);
                            exitNodes.setEnabled(true);
                            result.addFirst(button);
                        }

                        nodeInput(result);

                        loadNotify();
                    }
                });

        super.add(buttons);
        super.add(addNodes);
    }

    private void nodeInput(LinkedList<String> result) {
        //nodes.setExitPolicy(string.split("\\r?\\n"));
        exitNodes.setText("");
        StringBuilder exitString = new StringBuilder();
        ListIterator<String> itr = result.listIterator();
        while(itr.hasNext()) {
            exitString.append(itr.next() + "\n"); //TODO(ben) add new line
        }
        exitNodes.setText(exitString.toString());
    }

    private void cleanUp() {
        //TODO(ben) a method to clean up the exit policy text area
    }

    @Override
    public void sync() {
        //cleanUp();
        OneSwarmRPCClient.getService().setExitNodeSharedService(exitNodes.getText(),
                new AsyncCallback<Void>() {
            @Override
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

    @Override
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
                    @Override
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    @Override
                    public void onSuccess(LinkedList<String> result) {
                        result.remove();
                        nodeInput(result);
                        System.out.println("exit policy loaded successfully");
                    }
                });

    }
}
