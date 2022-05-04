package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

public class TemperatureSensor extends AbstractBehavior<TemperatureSensor.TemperatureCommand> { //Typ an Nachrichten, die er verarbeiten kann

    public interface TemperatureCommand {}

    public static final class ReadTemperature implements TemperatureCommand {
        final Optional<Double> value;

        public ReadTemperature(Optional<Double> value) {
            this.value = value;
        }
    }

    public static Behavior<TemperatureCommand> create(ActorRef<AirCondition.AirConditionCommand> airCondition) {
        return Behaviors.setup(context -> new TemperatureSensor(context, airCondition));
    }

    private ActorRef<AirCondition.AirConditionCommand> airCondition;

    private TemperatureSensor(ActorContext<TemperatureCommand> context, ActorRef<AirCondition.AirConditionCommand> airCondition) {
        super(context);
        this.airCondition = airCondition;

        getContext().getLog().info("TemperatureSensor started");
    }

    @Override
    public Receive<TemperatureCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadTemperature.class, this::onReadTemperature) //WEnn ich diesen Nachrichtentyp erhalte, rufe bis onReadTemperature auf --> Reaktion auf Nachrichten. Hier kann man auch filtern
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<TemperatureCommand> onReadTemperature(ReadTemperature r) {
        getContext().getLog().info("TemperatureSensor received {}", r.value.get());
        this.airCondition.tell(new AirCondition.EnrichedTemperature(r.value, Optional.of("Celsius")));
        return this; //Behaviors.same ist das gleiche; Das Gleiche Verhalten wird beim nächsten mal erwartet. Man kann auch sagen, dass er sich komplett anders Verhalten soll
    }

    private TemperatureSensor onPostStop() {
        getContext().getLog().info("TemperatureSensor actor {}-{} stopped");
        return this;
    }

}
