## Idle Daddy (fork)

Steam Trading Card farmer for Android, originally by [Steev](https://github.com/steevp](https://github.com/steevp/UpdogFarmer)

Snapshot, signed APK builds can be found here:<br>
[![Build Signed Release APK](https://github.com/LossyDragon/UpdogFarmer/actions/workflows/release.yml/badge.svg)](https://github.com/LossyDragon/UpdogFarmer/actions/workflows/release.yml)

## Note
This fork is a hobby to me, it has no garantee to be bug free. Issues, suggestions, and PR's are welcome.

Signed APK builds can be found in the *Actions* tab. You must be logged into GitHub to download them. 
Recommended to choose the first green checkmark for the latest build.

Stable (enough) builds may be packaged as a release due to action artifacts having a short shelf life.

Some features have been removed to easy the burden of maintaing this app. Idling is the primary focus.

v100 is the last build to maintain the original codebase and UI design.

Subsequent versions will be a rewrite using Jetpack Compose, a modern UI framework for Android.
- Google has put the old View System into maintence mode Jun 1 2026
- Compose is a declarative UI framework, this makes it easier for newcomers to easily contribute.
- Less boilerplate and hacks the View system has been plagued with.

# What's different.
- Updated project to be openable in newer Android Studio versions (2026.1.2+)
- Various bug fixes and newer dependencies making the app work again.
- Uses the new Steam login flow, with Credential or QR sign in.
- Maintain Android Oreo (API 26) support as long as possible.
- (Soon to be) Rewritten using modern Android features ensuring compatbility for newer devices. 
