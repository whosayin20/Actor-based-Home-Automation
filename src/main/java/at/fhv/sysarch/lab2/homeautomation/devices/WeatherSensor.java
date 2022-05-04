package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherCommand> {
    public interface WeatherCommand {}

    public static final class DetermineWeatherCondition implements WeatherCommand {
        final Optional<Boolean> isSunny;

        public DetermineWeatherCondition(Optional<Boolean> isSunny) {
            this.isSunny = isSunny;
        }
    }

    public static Behavior<WeatherCommand> create(ActorRef<Blind.BlindCommand> blind) {
        return Behaviors.setup(context -> new WeatherSensor(context, blind));
    }
    private ActorRef<Blind.BlindCommand> blind;

    private WeatherSensor(ActorContext<WeatherCommand> context, ActorRef<Blind.BlindCommand> blind) {
        super(context);
        this.blind = blind;
        getContext().getLog().info("WeatherSensor started");
    }

    @Override
    public Receive<WeatherCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(DetermineWeatherCondition.class, this::onReadWeather)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WeatherSensor.WeatherCommand> onReadWeather(DetermineWeatherCondition r) {
        String condition = r.isSunny.get() ? "SUNNY weather" : "CLOUDY weather";
        getContext().getLog().info("WeatherSensor detected {}", condition);
        this.blind.tell(new Blind.OpenCloseBlind(r.isSunny));
        return this;
    }

    private WeatherSensor onPostStop() {
        getContext().getLog().info("WeatherSensor actor {}-{} stopped");
        return this;
    }
}
