package at.fhv.sysarch.lab2.homeautomation;

import akka.actor.Actor;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.ui.UI;

//erzeugt andere Aktoren, die miteinander kommunizieren. Mit Basiswerten initialisieren
public class HomeAutomationController extends AbstractBehavior<Void>{ //Controller is der Parent von den anderen
    private ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private  ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private ActorRef<MediaStation.MediaCommand> mediasStation;
    private ActorRef<Blind.BlindCommand> blind;

    private ActorRef<MediaBlindControl.MediaBlindCommand> mediaBlindControl;

    public static Behavior<Void> create() {
        return Behaviors.setup(HomeAutomationController::new);
    }

    private  HomeAutomationController(ActorContext<Void> context) {
        //AirConditioner nimmt eine referenz vom tempSensor und fragt ihn regelmäßig ab;
        //Als Listen sammeln, oder controller dient als dispatcher der nachrichten empfangt der abhängig davon einen redirect macht. Der Controller weiß, wer interessiert daran ist
        super(context);
        // TODO: consider guardians and hierarchies. Who should create and communicate with which Actors?
        this.airCondition = getContext().spawn(AirCondition.create("2", "1"), "AirCondition");
        this.tempSensor = getContext().spawn(TemperatureSensor.create(this.airCondition, "1", "1"), "temperatureSensor");
        this.blind = getContext().spawn(Blind.create(this.mediasStation), "Blind");
        this.weatherSensor = getContext().spawn(WeatherSensor.create(this.blind), "weatherSensor");
        this.mediasStation = getContext().spawn(MediaStation.create(this.blind), "MediaStation");

        //getContext().spawn(Environment.create(), "Environment");
        ActorRef<Void> ui = getContext().spawn(UI.create(this.tempSensor, this.airCondition, this.weatherSensor, this.mediasStation), "UI");
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

