package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

/**
 * This class shows ONE way to switch behaviors in object-oriented style. Another approach is the use of static
 * methods for each behavior.
 *
 * The switching of behaviors is not strictly necessary for this example, but is rather used for demonstration
 * purpose only.
 *
 * For an example with functional-style please refer to: {@link https://doc.akka.io/docs/akka/current/typed/style-guide.html#functional-versus-object-oriented-style}
 *
 */
import java.util.Optional;

public class AirCondition extends AbstractBehavior<AirCondition.AirConditionCommand> {
    public interface AirConditionCommand {}

    public static final class PowerAirCondition implements AirConditionCommand {
        final Optional<Boolean> isPowerOn;

        public PowerAirCondition(Optional<Boolean> isPowerOn) {
            this.isPowerOn = isPowerOn;
        }
    }

    public static final class EnrichedTemperature implements AirConditionCommand {
        Optional<Double> value;
        Optional<String> unit;

        public EnrichedTemperature(Optional<Double> value, Optional<String> unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    public static Behavior<AirConditionCommand> create() {
        return Behaviors.setup(context -> new AirCondition(context));
    }

    private AirCondition(ActorContext<AirConditionCommand> context) {
        super(context);
        getContext().getLog().info("AirCondition started");
    }

    @Override
    public Receive<AirConditionCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(EnrichedTemperature.class, this::onReadTemperature)
                .onMessage(PowerAirCondition.class, this::onPowerAirConditionOff)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<AirConditionCommand> onReadTemperature(EnrichedTemperature r) {
        if(r.value.get() >= 20) {
            getContext().getLog().info("Aircondition actived");
        } else {
            getContext().getLog().info("Aircondition deactived");
        }
        return Behaviors.same();
    }

    private Behavior<AirConditionCommand> onPowerAirConditionOff(PowerAirCondition r) {
        if(r.isPowerOn.get() == false) {
            getContext().getLog().info("Powering Aircondition off");
            return this.powerOff();
        } else {
            getContext().getLog().info("Aircondition is already powered on");
        }
        return Behaviors.same();
    }

    private Behavior<AirConditionCommand> onPowerAirConditionOn(PowerAirCondition r) {
        if(r.isPowerOn.get() == true) {
            getContext().getLog().info("Powering Aircondition on");
            return Behaviors.receive(AirConditionCommand.class)
                    .onMessage(EnrichedTemperature.class, this::onReadTemperature)
                    .onMessage(PowerAirCondition.class, this::onPowerAirConditionOff)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        } else {
            getContext().getLog().info("Aircondition is already powered off");
        }
        return Behaviors.same();
    }

    private Behavior<AirConditionCommand> powerOff() {
        return Behaviors.receive(AirConditionCommand.class)
                .onMessage(PowerAirCondition.class, this::onPowerAirConditionOn)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private AirCondition onPostStop() {
        getContext().getLog().info("TemperatureSensor actor {}-{} stopped");
        return this;
    }
}
