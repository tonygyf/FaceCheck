## 目标
- 设置页将“关于 FaceCheck”从底部弹层移到主页面“更多设置”下方。
- 完成“更多设置”中的 WebDAV 配置、立即同步、缓存清理等功能实现。

## 变更范围
- `fragment_settings.xml`：新增“关于 FaceCheck”卡片（位于“更多设置”下方）。
- `SettingsFragment`：
  - 为新增的“关于 FaceCheck”卡片添加点击逻辑；显示版本信息弹窗。
  - 接管“更多设置”底部弹层各按钮的真实实现：WebDAV 配置对话框、立即同步、清理缓存。
- 新增/使用偏好键：`settings_prefs.theme_mode`（已用）、`webdav_prefs`（新增：`webdav_url`,`webdav_username`,`webdav_password`,`webdav_enabled`,`webdav_last_sync`）。

## 功能实现
### 关于 FaceCheck
1. 在设置页主界面新增“关于”卡片，点击后显示版本信息弹窗（读取 `packageManager.getPackageInfo`）。

### WebDAV 配置
1. 使用现有布局 `dialog_webdav_config.xml` 弹出配置对话框，包含 URL/用户名/密码，按钮：测试连接、保存、取消。
2. 交互：
   - 测试连接：基于已集成的 `WebDavManager`，调用 `testConnection()` 显示成功/失败文案。
   - 保存：将表单值写入 `webdav_prefs` 并设置 `webdav_enabled=true`；不记录日志中的敏感信息。

### 立即同步
1. 从 `webdav_prefs` 读取凭据；若未启用或缺少配置，给出提示。
2. 执行同步：
   - 初始化远端目录结构：`WebDavManager.initializeDirectoryStructure()`。
   - 同步本地数据库：通过 `DatabaseHelper.getDatabaseAbsolutePath()`，调用 `WebDavManager.syncDatabase(...)`。
   - 同步头像与考勤照片：遍历 `PhotoStorageManager.getAvatarPhotosDir(...)` 与 `getAttendancePhotosDir(...)` 下文件，分别调用 `syncAvatar(...)`、`syncAttendancePhoto(...)`；按文件名上传。
3. 结果回写：成功时更新 `webdav_last_sync` 时间戳，并以 Toast 显示完成信息；失败时显示失败原因。

### 缓存处理
1. 使用现有 `CacheManager`：
   - 清理所有缓存：`clearAllCache(callback)`。
   - 仅清理图片缓存：`clearImageCache(callback)`。
   - 清理临时缓存：`clearTempCache(callback)`。
2. 显示进度/结果：通过回调在主线程展示 Toast，完成后给出释放空间统计。

## 步骤与验证
1. 第一步：调整 `fragment_settings.xml` 和 `SettingsFragment`，实现“关于 FaceCheck”卡片与点击弹窗。
2. 第二步：实现 WebDAV 配置对话框逻辑与偏好存储、连接测试。
3. 第三步：实现“立即同步”端到端上传（数据库、头像、考勤照片）。
4. 第四步：实现缓存清理三个按钮的回调。
5. 编译验证（Gradle 组装），手动运行核验交互；不提交代码。

## 说明
- 所有变更遵循最小原则，仅修改设置页相关逻辑。
- WebDAV 凭据保存于应用内部偏好；不会打印或外泄。
- 如需扩展同步范围（处理后人脸等），可在确认后补充相应目录的上传。