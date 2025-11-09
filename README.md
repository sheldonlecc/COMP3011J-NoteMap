## Build & Run Instructions: AMap SDK Key Restriction

**IMPORTANT NOTE:**

This project uses the AMap (GaoDe Maps) SDK for all map, location, and routing functionalities.

As per AMap's security policy, the API Key is strictly bound to the developer's SHA1 signature certificate. The API Key currently in the source code is bound to the SHA1 debug certificate on my local development machine.

**What this means for you (the reviewer):**

1.  When you build and run the source code on your computer, Android Studio will use **your local SHA1 debug certificate**, which is different from mine.
2.  This will cause the AMap server to **fail the authentication** (this is the intended security mechanism).
3.  As a result, the map will **fail to load** (it will likely appear as a blank grid), and you will see authentication errors in the Logcat.

**How to Review This Project:**

**All implemented features (map loading, clustering, route planning, adding notes, etc.) are fully demonstrated in the accompanying MP4 video file: `[Your-Video-File-Name].mp4`.**

This video was recorded in the correctly configured environment and serves as proof of all current functionalities.

If you wish to run the project locally, you would need to:
1.  Go to the AMap Open Platform (amap.com) to register for a new API Key.
2.  Get the SHA1 debug certificate fingerprint from your local machine.
3.  Bind your new Key to your SHA1.
4.  Replace the API Key in the project's `AndroidManifest.xml` file with your new key.