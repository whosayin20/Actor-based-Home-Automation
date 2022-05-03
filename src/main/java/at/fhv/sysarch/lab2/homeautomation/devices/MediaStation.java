package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

public class MediaStation extends AbstractBehavior<MediaStation.MediaCommand> {
    public interface MediaCommand {
    }

    public static final class PowerMediaStation implements MediaCommand {
        final Optional<Boolean> value;

        public PowerMediaStation(Optional<Boolean> value) {
            this.value = value;
        }
    }

    public static final class PlayMovie implements MediaCommand {
        final Optional<Boolean> play;

        public PlayMovie(Optional<Boolean> play) {
            this.play = play;
        }
    }

    public static final class Request implements MediaCommand {
        public final ActorRef<Blind.BlindCommand> replyTo;

        public Request(ActorRef<Blind.BlindCommand> replyTo) {
            this.replyTo = replyTo;
        }
    }


    public static Behavior<MediaCommand> create(ActorRef<Blind.BlindCommand> blind) {
        return Behaviors.setup(context -> new MediaStation(context, blind));
    }

    private ActorRef<Blind.BlindCommand> blind;
    private MediaStation(ActorContext<MediaCommand> context, ActorRef<Blind.BlindCommand> blind) {
        super(context);
        this.blind = blind;
        getContext().getLog().info("Mediastation started");
    }

    @Override
    public Receive<MediaCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(PlayMovie.class, this::onPlayMovie)
                .onMessage(Request.class, this::onRequest)
                .onMessage(PowerMediaStation.class, this::onPowerMediaStationOff)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }


    private Behavior<MediaCommand> onRequest(Request request) {
        request.replyTo.tell(new Blind.Response(Optional.of(true)));
        return Behaviors.same();
    }

    private Behavior<MediaCommand> onPowerMediaStationOff(PowerMediaStation p) {
        if (p.value.get() == false) { //and movie not playing
            getContext().getLog().info("Turning Off Media Station");
            return Behaviors.receive(MediaCommand.class)
                    .onMessage(PowerMediaStation.class, this::onPowerMediaStationOn)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        } else {
            getContext().getLog().info("Media Station already playing");
        }

        return this;
    }

    private Behavior<MediaCommand> onPowerMediaStationOn(PowerMediaStation p) {
        if (p.value.get() == true) {
            getContext().getLog().info("Turning On Media Station");
            return Behaviors.receive(MediaCommand.class)
                    .onMessage(PlayMovie.class, this::onPlayMovie)
                    .onMessage(Request.class, this::onRequest)
                    .onMessage(PowerMediaStation.class, this::onPowerMediaStationOff)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        } else {
            getContext().getLog().info("Media Station already closed");
        }
        return Behaviors.same();
    }

    private Behavior<MediaStation.MediaCommand> onPlayMovie(PlayMovie pm) {
        if(pm.play.get() == true) {
            getContext().getLog().info("Turning on Movie");
            this.blind.tell(new Blind.OpenCloseBlind(Optional.of(Boolean.TRUE)));
        } else {
            getContext().getLog().info("Turning off Movie");
        }
        return this;
    }

    private MediaStation onPostStop() {
        getContext().getLog().info("MediaStation actor {}-{} stopped");
        return this;
    }
}