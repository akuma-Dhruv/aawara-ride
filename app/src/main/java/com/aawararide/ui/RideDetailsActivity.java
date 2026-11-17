package com.aawararide.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aawararide.R;
import com.aawararide.ride.DurationFormatter;
import com.aawararide.ride.RideRecord;
import com.aawararide.ride.RideStore;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public final class RideDetailsActivity extends Activity {
    public static final String EXTRA_RIDE_ID = "ride_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.ride_details);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(24, 24, 24, 24);
        setContentView(content);

        String rideId = getIntent().getStringExtra(EXTRA_RIDE_ID);
        RideStore store = new RideStore(this);
        RideRecord ride = rideId == null ? null : store.findCompletedRide(rideId);
        if (ride == null) {
            content.addView(text(getString(R.string.ride_not_found)));
            return;
        }
        DateFormat format = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
        Double movingAverage = ride.movingAverageSpeedKmh();
        content.addView(text(getString(
                R.string.ride_details_text,
                format.format(new Date(ride.startTimestamp)),
                format.format(new Date(ride.endTimestamp)),
                ride.totalDistanceMeters / 1000.0,
                DurationFormatter.format(ride.totalDurationSeconds),
                DurationFormatter.format(ride.movingDurationSeconds),
                DurationFormatter.format(ride.idleDurationSeconds),
                ride.overallAverageSpeedKmh(),
                movingAverage == null
                        ? getString(R.string.unavailable)
                        : String.format(Locale.US, "%.1f km/h", movingAverage),
                ride.topSpeedKmh,
                ride.startLatitude,
                ride.startLongitude,
                ride.endLatitude,
                ride.endLongitude,
                store.pointsForRide(ride.rideId).size())));
    }

    private TextView text(String value) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(16);
        return view;
    }
}
