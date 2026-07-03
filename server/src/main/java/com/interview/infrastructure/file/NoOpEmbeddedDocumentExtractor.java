package com.interview.infrastructure.file;

import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件功能说明
 * <p>负责跳过 Tika 嵌入文档解析。</p>
 *
 * @author NobuNo
 * @date 2026-04-03
 */
public class NoOpEmbeddedDocumentExtractor implements EmbeddedDocumentExtractor {

    /**
     * 功能说明
     * <p>判断是否解析嵌入文档。</p>
     *
     * @param metadata 文档元数据
     * @return 是否解析嵌入文档
     * @author NobuNo
     * @date 2026-04-03
     */
    @Override
    public boolean shouldParseEmbedded(Metadata metadata) {
        return false;
    }

    /**
     * 功能说明
     * <p>解析嵌入文档。</p>
     *
     * @param stream 输入流
     * @param handler 内容处理器
     * @param metadata 文档元数据
     * @param outputHtml 是否输出 HTML
     * @throws SAXException 当 SAX 处理失败时抛出
     * @throws IOException 当输入流读取失败时抛出
     * @author NobuNo
     * @date 2026-04-03
     */
    @Override
    public void parseEmbedded(InputStream stream, ContentHandler handler, Metadata metadata, boolean outputHtml) throws SAXException, IOException {
        // 显式留空：当前阶段不解析嵌入资源。
    }
}
