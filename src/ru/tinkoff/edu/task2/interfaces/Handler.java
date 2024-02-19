package ru.tinkoff.edu.task2.interfaces;

import java.time.Duration;

public interface Handler {
    Duration timeout();

    void performOperation();
}
