**目标**
- 设置页采用与其他页面一致的“单 Activity + 多 Fragment”结构，复用 `MainActivity` 的 `toolbar` 和底部栏。
- 用 `fragment_settings.xml` 替换当前 `activity_settings.xml` 的视觉与结构，迁移 `fragment_profile.xml` 中除个人资料相关外的全部界面代码。
- 保留并接入现有设置逻辑（主题、WebDAV、同步、缓存、关于）。

**导航与结构改造**
- 新增 `SettingsFragment`（`com.example.facecheck.fragments.SettingsFragment`），在 `onCreateView` 加载新布局，在 `onViewCreated` 绑定与初始化各控件逻辑。
- 修改 `MainActivity` 中设置按钮点击逻辑：将 `startActivity(SettingsActivity)` 改为切换到 `SettingsFragment` 以共享 `toolbar` 与底部栏。
  - 改动点：`d:\typer\android_demo\FaceCheck\app\src\main\java\com\example\facecheck\MainActivity.java:121–125`
  - 保持现有 Fragment 切换模式：`replace(R.id.fragment_container, new SettingsFragment())` 并 `addToBackStack("settings")`。
- 暂不删除 `SettingsActivity` 与清单声明，待功能验证后可移除（避免引入风险）。

**布局迁移与整理**
- 新增 `app/src/main/res/layout/fragment_settings.xml`：以 `fragment_profile.xml` 为基础，原封不动搬迁以下“非个人资料”块（ID 保持一致），并在 Fragment 中将其 `visibility` 设置为可见：
  - 页面 Logo `tv_logo_title`（`fragment_profile.xml:24–35`）
  - 主题选择区 `btn_theme_system`（`52–61`）、`btn_theme_dark`（`67–75`）、`btn_theme_light`（`82–90`）
  - WebDAV 与同步：`switch_webdav`（`114–119`）、`btn_webdav_config`（`251–260`）、`tv_webdav_status`（`262–272`）、`btn_sync_now`（`274–282`）
  - 关于入口 `item_about`（`147–170`）
  - 缓存状态显示 `tv_cache_status`（`284–296`）与页面级 `progress_bar`（`320–327`）
- 为不丢失现有缓存清理功能，保留 `activity_settings.xml` 中的三枚缓存清理按钮与横向进度条（ID 不变）：
  - `btn_clear_all_cache`（`activity_settings.xml:198–205`）、`btn_clear_image_cache`（`211–218`）、`btn_clear_temp_cache`（`224–231`）
  - `progress_bar_cache`（`234–241`）
  - 将以上控件并入 `fragment_settings.xml` 的“缓存管理”分区，使逻辑无缝迁移。
- 视觉策略：沿用 `fragment_profile.xml` 的分页结构、分割线、留白与色彩（`@color/primary`, `@color/text_primary` 等），通过显示 `tv_logo_title` 提升观感；其他属性与控件顺序保持与 Profile 一致。

**逻辑迁移与适配**
- 将 `SettingsActivity` 中的逻辑方法迁移到 `SettingsFragment`，并做 Fragment 语境适配：
  - 主题：`initThemeFromPrefs`、`applyThemeMode`、`updateThemeButtons`（保留 `AppCompatDelegate.setDefaultNightMode`）
  - WebDAV：`initWebDavManager`、`updateWebDavStatus`、`showWebDavConfigDialog`（用 `requireContext()`/`requireActivity()`、`AlertDialog.Builder(requireContext())`）
  - 同步：`syncWithWebDav`（线程逻辑保留，回调改为 `requireActivity().runOnUiThread(...)`）
  - 缓存：`initCacheManager`、`loadCacheSize`、`updateCacheSizeDisplay`、`clearAllCache`、`clearImageCache`、`clearTempCache`、`showCacheCleaningProgress`、`formatFileSize`
- 视图绑定：在 `onViewCreated` 用 `view.findViewById(...)` 绑定迁移控件，ID 与 Activity 版保持一致，减少改动面。
- 可见性：对 Profile 中默认 `visibility="gone"` 的块，在 `SettingsFragment` 中统一 `setVisibility(View.VISIBLE)`，避免变更 XML 以符合“代码原封不动迁移”的要求。

**代码引用与接入点**
- Profile 布局加载：`d:\typer\android_demo\FaceCheck\app\src\main\java\com\example\facecheck\fragments\ProfileFragment.java:82`
- 主容器替换：`d:\typer\android_demo\FaceCheck\app\src\main\java\com\example\facecheck\MainActivity.java:198`
- 现有设置入口（将替换）：`d:\typer\android_demo\FaceCheck\app\src\main\java\com\example\facecheck\MainActivity.java:121–125`

**验证步骤**
- 启动应用，点击底部“设置”按钮切换到 `SettingsFragment`，确认 `toolbar` 与底部栏持续存在。
- 验证主题切换按钮与选中态背景变更。
- 打开 WebDAV 配置对话框，测试连接、保存配置，观察状态与按钮使能变化。
- 运行“立即同步”，检查进度条与成功/失败提示。
- 检查缓存状态显示与三类缓存清理流程，观察进度与提示文本更新。
- 打开“关于 FaceCheck”，版本信息正确显示。

如确认以上方案，进入实现与联调阶段。