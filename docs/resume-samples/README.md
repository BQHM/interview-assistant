# 简历测试样本

用于验证简历上传、Tika 文本解析、AI 分析与规则兜底、文件去重这几条链路。
内容为虚构人物（陆晨，Java 后端 3 年），联系方式全部是占位数据，可放心提交和截图。

## 文件清单

| 文件 | 用途 |
| --- | --- |
| `sample-resume-java-backend.docx` | 主样本，覆盖 `application/vnd.openxmlformats-officedocument.wordprocessingml.document` 解析路径 |
| `sample-resume-java-backend.pdf` | 同一份内容的 PDF 版，覆盖 `application/pdf` 解析路径，且是真实世界里最常见的投递格式 |
| `sample-resume-java-backend.txt` | 纯文本版，覆盖 `text/plain` 解析路径，出问题时最容易对比“解析出来的文本对不对” |
| `resume-template-blank.docx` | 空白模板，`【占位符】` 结构相同，改成自己的经历后可直接复用 |
| `build_resume_samples.py` | 生成脚本，改内容或改排版后重新执行即可，保证 DOCX 与 TXT 内容一致 |

三份样本是同一份内容，只差载体格式。用它们对比解析结果，就能判断问题出在解析层还是分析层：
三种格式解析出的正文应该基本一致，如果只有 PDF 解析异常，问题就在 Tika 的 PDF 抽取。

## 上传方式

前端：`/resumes/upload` 页面，或直接拖进上传区。
接口：`POST /api/resumes/upload`，`multipart/form-data`，字段名固定为 `file`。

## 正向用例与预期结果

### 1. 上传 DOCX / PDF / TXT（任选其一）

- 返回 `Result<ResumeUploadResponseDTO>`，`duplicate = false`，带新建的 `resumeId`。
- `GET /api/resumes/{id}` 的 `resumeText` 里应能看到：`陆晨`、`浙江理工大学`、`Spring Boot`、`Redis`、`QPS`。
  只要这几个词在，说明 Tika 抽取和 `TextCleaningService` 清洗都没问题。
- `GET /api/resumes/{id}/analysis` 返回评分、总结、优点、建议；`analyzeStatus` 应为 `COMPLETED`。

### 2. AI 正常时的预期

五维评分和总结由 `ResumeGradingService` 调 AI 生成，分值不是固定的，但可以看合理性：

- 简历结构完整（教育、技能、工作、实习、项目、自我评价），五个维度不应出现明显低分（比如内容完整性 < 10）。
- `suggestions` 应该指向真实可改进点，而不是“补充项目经历”这类与简历内容矛盾的建议。
- 如果 AI 返回的建议说“缺少项目经历”，说明 Prompt 或模型输出有问题，值得排查。

### 3. AI 失败时的规则兜底（可复现的确定值）

断网、Key 失效或让模型返回非法 JSON 时，`analyzeResume` 会降级到规则版，这份样本的预期值固定为：

| 维度 | 分值 | 依据 |
| --- | --- | --- |
| 内容完整性 | 25 | 正文 1465 字（≥1000 得 18），命中 5 个章节关键词 +10，封顶 25 |
| 结构清晰度 | 18 | 教育、技能、工作经历、实习、项目 共 5 个关键词（≥4 即 18） |
| 技能匹配度 | 22 | Java、Spring、Spring Boot、MySQL、Redis、Docker、Linux、Git 全部命中（>5 即 22） |
| 表达专业性 | 13 | 同时命中动作词（负责/设计/实现/优化）和量化词（%/提升/降低/QPS/耗时） |
| 项目经验 | 14 | 同时命中“项目”+ 动作词 + 成果词（提升/降低/%/上线） |
| **合计** | **92** | 规则版上限就是 92 |

看到 92 分不要误以为 AI 给了高分：它意味着这次走的是兜底路径，日志里会有
`AI 简历分析失败, fallback to rule-based analysis`。

### 4. 重复上传（去重）

同一个文件再传一次：

- 返回 `duplicate = true`，`resumeId` 与第一次相同。
- 数据库 `resumes` 表不会多出一行，对象存储也不会多一个文件。
- 前端会提示“该简历已存在，正在跳转到详情页…”。

注意去重依据是**文件内容哈希**，不是文件名。把 PDF 用 Word 重新导出一次，内容变了，哈希就变了，会被当成新简历。

## 负向用例

| 操作 | 预期 |
| --- | --- |
| 把 `.txt` 改成 `.png` 再上传 | `RESUME_FILE_TYPE_NOT_SUPPORTED`：类型按文件内容识别，改扩展名绕不过去 |
| 上传 0 字节文件 | `RESUME_FILE_EMPTY` |
| 上传超过 10MB 的文件 | `BAD_REQUEST`，提示“简历文件大小不能超过 10MB” |
| 上传扫描版 PDF（只有图片、没有文字层） | `RESUME_PARSE_FAILED`：解析文本为空，`ResumeUploadService` 会拦住，不会写库 |
| 上传中文文件名 | 正常入库，`storageKey` 里的文件名会被 `FileStorageService.convertToPinyin` 转成拼音 |

扫描版 PDF 值得单独测一次：它区分了“文件类型合法”和“文本可提取”两件事，也是后续引入 OCR 的入口。

## 重新生成

```powershell
python build_resume_samples.py
```

脚本只依赖 `python-docx`，会重新生成 DOCX、TXT 和空白模板，并打印正文长度与规则版预期分值。
PDF 需要另存：在 Word 里打开 DOCX 后“另存为 PDF”，或用 LibreOffice 命令行导出。

排版参数都在脚本顶部和 `add_*` 函数里：正文字号 9.5pt、行距 1.08、页边距 1.0/0.9/1.8/1.8，
是为了把这份内容压进一页。内容再增加时优先删“自我评价”，不要先缩小字号。

## 后续演进

等补上 `ResumeUploadService` 的集成测试（MockMvc + Testcontainers）后，可以把这三份样本
移到 `server/src/test/resources/resume/`，作为“金标准样本”断言解析文本长度、关键词命中数和
规则版分值，避免以后改 Prompt 或清洗逻辑时悄悄回归。
