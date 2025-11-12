**问题定位**
- 图标高亮错误：设置入口点击后仍调用 `updateBottomBarButtonsState(R.id.nav_profile)`，导致齿轮图标被重置为灰色，蓝色仍留在“个人资料”图标；位置：`app/src/main/java/com/example/facecheck/MainActivity.java:115–126`。
- 主题切换跳回其他页：`AppCompatDelegate.setDefaultNightMode(...)` 触发 Activity 重建，`MainActivity` 恢复默认 `nav_selected_id = R.id.nav_profile`，导致设置页被覆盖；位置：`MainActivity.java:128–132`。
- WebDAV 配置无效：当前逻辑在 WebDAV 关闭时禁用配置按钮，用户点击无响应；位置：`SettingsFragment.java` 的 `updateWebDavStatus()`。
- 结构一致性：设置页布局与其他页（首页、考勤、课堂）风格不完全统一（背景、容器类型、padding）；需要统一为相同的根容器与间距规范。

**改造目标**
- 将“设置”视为与首页/课堂/考勤/个人资料同级的底部标签页，使用相同容器 `fragment_container`，共享 `toolbar` 与底部栏，统一交互结构与视觉。
- 修复主题切换后页面跳转与图标高亮问题，并保持重建后仍停留在设置页。
- 使 WebDAV 配置按钮在任何状态下均可打开配置对话框，提升可用性。

**实现步骤**
1) 新增设置标签 ID 并接入底栏状态
- 新增资源 ID：`res/values/ids.xml` 内定义 `@id/nav_settings`。
- 修改 `MainActivity.handleNavigation(int)` 支持 `R.id.nav_settings` 分支：加载 `SettingsFragment`，设置标题“设置”。
- 修改齿轮点击逻辑：
  - 加载 `SettingsFragment`，调用 `updateBottomBarButtonsState(R.id.nav_settings)`；
  - 将 `ui_prefs.nav_selected_id` 保存为 `R.id.nav_settings`，用于重建恢复；位置：`MainActivity.java:115–126`。
- 扩展 `updateBottomBarButtonsState(int)`：为 `btnNavSettings` 增加选中态逻辑，当传入 `R.id.nav_settings` 时将齿轮着色为选中颜色，其他图标置为默认。

2) 保持主题切换后仍在设置页
- 在 `SettingsFragment.applyThemeMode(...)` 设置主题后，立即将 `ui_prefs.nav_selected_id = R.id.nav_settings` 写入，以覆盖默认值。
- 在 `MainActivity.onCreate(...)` 读取 `nav_selected_id` 时，若值为 `R.id.nav_settings`，直接加载 `SettingsFragment`（无需走其他分支），以确保重建后停留在设置页；位置：`MainActivity.java:128–132`。

3) 修复 WebDAV 配置不可点击与交互提示
- 调整 `SettingsFragment.updateWebDavStatus()`：
  - `webdavConfigButton.setEnabled(true)` 始终可点；
  - `syncNowButton` 在未配置或禁用时保留禁用，但点击时弹出提示“请先启用并配置 WebDAV”；
  - 文案区分“已启用”“未配置”“已禁用”，但配置入口始终可用。
- 保持对话框 `showWebDavConfigDialog()` 逻辑不变，确认保存后刷新状态。

4) 统一设置页布局结构
- 保持 `fragment_settings.xml` 的 `NestedScrollView + LinearLayout`，统一 padding 与分割线风格参照 `fragment_home.xml`/`fragment_profile.xml`（`start/end/bottom = 16dp, top = 0dp`）。
- 去除显式 `android:background="@color/colorBackground"` 以与其它 Fragment 保持一致背景；如需兼容夜间模式，依赖主题自动切换即可。
- 保留 Profile 非个人资料区块的原样迁移与按钮 ID（确保逻辑复用），并保持“缓存管理”三按钮与横向进度条。

**代码改动文件**
- `app/src/main/java/com/example/facecheck/MainActivity.java`：新增 `nav_settings` 处理分支，修正齿轮点击逻辑与状态保存、`updateBottomBarButtonsState` 支持设置选中态。
- `app/src/main/res/values/ids.xml`：新增 `@id/nav_settings`。
- `app/src/main/java/com/example/facecheck/fragments/SettingsFragment.java`：
  - `applyThemeMode(...)` 中写入 `ui_prefs.nav_selected_id = R.id.nav_settings`；
  - `updateWebDavStatus()` 始终启用 `webdavConfigButton`，并在 `syncNowButton` 点击时增加禁用/未配置的提示；
  - 其他逻辑保持原迁移不变。
- `app/src/main/res/layout/fragment_settings.xml`：移除显式背景，检查 padding 与分割线一致性（与首页/个人资料对齐）。

**验证清单**
- 点击底部齿轮进入设置：标题显示“设置”，齿轮高亮为蓝色，其它图标回到默认色。
- 切换主题为深色/浅色：Activity 重建后仍停留在设置页，齿轮仍为高亮，结构与高度不变。
- 点击“WebDAV 配置”：始终弹出配置对话框；保存后状态更新，启用“立即同步”。未启用时点击“立即同步”提示启用/配置。
- 返回键可回到之前页面；从设置返回到其他标签正常。

如认可该方案，我将按以上步骤实现并回归测试。