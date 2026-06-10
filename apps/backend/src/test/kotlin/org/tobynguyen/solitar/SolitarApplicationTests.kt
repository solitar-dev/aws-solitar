package org.tobynguyen.solitar

import com.jayway.jsonpath.JsonPath
import java.time.Duration
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import software.amazon.awssdk.services.sqs.SqsClient

/**
 * Full-stack integration test against LocalStack (DynamoDB + SQS) via Testcontainers. Tables and
 * queues are provisioned with the AWS SDK in the companion initializer (before the Spring context
 * starts) so the `@SqsListener` containers and repositories have their backing resources ready —
 * this avoids the LocalStack init-script readiness race. Requires a running Docker daemon.
 */
@SpringBootTest(
    properties = ["app.rate-limiter.capacity=20", "app.rate-limiter.refill-period-in-seconds=3600"]
)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class SolitarApplicationTests {

    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    @Order(1)
    fun `create then redirect returns 301 to original url`() {
        val target = "https://example.com/landing"
        val shortCode = createShortCode(target)

        mockMvc
            .perform(get("/$shortCode"))
            .andExpect(status().isMovedPermanently)
            .andExpect(header().string("Location", target))
    }

    @Test
    @Order(2)
    fun `redirect increments totalClicks via SQS aggregation`() {
        val shortCode = createShortCode("https://example.com/clicked")

        mockMvc.perform(get("/$shortCode")).andExpect(status().isMovedPermanently)

        // Aggregation is async (SQS -> @SqsListener -> DynamoDB ADD); poll until it lands.
        await().atMost(Duration.ofSeconds(20)).untilAsserted {
            val body = mockMvc.perform(get("/statistics")).andReturn().response.contentAsString
            val clicks = (JsonPath.read(body, "$.totalClicks") as Number).toLong()
            check(clicks >= 1) { "expected totalClicks >= 1 but was $clicks" }
        }
    }

    @Test
    @Order(3)
    fun `unknown code returns 404`() {
        mockMvc.perform(get("/nonexistentCode")).andExpect(status().isNotFound)
    }

    @Test
    @Order(4)
    fun `rate limiter trips with 429 past capacity`() {
        // Capacity is 20 for this context; hammer past it and assert throttling kicks in.
        var sawTooMany = false
        repeat(30) {
            val resultStatus =
                mockMvc
                    .perform(
                        post("/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"url":"https://example.com/rl$it"}""")
                    )
                    .andReturn()
                    .response
                    .status
            if (resultStatus == 429) sawTooMany = true
        }
        check(sawTooMany) { "expected at least one 429 once capacity was exhausted" }
    }

    private fun createShortCode(url: String): String {
        val body =
            mockMvc
                .perform(
                    post("/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"url":"$url"}""")
                )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        return JsonPath.read(body, "$.shortCode") as String
    }

    companion object {
        private val localstack =
            LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
                .withServices("dynamodb", "sqs")

        init {
            localstack.start()
            provisionResources()
        }

        private fun provisionResources() {
            val credentials =
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(localstack.accessKey, localstack.secretKey)
                )
            val region = Region.of(localstack.region)

            DynamoDbClient.builder()
                .endpointOverride(localstack.endpoint)
                .credentialsProvider(credentials)
                .region(region)
                .build()
                .use { ddb ->
                    listOf("urls", "statistics").forEach { table ->
                        ddb.createTable { req ->
                            req.tableName(table)
                                .keySchema(
                                    KeySchemaElement.builder()
                                        .attributeName("id")
                                        .keyType(KeyType.HASH)
                                        .build()
                                )
                                .attributeDefinitions(
                                    AttributeDefinition.builder()
                                        .attributeName("id")
                                        .attributeType(ScalarAttributeType.S)
                                        .build()
                                )
                                .billingMode(BillingMode.PAY_PER_REQUEST)
                        }
                    }
                }

            SqsClient.builder()
                .endpointOverride(localstack.endpoint)
                .credentialsProvider(credentials)
                .region(region)
                .build()
                .use { sqs ->
                    listOf("link-created", "link-forwarded", "link-events-dlq").forEach { queue ->
                        sqs.createQueue { req -> req.queueName(queue) }
                    }
                }
        }

        @JvmStatic
        @DynamicPropertySource
        fun awsProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.cloud.aws.region.static") { localstack.region }
            registry.add("spring.cloud.aws.credentials.access-key") { localstack.accessKey }
            registry.add("spring.cloud.aws.credentials.secret-key") { localstack.secretKey }
            registry.add("spring.cloud.aws.dynamodb.endpoint") { localstack.endpoint.toString() }
            registry.add("spring.cloud.aws.sqs.endpoint") { localstack.endpoint.toString() }
        }
    }
}
