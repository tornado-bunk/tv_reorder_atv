package com.android.tv.ui.sidepanel;

import android.os.Bundle;
import androidx.leanback.widget.VerticalGridView;
import android.view.KeyEvent;
import android.view.View;
import com.android.tv.R;
import com.android.tv.data.ChannelImpl;
import com.android.tv.data.ChannelReorderManager;
import com.android.tv.data.api.Channel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fragment for reordering channels by swapping them using the remote (DPAD UP/DOWN).
 */
public class ChannelReorderFragment extends SideFragment<Item> {
    private static final String TRACKER_LABEL = "channel reorder";
    private final List<Channel> mChannels = new ArrayList<>();
    private final List<Item> mItems = new ArrayList<>();
    private int mSelectedMoveIndex = -1; // Index of the item in move mode (-1 if none)

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChannels.addAll(getChannelDataManager().getChannelList());
        // Initially sort with the current comparator (which now considers the custom order)
        TvInputAdapter inputAdapter = new TvInputAdapter();
        Collections.sort(mChannels, new ChannelImpl.DefaultComparator(getActivity(), getMainActivity().getTvInputManagerHelper()));
    }

    @Override
    public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        final VerticalGridView listView = (VerticalGridView) view.findViewById(R.id.side_panel_list);
        
        listView.setOnKeyInterceptListener(new VerticalGridView.OnKeyInterceptListener() {
            @Override
            public boolean onInterceptKeyEvent(KeyEvent event) {
                int keyCode = event.getKeyCode();
                if (event.getAction() == KeyEvent.ACTION_DOWN && mSelectedMoveIndex != -1) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        moveSelectedChannel(true);
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        moveSelectedChannel(false);
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                        // Exit move mode
                        mSelectedMoveIndex = -1;
                        updateItemsList();
                        setItems(mItems);
                        return true;
                    }
                }
                return false;
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getMainActivity().startShrunkenTvView(false, true);
    }

    @Override
    public void onPause() {
        getMainActivity().endShrunkenTvView();
        super.onPause();
    }

    private void moveSelectedChannel(boolean up) {
        int targetIndex = up ? mSelectedMoveIndex - 1 : mSelectedMoveIndex + 1;
        if (targetIndex < 0 || targetIndex >= mChannels.size()) {
            return;
        }

        Channel current = mChannels.get(mSelectedMoveIndex);
        Channel target = mChannels.get(targetIndex);

        // Swap displayed numbers (custom or original)
        String currentNum = current.getDisplayNumber();
        String targetNum = target.getDisplayNumber();

        ChannelReorderManager.setCustomDisplayNumber(current.getId(), targetNum);
        ChannelReorderManager.setCustomDisplayNumber(target.getId(), currentNum);

        // Update local list
        Collections.swap(mChannels, mSelectedMoveIndex, targetIndex);
        mSelectedMoveIndex = targetIndex;

        // Notify data manager of the change
        getChannelDataManager().reorderChannels();

        // Update the adapter while maintaining focus
        updateItemsList();
        setItems(mItems);
        setSelectedPosition(mSelectedMoveIndex);
    }

    @Override
    protected String getTitle() {
        return getString(R.string.reorder_channels_title);
    }

    @Override
    public String getTrackerLabel() {
        return TRACKER_LABEL;
    }

    @Override
    protected List<Item> getItemList() {
        updateItemsList();
        return mItems;
    }

    private void updateItemsList() {
        mItems.clear();
        for (int i = 0; i < mChannels.size(); i++) {
            Channel channel = mChannels.get(i);
            boolean isMoving = (i == mSelectedMoveIndex);
            mItems.add(new ReorderChannelItem(channel, i, isMoving));
        }
    }

    private class ReorderChannelItem extends ActionItem {
        private final Channel mChannel;
        private final int mIndex;
        private final boolean mIsMoving;

        public ReorderChannelItem(Channel channel, int index, boolean isMoving) {
            super(isMoving ? getString(R.string.reorder_channel_moving_format, channel.getDisplayName()) : channel.getDisplayName(), 
                  getString(R.string.reorder_channel_number_format, channel.getDisplayNumber()));
            mChannel = channel;
            mIndex = index;
            mIsMoving = isMoving;
        }

        @Override
        protected void onSelected() {
            if (mSelectedMoveIndex == -1) {
                // Enter move mode
                mSelectedMoveIndex = mIndex;
            } else if (mSelectedMoveIndex == mIndex) {
                // Exit move mode
                mSelectedMoveIndex = -1;
            } else {
                // If another channel was in move mode, exit and select this one
                mSelectedMoveIndex = mIndex;
            }
            updateItemsList();
            setItems(mItems);
            setSelectedPosition(mIndex);
        }
    }

    // Dummy helper class for default constructor requirements
    private static class TvInputAdapter {}
}
