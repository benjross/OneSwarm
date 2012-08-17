package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;

public class StatisticsPanel extends OneSwarmDialogBox{
    public StatisticsPanel(final StatisticsCallback statisticsCallback){
        super();
    }
    public interface StatisticsCallback {
        public void statisticsCanceled();

        public void statisticsCompleted(double rate);
    }
}
