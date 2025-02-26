import io.github.qifan777.knowledge.ServerApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = ServerApplication.class)
public class EmbeddingTest {
  @Autowired private EmbeddingModel embeddingModel;

  @Test
  public void search() {

    float[] tests = embeddingModel.embed("test");
    System.out.print(tests);
  }
}
