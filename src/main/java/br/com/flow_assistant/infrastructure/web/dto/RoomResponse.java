package br.com.flow_assistant.infrastructure.web.dto;

public record RoomResponse(Long id, String name, int capacity, String location, String equipment, boolean active) {
}
