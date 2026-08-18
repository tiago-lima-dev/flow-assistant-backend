package br.com.flow_assistant.domain.model;

public record Room(Long id, String name, int capacity, String location, String equipment, boolean active) {

    public boolean hasCapacityFor(int attendeesCount) {
        return attendeesCount <= capacity;
    }
}
