-- resumes 表的 file_hash 上存在两个内容相同的唯一约束：
--   idx_resume_hash        （历史遗留，命名也不符合约束的习惯）
--   resumes_file_hash_key  （保留这一个）
-- 重复约束会让每次写入多维护一个索引，且报错时提示的约束名不确定。
ALTER TABLE public.resumes DROP CONSTRAINT IF EXISTS idx_resume_hash;