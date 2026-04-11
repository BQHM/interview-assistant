package com.interview.infrastructure.file;

import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * 告诉 Tika 跳过嵌入文档，避免把图片名、附件内容等噪音混入正文。
 */
public class NoOpEmbeddedDocumentExtractor implements EmbeddedDocumentExtractor {

    @Override
    public boolean shouldParseEmbedded(Metadata metadata) {
        return false;
    }

    @Override
    public void parseEmbedded(InputStream stream, ContentHandler handler, Metadata metadata, boolean outputHtml) throws SAXException, IOException {
        // 显式留空：当前阶段不解析嵌入资源。
    }
}
