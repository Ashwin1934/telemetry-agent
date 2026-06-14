package com.telemetry.actors;

import java.util.HashSet;
import java.util.Set;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class RoomRegistryActor
        extends AbstractBehavior<RoomRegistryActor.Command> {

    public interface Command {}

    public static class RegisterRoom implements Command {

        public final String roomName;

        public RegisterRoom(String roomName) {
            this.roomName = roomName;
        }
    }

    public static class GetRooms implements Command {

        public final ActorRef<RoomsResponse> replyTo;

        public GetRooms(
                ActorRef<RoomsResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static class RoomsResponse {

        public final Set<String> rooms;

        public RoomsResponse(Set<String> rooms) {
            this.rooms = rooms;
        }
    }

    private final Set<String> rooms =
            new HashSet<>();

    private RoomRegistryActor(
            ActorContext<Command> context) {

        super(context);
    }

    public static Behavior<Command> create() {

        return Behaviors.setup(
                RoomRegistryActor::new
        );
    }

    @Override
    public Receive<Command> createReceive() {

        return newReceiveBuilder()
                .onMessage(
                        RegisterRoom.class,
                        this::onRegisterRoom
                )
                .onMessage(
                        GetRooms.class,
                        this::onGetRooms
                )
                .build();
    }

    private Behavior<Command> onRegisterRoom(
            RegisterRoom command) {

        rooms.add(command.roomName);

        return this;
    }

    private Behavior<Command> onGetRooms(
            GetRooms command) {

        command.replyTo.tell(
                new RoomsResponse(
                        Set.copyOf(rooms)
                )
        );

        return this;
    }
}