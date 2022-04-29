package at.fhv.sysarch.lab2.homeautomation.devices;


import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.Random;

public class Environment extends AbstractBehavior<Environment.EnvironmentCommand> {

    public interface EnvironmentCommand {}

    public static final class TemperatureChanger implements EnvironmentCommand {
        public TemperatureChanger() {}
    }

    public static final class WeatherConditionsChanger implements EnvironmentCommand {
        final Optional<Boolean> isSunny;

        public WeatherConditionsChanger(Optional<Boolean> isSunny) { this.isSunny = isSunny; }
    }

    private double temperature = 20;
    private boolean isSunny = false;

    private final TimerScheduler<EnvironmentCommand> temperatureTimeScheduler;
    private final TimerScheduler<EnvironmentCommand> weatherTimeScheduler;

    public static Behavior<EnvironmentCommand> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new Environment(context, timers, timers)));
    }

    public Environment(ActorContext<EnvironmentCommand> context, TimerScheduler<EnvironmentCommand> tempTimer, TimerScheduler<EnvironmentCommand> weatherTimer) {
        super(context);
        this.temperatureTimeScheduler = tempTimer;
        this.weatherTimeScheduler = weatherTimer;
        this.temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChanger(), Duration.ofSeconds(5));
        this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherConditionsChanger(Optional.of(isSunny)), Duration.ofSeconds(30));

        getContext().getLog().info("Environment ready for simulation");
    }

    @Override
    public Receive<EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(TemperatureChanger.class, this::onChangeTemperature)
                .onMessage(WeatherConditionsChanger.class, this::onChangeWeather)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<EnvironmentCommand> onChangeTemperature(TemperatureChanger t) {
        Random random = new Random();
        double value = BigDecimal.valueOf(random.nextDouble()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        this.temperature += value;

        getContext().getLog().info("Temperature changed " + temperature );
        return this;
    }

    private Behavior<EnvironmentCommand> onChangeWeather(WeatherConditionsChanger w) {
        this.temperature = 10;
        return this;
    }

    private Environment onPostStop() {
        getContext().getLog().info("Environment Application stopped");
        return this;
    }

}
