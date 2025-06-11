package client;


import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaClient {

    private static int creatorId = -1;
    private static int request;
    private static String topicName;
    private static final Properties properties = new Properties();

    private final static Random rand = new Random();


    private static void create() {
        int progress = 0;
        int failure = 0;
        while (progress < request) {
            try (final Admin client = Admin.create(properties)) {
                while (progress < request) {
                    final NewTopic newTopic = new NewTopic(
                            "test-" + creatorId + "-" + progress, 2, (short) 2);
                    final CreateTopicsResult result = client.createTopics(Collections.singleton(newTopic));
                    result.all().get();
                    progress++;
                    System.out.println("Creator Progress :"+progress);
                }
            } catch (final Exception e) {
                failure++;
                System.out.println("Creator produce :"+e.getMessage());
                if (failure > 3) {
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (final Exception ignored) {}
            }
        }
    }

    private static void produce() {
        int progress = 0;
        int failure = 0;
        while (progress < request) {
            try (final Producer<String, String> producer = new KafkaProducer<String, String>(properties)) {
                while (progress < request) {
                    producer.send(new ProducerRecord<String, String>(topicName,
                            Integer.toString(rand.nextInt(request)),
                            Integer.toString(rand.nextInt(request))));
                    progress++;
                    System.out.println("Produer Progress :"+progress);
                }
            } catch (final Exception e) {
                failure++;
                System.out.println("Producer exception :"+e.getMessage());
                if (failure > 3) {
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (final Exception ignored) {}
            }
        }
    }

    private static void consume() {
        int progress = 0;
        int failure = 0;
        while (progress < request) {
            try (final Consumer<String, String> consumer = new KafkaConsumer<String, String>(properties)) {
                consumer.subscribe(Collections.singleton(topicName));
                System.out.println("Consumer Progress subscribe");
                while (progress < request) {
                    System.out.println("Consumer Progress poll");
                    final ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                    for (final ConsumerRecord<String, String> record : records) {
                        progress++;
                        System.out.println("Consumer Progress :"+progress);
                    }
                }
            } catch (final Exception e) {
                failure++;
                System.out.println("Consumer exception :"+progress);
                if (failure > 3) {
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (final Exception ignored) {}
            }
        }
    }

    private static void run(final String[] args) {
        final String bootstrapServers = args[0];
        final String command = args[1];
        properties.put("bootstrap.servers", bootstrapServers);
        switch (command) {
            case "create":
                creatorId = Integer.parseInt(args[2]);
                request = Integer.parseInt(args[3]);
                create();
                break;
            case "produce":
                properties.put("key.serializer",
                        "org.apache.kafka.common.serialization.StringSerializer");
                properties.put("value.serializer",
                        "org.apache.kafka.common.serialization.StringSerializer");
                topicName = args[2];
                request = Integer.parseInt(args[3]);
                try {
                    Thread.sleep(10000);
                } catch (Exception ignored) { }
                produce();
                break;
            case "consume":
                properties.put("key.deserializer",
                        "org.apache.kafka.common.serialization.StringDeserializer");
                properties.put("value.deserializer",
                        "org.apache.kafka.common.serialization.StringDeserializer");
                topicName = args[2];
                final String consumerGroupId = args[3];
                properties.put("group.id", consumerGroupId);
                request = Integer.parseInt(args[4]);
                consume();
                break;
            default:
                System.out.println("undefined command");
        }
    }

    public static void main(final String[] args) {
        final String name = ManagementFactory.getRuntimeMXBean().getName();
        final long pid = Long.parseLong(name.substring(0, name.indexOf('@')));
        System.out.println("Pid :"+pid);
        run(args);
    }
}
