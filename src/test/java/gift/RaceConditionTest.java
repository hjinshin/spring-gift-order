package gift;

import gift.common.enums.Role;
import gift.config.RedisConfig;
import gift.controller.dto.request.OrderRequest;
import gift.controller.dto.response.OrderResponse;
import gift.model.Category;
import gift.model.Member;
import gift.model.Option;
import gift.model.Product;
import gift.repository.CategoryRepository;
import gift.repository.MemberRepository;
import gift.repository.ProductRepository;
import gift.service.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.TransactionTimedOutException;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Import(RedisConfig.class)
public class RaceConditionTest {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private RedisService redisService;

    @Test
    @DisplayName("RedisService에서 동시에 삭제 요청[성공]-Redisson 분산락")
    void subtractRequestAtTheSameTime() throws InterruptedException {
        // given
        int quantity = 100;
        int subtractAmount = 1;
        Category category = categoryRepository.save(new Category("cname", "ccolor", "cImage", ""));
        List<Option> options = List.of(new Option("oName", quantity));
        Product product = productRepository.save(new Product("pName", 0, "purl", category, options));
        Long optionId = product.getOptions().get(0).getId();
        Member member = memberRepository.save(new Member("test@email", "1234", Role.USER));
        OrderRequest request = new OrderRequest(optionId, subtractAmount, "message");

        int threadCount = 100; // 스레드 개수
        ExecutorService executorService = Executors.newFixedThreadPool(32); // 스레드 풀 크기
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    OrderResponse response = redisService.createOrderRedisLock(member.getId(), request);
                } catch (TransactionTimedOutException e) {
                    System.out.println("TimeOut");
                }catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // when
        product = productRepository.findProductAndOptionByIdFetchJoin(product.getId()).get();
        Option actual = product.findOptionByOptionId(optionId);

        // then
        int expactedQuantity = quantity - subtractAmount * threadCount;
        assertThat(actual.getQuantity()).isEqualTo(expactedQuantity);
    }
}