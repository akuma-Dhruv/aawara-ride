# AawaraRide – Ride Log & GPS Tracking
## Feature Specification

**Document Type:** Feature Requirement Document  
**Feature:** Ride Log / GPS Ride Tracking  
**Target Version:** `0.1.m2b`  
**Platform:** Android  
**Status:** Proposed – Pending Implementation  
**Priority:** High

---

## 1. Objective

Introduce a Ride Log feature that automatically records a user's ride whenever Riding Mode is activated.

The feature will capture GPS movement, calculate distance and speed metrics, track moving and idle time, and generate a ride summary when the user finishes the ride.

The primary objective is to give riders a clear understanding of their journeys, including:
- How far they travelled.
- How long the journey took.
- How much time they spent actually moving.
- How much time they spent stationary or crawling in traffic.
- Their average and maximum speeds.

## 2. Core Product Behaviour

Activating Riding Mode must not immediately start the ride timer.

Instead, the application should enter an armed tracking state and wait for the first valid GPS location.

### Ride lifecycle

**State 1 – Riding Mode Activated**
- User activates Riding Mode.
- GPS tracking service is initialized.
- The application starts waiting for a valid GPS location.
- No ride duration or distance is recorded yet.
- UI indicates that the application is waiting for GPS.

**State 2 – First Valid GPS Location**
- The first accepted GPS location officially starts the ride.
- Record the ride start timestamp.
- Save the initial latitude and longitude.
- Start the ride timer.
- Begin collecting subsequent GPS locations.

**State 3 – Active Ride**
- Collect GPS location updates continuously.
- Calculate distance travelled.
- Track current speed and maximum speed.
- Classify time into moving and idle/crawling time.
- Maintain ride statistics throughout the journey.
- Continue tracking when the application is in the background or the screen is off.

**State 4 – Ride Ended**
- When the user exits Riding Mode or explicitly ends the ride, stop location tracking.
- Finalize the ride calculations.
- Persist the ride record.
- Display the ride summary.

If Riding Mode is deactivated before the first valid GPS location, no completed ride should be created.

## 3. Ride Tracking Metrics

The application must record and display the following metrics.

| Metric | Description |
|---|---|
| Total Distance | Total accepted GPS distance travelled |
| Total Ride Time | Time between first valid GPS fix and ride end |
| Moving Time | Time classified as moving |
| Idle / Traffic Time | Time classified as stationary or crawling |
| Overall Average Speed | Total distance divided by total ride time |
| Moving Average Speed | Total distance divided by moving time |
| Top Speed | Highest validated GPS speed |
| Start Time | Timestamp of first valid GPS fix |
| End Time | Timestamp when the ride ends |

### 3.1 Average Speed Calculations

Overall Average Speed:

`Total Distance / Total Ride Time`

Moving Average Speed:

`Total Distance / Moving Time`

Both averages must be displayed separately.

If moving time is zero, moving average speed should be displayed as unavailable rather than causing a division error.

### 3.2 Moving Time and Idle / Traffic Time

Ride duration must be divided into two categories:

**Moving Time**
- Time spent travelling above the configured movement threshold.

**Idle / Traffic Time**
- Time spent stationary.
- Time spent crawling at very low speeds in traffic.

Initial proposed classification:

| GPS Speed | Classification |
|---|---|
| 0–5 km/h | Idle / Crawling |
| Above 5 km/h | Moving |

The threshold should be configurable internally and easy to tune during testing.

Use hysteresis or equivalent state-transition logic to avoid frequent switching between Moving and Idle because of GPS fluctuations.

Moving Time + Idle / Traffic Time should approximately equal Total Ride Time, allowing for explicitly handled GPS sampling gaps.

## 4. GPS Tracking Requirements

The application must:

1. Use Android's location APIs for GPS tracking.
2. Run tracking through an appropriate foreground service while an active ride is being recorded.
3. Continue tracking when the application is backgrounded or the screen is off.
4. Record accepted location points with:
   - Timestamp
   - Latitude
   - Longitude
   - GPS accuracy
   - Speed, when available
   - Altitude, when available
5. Calculate distance using consecutive accepted GPS coordinates.
6. Reject or filter inaccurate location updates.
7. Avoid adding distance from unrealistic GPS jumps.
8. Filter invalid speed readings and GPS spikes before calculating top speed.
9. Handle temporary GPS signal loss without immediately terminating the ride.

GPS sampling frequency and accuracy requirements should balance tracking quality, battery consumption and Android background execution constraints.

## 5. Ride Data Model

Each completed ride should be stored locally.

Suggested ride entity:

| Field | Description |
|---|---|
| rideId | Unique ride identifier |
| startTimestamp | First valid GPS fix timestamp |
| endTimestamp | Ride end timestamp |
| totalDistance | Total distance in metres |
| totalDuration | Total ride duration in seconds |
| movingDuration | Moving time in seconds |
| idleDuration | Idle / traffic time in seconds |
| averageSpeed | Overall average speed |
| movingAverageSpeed | Moving average speed |
| topSpeed | Maximum validated speed |
| startLatitude | Starting latitude |
| startLongitude | Starting longitude |
| endLatitude | Ending latitude |
| endLongitude | Ending longitude |
| createdAt | Record creation timestamp |

