# MapNotes Android Project - 前端交接文档 (Frontend Handover)

**提交人:** Member A (Frontend Development)  
**日期:** 2025-11-17  
**版本:** Alpha v1.0 (UI & Logic Framework)

---

## 1. 项目概况与完成度
本项目 **MapNotes** 的前端 UI 框架与核心交互逻辑已搭建完成。目前应用处于 **"UI Ready, Mock Data Driven"**（UI 就绪，本地模拟数据驱动）状态。
所有核心页面（地图、详情、发布、个人中心、登录注册）均已实现布局与跳转逻辑，地图部分已集成高德 SDK 并实现了复杂的聚合（Clustering）与自定义标记功能。

### ✅ 已完成模块 (Delivered Modules)

| 模块 | 状态 | 描述 | 关键文件 (Java / XML) |
| :--- | :--- | :--- | :--- |
| **地图主页** | ✅ 完成 | 集成高德地图，实现定位、POI 搜索、**照片标记 (Photo Marker)**、**点聚合 (Clustering)**。 | `MainActivity`<br>`activity_main.xml` |
| **笔记发布** | ✅ 完成 | 包含图片选择(UI)、位置自动反向地理编码(显示街道名)、类型选择。 | `AddNoteActivity`<br>`activity_add_note.xml` |
| **笔记详情** | ✅ 完成 | 展示笔记大图、标题、描述、位置、类型。支持从地图 Marker 跳转。 | `NoteDetailActivity`<br>`activity_note_detail.xml` |
| **聚合列表** | ✅ 完成 | 点击聚合点后显示的瀑布流列表，展示该区域所有笔记。 | `ClusterDetailActivity`<br>`activity_cluster_detail.xml` |
| **个人中心** | ✅ 完成 | 包含用户信息头、"作品/点赞" Tab 切换逻辑。 | `ProfileActivity`<br>`activity_profile.xml` |
| **认证页面** | ✅ 完成 | 登录与注册页面的完整 UI 布局与空壳逻辑。 | `LoginActivity`, `RegisterActivity`<br>`activity_login.xml` |

---

## 2. 核心技术实现说明 (Key Technical Details)

### A. 地图聚合与自定义 Marker (核心亮点)
为了实现规划书中 *"Map-first content discovery"* 的目标，地图层采用了自定义渲染：
* **自定义 Marker**: 没有使用默认大头针。在 `MainActivity` 中预留了 `getCustomMarkerIcon()` 方法，通过 `layout_marker_photo.xml` 将图片动态转换为 Marker 图标。
* **聚合 (Clustering)**: 引入了 `ClusterOverlay`, `Cluster`, `ClusterRender` 等辅助类。当大量笔记聚集时，会自动合并为一个带有数字的圆圈，点击后跳转至 `ClusterDetailActivity` 查看列表。

### B. 数据传递模型
页面间通过 Intent 传递数据，核心模型为 `RegionItem` (需确保后端对接时保持一致或进行适配)：
* **传递 Key**: `NoteDetailActivity.EXTRA_NOTE_DATA`
* **数据流**: `MainActivity` (Mock Data) -> `Intent` -> `NoteDetailActivity` / `ClusterDetailActivity`。

### C. 第三方库依赖
* **高德地图 SDK**: 用于地图显示、定位、逆地理编码 (Regeocode)。
* **Glide**: 用于加载笔记图片和用户头像。
* **Material Design**: 使用了 `TabLayout`, `FloatingActionButton`, `CardView` 等组件。

---

## 3. 后端开发对接指南 (For Member B)

Member B，你的主要任务是将目前的“本地假数据”替换为 **Firebase** 的真实数据，并跑通“发布-存储-展示”的闭环。

### 📋 待办事项清单

#### 0. 环境与配置 (Environment Setup)
* **Firebase Console**: 请新建项目，下载 `google-services.json` 放入 `app/` 目录。
* **Gradle**: 添加 Firestore, Storage, Analytics 等 SDK 依赖。
* **权限**: 请检查 `AndroidManifest.xml`，确保已申请 `INTERNET` 和读写存储权限（用于图片上传）。

#### 1. 数据库设计 (Firestore)
* **User Collection**: `uid`, `username`, `email`, `avatarUrl`...
* **Notes Collection**:
    * `title` (String)
    * `description` (String)
    * `type` (String: "美食", "风景"...)
    * `geo_point` (GeoPoint: lat, lng) - **关键**：用于地图标记
    * `location_name` (String: "北京市朝阳区...")
    * `image_url` (String 或 List<String>) - *建议设计为 Array 以支持未来的多图功能*。
    * `author_id` (String) - *注：在登录功能完成前，发布时可暂时硬编码为 "test_user_id"*。
    * `timestamp` (ServerTimestamp)

