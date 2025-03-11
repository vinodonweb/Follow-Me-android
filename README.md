# Follow Me - Android Mobile Application

## Overview

**Follow Me** is an Android mobile application developed as part of the CSC 392/492: Mobile Applications Development for Android II course at DePaul University's Jarvis College of Computing and Digital Media. This app enables real-time location sharing and tracking, designed for both "Trip Leaders" (those sharing their journey) and "Trip Followers" (those tracking the journey). It was created to fulfill Assignment 4, earning up to 450 points, and showcases core Android development skills such as location services, API integration, and interactive map displays.

The app allows Trip Leaders to share their real-time location during a trip—whether by car or any mode of transportation with location tracking and internet connectivity—while Trip Followers can monitor the journey’s progress using a unique Trip ID. The app leverages Google Maps for visualization and a custom API for data management.

## Features

### Core Functionality
- **Real-Time Location Sharing**: Trip Leaders can broadcast their location updates, which are tracked and displayed on a map.
- **Unique Trip ID**: Each trip is assigned a unique identifier, shareable via intents (e.g., text or email) for followers to join.
- **Interactive Map**: Displays the Trip Leader’s route as a polyline, with a car icon marking their current position and a marker for the starting point. Users can zoom, pan, or auto-center the map.
- **Trip Details**: Shows total distance traveled, elapsed time, and trip start time in real-time.
- **Background Location Updates**: Location sharing continues even when the app is in the background.
- **Pause/End Trip**: Trip Leaders can pause location sharing for privacy or end the trip entirely, with data preserved for later review.

### User Roles
- **Trip Leader**:
  - Register and log in to the Follow Me API.
  - Start a trip with a custom or auto-generated Trip ID.
  - Share trip details and monitor their own progress on a map.
- **Trip Follower**:
  - Join a trip using a Trip ID.
  - Track the Trip Leader’s route and current location in real-time.
  - Receive notifications when the trip ends.

### Technical Highlights
- **Activities**:
  - `MainActivity`: Splash screen and entry point to start or follow a trip.
  - `TripLeadActivity`: Manages the Trip Leader’s map and location updates.
  - `TripFollowerActivity`: Displays the trip for followers with auto-centering options.
- **API Integration**: Connects to a custom Follow Me API (base URL: `http://christopherhield-001-site4.htempurl.com`) for user authentication, trip data retrieval, and location point updates.
- **Permissions**: Requires `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS`, and `ACCESS_BACKGROUND_LOCATION`.
- **Location Services**: Implements a background service with a location listener and broadcaster for continuous updates.
- **UI Elements**: Includes pulsing animations (via `ObjectAnimator`) and shareable trip info via intents.

## Requirements
- **Minimum SDK**: API 29
- **Tested Devices**: Emulators like Pixel, Pixel 2 (1080x1920), and Pixel 7, 8 (1080x2400) with Play Store.
- **Orientation**: Locked to portrait mode.

## Installation
1. Clone the repository:
   ```bash
   git clone git@github.com:vinodonweb/Follow-Me-android.git

2. update: ```app/src/main/res/values/google_maps_api.xml ```
     with your GOOGLE_MAP_API_KEY

## Screenshots 📸

![Screenshot 2025-03-11 120448](https://github.com/user-attachments/assets/d73730fe-d431-4df8-b647-6664402cab50)
![Screenshot 2025-03-11 120522](https://github.com/user-attachments/assets/c1183ecb-749b-43c1-8af3-f320aef0345f)
![Screenshot 2025-03-11 120514](https://github.com/user-attachments/assets/3bc15440-dfe4-481f-9c34-65348ef74da2)
![Screenshot 2025-03-11 120541](https://github.com/user-attachments/assets/a5da8256-9628-445d-93de-04c0ca14a317)
![Screenshot 2025-03-11 120549](https://github.com/user-attachments/assets/dc3b64cd-eb44-4ef4-a43e-39a9a6af80bd)
![Screenshot 2025-03-11 120603](https://github.com/user-attachments/assets/9f13952d-ad42-4d37-8616-2dfb42d63bc5)
![Screenshot 2025-03-11 120454](https://github.com/user-attachments/assets/806259f8-8e8b-493d-a202-1b68fd1c429d)
![Screenshot 2025-03-11 120503](https://github.com/user-attachments/assets/caa25b59-ccd7-4a40-80f3-1d15f4e704c5)


License 📜
This project is for educational purposes only.
