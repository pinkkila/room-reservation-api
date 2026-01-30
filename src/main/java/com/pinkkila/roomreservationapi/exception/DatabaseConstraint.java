package com.pinkkila.roomreservationapi.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum DatabaseConstraint {
    RESERVATION_OVERLAP("reservation_overlap_excl"),
    START_BEFORE_END("start_before_end"),
    ROOM_ID_FOREIGN_KEY("reservation_room_id_fkey");
    
    private final String constraintName;
    
    private static final Map<String, DatabaseConstraint> LOOKUP = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(DatabaseConstraint::getConstraintName, Function.identity()));
    
    public static Optional<DatabaseConstraint> fromName(String name) {
        return Optional.ofNullable(LOOKUP.get(name));
    }
}