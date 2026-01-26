package com.pinkkila.roomreservationapi.room.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(Integer roomId) {
        super("Room with ID " + roomId + " not found");
    }
}
