package io.github.qifan777.knowledge.ai.document;

import cn.dev33.satoken.stp.StpUtil;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("document")
@RestController
@AllArgsConstructor
public class DocumentController {

  private final VectorStore vectorStore;

  /**
   * 嵌入文件
   *
   * @param file 待嵌入的文件
   * @return 是否成功
   */
  @SneakyThrows
  @PostMapping("embedding")
  public Boolean embedding(@RequestParam MultipartFile file) {
    // 从IO流中读取文件
    TikaDocumentReader tikaDocumentReader =
        new TikaDocumentReader(new InputStreamResource(file.getInputStream()));
    // 将文本内容划分成更小的块
    List<Document> splitDocuments = new TokenTextSplitter().apply(tikaDocumentReader.read());

    String userId = (String) StpUtil.getLoginId();

    // 为每个文档添加用户ID
    for (Document doc : splitDocuments) {
      doc.getMetadata().put("userId", userId); // 将用户ID存入文档的元数据中
    }

    // 存入向量数据库，这个过程会自动调用embeddingModel,将文本变成向量再存入。
    vectorStore.add(splitDocuments);
    return true;
  }
}
