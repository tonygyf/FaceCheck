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

## 影响范围与安全
- 仅新增唯一索引与工具方法，不变更表结构；对现有重复数据进行安全重命名。
- 改动遵循最小原则，避免数据丢失；重命名策略可在后续按你的规则微调（如按学号规范）。

## 交付顺序
1) 添加唯一索引与工具方法
2) 执行重复清理
3) 更新插入与导入逻辑
4) 验证与回归

如果确认按此方案推进，我将开始实现并逐步验证。