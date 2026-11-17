package com.aawararide.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aawararide.R;
import com.aawararide.ride.DurationFormatter;
import com.aawararide.ride.RideRecord;
import com.aawararide.ride.RideStore;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class RideHistoryActivity extends Activity {
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.ride_history);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(24, 24, 24, 24);
        setContentView(list);

        List<RideRecord> rides = new RideStore(this).completedRides();
        if (rides.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_ride_history);
            empty.setGravity(Gravity.CENTER);
            list.addView(empty, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            return;
        }
        for (RideRecord ride : rides) {
            Button item = new Button(this);
            item.setAllCaps(false);
            item.setText(historyText(ride));
            item.setOnClickListener(view -> {
                Intent details = new Intent(this, RideDetailsActivity.class);
                details.putExtra(RideDetailsActivity.EXTRA_RIDE_ID, ride.rideId);
                startActivity(details);
            });
            list.addView(item, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private String historyText(RideRecord ride) {
        Double movingAverage = ride.movingAverageSpeedKmh();
        return dateFormat.format(new Date(ride.startTimestamp))
                + "\n" + getString(
                R.string.history_row,
                ride.totalDistanceMeters / 1000.0,
                DurationFormatter.format(ride.totalDurationSeconds),
                DurationFormatter.format(ride.movingDurationSeconds),
                DurationFormatter.format(ride.idleDurationSeconds),
                ride.overallAverageSpeedKmh(),
                movingAverage == null
                        ? getString(R.string.unavailable)
                        : String.format(Locale.US, "%.1f km/h", movingAverage),
                ride.topSpeedKmh);
    }
}
