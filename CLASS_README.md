# NoteMap 全类说明

以下按包划分，对项目中的每个类的作用与主要技术实现做中文说明，便于快速理解代码结构。

## 地图聚合与标记 (com.amap.apis.cluster.*)
- **Cluster**：单个聚合点的数据载体，记录聚合中心 `LatLng`、包含的 `ClusterItem` 列表以及地图上的 `Marker` 引用。提供添加元素、获取数量、中心点和绑定 `Marker` 的方法。
- **ClusterItem**：聚合元素接口，要求实现 `getPosition()` 返回地理坐标。`RegionItem` 等业务实体通过实现该接口参与聚合。
- **ClusterClickListener**：聚合点点击回调接口，返回被点击的 `Marker` 及其包含的 `ClusterItem` 列表，供业务跳转或展开。
- **ClusterRender**：聚合点自定义渲染接口，返回 `BitmapDescriptor`。未实现时由 `ClusterOverlay` 使用默认的圆形+数字样式。
- **ClusterOverlay**：聚合核心实现。内部使用两个 `HandlerThread` 分别计算聚合和添加/更新 `Marker`，根据屏幕可见区域和像素距离将点归并；默认 TextView+圆形背景生成聚合图标，支持 `ClusterRender` 自定义；监听相机变化实时重算，点击事件通过 `ClusterClickListener` 分发；包含渐隐删除和渐显新增的动画管理及 LruCache 缓存图标。
- **demo.RegionItem**：业务层的聚合数据模型，实现 `ClusterItem` 与 `Serializable`。包含地图坐标、笔记 ID/标题/封面、作者信息、点赞数、隐私标记、创建时间、图片列表等。支持延迟反序列化 `LatLng`，提供点赞状态、作者 ID、是否私密的读写接口，便于在地图和详情页复用。

## 数据层 (com.noworld.notemap.data.*)
- **ApiClient**：Retrofit 单例配置，基地址 `http://47.94.183.236:3000/`，使用 Gson 转换、OkHttp 日志拦截器和鉴权拦截器自动附加 `Authorization: Bearer <token>`，并配置超时。
- **ApiService**：后端接口定义，包括登录/注册、笔记 CRUD（查询、发布、更新、删除、点赞）、OSS 预签名上传、用户信息更新、评论增删查及点赞、通知列表/未读数/全部已读等 REST 方法。
- **AuthRepository**：登录、注册封装。调用 `ApiService`，成功后保存 token 至 `TokenStore`、用户信息至 `UserStore`，并在缺失时从 JWT 补齐 UID 和昵称，结果通过回调暴露。
- **MapNote**：业务笔记模型。定义字段常量；可从 `MapNoteResponse` 构建并向 `RegionItem` 转换，传递作者/隐私/创建时间等状态；提供封面获取、点赞数写入等。
- **AliNoteRepository**：主要数据仓库。职责包括：拉取笔记列表/全部笔记并结合本地点赞状态；发布笔记；通过 OSS 预签名上传图片（OkHttp PUT，支持 http->https 重试）；点赞笔记与本地缓存同步；获取/添加/删除/点赞评论并映射为 `CommentItem`；更新/删除笔记；更新用户资料；辅助获取当前 UID（从 token 解出或本地生成）。
- **TokenStore**：SharedPreferences 持久化 token，提供保存/读取/清除接口。
- **UserStore**：本地用户信息存储。生成/确保唯一 UID（可解析 JWT payload），保存昵称与头像并按 UID 记忆头像，提供清除与背景图 URI 存储；同时暴露当前用户对象获取。
- **LikedStore**：本地点赞状态缓存。按 UID 保存已点赞的笔记 ID 集合与每条笔记的点赞数，支持切换点赞状态与读取存储的计数。
- **model.CommentItem**：UI 层评论数据模型，包含评论 ID、用户/被回复者信息、时间、头像、父评论 ID、作者 ID、点赞数量与状态，以及“展开更多”占位标记与剩余数量。