#### 2. 接入真实数据源 (Data Integration)

* **MainActivity (地图主页)**
    * **现状**: 目前代码逻辑是 **高德 POI 搜索**。通过 `doSearchQuery()` 搜索并在 `onPoiSearched()` 中显示默认蓝色 Marker。
    * **Member B 任务**:
        1.  **移除/注释**: 暂时注释掉 `doSearchQuery()` 和 POI 相关的回调逻辑。
        2.  **监听数据**: 在 `onCreate` 或 `onResume` 中监听 Firestore 的 `Notes` 集合 (`addSnapshotListener`)。
        3.  **渲染 Marker**: 获取数据后，遍历文档，提取经纬度，调用 `aMap.addMarker()`。
        4.  **自定义图标 (重要)**: 目前前端未实现 `layout_marker_photo.xml`。你需要编写代码，使用 `Glide` 下载 `image_url`，将其转为 `BitmapDescriptor`，实现项目规划中的“照片 Marker”功能。

* **AddNoteActivity (发布页)**
    * **现状**: UI 完备。
        * **图片**: 已通过 `ActivityResultLauncher` 拿到系统图库的 `Uri`。
        * **位置**: 已通过 `RegeocodeQuery` 拿到街道名称。
        * **按钮**: `btn_publish` 点击事件目前为空。
    * **Member B 任务**: 请在发布按钮点击事件中实现以下逻辑：
        1.  **上传图片**: 使用 Firebase Storage SDK 上传当前选中的 `Uri`。
        2.  **获取链接**: 上传成功后拿到 `downloadUrl`。
        3.  **写入数据库**: 将标题、位置、图片 URL 等封装写入 Firestore。
        4.  **收尾**: 成功后调用 `finish()` 关闭页面。

#### 3. 图片加载适配
* 前端目前在 `NoteDetailActivity` 和 `NoteCardAdapter` 中使用了 Glide 加载图片。
* 接入后，请确保传给 `RegionItem` (或你定义的新模型) 的 `photoUrl` 是 Firebase Storage 的网络地址，Glide 会自动处理显示。
---

## 4. 交互与优化对接指南 (For Member C)

Member C，你的任务是在 Member B 完成数据层后，完善用户系统和细节体验。

### 📋 待办事项清单
1.  **认证逻辑实现**
    * 我在 `LoginActivity` 和 `RegisterActivity` 中留下了 `TODO` 注释。
    * 请接入 **Firebase Auth** (Email/Password)，并在登录成功后保存用户信息。
    * **入口**: 目前 `ProfileActivity` 点击头像可跳转登录页，请完善“已登录/未登录”的 UI 状态切换。

2.  **互动功能**
    * **点赞**: `NoteCardAdapter` 中有爱心图标 (`iv_like_icon`)，请添加点击事件监听，更新 Firestore 计数。

3.  **UI/UX 增强 (可选/加分项)**
    * **多图轮播**: 目前详情页是单图。如果时间允许，请将 `NoteDetailActivity` 的 `ImageView` 替换为 `ViewPager2`。
    * **筛选功能**: 我在 `MainActivity` 中预留了筛选逻辑。请配合 Member B 的查询接口，实现基于“标签”的地图点过滤。

---

## 5. 文件结构导航

```text
com.noworld.notemap
├── overlay/                # 地图覆盖物相关 (Route, Overlay)
│   ├── ClusterOverlay.java # 聚合核心逻辑
│   └── ...
├── service/                # 应用级服务
│   └── App.java            # SDK 初始化与隐私检查
├── ui/                     # 界面层
│   ├── AddNoteActivity.java      # 发布笔记
│   ├── ClusterDetailActivity.java # 聚合列表页
│   ├── LoginActivity.java        # 登录 (UI Only)
│   ├── MainActivity.java         # 地图主页 (Core)
│   ├── NoteCardAdapter.java      # 列表适配器
│   ├── NoteDetailActivity.java   # 笔记详情
│   ├── PictureActivity.java      # (辅助) 图片查看
│   ├── ProfileActivity.java      # 个人中心
│   └── RegisterActivity.java     # 注册 (UI Only)
└── utils/                  # 工具类
    ├── MapUtil.java
    └── ChString.java