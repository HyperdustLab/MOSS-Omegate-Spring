import cn.hutool.core.util.StrUtil;
import io.github.qifan777.knowledge.ServerApplication;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;
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
public class Test1 {

  @Resource private VectorStore vectorStore;

  @Test
  public void test() {

    FilterExpressionBuilder b = new FilterExpressionBuilder();

    Expression exp =
        b.in("userId", "0x8576268b8299b0fff3c570d1b0c8e77b91d3d70cc1e72bd5bd98e74e8fc5d143")
            .build();

    log.info("filterExpression: {}", exp);

    SearchRequest searchRequest =
        SearchRequest.builder().query("MOSS的推特是多少").filterExpression(exp).build();

    List<Document> documentList = vectorStore.similaritySearch(searchRequest);

    log.info(
        "documentList: {}",
        StrUtil.join(
            "\n", documentList.stream().map(Document::getText).collect(Collectors.toList())));
  }
}
