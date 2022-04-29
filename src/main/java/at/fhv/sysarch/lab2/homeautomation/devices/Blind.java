package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

public class Blind extends AbstractBehavior<Blind.BlindCommand> {
    public interface BlindCommand { }

    public static final class CloseBlind implements BlindCommand {
        final Optional<Boolean> isClosed;

        public CloseBlind(Optional<Boolean> isClosed) {
            this.isClosed = isClosed;
        }
    }


    private boolean isClosed = true;

    private Blind(ActorContext<Blind.BlindCommand> context) {
        super(context);
        getContext().getLog().info("Blind opened");
    }

    public static Behavior<BlindCommand> create() {
        return Behaviors.setup(Blind::new);
    }


    @Override
    public Receive<BlindCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(CloseBlind.class, this::onCloseBlind)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindCommand> onCloseBlind(CloseBlind c) {
        getContext().getLog().info("Blind closed");

        if(c.isClosed.get()) {
            this.isClosed = true;
        }

        return this;
    }

    private Behavior<BlindCommand> onOpenBlind(CloseBlind c) {
        getContext().getLog().info("Blind closed");

        if(!c.isClosed.get()) {
            this.isClosed = false;
        }

        return this;
    }


    private Blind onPostStop() {
        getContext().getLog().info("Blind actor {}-{} stopped");
        return this;
    }
}
