package at.fhv.sysarch.lab2.homeautomation.devices.environment;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.Optional;

public class Environment extends AbstractBehavior<Environment.EnvironmentCommand> {

    private final int TEMPERATURE_SCHEDULER = 10;
    private final int WEATHER_SCHEDULER = 30;

    public interface EnvironmentCommand {
    }

    public static final class ActivateEnvironment implements EnvironmentCommand {
        final Optional<Boolean> isActive;
        public ActivateEnvironment(Optional<Boolean> isActive) {
            this.isActive = isActive; }
    }

    public static final class TemperatureChanger implements EnvironmentCommand {
        public TemperatureChanger() { }
    }

    public static final class WeatherConditionsChanger implements EnvironmentCommand {
        public WeatherConditionsChanger() { }
    }

    public static Behavior<EnvironmentCommand> create(ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<WeatherSensor.WeatherCommand> weatherSensor) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new Environment(context, timers, timers, tempSensor, weatherSensor)));
    }

    private double temperature = 14;
    private Weather weather = Weather.CLOUDY;
    private final TimerScheduler<EnvironmentCommand> temperatureTimeScheduler;
    private final TimerScheduler<EnvironmentCommand> weatherTimeScheduler;
    private ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private ActorRef<WeatherSensor.WeatherCommand> weatherSensor;

    public Environment(ActorContext<EnvironmentCommand> context, TimerScheduler<EnvironmentCommand> tempTimer, TimerScheduler<EnvironmentCommand> weatherTimer, ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<WeatherSensor.WeatherCommand> weatherSensor) {
        super(context);
        this.temperatureTimeScheduler = tempTimer;
        this.weatherTimeScheduler = weatherTimer;
        this.temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChanger(), Duration.ofSeconds(TEMPERATURE_SCHEDULER));
        this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherConditionsChanger(), Duration.ofSeconds(WEATHER_SCHEDULER));
        this.tempSensor = tempSensor;
        this.weatherSensor = weatherSensor;
        getContext().getLog().info("Environment ready for simulation");
    }

    @Override
    public Receive<EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ActivateEnvironment.class, this::onActivateEnvironment)
                .onMessage(TemperatureChanger.class, this::onChangeTemperature)
                .onMessage(WeatherConditionsChanger.class, this::onChangeWeather)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<EnvironmentCommand> onActivateEnvironment(ActivateEnvironment ae) {
        if(ae.isActive.get()) {
            getContext().getLog().info("Environment active");
            this.temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChanger(), Duration.ofSeconds(TEMPERATURE_SCHEDULER));
            this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherConditionsChanger(), Duration.ofSeconds(WEATHER_SCHEDULER));
        } else {
            getContext().getLog().info("Environment stopped");
            this.temperatureTimeScheduler.cancelAll();
            this.weatherTimeScheduler.cancelAll();
        }
        return this;
    }

    private Behavior<EnvironmentCommand> onChangeTemperature(TemperatureChanger t) {
        int value = (int) (Math.random() * 4 + 1);
        this.temperature += value;
        this.tempSensor.tell(new TemperatureSensor.ReadTemperature(Optional.of(this.temperature)));
        return this;
    }

    private Behavior<EnvironmentCommand> onChangeWeather(WeatherConditionsChanger w) {
        this.weather = this.weather == Weather.SUNNY ? Weather.CLOUDY : Weather.SUNNY;

        if (this.weather.equals(Weather.CLOUDY)) {
            //Temperature drops to 14°C if it is cloudy
            this.temperature = 14;
            this.tempSensor.tell(new TemperatureSensor.ReadTemperature(Optional.of(this.temperature)));
        }
        this.weatherSensor.tell(new WeatherSensor.DetermineWeatherCondition(Optional.of(this.weather)));
        return this;
    }

    private Environment onPostStop() {
        getContext().getLog().info("Environment Application stopped");
        return this;
    }
}
