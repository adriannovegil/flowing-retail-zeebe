package io.flowing.retail.kafka.order.flow.adapter;

import java.time.Duration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.flowing.retail.kafka.order.domain.OrderFlowContext;
import io.flowing.retail.kafka.order.flow.payload.OrderCompletedEventPayload;
import io.flowing.retail.kafka.order.messages.Message;
import io.flowing.retail.kafka.order.messages.MessageSender;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.clients.JobClient;
import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.subscription.JobHandler;
import io.zeebe.client.api.subscription.JobWorker;

@Component
public class OrderCompletedAdapter implements JobHandler {

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private ZeebeClient zeebe;

    private JobWorker subscription;

    @PostConstruct
    public void subscribe() {
        subscription = zeebe.jobClient().newWorker()
                .jobType("order-completed")
                .handler(this)
                .timeout(Duration.ofMinutes(1))
                .open();
    }

    @PreDestroy
    public void closeSubscription() {
        subscription.close();
    }

    @Override
    public void handle(JobClient client, ActivatedJob job) {
        OrderFlowContext context = OrderFlowContext.fromJson(job.getPayload());

        messageSender.send( //
                new Message<OrderCompletedEventPayload>( //
                        "OrderCompletedEvent", //
                        context.getTraceId(), //
                        new OrderCompletedEventPayload() //
                                .setOrderId(context.getOrderId())));

        //TODO: Reintorduce traceId?     .setCorrelationId(event.get)));
        client.newCompleteCommand(job.getKey()).send().join();
    }

}
