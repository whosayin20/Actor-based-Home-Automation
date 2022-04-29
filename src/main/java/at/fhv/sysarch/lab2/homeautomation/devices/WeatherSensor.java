package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherSensorCommand> {
    public interface WeatherSensorCommand {}

    public static final class readWeatherCondition implements WeatherSensor.WeatherSensorCommand {
        final Optional<Boolean> isSunny;

        public readWeatherCondition(Optional<Boolean> isSunny) {
            this.isSunny = isSunny;
        }
    }

    public static Behavior<WeatherSensor.WeatherSensorCommand> create(ActorRef<Blind.BlindCommand> blind, String groupId, String deviceId) {
        return Behaviors.setup(context -> new WeatherSensor(context, blind, groupId, deviceId));
    }

    public WeatherSensor(ActorContext<WeatherSensorCommand> context, ActorRef<Blind.BlindCommand> blind, String groupId, String deviceId) {
        super(context);


        getContext().getLog().info("WeatherSensor started");
    }

    @Override
    public Receive<WeatherSensorCommand> createReceive() {
        return null;
    }

    private WeatherSensor onPostStop() {
        getContext().getLog().info("WeatherSensor actor {}-{} stopped");
        return this;
    }
}
