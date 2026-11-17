package com.aawararide.ride

interface RideDao {
    fun activeRide(): RideRecord?
    fun saveActiveRide(ride: RideRecord)
    fun completeRide(ride: RideRecord)
    fun findCompletedRide(rideId: String): RideRecord?
    fun completedRides(): List<RideRecord>
    fun pointsForRide(rideId: String): List<RidePoint>
}
