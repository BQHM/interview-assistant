# AES-GCM 密钥加密实战

本文记录 `interview-assistant` 里 API Key 落库加密的设计与实现要点，属于 B 期（LLM Provider 数据库化）第 2 步。

代码位置：`server/src/main/java/com/interview/infrastructure/crypto/AesGcmCipher.java`
测试位置：`server/src/test/java/com/interview/infrastructure/crypto/AesGcmCipherTest.java`

***

## 一、要解决的问题

数据库密码只解决"能不能连进来"，连进来之后数据仍然是明文。真实泄露路径包括：

- DBA 或有权限的同事直接 `SELECT * FROM llm_providers`
- 数据库备份文件被下载、误传网盘
- 实体 `toString` 被日志打印
- 本地调试截图外发

加密之后，"拿到数据库"只等于"拿到密文"，解密需要主密钥，而主密钥不在数据库、不在代码、不在 Git。

***

## 二、算法选择

| 项目 | 取值 | 说明 |
| --- | --- | --- |
| 算法 | AES | 对称加密，JDK 自带，无新增依赖 |
| 模式 | GCM | 认证加密，既保密又能发现篡改 |
| 填充 | NoPadding | GCM 是流式模式，自带完整性校验，不需要填充 |
| 密钥长度 | 256 位（32 字节） | 环境变量里以 Base64 存放 |
| nonce 长度 | 12 字节 | GCM 推荐值，Base64 后 16 字符 |
| 认证标签 | 128 位（16 字节） | 追加在密文末尾，密文比明文长 16 字节 |

### 为什么是 GCM 而不是 CBC

- CBC 只保证机密性，数据被改动后解密只会得到乱码，程序无法区分"被篡改"和"解密正常"
- GCM 在密文末尾附认证标签，解密时重新计算并比对，不一致直接抛 `AEADBadTagException`
- GCM 不需要单独管理填充和 IV，使用成本更低

***

## 三、数据流

### 加密

```text
明文 "sk-xxx"
  → 生成 12 字节随机 nonce
  → Cipher(ENCRYPT, 主密钥, GCMParameterSpec(128, nonce))
  → doFinal(明文 UTF-8 字节)
  → 密文字节（明文 + 16 字节标签）
  → 密文和 nonce 各自 Base64
  → 两个字段一起落库（api_key_ciphertext / api_key_nonce）
```

### 解密

```text
数据库取密文 + nonce
  → 各自 Base64 解码
  → 校验 nonce 为 12 字节
  → Cipher(DECRYPT, 主密钥, GCMParameterSpec(128, nonce))
  → doFinal(密文字节)
  → new String(明文字节, UTF-8)
```

***

## 四、四个必须理解的点

### 1. nonce 每次必须重新生成

同一个密钥下 nonce 绝不能重复，否则两段密文可以被用来推算明文之间的关系，这是 GCM 的致命弱点。

实现上就是每次加密都 `new byte[12]` 再 `secureRandom.nextBytes(nonceBytes)`，不能把 nonce 缓存成成员变量，也不能写死。

对应测试：`encryptTwice_shouldProduceDifferentNonceAndCiphertext`。

### 2. 解密失败必须抛异常，不能返回空字符串或 null

否则"主密钥配错"会表现为"API Key 是空的"，最终错误出现在调用大模型的 401 上，排查方向完全跑偏。失败时抛 `IllegalStateException` 并写明"认证校验未通过，加密主密钥可能已更换或密文被篡改"。

对应测试：`decryptWithDifferentKey_shouldFailWithAuthTagMessage`。

### 3. 主密钥只能来自环境变量

- 硬编码在代码里：代码泄露即密钥泄露
- 存在数据库另一张表：库被拖走时密文和密钥一起丢
- 每次启动随机生成：重启后旧密文全部解不开

配置方式：`application.yml` 中 `encryption-key: ${AI_LLM_ENCRYPTION_KEY:}`，构造器里校验非空、Base64 合法、长度 32 字节，任一不满足直接让应用启动失败。

### 4. 认证标签是"封条"，不是冗余数据

GCM 会按密钥、明文、nonce 计算标签。密文被改一个字节，解密时标签比对失败，抛 `AEADBadTagException`，而不是解出一堆乱码。

对应测试：`decryptTamperedCiphertext_shouldFail`。

***

## 五、三个编码细节

### 1. Cipher 不能复用，SecureRandom 可以

`Cipher` 内部保存当前状态，多线程共用会互相污染，每次加解密都要 `Cipher.getInstance(...)` 新建。`SecureRandom` 线程安全，可以作为字段复用。

### 2. 字符串与字节转换必须显式指定字符集

`plaintext.getBytes(StandardCharsets.UTF_8)` 与 `new String(bytes, StandardCharsets.UTF_8)` 必须配对。无参 `getBytes()` 使用平台默认字符集，Windows 与 Linux 容器可能不同，中文场景下会得到"本机正常、服务器乱码"的问题。

### 3. catch 顺序必须从具体到宽泛

```java
catch (AEADBadTagException e)       // 子类，最先接
catch (IllegalArgumentException e)  // Base64 脏数据，运行时异常
catch (GeneralSecurityException e)  // 兜底
```

反过来写会直接编译失败：`exception AEADBadTagException has already been caught`。

***

## 六、错误信息对照

| 现象 | 含义 | 处理方向 |
| --- | --- | --- |
| 启动失败：未配置 API Key 加密主密钥 | 环境变量缺失 | 配置 `AI_LLM_ENCRYPTION_KEY` |
| 启动失败：不是合法的 Base64 字符串 | 密钥含非法字符 | 重新生成，注意不要带多余字符 |
| 启动失败：长度必须是 32 字节 | 密钥长度不对 | 重新生成 32 字节随机数 |
| 解密失败：认证校验未通过 | 主密钥变了或密文被改 | 核对密钥来源，检查数据是否被人工修改 |
| 解密失败：密文或 nonce 不是合法的 Base64 | 库中数据脏 | 检查迁移或手工写入的数据 |

***

## 七、自测题

遮住代码，口述加密 6 步、解密 6 步，并回答：

1. 为什么每次都要新 nonce？
2. 为什么解密失败要抛异常而不是返回空？
3. 为什么密文比明文长 16 字节？
4. 为什么主密钥不能存数据库？

能答出这四个问题，说明掌握了设计；API 细节忘掉可以随时查。

***

## 八、当前局限与后续演进

- 加密字段无法直接做等值查询：同一明文每次密文不同。若后续需要按敏感字段精确检索，需要额外存一列确定性哈希（如 HMAC）。
- 主密钥只有一把，所有 Provider 共用。生产环境可以向 KMS 或密钥管理服务演进，支持密钥轮换：密文里记录密钥版本，解密时按版本选择密钥。
- 密钥轮换时需要一次性重加密所有历史密文，当前阶段通过"主密钥不变"规避。
