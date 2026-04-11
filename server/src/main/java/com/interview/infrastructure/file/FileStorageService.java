package com.interview.infrastructure.file;

import com.interview.common.config.StorageConfigProperties;
import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 封装对 RustFS / S3 的文件上传、删除和查询操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Client s3Client;
    private final StorageConfigProperties storageConfig;

    /**
     * 上传简历文件
     */
    public String uploadResume(MultipartFile file , String contentType) {
        return uploadFile(file, contentType,"resumes");
    }

    /**
     * 删除简历文件
     */
    public void deleteResume(String fileKey) {
        deleteFile(fileKey);
    }

    /**
     * 上传知识库文件
     */
    public String uploadKnowledgeBase(MultipartFile file,String contentType) {
        return uploadFile(file, contentType,"knowledgebases");
    }

    /**
     * 删除知识库文件
     */
    public void deleteKnowledgeBase(String fileKey) {
        deleteFile(fileKey);
    }

    /**
     * 通用文件上传方法。
     * prefix 用来区分简历、知识库等不同业务目录。
     */
    private String uploadFile(MultipartFile file,String contentType, String prefix) {
        String originalFilename = file.getOriginalFilename();
        String fileKey = generateFileKey(originalFilename,prefix);

        try{
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .key(fileKey)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(),file.getSize()));
            log.info("文件上传成功: {} -> {}", originalFilename, fileKey);
            return fileKey;
        }catch (IOException e){
            log.error("读取上传文件失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED, "文件读取失败");
        }catch (S3Exception e) {
            log.error("上传文件到RustFS失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED, "文件存储失败: " + e.getMessage());
        }
    }


    /**
      * 生成对象存储中的 key，避免直接使用原始文件名造成冲突。
     */
    private String generateFileKey(String originalFilename, String prefix) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String safeName = sanitizeFilename(originalFilename);
        return String.format("%s/%s/%s_%s", prefix, datePath, uuid, safeName);
    }

    /**
     * 将原始文件名规整成更适合对象存储 key 的安全字符串。
     */
    private String sanitizeFilename(String fileName) {
        if (fileName == null || fileName.isEmpty()){
            return "unknown";
        }
        return convertToPinyin(fileName);
    }

    /**
     * 将中文文件名尽量转成拼音，减少对象存储 key 中的特殊字符。
     */
    private String convertToPinyin(String input){
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

        StringBuilder result = new StringBuilder();
        for (char ch : input.toCharArray()){
            try{
                String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(ch, format);
                if (pinyins != null && pinyins.length > 0) {

                    result.append(capitalize(pinyins[0]));
                }else {
                    result.append(sanitizeChar(ch));
                }
            }catch (BadHanyuPinyinOutputFormatCombination e){
                result.append(sanitizeChar(ch));
            }
        }
        return result.toString();
    }

    private char sanitizeChar(char ch){
        if ((ch>='a' && ch<='z')||(ch>='A' && ch<='Z')||(ch>='0' && ch<='9')||ch=='.'||ch=='_'||ch=='-'){
            return ch;
        }
        return '_';
    }

    private String capitalize(String str){
        if (str == null || str.isEmpty()){
            return str;
        }
        return str.substring(0,1).toUpperCase() + str.substring(1);
    }

    /**
     * 通用文件删除方法。
     */
    private void deleteFile(String fileKey) {
        // 空键直接跳过
        if (fileKey == null || fileKey.isEmpty()) {
            log.debug("文件键为空，跳过删除");
            return;
        }

        // 检查文件是否存在，避免不必要的删除操作
        if (!fileExists(fileKey)) {
            log.warn("文件不存在，跳过删除: {}", fileKey);
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .key(fileKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("文件删除成功: {}", fileKey);
        } catch (S3Exception e) {
            log.error("删除文件失败: {} - {}", fileKey, e.getMessage(), e);
            throw new BusinessException(ErrorCode.STORAGE_DELETE_FAILED, "文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String fileKey) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .key(fileKey)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.warn("检查文件存在性失败: {} - {}", fileKey, e.getMessage());
            return false;
        }
    }

    /**
     * 获取文件大小（字节）
     */
    public long getFileSize(String fileKey) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .key(fileKey)
                    .build();
            return s3Client.headObject(headRequest).contentLength();
        } catch (S3Exception e) {
            log.error("获取文件大小失败: {} - {}", fileKey, e.getMessage());
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, "获取文件信息失败");
        }
    }

    /**
     * 确保存储桶存在
     */
    public void ensureBucketExists() {
        try {
            HeadBucketRequest headRequest = HeadBucketRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .build();
            s3Client.headBucket(headRequest);
            log.info("存储桶已存在: {}", storageConfig.getBucket());
        } catch (NoSuchBucketException e) {
            log.info("存储桶不存在，正在创建: {}", storageConfig.getBucket());
            CreateBucketRequest createRequest = CreateBucketRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .build();
            s3Client.createBucket(createRequest);
            log.info("存储桶创建成功: {}", storageConfig.getBucket());
        } catch (S3Exception e) {
            log.error("检查存储桶失败: {}", e.getMessage(), e);
        }
    }
}
