package at.fhv.sysarch.lab2.homeautomation;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.devices.environment.TemperatureSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.environment.WeatherSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.Fridge;
import at.fhv.sysarch.lab2.homeautomation.ui.UI;

//Erzeugt andere Aktoren, die miteinander kommunizieren. Mit Basiswerten initialisieren
//Controller is der Parent von den anderen
public class HomeAutomationController extends AbstractBehavior<Void>{
    private ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private  ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private ActorRef<MediaStation.MediaCommand> mediasStation;
    private ActorRef<Blind.BlindCommand> blind;

    private ActorRef<Fridge.FridgeCommand> fridge;

    public static Behavior<Void> create() {
        return Behaviors.setup(HomeAutomationController::new);
    }

    private HomeAutomationController(ActorContext<Void> context) {
        super(context);
        this.blind = getContext().spawn(Blind.create(), "Blind");
        this.mediasStation = getContext().spawn(MediaStation.create(this.blind), "MediaStation");
        this.airCondition = getContext().spawn(AirCondition.create(), "AirCondition");
        this.tempSensor = getContext().spawn(TemperatureSensor.create(this.airCondition), "temperatureSensor");
        this.weatherSensor = getContext().spawn(WeatherSensor.create(this.blind), "weatherSensor");
        this.fridge = getContext().spawn(Fridge.create(), "Fridge");
        //getContext().spawn(Environment.create(this.tempSensor, this.weatherSensor), "Environment");
        ActorRef<Void> ui = getContext().spawn(UI.create(this.tempSensor, this.airCondition, this.weatherSensor, this.mediasStation, this.fridge), "UI");
        getContext().getLog().info("HomeAutomation Application started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private HomeAutomationController onPostStop() {
        getContext().getLog().info("HomeAutomation Application stopped");
        return this;
    }
}

