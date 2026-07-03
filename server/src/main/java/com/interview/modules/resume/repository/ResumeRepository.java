package com.interview.modules.resume.repository;

import com.interview.modules.resume.model.entity.ResumeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文件功能说明
 * <p>负责简历数据访问。</p>
 *
 * @author NobuNo
 * @date 2026-03-31
 */
@Repository
public interface ResumeRepository extends JpaRepository<ResumeEntity, Long> {

    /**
     * 功能说明
     * <p>根据文件哈希查询简历。</p>
     *
     * @param fileHash 文件哈希
     * @return 简历实体
     * @author NobuNo
     * @date 2026-03-31
     */
    Optional<ResumeEntity> findByFileHash(String fileHash);

    /**
     * 功能说明
     * <p>按上传时间倒序查询简历。</p>
     *
     * @return 简历列表
     * @author NobuNo
     * @date 2026-03-31
     */
    List<ResumeEntity> findAllByOrderByUploadedAtDesc();

}
