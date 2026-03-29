
# FaceCheck历史记录-Bug修复

---
## 原始文件: 修复 Student 表 sid 唯一性与班级初始化问题.md
---

## 问题复盘
- 目前 `Student` 表仅建立普通索引 `idx_student_sid`，未强制唯一，导致出现相同 `sid` 的学生。
- 资产初始化（从 assets 导入）与后续导入（如小人脸导入）使用简单递增或固定起始值，未检查班级内是否已存在相同 `sid`。

## 修复目标
- 在“班级内”保证 `sid` 唯一（`(classId, sid)` 组合唯一），更符合实际学校场景。
- 清理已存在重复 `sid` 的数据，生成不冲突的 `sid`。
- 后续插入与更新均自动避重，避免再次产生重复。

## 实施步骤
### 第1步：数据库约束
- 新增唯一索引：`CREATE UNIQUE INDEX IF NOT EXISTS uniq_student_class_sid ON Student(classId, sid)`。
- 保留原有非唯一索引，提升查询效率。

### 第2步：重复数据清理
- 在 `DatabaseHelper` 增加工具方法：
  - `fixDuplicateSids()`：按 `classId` 分组，查找重复 `sid`，对第二个及后续记录生成新的不冲突 `sid`（规则：若 `sid` 为纯数字，则在原值后加序号；若包含字母则加后缀 `-n`）。
  - 生成方式：
    - 获取该班级当前已占用的 `sid` 集合
    - 从原 `sid` 开始递增或加后缀，直到唯一
  - 在应用启动（`FaceCheckApp.onCreate`）或迁移入口调用一次，确保上线后数据合规。

### 第3步：插入与更新防重
- 修改 `DatabaseHelper.insertStudent(...)` 与相关导入路径：
  - 新增 `getNextAvailableSid(classId, baseSid)`，若 `baseSid` 冲突则生成下一个可用 `sid`，用于插入；更新时亦校验冲突并调整。
- 修改“小人脸导入”页面 `FaceMiniDetectActivity`：
  - 使用 `getNextAvailableSid(classId, String.valueOf(index))` 生成不冲突 `sid`。

### 第4步：资产初始化稳健化
- 在 `seedStudentsFromAssets(...)` 导入时：
  - 仍使用基数（如 1000/2000）递增，但每次通过 `getNextAvailableSid(...)` 校验唯一性；如已存在则自动跳过到下一可用值。

### 第5步：验证
- 运行迁移：执行 `fixDuplicateSids()` 后重建唯一索引；
- 新增/导入学生：在有重复的班级中插入时能自动避重；
- 全局检查：`SELECT classId, sid, COUNT(*) FROM Student GROUP BY classId, sid HAVING COUNT(*)>1` 返回空。

---
## 原始文件: 修复个人资料页空指针并完善启动逻辑.md
---

## 问题复盘
- 个人资料页存在大量 `getActivity()` 直接使用，Fragment 未附着或视图已销毁时易触发 NPE。
- 进入应用仍为空白且未直达首页：当前 Launcher 为 `LoginActivity`，自动登录依赖 `remember_password=true` 且 `teacher_id != -1`，不满足时停留登录页。

## 改动方案
### ProfileFragment 关键空指针修复
- 将 `new DatabaseHelper(getActivity())` 改为 `new DatabaseHelper(requireContext())`。
- 关键路径统一使用安全上下文：`SharedPreferences/AlertDialog/FileProvider` 等改用 `requireContext()/requireActivity()`，并增加 `isAdded()` 校验。
- UI 更新加生命周期保护，并在 `onDestroyView()` 中将视图置 `null`。

### 进入应用空白与未直达首页处理
- 修改自动登录条件：只要 `teacher_id != -1` 即跳转 `MainActivity`，不再依赖 `remember_password`。
- 在 `MainActivity.onCreate` 增加容错，若未成功加载 Fragment，则强制加载首页。

---
## 原始文件: 修复学生登录白屏与循环跳转问题.md
---

## 问题分析
- **根因**: 学生登录成功后，默认加载的 `ProfileFragment`（我的）页面是为教师设计的。该页面检测到没有 `teacher_id` 时，会强制跳转回登录页，而登录页检测到学生已登录又会跳回主页，形成死循环。

## 修复方案
1.  **登录后按角色重置首页**: 登录成功时，如果是学生，强制将默认导航页设为首页 (`R.id.nav_home`)。
2.  **主页根据角色修正导航**: `MainActivity` 启动时，检查用户角色。如果学生尝试访问教师专用页面（如“我的”），强制切换到首页。
3.  **`ProfileFragment` 降级处理**: `ProfileFragment` 对学生角色不再强制跳转，而是显示提示信息并禁用教师专属功能。

