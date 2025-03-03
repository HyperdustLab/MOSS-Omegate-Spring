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
        b.eq("userId", "0x3efe0ce7b0741282e4e5b1da44b46991fb1c429060fa0eb245920812cd663766")
            .build();

    log.info("filterExpression: {}", exp);

    SearchRequest searchRequest =
        SearchRequest.builder().filterExpression(exp).query("moss twitter link?").build();

    List<Document> documentList = vectorStore.similaritySearch(searchRequest);

    for (Document document : documentList) {

      System.out.println(document.getText() + "\n");
    }
  }
}
