package edu.washington.cs.oneswarm.ui.gwt.client.newui.sidebar;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.EntireUIRoot;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.ImageConstants;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.FriendPropertiesDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportWizard;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendList;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;

public class FriendListSidebarWidget extends SidebarWidget {
    private static OSMessages msg = OneSwarmGWT.msg;

    private static final String CSS_FRIEND_LIST_ELEMENT = "os-friendListElement";

    private final VerticalPanel content = new VerticalPanel();
    private final VerticalPanel friendListVP = new VerticalPanel();
    private final Map<String, FriendPanel> friendPanels = new HashMap<String, FriendPanel>();
    private boolean fullUpdateNeeded = true;
    private FriendPanel mSelectedFriend = null;

    private FriendInfoLite[] newFriendsList = new FriendInfoLite[0];
    private FriendInfoLite[] oldFriendsList = new FriendInfoLite[0];

    private final HTML friendRequestsPanel = new HTML();
    private Map<String, Integer> newFriendRequestCounts = new HashMap<String, Integer>();

    public FriendListSidebarWidget() {
        super(msg.friends_sidebar_header());

        friendListVP.setWidth("100%");

        friendRequestsPanel.setVisible(false);
        friendRequestsPanel.addStyleName("os-friendListRequestPanel");
        friendRequestsPanel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                OneSwarmDialogBox dlg = new FriendsImportWizard(newFriendRequestCounts);
                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Math.max(40, dlg.getPopupTop() - 200));
                dlg.setVisible(true);
            }
        });

        content.add(friendRequestsPanel);
        content.add(friendListVP);

        // the menu below the friend list
        MenuBar footerMenu = new MenuBar();
        footerMenu.addStyleName(OneSwarmCss.SidebarWidget.FOOTER_MENU_BAR);
        footerMenu.setWidth("100%");

        setContent(content);

        addFooterMenuItem(true, new MenuItem(msg.friends_sidebar_add_friends(), new Command() {
            @Override
            public void execute() {
                OneSwarmDialogBox dlg = new FriendsImportWizard(newFriendRequestCounts);
                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Math.max(40, dlg.getPopupTop() - 200));
                dlg.setVisible(true);
            }
        }));
    }

    public void clearSelectedFriend() {
        if (mSelectedFriend != null) {
            mSelectedFriend.clearSelected();
        }
    }

    public FriendInfoLite getSelectedFriend() {
        if (mSelectedFriend != null) {
            return mSelectedFriend.getFriendInfo();
        } else {
            return null;
        }
    }

    private int prevListId = 0;

    private long nextNewMessageRPCCheck = 0;

    protected Map<String, Integer> mUnreadChatCounts = new HashMap<String, Integer>();

    @Override
    public void update() {
        String session = OneSwarmRPCClient.getSessionID();
        OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

        if (System.currentTimeMillis() > nextNewMessageRPCCheck) {
            nextNewMessageRPCCheck = System.currentTimeMillis() + 10 * 1000;
            service.getUnreadMessageCounts(session, new AsyncCallback<HashMap<String, Integer>>() {
                @Override
                public void onFailure(Throwable caught) {
                    caught.printStackTrace();
                }

                @Override
                public void onSuccess(HashMap<String, Integer> result) {
                    mUnreadChatCounts = result;
                    int total = 0;
                    for (FriendPanel panel : friendPanels.values()) {
                        panel.updateChatCount();
                    }
                    if (result.size() > 0) {
                        for (Integer v : result.values()) {
                            total += v;
                        }
                    }
                    EntireUIRoot.getRoot(FriendListSidebarWidget.this.asWidget())
                            .setUnreadChatCount(total);
                    nextNewMessageRPCCheck = System.currentTimeMillis() + 2000;
                }
            });
        }

        service.getFriends(session, prevListId, false, false, new AsyncCallback<FriendList>() {
            @Override
            public void onFailure(Throwable caught) {
                // well, do nothing...
                newFriendsList = new FriendInfoLite[0];
                OneSwarmGWT.log("error " + caught.getMessage());
            }

            @Override
            public void onSuccess(FriendList result) {
                /*
                 * check if something changed on the back end, if not, don't
                 * update
                 */
                if (prevListId != result.getListId()) {
                    OneSwarmGWT.log("updating friend list: " + (prevListId != result.getListId())
                            + " prev=" + prevListId + " next=" + result.getListId());
                    newFriendsList = result.getFriendList();
                    prevListId = result.getListId();
                    updateUI(true);
                }
            }
        });

        service.getNewFriendsCountsFromAutoCheck(session,
                new AsyncCallback<HashMap<String, Integer>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        OneSwarmGWT.log("error " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(HashMap<String, Integer> result) {
                        FriendListSidebarWidget.this.newFriendRequestCounts = result;

                        int sum = 0;
                        for (String key : result.keySet()) {
                            int count = result.get(key);
                            sum += count;
                        }
                        if (sum > 0) {
                            friendRequestsPanel.setHTML(msg
                                    .friends_sidebar_notice_friend_updates_HTML(sum));
                            friendRequestsPanel.setVisible(true);
                        } else {
                            friendRequestsPanel.setVisible(false);
                        }
                    }
                });
    }

    public void updateUI() {
        updateUI(false);
    }

    public void updateUI(boolean forceFullUpdate) {

        if (forceFullUpdate) {
            fullUpdateNeeded = true;
        }

        FriendInfoLite selected = getSelectedFriend();
        if (newFriendsList.length != oldFriendsList.length) {
            // need a complete update here
            fullUpdateNeeded = true;
        }
        System.out.println("updating friend list full=" + fullUpdateNeeded);

        if (!fullUpdateNeeded) {
            // same number, just update
            for (int i = 0; i < oldFriendsList.length; i++) {
                final FriendInfoLite f = newFriendsList[i];
                FriendPanel p = friendPanels.get(f.getPublicKey());
                if (p == null) {
                    fullUpdateNeeded = true;
                    break;
                }
                p.update(f);
                if (selected != null && selected.equals(f)) {
                    if (f.getStatus() == FriendInfoLite.STATUS_ONLINE) {
                        mSelectedFriend.clearSelected();
                        p.setSelected();
                    } else {
                        // selected friend went offline
                        p.clearSelected();
                        mSelectedFriend = null;
                    }
                } else {
                    p.clearSelected();
                }
            }
        }

        if (fullUpdateNeeded) {
            friendListVP.setVisible(false);
            friendListVP.clear();
            friendPanels.clear();

            Map<String, GroupPanel> groups = new HashMap<String, GroupPanel>();

            for (FriendInfoLite f : newFriendsList) {

                FriendPanel p = new FriendPanel(f);
                friendPanels.put(f.getPublicKey(), p);

                if (f.getGroup() != null) {
                    if (f.getGroup().length() > 0) {
                        GroupPanel groupTree = groups.get(f.getGroup());
                        if (groupTree == null) {
                            groupTree = new GroupPanel(f.getGroup());
                            groups.put(f.getGroup(), groupTree);
                        }
                        groupTree.addChild(p);
                    } else {
                        friendListVP.add(p);
                    }
                } else {
                    friendListVP.add(p);
                }

                if (f.equals(getSelectedFriend())) {
                    if (f.getStatus() == FriendInfoLite.STATUS_ONLINE) {
                        p.setSelected();
                    } else {
                        // selected friend went offline
                        p.clearSelected();
                        mSelectedFriend = null;
                    }
                }
            }

            GroupPanel[] sortedPanels = groups.values().toArray(new GroupPanel[0]);
            Arrays.sort(sortedPanels, new Comparator<GroupPanel>() {
                @Override
                public int compare(GroupPanel o1, GroupPanel o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            for (GroupPanel disc : sortedPanels) {
                friendListVP.add(disc);
            }

            friendListVP.setVisible(true);
            // System.out.println("full update needed");
        }

        oldFriendsList = newFriendsList;

        friendListVP.setVisible(true);
        fullUpdateNeeded = false;
        setClosedHeaderTextSuffix("(" + oldFriendsList.length + ")");
    }

    private class GroupPanel extends VerticalPanel {

        public static final String CSS_GROUP_PANEL_HEADER = "os-group_panel_header";

        Image hideShowImage = new Image(ImageConstants.ICON_FRIEND_GROUP_CLOSED);
        Label countLabel = new Label();

        ClickHandler swapHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                boolean isOpen;
                String neu = ImageConstants.ICON_FRIEND_GROUP_OPEN;
                if (isOpen()) {
                    neu = ImageConstants.ICON_FRIEND_GROUP_CLOSED;
                    for (int i = 1; i < getWidgetCount(); i++) {
                        getWidget(i).setVisible(false);
                    }
                    isOpen = false;
                } else {
                    for (int i = 1; i < getWidgetCount(); i++) {
                        getWidget(i).setVisible(true);
                    }
                    isOpen = true;
                }
                hideShowImage.setUrl(neu);
                countLabel.setVisible(!isOpen);
                Cookies.setCookie(mName, isOpen + "", OneSwarmConstants.TEN_YEARS_FROM_NOW);
            }
        };

        private final String mName;

        public String getName() {
            return mName;
        }

        private boolean isOpen() {
            return hideShowImage.getUrl().endsWith(ImageConstants.ICON_FRIEND_GROUP_OPEN);
        }

        public GroupPanel(String inName) {
            addStyleName(CSS_FRIEND_LIST_ELEMENT);

            mName = inName;

            HorizontalPanel header = new HorizontalPanel();
            header.add(hideShowImage);
            header.addStyleName(CSS_GROUP_PANEL_HEADER);

            hideShowImage.addClickHandler(swapHandler);

            Label l = new Label(inName);
            l.addClickHandler(swapHandler);
            header.add(l);

            // countLabel.addStyleName(OneSwarmCss.TEXT_BOLD);
            countLabel.addClickHandler(swapHandler);
            header.add(countLabel);

            header.setCellVerticalAlignment(hideShowImage, VerticalPanel.ALIGN_MIDDLE);
            header.setCellVerticalAlignment(l, VerticalPanel.ALIGN_MIDDLE);
            header.setCellHorizontalAlignment(l, HorizontalPanel.ALIGN_LEFT);
            header.setCellWidth(hideShowImage, "15px");
            header.setCellHorizontalAlignment(hideShowImage, HorizontalPanel.ALIGN_CENTER);
            header.setWidth("100%");

            header.setCellHorizontalAlignment(countLabel, HorizontalPanel.ALIGN_RIGHT);

            add(header);

            setWidth("100%");

            String isOpenCookie = Cookies.getCookie(mName);
            if (isOpenCookie != null && Boolean.parseBoolean(isOpenCookie)) {
                hideShowImage.setUrl(ImageConstants.ICON_FRIEND_GROUP_OPEN);
                countLabel.setVisible(false);
            }
        }

        public void addChild(Widget kid) {
            if (isOpen()) {
                kid.setVisible(true);
            } else {
                kid.setVisible(false);
            }
            add(kid);
            countLabel.setText(" (" + (getWidgetCount() - 1) + ")");
        }

    }

    private class FriendPanel extends FocusPanel {
        private static final int MAX_LABEL_NAME_LENGTH = 27;

        private final ClickHandler clickListener = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (!isSelected && friendInfoLite.getStatus() == FriendInfoLite.STATUS_ONLINE) {
                    FriendPanel.this.addStyleName(OneSwarmCss.SidebarWidget.SELECTED_ITEM);
                    if (mSelectedFriend != null) {
                        mSelectedFriend.clearSelected();
                    }
                    History.newItem("friend-"
                            + FriendListSidebarWidget.this.getSelectedFriend().getId());
                    setSelected();

                    mSelectedFriend = FriendPanel.this;
                    OneSwarmGWT.log("selected friend conn id: " + friendInfoLite.getConnectionId());
                }
            }
        };

        private final DoubleClickHandler doubleClickHandler = new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                EntireUIRoot.getRoot(FriendListSidebarWidget.this.asWidget()).startChat(
                        friendInfoLite);
            }
        };

        private FriendInfoLite friendInfoLite;

        private final Image statusImage = new Image("images/friend_offline.png");

        private final Label nameLabel = new Label("");
        private final HorizontalPanel imagePanel = new HorizontalPanel();

        private final HorizontalPanel mainPanel = new HorizontalPanel();
        private final AbsolutePanel labelPanel = new AbsolutePanel();
        private boolean isSelected = false;
        // final ;
        private final HorizontalPanel iconsPanel = new HorizontalPanel();
        private final static int TOTAL_WIDTH = 170;
        private final static int STATUS_IMAGE = 12;
        private final static int NAME_LABEL_WIDTH = TOTAL_WIDTH - STATUS_IMAGE - 4;
        private final static int HEIGHT = 18;

        public FriendPanel(FriendInfoLite friend) {
            // panel.setVisible(false);
            mainPanel.setWidth(TOTAL_WIDTH + "px");

            statusImage.setHeight(STATUS_IMAGE + "px");
            statusImage.setWidth(STATUS_IMAGE + "px");
            imagePanel.add(statusImage);
            imagePanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
            imagePanel.setWidth(STATUS_IMAGE + 2 + "px");

            mainPanel.add(imagePanel);
            mainPanel.setCellVerticalAlignment(imagePanel, HorizontalPanel.ALIGN_TOP);
            mainPanel.setCellHorizontalAlignment(imagePanel, HorizontalPanel.ALIGN_CENTER);
            statusImage.addClickHandler(clickListener);
            statusImage.addDoubleClickHandler(doubleClickHandler);

            labelPanel.setHeight(HEIGHT + "px");

            labelPanel.setWidth(NAME_LABEL_WIDTH + "px");

            nameLabel.setWidth(NAME_LABEL_WIDTH + "px");
            nameLabel.setHeight(14 + "px");
            nameLabel.setText("test");
            labelPanel.add(nameLabel, 2, 0);

            nameLabel.addClickHandler(clickListener);
            nameLabel.addDoubleClickHandler(doubleClickHandler);

            mainPanel.add(labelPanel);
            iconsPanel.setVisible(false);
            iconsPanel.addStyleName(OneSwarmCss.CLICKABLE);

            super.add(mainPanel);
            this.addMouseOverHandler(new MouseOverHandler() {
                @Override
                public void onMouseOver(MouseOverEvent event) {
                    iconsPanel.setVisible(true);
                }
            });
            this.addMouseOutHandler(new MouseOutHandler() {
                @Override
                public void onMouseOut(MouseOutEvent event) {
                    iconsPanel.setVisible(false);
                }
            });

            update(friend);
            // panel.setVisible(true);
        }

        private int iconsPanelSize = 0;

        private void addIcon(Image image) {
            int iconSize = 16;
            int totalSize = 20;
            iconsPanelSize += totalSize;
            image.setHeight(iconSize + "px");
            image.setWidth(iconSize + "px");
            iconsPanel.add(image);
            iconsPanel.setCellWidth(image, totalSize + "px");
            iconsPanel.setCellHeight(image, totalSize + "px");
            iconsPanel.setCellHorizontalAlignment(image, HorizontalPanel.ALIGN_CENTER);
        }

        protected void updateChatCount() {
            if (mUnreadChatCounts.containsKey(friendInfoLite.getPublicKey())) {
                statusImage.setUrl(ImageConstants.ICON_CHAT);
                String pre = "(" + mUnreadChatCounts.get(this.friendInfoLite.getPublicKey()) + ") ";
                nameLabel.setText(pre
                        + StringTools.truncate(friendInfoLite.getName(),
                                FriendPanel.MAX_LABEL_NAME_LENGTH - pre.length(), false));
            } else {
                if (statusImage.getUrl().endsWith(ImageConstants.ICON_CHAT)) {
                    nameLabel.setText(StringTools.truncate(friendInfoLite.getName(),
                            FriendPanel.MAX_LABEL_NAME_LENGTH, false));
                    updateStatus(this.friendInfoLite, false);
                }
            }
        }

        private Image createEditImage() {
            final Image editImage = new Image(ImageConstants.ICON_TRIANGLE);

            editImage.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    FriendPropertiesDialog dlg = new FriendPropertiesDialog(
                            FriendListSidebarWidget.this, friendInfoLite, OneSwarmGWT
                                    .hasDevUpdates());
                    dlg.show();
                    dlg.setVisible(false);
                    dlg.center();
                    dlg.setPopupPosition(editImage.getAbsoluteLeft(), editImage.getAbsoluteTop());
                    // since we
                    // don't know the size until the list loads, we fix this to
                    // high up on the screen
                    dlg.setVisible(true);
                    iconsPanel.setVisible(false);
                }
            });

            return editImage;
        }

        private Image createChatImage() {
            final Image editImage = new Image(ImageConstants.ICON_CHAT);

            editImage.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    EntireUIRoot.getRoot(editImage).startChat(friendInfoLite);
                }
            });

            return editImage;
        }

        public void update(FriendInfoLite friend) {
            // OneSwarmGWT.log("updating: " + friend.getName());
            this.friendInfoLite = friend;
            updateName(friendInfoLite);
            updateStatus(friendInfoLite, true);
            updateIcons(friendInfoLite);
        }

        private void updateIcons(FriendInfoLite friend) {
            iconsPanel.clear();
            iconsPanelSize = 0;

            if (friend.isSupportsChat() && friend.isAllowChat()) {
                Image chatImage = createChatImage();
                addIcon(chatImage);
            }

            Image editImage = createEditImage();
            addIcon(editImage);

            iconsPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
            iconsPanel.setWidth(iconsPanelSize + "px");
            labelPanel.add(iconsPanel, NAME_LABEL_WIDTH + 2 - iconsPanelSize, 0);
        }

        public void setSelected() {

            EntireUIRoot.getRoot(FriendListSidebarWidget.this.asWidget()).clearSidebarSelection();
            if (mSelectedFriend != this) {
                mSelectedFriend = this;
                FriendPanel.this.addStyleName(OneSwarmCss.SidebarWidget.SELECTED_ITEM);
            }
            isSelected = true;
        }

        public void clearSelected() {
            if (mSelectedFriend == this) {
                mSelectedFriend = null;
                FriendPanel.this.removeStyleName(OneSwarmCss.SidebarWidget.SELECTED_ITEM);
            }
        }

        public FriendInfoLite getFriendInfo() {
            return friendInfoLite;
        }

        private void setImageSettings(HorizontalPanel panel, Image i, int imageWidth,
                int imageHeigth) {
            panel.add(i);
            i.setHeight(imageHeigth + "px");
            i.setWidth(imageWidth + "px");
        }

        private String oldName;

        private void updateName(FriendInfoLite f) {

            final String name = f.getName();
            if (name.equals(oldName)) {
                return;
            }
            oldName = name;

            String labelName;
            if (name.length() > MAX_LABEL_NAME_LENGTH) {
                labelName = name.substring(0, MAX_LABEL_NAME_LENGTH - 2) + "...";
            } else {
                labelName = name;
            }
            nameLabel.setText(labelName);
            nameLabel.setTitle(name
                    + (this.friendInfoLite.isCanSeeFileList() == false ? " (limited)" : ""));
            // System.out.println("updating: " + labelName);
        }

        private int oldStatus = -1;

        private void updateStatus(FriendInfoLite f, boolean updateChat) {

            if (f.getStatus() != oldStatus) {
                oldStatus = f.getStatus();

                imagePanel.clear();
                int imageWidth = 12;
                int imageHeigth = 12;
                if (f.getStatus() == FriendInfoLite.STATUS_ONLINE) {
                    nameLabel.setStylePrimaryName(CSS_FRIEND_LIST_ELEMENT);
                    if (f.isCanSeeFileList() == true) {
                        statusImage.setUrl(ImageConstants.ICON_FRIEND_ONLINE);
                    } else {
                        statusImage.setUrl(ImageConstants.ICON_FRIEND_LIMITED);
                    }
                    statusImage.setTitle("Connected"
                            + (f.isCanSeeFileList() == false ? " (limited)" : ""));
                } else {
                    nameLabel.setStylePrimaryName("os-friendListElementDisconnected");
                    if (f.isBlocked()) {
                        statusImage.setUrl(ImageConstants.ICON_FRIEND_BLOCKED);
                        statusImage.setTitle("Deleted");
                        // imageWidth = 14;
                        // imageHeigth = 14;
                    } else if (f.getStatus() == FriendInfoLite.STATUS_CONNECTING) {
                        statusImage.setUrl(ImageConstants.ICON_FRIEND_CONNECTING);
                        statusImage.setTitle("Connecting...");
                        // imageWidth = 14;
                        // imageHeigth = 14;
                    } else if (f.getStatus() == FriendInfoLite.STATUS_HANDSHAKING) {
                        statusImage.setUrl(ImageConstants.ICON_FRIEND_HANDSHAKING);
                        statusImage.setTitle("Handshaking...");
                        // imageWidth = 14;
                        // imageHeigth = 14;
                    } else if (f.getLastConnectedDate() == null) {
                        statusImage.setUrl(ImageConstants.ICON_FRIEND_UNKNOWN);
                        statusImage.setTitle("Never connected");
                        // imageWidth = 14;
                        // imageHeigth = 14;
                    } else if (f.getStatus() == FriendInfoLite.STATUS_OFFLINE) {
                        statusImage.setUrl(ImageConstants.ICON_FRIEND_OFFLINE);
                        statusImage.setTitle("Offline");
                    }
                }
                imagePanel.add(statusImage);
                setImageSettings(imagePanel, statusImage, imageWidth, imageHeigth);

            }
            if (updateChat) {
                updateChatCount();
            }

            // System.out.println("updating: " + nameLabel.getText() +
            // " status=" + f.getStatus() + " blocked=" + f.isBlocked());
        }
    }

    public FriendInfoLite[] getAllFriends() {
        return newFriendsList;
    }
}