### GPS Point Entity

Each ride should have an associated collection of GPS points.

Suggested fields:
- pointId
- rideId
- timestamp
- latitude
- longitude
- accuracy
- speed
- altitude

Use an appropriate local database, preferably the existing Room implementation if the application already uses it.

The implementation should preserve enough GPS data to support future route visualisation and ride history.

## 6. User Interface Requirements

### 6.1 Active Ride Screen

During an active ride, show the most important metrics:

- Current speed
- Distance travelled
- Elapsed ride time
- Moving time
- Idle / traffic time
- Top speed

The interface should be readable at a glance and should not encourage unnecessary interaction while riding.

### 6.2 Ride Summary Screen

After the ride ends, show:

**Primary metrics**
- Total distance
- Total ride duration
- Moving time
- Idle / traffic time

**Speed metrics**
- Overall average speed
- Moving average speed
- Top speed

**Additional information**
- Ride start time
- Ride end time

Route visualisation and historical ride comparison can be introduced in a future milestone.

### 6.3 Ride History

The architecture should support storing multiple rides and retrieving previous records.

A dedicated ride history interface can be implemented as part of this milestone if it fits the existing application structure without expanding the scope unnecessarily.

## 7. Background Execution and Reliability

The implementation must account for:

- Screen turning off.
- Application moving into the background.
- Temporary GPS signal loss.
- Android process termination.
- Device restart.
- Location permission changes.
- Battery optimisation restrictions.

An active ride should be persisted incrementally so that a process termination does not silently erase the entire ride.

On application restart, the application should detect an unfinished ride and provide an appropriate recovery or resume flow.

Use Android-supported foreground service and location permission mechanisms. Do not assume that a background service alone guarantees uninterrupted GPS tracking.

## 8. Permissions

Inspect the existing permission implementation before adding anything.

Required capabilities may include:
- Fine location access.
- Appropriate background location access, if needed for the selected implementation.
- Foreground service permissions for location tracking on supported Android versions.
- Notification permission where applicable.

Request permissions contextually and explain why location access is required.

Do not introduce unrelated permissions.

## 9. Out of Scope – Version 0.1.m2b

The following are explicitly excluded from this milestone:

- Cloud-based ride synchronisation.
- Social sharing of ride statistics.
- Public ride profiles.
- Leaderboards and rider rankings.
- Group ride tracking.
- Live location sharing with other riders.
- Route navigation.
- AI-based riding analysis.
- Fuel consumption and mileage calculations.
- Automatic crash detection.

These can be considered for future milestones.

## 10. Acceptance Criteria

The feature will be considered complete when:

1. Activating Riding Mode arms the tracker without immediately starting the ride timer.
2. The ride starts only after the first valid GPS fix is received.
3. The recorded start timestamp corresponds to that first accepted GPS fix.
4. Distance is calculated from accepted GPS points.
5. Current speed and top speed are calculated correctly.
6. Overall average speed is calculated using total ride duration.
7. Moving average speed is calculated using moving duration.
8. Moving and idle/traffic time are tracked separately.
9. GPS noise does not cause significant false distance or speed spikes.
10. Tracking continues with the screen off and application in the background.
11. Ending Riding Mode stops tracking and saves the completed ride.
12. Completed rides remain available after restarting the application.
13. Temporary GPS loss is handled gracefully.
14. Existing caller announcement and Riding Mode functionality from Milestone 1 remain unaffected.
15. Existing voice-command functionality is not modified as part of this feature.

## 11. Versioning and Release Management

**Target Base Version:** `0.1.m2b`

Any fixes after the initial implementation must follow the existing fix-cycle convention:

- Initial implementation: `0.1.m2b`
- First fix: `0.1.m2b-f1`
- Second fix: `0.1.m2b-f2`
- Subsequent fixes increment sequentially.

### Mandatory release requirements

1. Inspect `VERSIONING.md` before making any build.
2. Update Android `versionName` to match the target identifier.
3. Increment Android `versionCode` monotonically for every distributed build.
4. Name the APK using the version identifier, for example:
   `AawaraRide-0.1.m2b.apk`
5. Preserve previous APKs as rollback baselines.
6. Update `VERSIONING.md` with the milestone and release history.
7. Document implemented functionality, fixes, known limitations and build status.
8. Reference `VERSIONING.md` from the project's README.

## 12. Implementation Guidelines

Before implementation:

- Inspect the existing Android project and architecture.
- Identify the existing Riding Mode lifecycle.
- Identify current location and permission handling.
- Check existing foreground services and persistence mechanisms.
- Reuse existing components wherever possible.
- Do not rewrite working Milestone 1 functionality.

Implement the feature incrementally, keeping GPS collection, distance calculation, speed calculation, movement classification and persistence logically separated.

The first implementation should prioritize reliable GPS tracking and accurate ride statistics over visual enhancements.

**Target release:** `0.1.m3`  
**Feature name:** Ride Log & GPS Tracking  
**Implementation status:** Pending

