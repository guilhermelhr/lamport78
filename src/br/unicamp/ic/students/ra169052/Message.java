package br.unicamp.ic.students.ra169052;

public class Message {
    public enum Action {
        REQUEST, RELEASE, ACK
    }

    public Clock clock;
    public Action action;

    public Message(Clock clock, Action action){
        this.clock = clock;
        this.action = action;
    }

    public Message copy(){
        return new Message(new Clock(clock.pid, clock.value), action);
    }
}
