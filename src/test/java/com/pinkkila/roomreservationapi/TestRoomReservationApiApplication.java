package com.pinkkila.roomreservationapi;

import org.springframework.boot.SpringApplication;

public class TestRoomReservationApiApplication {
    
    public static void main(String[] args) {
        SpringApplication.from(RoomReservationApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
    
}
