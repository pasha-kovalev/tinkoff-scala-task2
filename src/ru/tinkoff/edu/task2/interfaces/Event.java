package ru.tinkoff.edu.task2.interfaces;

import java.util.List;

public record Event(List<Address> recipients, Payload payload) {
}
