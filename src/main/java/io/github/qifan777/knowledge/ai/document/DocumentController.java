package io.github.qifan777.knowledge.ai.document;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("document")
@RestController
@AllArgsConstructor
@Slf4j
public class DocumentController {

  private final VectorStore vectorStore;

  /**
   * 嵌入多个文件
   *
   * @param files 待嵌入的文件列表
   * @param userId 用户ID
   * @return 是否成功
   */
  @SneakyThrows
  @PostMapping("embedding")
  public Boolean embedding(@RequestParam List<MultipartFile> files, @RequestParam String userId) {

    FilterExpressionBuilder b = new FilterExpressionBuilder();

    Expression exp = b.eq("userId", userId).build();

    log.info("filterExpression: {}", exp);

    SearchRequest searchRequest = SearchRequest.defaults().withFilterExpression(exp);

    List<Document> documentList = vectorStore.similaritySearch(searchRequest);

    List<String> deleteIds = documentList.stream().map(Document::getId).toList();

    // 先删除该用户之前的内容
    vectorStore.delete(deleteIds);

    List<Document> allDocuments = new ArrayList<>();

    for (MultipartFile file : files) {
      // 从IO流中读取文件
      TikaDocumentReader tikaDocumentReader =
          new TikaDocumentReader(new InputStreamResource(file.getInputStream()));

      // 将文本内容划分成更小的块
      List<Document> splitDocuments = new TokenTextSplitter().apply(tikaDocumentReader.read());

      // 为每个文档添加用户ID
      for (Document doc : splitDocuments) {
        doc.getMetadata().put("userId", userId);
      }

      allDocuments.addAll(splitDocuments);
    }

    log.info("allDocuments: {}", allDocuments);
    // 存入向量数据库
    vectorStore.add(allDocuments);

    log.info("embedding success");

    return true;
  }
}
