package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import java.time.Duration;
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

    public static final class CheckMovieStatus implements BlindCommand {
        public CheckMovieStatus() {
        }
    }

    public static final class Response implements BlindCommand {
        public Optional<Boolean> isPlaying;

        public Response(Optional<Boolean> isPlaying) {
            this.isPlaying = isPlaying;
        }
    }

    public static Behavior<BlindCommand> create(ActorRef<MediaStation.MediaCommand> mediaStation) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new Blind(context, timers, mediaStation)));
    }

    private ActorRef<MediaStation.MediaCommand> mediaStation;

    private final TimerScheduler<BlindCommand> movieTimeScheduler;

    private boolean isMovieCurrentlyPlaying;

    private Blind(ActorContext<BlindCommand> context, TimerScheduler<BlindCommand> tempTimer, ActorRef<MediaStation.MediaCommand> mediaStation) {
        super(context);
        this.mediaStation = mediaStation;
        this.movieTimeScheduler = tempTimer;
        this.movieTimeScheduler.startTimerAtFixedRate(new CheckMovieStatus(), Duration.ofSeconds(10));
        getContext().getLog().info("Opened Blind");
    }

    @Override
    public Receive<BlindCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(CheckMovieStatus.class, this::reqMovieStatus)
                .onMessage(OpenCloseBlind.class, this::onCloseBlind)
                .onMessage(Response.class, this::processResponseFromMediaStation)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindCommand> onCloseBlind(OpenCloseBlind ocb) {
        if (ocb.isSunny.get() == true) {
            getContext().getLog().info("Closing Blind");
            return close();
        }
        return Behaviors.same();
    }

    private Behavior<Blind.BlindCommand> close() {
        return Behaviors.receive(BlindCommand.class)
                .onMessage(CheckMovieStatus.class, this::reqMovieStatus)
                .onMessage(OpenCloseBlind.class, this::onOpenBlind)
                .onMessage(Response.class, this::processResponseFromMediaStation)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    //Überprüft in einem Zeitintervall, ob ein Film abspielt. Wenn ja, close blind.
    private Behavior<BlindCommand> reqMovieStatus(CheckMovieStatus cms) {
        this.mediaStation.tell(new MediaStation.Request(super.getContext().getSelf()));
        return Behaviors.same();
    }

    private Behavior<BlindCommand> onOpenBlind(OpenCloseBlind ocb) {
        if (ocb.isSunny.get() == false) {
            if (isMovieCurrentlyPlaying == false) {
                getContext().getLog().info("Opening Blind");
                return this.createReceive();
            } else {
                getContext().getLog().info("Can not Open Blind - Movie is currently playing");
            }
        }
        return Behaviors.same();
    }

    private Behavior<BlindCommand> processResponseFromMediaStation(Response r) {
        isMovieCurrentlyPlaying = r.isPlaying.get();
        if (r.isPlaying.get()) {
            getContext().getLog().info("Movie is currently playing - Blinds are closed");
            return close();
        }
        return Behaviors.same();
    }

    private Blind onPostStop() {
        getContext().getLog().info("Blind actor {}-{} stopped");
        return this;
    }
}