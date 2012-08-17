package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;


class ServiceStatisticsPanel extends SettingsPanel{
    

    private boolean newKeyPressed = false;
    
    public ServiceStatisticsPanel() {
        HorizontalPanel h = new HorizontalPanel();
        
        Button newServiceKeyButton = new Button("new service key"); //TODO(ben) msg.new_service_key_button()
        newServiceKeyButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
        newServiceKeyButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                newKeyPressed = true;
            }
        });
        
        Button statisticsButton = new Button("statistics"); //TODO(ben) msg.statistics_button()
        statisticsButton.addStyleName(OneSwarmCss.SMALL_BUTTON);

        
        statisticsButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                StatisticsDialog dlg = new StatisticsDialog();
                dlg.show();
                dlg.setVisible(false);
                dlg.setText("statistics"); //TODO(ben) msg.statisics_window_title()
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 125);
                dlg.setVisible(true);
            }
        });
        
        h.add(newServiceKeyButton);
        h.add(statisticsButton);
        
        super.add(h);
    }

    @Override
    public void sync() {
        if (newKeyPressed){
            OneSwarmRPCClient.getService().getNewServiceKey(new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    caught.printStackTrace();
                }

                @Override
                public void onSuccess(Void result) {
                    System.out.println("new service key successfully");
                }
            });
            newKeyPressed = false;
        }

    }

    @Override
    public
    String validData() {
       return null;
    }
    
    public class StatisticsDialog extends OneSwarmDialogBox {
        public StatisticsDialog(){
            super();
        }

    }
}
