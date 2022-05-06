package at.fhv.sysarch.lab2.homeautomation.devices.environment;


import akka.actor.typed.ActorRef;
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

        public WeatherConditionsChanger(Optional<Boolean> isSunny)  {this.isSunny = isSunny; }
    }

    public static Behavior<EnvironmentCommand> create(ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<WeatherSensor.WeatherCommand> weatherSensor) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new Environment(context, timers, timers, tempSensor, weatherSensor)));
    }

    private double temperature = 20;
    private boolean isSunny = false;

    private final TimerScheduler<EnvironmentCommand> temperatureTimeScheduler;
    private final TimerScheduler<EnvironmentCommand> weatherTimeScheduler;
    private ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private ActorRef<WeatherSensor.WeatherCommand> weatherSensor;

    public Environment(ActorContext<EnvironmentCommand> context, TimerScheduler<EnvironmentCommand> tempTimer, TimerScheduler<EnvironmentCommand> weatherTimer, ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<WeatherSensor.WeatherCommand> weatherSensor) {
        super(context);
        this.temperatureTimeScheduler = tempTimer;
        this.weatherTimeScheduler = weatherTimer;
        this.temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChanger(), Duration.ofSeconds(5));
        this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherConditionsChanger(Optional.of(isSunny)), Duration.ofSeconds(30));
        this.tempSensor = tempSensor;
        this.weatherSensor = weatherSensor;
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
        this.tempSensor.tell(new TemperatureSensor.ReadTemperature(Optional.of(this.temperature)));
        getContext().getLog().info("Temperature changed " + temperature );
        return this;
    }

    private Behavior<EnvironmentCommand> onChangeWeather(WeatherConditionsChanger w) {
        this.temperature = 10;
        this.weatherSensor.tell(new WeatherSensor.DetermineWeatherCondition(Optional.of(this.isSunny)));
        return this;
    }

    private Environment onPostStop() {
        getContext().getLog().info("Environment Application stopped");
        return this;
    }
}
