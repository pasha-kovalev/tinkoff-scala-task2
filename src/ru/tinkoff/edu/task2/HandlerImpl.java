package ru.tinkoff.edu.task2;

import ru.tinkoff.edu.task2.interfaces.Address;
import ru.tinkoff.edu.task2.interfaces.Client;
import ru.tinkoff.edu.task2.interfaces.Event;
import ru.tinkoff.edu.task2.interfaces.Handler;
import ru.tinkoff.edu.task2.interfaces.Payload;
import ru.tinkoff.edu.task2.interfaces.Result;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

public class HandlerImpl implements Handler {
    private static final int SENDER_THREADS = 5;
    private static final int INITIAL_DELAY = 1;
    private static final int MULTIPLIER = 2;
    private static final int MAX_ATTEMPTS = 10;
    private static final int MAX_DELAY = 60;
    private static final Duration TIMEOUT_DURATION = Duration.ofSeconds(1);
    private final Client client;
    private final ExecutorService executor;
    private final BlockingQueue<Event> queue;
    public HandlerImpl(Client client) {
        this.client = client;
        this.executor = Executors.newCachedThreadPool();
        this.queue = new LinkedBlockingQueue<>();
    }
    @Override
    public Duration timeout() {
        return TIMEOUT_DURATION;
    }
    @Override
    public void performOperation() {
        readData();
        distributeData();
    }
    private void readData() {
        executor.execute(() -> {
            while (true) {
                Event event = client.readData();
                boolean success = queue.offer(event);
                if (!success) {
                    try {
                        Thread.sleep(timeout().toMillis());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    private void distributeData() {
        IntStream.range(0, SENDER_THREADS)
                .forEach(i -> executor.execute(this::sendData));
    }
    private void sendData() {
        while (true) {
            try {
                Event event = queue.take();
                for (Address recipient : event.recipients()) {
                    executor.execute(() -> sendDataToRecipient(recipient, event.payload()));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    private void sendDataToRecipient(Address dest, Payload payload) {
        int attempts = 0;
        int delay = INITIAL_DELAY;
        while (attempts < MAX_ATTEMPTS) {
            Result result = client.sendData(dest, payload);
            if (result == Result.ACCEPTED) {
                break;
            } else {
                attempts++;
                try {
                    Thread.sleep(delay * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                delay = Math.min(delay * MULTIPLIER, MAX_DELAY);
            }
        }
    }
}

