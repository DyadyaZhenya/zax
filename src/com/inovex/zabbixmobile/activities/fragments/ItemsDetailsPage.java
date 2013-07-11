package com.inovex.zabbixmobile.activities.fragments;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.inovex.zabbixmobile.R;
import com.inovex.zabbixmobile.adapters.EventsDetailsPagerAdapter;
import com.inovex.zabbixmobile.listeners.OnHistoryDetailsLoadedListener;
import com.inovex.zabbixmobile.model.HistoryDetail;
import com.inovex.zabbixmobile.model.Item;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;

/**
 * Represents one page of the event details view pager (see
 * {@link EventsDetailsPagerAdapter} ). Shows the details of a specific event.
 * 
 */
public class ItemsDetailsPage extends BaseServiceConnectedFragment implements
		OnHistoryDetailsLoadedListener {

	private Item mItem;
	private String mTitle = "";
	private CharSequence historyDetailsString;
	private Collection<HistoryDetail> mHistoryDetails;
	private Activity mActivity;
	private boolean mHistoryDetailsImported = false;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.mActivity = activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.page_item_details, null);
		// if (savedInstanceState != null)
		// mEventId = savedInstanceState.getLong(ARG_EVENT_ID, -1);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// TODO: on orientation change, mEvent is not set ->
		// NullPointerException
		StringBuilder sb = new StringBuilder();
		sb.append("Item: \n\n");
		sb.append("ID: " + mItem.getId() + "\n");
		sb.append("description: " + mItem.getDescription() + "\n");
		sb.append("last value: " + mItem.getLastValue() + mItem.getUnits()
				+ "\n");
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(mItem.getLastClock());
		java.text.DateFormat dateFormatter = SimpleDateFormat
				.getDateTimeInstance(SimpleDateFormat.SHORT,
						SimpleDateFormat.SHORT, Locale.getDefault());
		sb.append("last clock: "
				+ String.valueOf(dateFormatter.format(cal.getTime())) + "\n");
		TextView text = (TextView) getView().findViewById(R.id.item_details);
		text.setText(sb.toString());
		if (mHistoryDetails != null)
			showGraph();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		super.onServiceConnected(name, service);
		if (!mHistoryDetailsImported && mItem != null)
			mZabbixDataService.loadHistoryDetailsByItemId(mItem.getId(), this);
	}

	/**
	 * Sets the item for this page. This also triggers an import of history
	 * details for displaying the graph.
	 * 
	 * @param item
	 */
	public void setItem(Item item) {
		this.mItem = item;
		if (!mHistoryDetailsImported && mZabbixDataService != null)
			mZabbixDataService.loadHistoryDetailsByItemId(mItem.getId(), this);
	}

	public void setTitle(String title) {
		this.mTitle = title;
	}

	public String getTitle() {
		return mTitle;
	}

	@Override
	public void onHistoryDetailsLoaded(Collection<HistoryDetail> historyDetails) {
		mHistoryDetailsImported = true;
		mHistoryDetails = historyDetails;
		if (getView() != null) {
			showGraph();
		}
	}

	private void showGraph() {
		LinearLayout graphLayout = (LinearLayout) getView().findViewById(
				R.id.item_graph);
		// create graph and add it to the layout
		int numEntries = mHistoryDetails.size();
		if (numEntries > 0) {
			long lowestclock = 0;
			long highestclock = 0;

			GraphViewData[] values = new GraphViewData[numEntries];
			int i = 0;
			for (HistoryDetail detail : mHistoryDetails) {
				long clock = detail.getClock() / 1000;
				double value = detail.getValue();
				if (i == 0) {
					lowestclock = highestclock = clock;
				} else {
					highestclock = Math.max(highestclock, clock);
					lowestclock = Math.min(lowestclock, clock);
				}
				values[i] = new GraphViewData(clock, value);
				i++;
			}

			final java.text.DateFormat dateTimeFormatter = DateFormat
					.getTimeFormat(mActivity);
			LineGraphView graph = new LineGraphView(mActivity,
					mItem.getDescription() // title
			) {
				@Override
				protected String formatLabel(double value, boolean isValueX) {
					if (isValueX) {
						// transform number to time
						return dateTimeFormatter.format(new Date(
								(long) value * 1000));
					} else
						return super.formatLabel(value, isValueX);
				}
			};
			graph.addSeries(new GraphViewSeries(values));
			graph.setDrawBackground(true);
			long size = (highestclock - lowestclock) * 2 / 3; // we show 2/3
			// graph.setViewPort(highestclock - size, size);
			graph.setViewPort(lowestclock, (highestclock - lowestclock));
			graph.setScalable(true);
			graph.setDiscableTouch(true);
			// graph.setScalable(false);
			GraphViewStyle style = new GraphViewStyle();
			style.setHorizontalLabelsColor(getResources().getColor(
					android.R.color.black));
			style.setVerticalLabelsColor(getResources().getColor(
					android.R.color.black));
			graph.setGraphViewStyle(style);
			graphLayout.removeAllViews();
			graphLayout.addView(graph);
		} else {
			// no history data available
			graphLayout.removeAllViews();
			// mActivity.getLayoutInflater().inflate(R.layout.details_no_data,
			// layout);
			// ((TextView)
			// layout.findViewById(R.id.details_no_data_text)).setText(R.string.no_history_data_found);
		}
	}
}
