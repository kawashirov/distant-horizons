package com.seibel.distanthorizons.common.networking.messages;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MessageRegistry {
    private final Map<Integer, Supplier<Message>> idToConstructor = Map.ofEntries(
            // Do NOT remove messages from middle of the list;
            // It will break backwards compatibility.
            // (Stub indexes out instead)
            Map.entry(1, HelloMessage::new)
    );

    private final Map<Class<?>, Integer> classToId = idToConstructor.entrySet().stream()
            .collect(Collectors.toMap(
                    e -> e.getValue().getClass(),
                    Map.Entry::getKey
            ));

    public Message createMessage(int id) throws IllegalArgumentException {
        try {
            return idToConstructor.get(id).get();
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Invalid message ID");
        }
    }

    public int getMessageId(Message message) {
        return classToId.get(message.getClass());
    }
}
