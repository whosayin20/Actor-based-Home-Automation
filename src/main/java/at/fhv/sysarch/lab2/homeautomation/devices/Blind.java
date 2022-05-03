package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

public class Blind extends AbstractBehavior<Blind.BlindCommand> {
    public interface BlindCommand {
    }

    public static final class OpenCloseBlind implements BlindCommand {
        final Optional<Boolean> isSunny;

        public OpenCloseBlind(Optional<Boolean> isSunny) {
            this.isSunny = isSunny;
        }
    }

    public static final class Response implements BlindCommand {
        public Optional<Boolean> isPlaying;

        public Response(Optional<Boolean> isPlaying) {
            this.isPlaying = isPlaying;
        }
    }

    public static Behavior<BlindCommand> create(ActorRef<MediaStation.MediaCommand> mediaStation) {
        return Behaviors.setup(context -> new Blind(context, mediaStation));
    }

    private ActorRef<MediaStation.MediaCommand> mediaStation;

    private Blind(ActorContext<BlindCommand> context, ActorRef<MediaStation.MediaCommand> mediaStation) {
        super(context);
        this.mediaStation = mediaStation;
        getContext().getLog().info("Opening Blind");
    }

    @Override
    public Receive<BlindCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(OpenCloseBlind.class, this::onCloseBlind)
                .onMessage(Response.class, this::processResponse)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindCommand> onCloseBlind(OpenCloseBlind ob) {
        if (ob.isSunny.get() == true) { //and movie not playing
            getContext().getLog().info("Closing Blind");
            return Behaviors.receive(BlindCommand.class)
                    .onMessage(OpenCloseBlind.class, this::onOpenBlind)
                    .onMessage(Response.class, this::processResponse)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        }
        return this;
    }

    private Behavior<BlindCommand> onOpenBlind(OpenCloseBlind ob) {
        if (ob.isSunny.get() == false) {
            getContext().getLog().info("Checking if Media Station is playing a movie");
            this.mediaStation.tell(new MediaStation.Request(super.getContext().getSelf()));
        }
        return this;
    }

    private Behavior<BlindCommand> processResponse(Response r) {
        if(!r.isPlaying.get()) {
            getContext().getLog().info("Opening Blind");
            return Behaviors.receive(BlindCommand.class)
                    .onMessage(OpenCloseBlind.class, this::onCloseBlind)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        } else {
            getContext().getLog().info("Blinds won't open - Movie is playing");
        }

        return this;
    }


    private Blind onPostStop() {
        getContext().getLog().info("Blind actor {}-{} stopped");
        return this;
    }
}