## DTO 传输对象 (com.noworld.notemap.data.dto.*)
- **LoginRequest / RegisterRequest / UpdateProfileRequest / UpdateNoteRequest / PublishNoteRequest / AddCommentRequest / OssPresignRequest**：分别封装登录、注册、更新个人资料、更新笔记（内容/可见性）、发布笔记、发表评论、申请 OSS 预签名的请求体。
- **LoginResponse**：登录/注册返回，含 token 与用户信息 `UserDto`。
- **UpdateProfileResponse**：更新个人资料后的返回，包含提示信息与新的用户头像/昵称/UID。
- **MapNoteResponse**：笔记查询返回结构，包含基础信息、地理位置、作者信息、头像、点赞数、可见性与创建时间，支持多种头像字段名的反序列化。
- **OssPresignResponse**：OSS 预签名返回，包含上传 URL、文件访问 URL 与需附带的 header。
- **LikeResponse**：点赞/取消点赞返回的状态与当前点赞总数。
- **CommentResponse**：评论返回结构，使用 `@SerializedName` 兼容多种字段命名；含内容、作者昵称/头像、时间、父评论、被回复人、点赞数、点赞状态、作者 ID。
- **NotificationResponse**：通知返回结构，包含通知类型、目标类型/ID、可选 noteId、消息文案、时间、已读状态与触发者信息。

## 应用与地图工具
- **service.App**：自定义 `Application`，在启动时统一同意高德定位/地图/搜索的隐私政策。
- **overlay.AMapServicesUtil**：高德工具封装，提供输入流转字节、LatLng 与 LatLonPoint 互转、列表批量转换、位图缩放等。
- **overlay.RouteOverlay**：通用路线叠加层，管理起终点/站点 `Marker` 与折线集合，提供添加/清理、缩放到线路范围、节点显隐控制以及默认的起终点/公交/步行/驾车图标与颜色配置。
- **overlay.WalkRouteOverlay**：步行路线叠加实现，基于 `WalkPath` 解析每段 `WalkStep`，生成节点 `Marker` 与折线，处理步行段衔接缺口，调用父类绘制并加入起终点。
- **utils.MapUtil / ChString**：地图辅助方法（坐标互转、友好时长/距离格式化）与通用英文文案常量。

## UI 活动 (com.noworld.notemap.ui.*)
- **MainActivity**：地图主界面。负责定位权限与蓝点配置、AMap 初始化、地理编码、笔记拉取与缓存、搜索关键词/类型过滤、底部搜索结果面板、通知徽标、FAB 操作（回到定位/发布笔记/个人主页/视图切换）、自定义 Marker 与聚合渲染（实现 `ClusterRender`/`ClusterClickListener`），点击聚合跳转详情或聚合列表，支持地名搜索定位。
- **NoteDetailActivity**：笔记详情页。展示标题/正文/类型/地点/时间/作者与图片轮播；支持点赞（同步本地缓存）、评论列表树状展开、回复/删除/点赞评论、滚动并高亮指定评论；作者专享的菜单项可切换私密/公开、编辑（跳转 `AddNoteActivity`）或删除笔记；未登录操作会跳转登录；图片点击进入全屏查看。
- **AddNoteActivity**：发布/编辑笔记页。处理图片多选与删除（最多 9 张）、位置选择（当前定位或地图选点+逆地理）、笔记类型选择、字段校验；发布模式下串行上传图片到 OSS 后调用发布接口；编辑模式下仅提交标题/正文更新；利用 ActivityResultLauncher 获取图片与位置权限/结果。
- **PictureActivity**：全屏图片查看与保存。使用 PhotoView 支持手势缩放，长按确认后通过 Glide 拉取位图，添加带作者信息的水印并请求存储权限后保存到图库。
- **SearchResultActivity**：搜索结果展示，上半部地图标记匹配笔记，下方 BottomSheet 列表使用 `NoteCardAdapter` 展示同一数据，初始化时自动移动摄像头到结果中心。
- **ClusterDetailActivity**：聚合列表页，将点击聚合得到的 `RegionItem` 列表以瀑布流两列展示，点击返回。
- **ProfileActivity**：个人主页。根据登录状态显示头像/昵称/UID/签名；支持更换头像（上传 OSS 后调用后端更新）、更换并模糊展示背景图、本地保存背景 URI；提供昵称编辑；Tab 切换“我的作品”和“我的点赞”，从后端获取笔记并结合本地点赞缓存，支持在点赞列表中取消点赞；包含登录/登出处理及加载状态。
- **NotificationActivity**：通知列表页。调用接口拉取通知，使用 `NotificationAdapter` 展示；点击通知根据目标类型跳转到对应笔记详情，若是评论类可定位到目标评论；加载时也可根据 commentId 逐笔记匹配。
- **SelectLocationActivity**：地图选点页，实现地图点击与正/逆地理编码搜索，支持搜索栏正向地理编码，返回选定经纬度与地址。
- **SplashActivity**：启动页，延时 2 秒跳转主界面并使用淡入淡出动画。
- **LoginActivity / RegisterActivity**：登录与注册页，做基本输入校验、展示加载状态，调用 `AuthRepository` 成功后跳转或关闭。
- **NotificationBadgeHelper**：徽标文本视图的包装，0 隐藏，显示时上限 99。

