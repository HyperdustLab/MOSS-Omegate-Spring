import io.github.qifan777.knowledge.ServerApplication;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = ServerApplication.class)
@Slf4j
public class TestMain {

  @Resource private VectorStore vectorStore;

  @Test
  public void test2() {

    FilterExpressionBuilder b = new FilterExpressionBuilder();

    Expression exp =
        b.eq("userId", "0x8576268b8299b0fff3c570d1b0c8e77b91d3d70cc1e72bd5bd98e74e8fc5d143")
            .build();

    log.info("filterExpression: {}", exp);

    SearchRequest searchRequest = SearchRequest.builder().filterExpression(exp).build();

    List<Document> documentList = vectorStore.similaritySearch(searchRequest);

    List<String> deleteIds = documentList.stream().map(Document::getId).toList();

    // 先删除该用户之前的内容
    vectorStore.delete(deleteIds);
  }
}
