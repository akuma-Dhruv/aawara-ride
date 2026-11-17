package com.aawararide.ride;

import java.util.List;

public interface RideDao {
    RideRecord activeRide();
    void saveActiveRide(RideRecord ride);
    void completeRide(RideRecord ride);
    RideRecord findCompletedRide(String rideId);
    List<RideRecord> completedRides();
    List<RidePoint> pointsForRide(String rideId);
}
