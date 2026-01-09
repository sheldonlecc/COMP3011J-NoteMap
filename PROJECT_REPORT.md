# Project Report: NoteMap

This document consolidates open-source usage, APIs/third-party integration, feature explanations, implementation details, workflow and progress, and UX evaluation. It also highlights changes from beta to final (fixing fuzzy search and adding private chat).

## 1) Open-Source Components: Features and Code Modules
The project integrates several open-source or third-party libraries. Below are the main modules and how they map to features:

- Map and location layer (AMap SDK)
  - Used for map rendering, location, geocoding, and routing overlays.
  - Modules: `service.App`, `overlay.*`, `ui.MainActivity`, `ui.SelectLocationActivity`.

- Network layer (Retrofit, OkHttp, Gson)
  - Used for API calls and JSON serialization.
  - Modules: `data.ApiClient`, `data.ApiService`, `data.dto.*`.

- Image loading and display (Glide, PhotoView)
  - Used for avatars, note images, chat media, full-screen viewing.
  - Modules: `ui.PictureActivity`, `ui.NoteDetailActivity`, adapters.

- UI and recycler components (AndroidX, RecyclerView)
  - Used for list screens and UI composition.
  - Modules: adapters in `ui.adapter.*`, list-based Activities.

## 2) Open-Source Integration: Technical Details
### 2.1 Retrofit + OkHttp + Gson
- Retrofit defines endpoints in `data.ApiService`.
- OkHttp adds interceptors for authorization and logging in `data.ApiClient`.
- Gson is used for parsing responses and mapping DTOs to UI models.

Code snippet (API definition):
```java
public interface ApiService {
    @POST("/api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/api/oss/presign")
    Call<OssPresignResponse> presign(@Body OssPresignRequest request);
}
```

### 2.2 AMap SDK
- Map rendering and location updates are handled in `MainActivity`.
- Geocode and reverse geocode are used in `SelectLocationActivity` and note publishing.
- Custom overlays (`overlay.RouteOverlay`, `overlay.WalkRouteOverlay`) render routes on the map.

### 2.3 Glide + PhotoView
- Glide loads remote images (avatars, note cover images, chat media).
- PhotoView provides zoom/pan on full-screen images in `PictureActivity`.

## 3) APIs and Third-Party Tools Integration
### 3.1 Backend API (Aliyun Deployment)
The app connects to a REST backend for:
- Auth: login/register/update profile.
- Notes: CRUD, like/unlike, fetch lists.
- Comments: add/delete/like/unlike, threaded display support.
- Notifications: list, unread count, mark read.
- OSS: presigned upload URL for media.

Backend deployment:
- Hosted on an Aliyun server.
- Database and backend services are self-deployed and maintained by the team.

### 3.2 OSS Upload Pipeline
Step-by-step flow:
1) Request presigned URL (`/api/oss/presign`).
2) Upload media with OkHttp PUT.
3) Use returned URL in note publish or profile update.

## 4) Detailed Outline: Proposed Work, Plan, Workflow
### 4.1 Proposed Work
- Map-based note creation and discovery.
- Social interactions (likes, comments, notifications).
- User profile management and personalization.
- Private chat (offline local store).

### 4.2 Workflow and Task Breakdown
- Design: UI flows and data models.
- Implementation:
  - Data layer: DTOs, repositories, caches.
  - UI layer: activities, adapters, navigation.
  - Media: upload, display, caching.
- Testing:
  - Manual flows for publish/view/like/comment.
  - Regression checks on search and chat.

### 4.3 Team Coordination
- Not included in this version.

## 5) Added New Features (Why They Matter)
### 5.1 Fuzzy Search Fix
- User value: improves discoverability and tolerance to typos.
- UX impact: faster results and fewer dead-ends in search.

### 5.2 Private Chat (Local Offline)
- User value: enables direct communication without leaving the app.
- UX impact: smoother collaboration and clarification around notes.
- Privacy: stored locally, not sent to server.

## 6) Technical Report: Implementation Details
### 6.1 Module Overview
- Data: `data.*` repositories, DTOs, stores.
- UI: `ui.*` activities, adapters.
- Utilities: `overlay.*`, `utils.*`.

Block diagram (high level):
```
UI (Activities/Adapters)
    -> Repository (AliNoteRepository, AuthRepository, ChatRepository)
        -> ApiService / Local Stores
            -> Backend API / Local Storage
```

### 6.2 Notes and Comments Implementation (Step-by-Step)
1) Fetch list: `AliNoteRepository` calls `ApiService`, maps DTO to `MapNote`.
2) Render list: adapter binds data to cards.
3) Open detail: `NoteDetailActivity` loads note and comments.
4) Like/comment: repository updates server and local cache (`LikedStore`).

Code snippet (repository usage):
```java
repository.toggleLike(noteId, new DataCallback<LikeResponse>() {
    @Override
    public void onSuccess(LikeResponse data) {
        // update UI and local cache
    }
});
```

### 6.3 Chat Implementation (Step-by-Step)
1) Open conversation list: `ChatListActivity` calls `ChatRepository`.
2) Load messages: `ChatActivity` fetches local store data.
3) Send text/media: `ChatRepository` creates message, persists to local store.
4) Update UI: adapter adds message and scrolls to bottom.

Code snippet (local message persistence):
```java
ChatMessageResponse message = buildMessage(peerId, text, null, "text");
chatStore.appendMessage(peerId, message, null, null, false);
```

### 6.4 Search and Filtering
- Keyword/type filtering logic is handled in UI and repository query params.
- Results are shown in lists and synced with detail screens.

### 6.5 Contribution Summary (Role-Based)
- Data layer and API integration.
- UI screens and adapters.
- Local chat store and media handling.

## 7) Workflow, Challenges, Tools, Learning Outcomes
### 7.1 Workflow
- Iterative development: outline -> alpha -> beta -> final.
- Each milestone includes review of core flows and stability.

### 7.2 Challenges
- Map SDK key restrictions for external reviewers.
- Media upload stability and retry handling.
- Syncing like/comment states across list and detail views.

### 7.3 Coding Assistant Tools
- Used for drafting documentation and refactoring assistance.
- Manual verification and testing follow each generated change.

### 7.4 Learning Outcomes
- Improved understanding of mobile networking and caching.
- Experience integrating map SDKs and media pipelines.
- Building modular UI with repositories and adapters.

## 8) Progress Follow-Up and Feedback
- Feedback items are tracked against each milestone and re-verified in later builds.
- Regression checks focus on search, detail view, and media upload.

## 9) Effort, Depth, and Stage Output
### 9.1 Outline
- Feature definition, UI sketches, data models, and API plan.
### 9.2 Alpha
- Core map + notes publishing/viewing + basic profile.
### 9.3 Beta
- Completed likes, comments, notifications, profile updates.
### 9.4 Final
- Bug fixes and UX refinement (fuzzy search fix, private chat).

## 10) Novelty and Usefulness
- Combines map discovery with social notes and media.
- Supports both public discovery and private communication.
- Useful for location-based journaling, sharing, and exploration.

## 11) GUI, Ease of Use, Responsiveness
- Clear navigation: map -> detail -> actions.
- Bottom sheets and cards reduce context switching.
- Image preview and full-screen view improve media consumption.

## 12) Submission Commitment and Quality
- Each milestone included updated documentation and demo artifacts.
- Final submission aligns with prior feedback and fixes beta issues.
