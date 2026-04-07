package com.interview.modules.resume;

import com.interview.common.result.Result;
import com.interview.modules.resume.model.ResumeEntity;
import com.interview.modules.resume.repository.ResumeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired  // Spring 自动注入
    private ResumeRepository resumeRepository;

    @GetMapping("/save-resume")  // 访问 /test/save-resume 时调用
    public Result<ResumeEntity> testSaveResume() {
        ResumeEntity resume = new ResumeEntity();  // 创建对象
        resume.setFileHash("test-hash-" + System.currentTimeMillis()); // 设值
        resume.setOriginalFilename("测试简历.pdf");

        ResumeEntity savedResume = resumeRepository.save(resume); // 保存到数据库
        return Result.success(savedResume);  // 返回结果
    }

    @GetMapping("/get-resume/{id}")  // {id} 是路径变量
    public Result<ResumeEntity> testGetResume(@PathVariable Long id) {
        Optional<ResumeEntity> resumeOpt = resumeRepository.findById(id);

        if (resumeOpt.isPresent()) {
            return Result.success(resumeOpt.get());
        } else {
            return Result.error("简历不存在");
        }
    }
}