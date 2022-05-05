package at.fhv.sysarch.lab2.homeautomation.devices;

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

    public static final class MovieNotification implements BlindCommand {
        final Optional<Boolean> isPlaying;

        public MovieNotification(Optional<Boolean> isPlaying) {
            this.isPlaying = isPlaying;
        }
    }

    public static Behavior<BlindCommand> create() {
        return Behaviors.setup(context -> new Blind(context));
    }

    private boolean isMovieCurrentlyPlaying;

    private Blind(ActorContext<BlindCommand> context) {
        super(context);
        getContext().getLog().info("Opened Blind");
    }

    @Override
    public Receive<BlindCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(MovieNotification.class, this::onMovieNotification)
                .onMessage(OpenCloseBlind.class, this::onCloseBlind)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindCommand> onMovieNotification(MovieNotification mn) {
        isMovieCurrentlyPlaying = mn.isPlaying.get();
        if(isMovieCurrentlyPlaying) return close();
        return Behaviors.same();
    }

    private Behavior<BlindCommand> onCloseBlind(OpenCloseBlind ocb) {
        if (ocb.isSunny.get() == true) {
            close();
        }
        return Behaviors.same();
    }

    private Behavior<BlindCommand> close() {
        getContext().getLog().info("Closing Blind");
        return Behaviors.receive(BlindCommand.class)
                .onMessage(MovieNotification.class, this::onMovieNotification)
                .onMessage(OpenCloseBlind.class, this::onOpenBlind)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindCommand> onOpenBlind(OpenCloseBlind ocb) {
        if (ocb.isSunny.get() == false) {
            if (isMovieCurrentlyPlaying == false) {
                getContext().getLog().info("Opening Blind");
                return this.createReceive();
            } else {
                getContext().getLog().info("Blind will not open - Movie is currently playing");
            }
        }
        return Behaviors.same();
    }

    private Blind onPostStop() {
        getContext().getLog().info("Blind actor {}-{} stopped");
        return this;
    }
}