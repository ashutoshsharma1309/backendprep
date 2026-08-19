# Phase 7 — Event-Driven Development
### Prerequisite: Phase 6 complete. Branch: `git checkout -b phase-7-yourname`

**Catalog service (Senior tier — pair with a mentor):** Event Messaging.

You'll publish a `BookBorrowedEvent` when `BorrowingService.borrowBook`
succeeds, and consume it to trigger the Phase 4 notification — decoupling
"a book was borrowed" from "send a confirmation," which don't need to
happen in the same synchronous request.

---

## Module 1 — Setup

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Run Kafka locally (the simplest path for training is Docker Compose):

```yaml
# project/docker-compose.yml
services:
  kafka:
    image: apache/kafka:3.7.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
```

```bash
docker compose -f project/docker-compose.yml up -d
```

```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=library-service
spring.kafka.consumer.auto-offset-reset=earliest
```

---

## Module 2 — Producing the Event

### 2.1 The Event Payload

```java
package com.example.library.event;

import java.time.Instant;

public class BookBorrowedEvent {
    private Long loanId;
    private Long bookId;
    private String patronEmail;
    private String bookTitle;
    private Instant occurredAt;

    public BookBorrowedEvent() {}

    public BookBorrowedEvent(Long loanId, Long bookId, String patronEmail, String bookTitle) {
        this.loanId = loanId;
        this.bookId = bookId;
        this.patronEmail = patronEmail;
        this.bookTitle = bookTitle;
        this.occurredAt = Instant.now();
    }

    // getters for all fields
    public Long getLoanId() { return loanId; }
    public Long getBookId() { return bookId; }
    public String getPatronEmail() { return patronEmail; }
    public String getBookTitle() { return bookTitle; }
    public Instant getOccurredAt() { return occurredAt; }
}
```

Keep this payload self-contained (it includes `bookTitle`, not just
`bookId`) so a consumer doesn't have to call back into the Library Service's
database to render a useful notification — a common event-design principle:
**events should carry enough context to be useful on their own.**

### 2.2 The Producer

```java
package com.example.library.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookEventPublisher {

    private static final String TOPIC = "book-events";
    private final KafkaTemplate<String, BookBorrowedEvent> kafkaTemplate;

    public BookEventPublisher(KafkaTemplate<String, BookBorrowedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishBorrowed(BookBorrowedEvent event) {
        // Partition key = bookId: guarantees all events for the same book
        // are processed in order relative to each other.
        kafkaTemplate.send(TOPIC, String.valueOf(event.getBookId()), event);
    }
}
```

Wire it into `BorrowingService.borrowBook` — publish right after the `Loan`
is saved:

```java
Loan savedLoan = loanRepository.save(new Loan(book, patronEmail, today, dueDate));
eventPublisher.publishBorrowed(new BookBorrowedEvent(
    savedLoan.getId(), book.getId(), patronEmail, book.getTitle()));
return savedLoan;
```

**Why `bookId` as the partition key, not `patronEmail` or a random key?**
Two borrow/return events for the *same book* need to be processed in order
(you don't want a "returned" event processed before its matching "borrowed"
event due to landing on different partitions). Events for *different* books
don't need relative ordering, so they're free to spread across partitions
for throughput.

---

## Module 3 — Consuming the Event

```java
package com.example.library.event;

import com.example.library.notification.OverdueNotifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookEventConsumer {

    private final OverdueNotifier notifier; // reuse Phase 4's sender underneath
    private final ProcessedEventRepository processedEventRepository; // idempotency, see below

    public BookEventConsumer(OverdueNotifier notifier, ProcessedEventRepository processedEventRepository) {
        this.notifier = notifier;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = "book-events", groupId = "library-service")
    public void onBookBorrowed(BookBorrowedEvent event) {
        String dedupeKey = "loan-confirmation-" + event.getLoanId();
        if (processedEventRepository.existsById(dedupeKey)) {
            return; // already handled this exact event — Kafka gives at-least-once delivery
        }
        notifier.notifyBorrowConfirmation(event.getPatronEmail(), event.getBookTitle());
        processedEventRepository.save(new ProcessedEvent(dedupeKey));
    }
}
```

```java
package com.example.library.event;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ProcessedEvent {
    @Id
    private String eventKey;
    public ProcessedEvent() {}
    public ProcessedEvent(String eventKey) { this.eventKey = eventKey; }
}
```

```java
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {}
```

**This is Phase 6's idempotency lesson, applied here — not optional.**
Kafka's normal delivery guarantee is **at-least-once**: your consumer *will*
see the same message again eventually (after a rebalance, a retry, a
restart before an offset commit). If `notifyBorrowConfirmation` isn't
protected against duplicate delivery, a patron gets the same "you borrowed
X" email two or three times over the life of the system. This isn't a rare
edge case — it's the normal operating behavior of the delivery guarantee.

**Your task this phase:** borrow a book via `POST /loans`, confirm the
notification fires once (check your `EmailNotificationSender`'s log line
from Phase 4). Then manually re-publish the same event (or simulate a
redelivery by re-processing the same `BookBorrowedEvent` object directly)
and confirm the notification does **not** fire a second time.

---

## Module 4 — Testing With Embedded Kafka

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

```java
package com.example.library.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "book-events")
@DirtiesContext
class BookEventFlowTest {

    @Autowired private BookEventPublisher publisher;

    @Test
    void publishingEvent_triggersConsumerExactlyOnce() {
        BookBorrowedEvent event = new BookBorrowedEvent(1L, 1L, "patron@example.com", "Effective Java");

        publisher.publishBorrowed(event);
        publisher.publishBorrowed(event); // simulate redelivery — SAME loanId

        await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() -> {
            // Assert your notification mechanism was invoked exactly once —
            // wire a test double/spy into OverdueNotifier to make this assertion possible.
        });
    }
}
```

`@EmbeddedKafka` spins up a real in-process Kafka broker for the test —
fast, isolated, no dependency on your local Docker Kafka being up.

**Your task this phase:** finish this test by wiring a spy/mock around
`OverdueNotifier` (or `NotificationSender`) and asserting it was called
exactly once despite the event being published twice.

---

## Phase 7 Completion Checklist

- [ ] `BookBorrowedEvent` published on every successful borrow, keyed by `bookId`
- [ ] Consumer wired to trigger the Phase 4 notification
- [ ] Duplicate delivery proven **not** to cause a duplicate notification
- [ ] `@EmbeddedKafka` integration test passing
- [ ] Can explain, in your own words, why at-least-once delivery makes idempotency mandatory here, not optional
- [ ] PR opened, mentor-reviewed, CI green

**Next:** [Phase 8 — Developer Governance & Support](../phase-8-governance-support/README.md)
