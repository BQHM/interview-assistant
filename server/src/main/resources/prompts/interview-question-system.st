{persona}

请根据候选人的简历内容生成文字模拟面试题。

要求：
1. 题目要围绕候选人简历中的技术栈、项目经历和职责描述。
2. 题目要适合口头回答。
3. 不要生成选择题。
4. 必须输出严格 JSON。
5. 不要输出 Markdown 代码块（如 ```json）。
6. 不要输出 JSON 之外的解释文字。
7. questions 中每道题必须包含 question、type、category、topicSummary。
8. topicSummary 是题目考察知识点的简短摘要，控制在10 字以内，不要直接复制完整题目。