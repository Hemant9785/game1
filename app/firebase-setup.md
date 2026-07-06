# Firebase Setup For Real Online Sessions

This app is now prepared for a real backend implementation using:

- Firebase Authentication with anonymous sign-in
- Firebase Realtime Database for live couple sessions

## Manual setup

1. Create a Firebase project in the Firebase console.
2. Add the Android app with package name `com.wordduel.app`.
3. Download `google-services.json`.
4. Place `google-services.json` in the [app](D:/planner/app1/app) module root.
5. In Firebase Authentication, enable `Anonymous` sign-in.
6. In Realtime Database, create a database and start in locked mode.
7. Add security rules before public testing.

## Official references

- Android setup: https://firebase.google.com/docs/android/setup
- Anonymous auth: https://firebase.google.com/docs/auth/android/anonymous-auth
- Realtime Database: https://firebase.google.com/docs/database/android/start

## Why this stack

- Realtime Database is better than a normal REST backend for presence and low-latency session updates.
- Anonymous Auth gives each player a real backend identity without forcing sign-up friction.
- This is the most practical MVP backend for long-distance couple sessions.