## UI 适配器与组件
- **NoteCardAdapter**：笔记卡片列表适配器，绑定标题/作者/封面/头像/点赞数，附带点赞按钮逻辑（调用仓库并同步 `LikedStore`），卡片点击跳转详情，支持外部监听取消点赞事件。
- **CommentAdapter**：评论列表适配器，区分评论项与“展开更多”占位，支持回复/长按删除/点赞回调，自动缩进子评论并显示被回复人，使用 Glide 加载头像。
- **NotificationAdapter**：通知列表适配器，绑定标题/副标题/时间与头像，点击透传到监听器。
- **ui.adapter.ImagePreviewAdapter**：发布页图片预览适配器（独立文件版），使用 Glide 加载选中图片并在红色删除按钮点击时通知外部移除。
- **ui.adapter.MyNotesAdapter**：展示 `MapNote` 的通用适配器，可配置显示“取消点赞”按钮及列表点击回调，支持按 ID 移除项。

## 其他
- **data/service/overlay/utils 等依赖类**：AMap 隐私/路线工具类、文本常量等，为地图与路线展示提供底层支持，已在上述对应章节说明。

## 后端 Node/Express 服务（server.js 概要）
- 技术栈与基础：Express + CORS + body-parser；MySQL（mysql2/promise 连接池）；JWT 认证；bcrypt 密码哈希；Joi 请求校验；阿里云 OSS SDK 生成 PUT 预签名；封装可选鉴权 `tryAuthMiddleware` 与必选鉴权 `authMiddleware`。
- 账户体系：注册（邮箱+密码+昵称，检查重复，bcrypt 加盐存储，签发 7 天 JWT）、登录（校验邮箱/密码返回 token+用户信息）、个人资料更新（昵称/头像）。JWT payload 存储 uid/username/avatar，所有受保护接口基于 Authorization Bearer。
- 笔记接口：获取笔记支持关键词/类型过滤，且隐私控制（仅作者可见私密笔记）；返回时补齐作者昵称/头像、创建时间格式化。发布笔记（含经纬度、位置名、类型、图片数组、可选私密），更新笔记（标题/描述/私密，需作者身份），删除笔记（作者可删，同时删点赞记录），笔记点赞/取消点赞（事务更新 likes 表与笔记 like_count，触发通知）。
- 评论接口：分页获取评论（带作者昵称/头像、作者 ID、点赞状态、父评论信息）；发表评论（支持 parentId，自动带被回复用户昵称，通知笔记作者及被回复者）；评论点赞/取消点赞（更新计数并通知被赞作者）；删除评论（仅作者，连带删除子回复和点赞）。
- 通知接口：获取未读数、分页拉取通知列表（带触发者昵称/头像）、标记全部已读。通知写入在点赞/评论等动作中调用 `addNotification`。
- OSS 上传：`/api/oss/presign` 返回 PUT 预签名 URL、文件访问 URL 和需附带的 Content-Type header，客户端直接上传到指定 bucket。

## 功能串联与资源存储速览
- 功能流转：`MainActivity` 拉取笔记 -> 聚合/Marker 渲染 -> `NoteCardAdapter`/搜索面板/聚合点击进入 `NoteDetailActivity` -> 详情页点赞、评论、图片查看 (`PictureActivity`)；发布/编辑 (`AddNoteActivity`) 通过仓库上传图片后调用后端；个人主页 (`ProfileActivity`) 汇总我发布/我点赞，支持头像/背景更新。
- 点赞/评论：前端调用 `AliNoteRepository.toggleLike` 和 `toggleCommentLike` 同步本地 `LikedStore`/UI；评论树由 `NoteDetailActivity + CommentAdapter` 管理，回复、删除、点赞全部透传到后端对应接口。
- 笔记图片/头像存储：
  - 图片：发布时通过 `/api/oss/presign` 获取 PUT URL，直接上传到阿里云 OSS `notemap-prod-oss`（或配置的 bucket），地址写入 `notes.image_urls` JSON；前端用 Glide 加载这些 URL。
  - 作者头像：用户更新头像同样走 OSS 上传+`/api/auth/update`，后端存 `users.avatar_url`；笔记查询时 LEFT JOIN 返回 `authorAvatarUrl`，前端 `MapNoteResponse -> MapNote -> RegionItem` 映射后显示。
  - 本地缓存：`LikedStore` 存点赞状态和计数；`UserStore` 存 token 解析出的 uid/昵称/头像，以及个人主页背景图 URI（仅本地）。
