-- V3：面试会话记录本次使用的 LLM Provider
-- 历史 7 条数据保留为空，表示"接入多 Provider 之前创建的会话"，
-- 不回填是为了不伪造事实：当时确实没有这个概念。
ALTER TABLE public.interview_sessions
    ADD COLUMN IF NOT EXISTS llm_provider VARCHAR(50);

COMMENT ON COLUMN public.interview_sessions.llm_provider IS '本次面试使用的 LLM Provider 名称';