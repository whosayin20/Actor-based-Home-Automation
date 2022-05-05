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

    public static Behavior<Void> create() {
        return Behaviors.setup(HomeAutomationController::new);
    }

    private HomeAutomationController(ActorContext<Void> context) {
        //AirConditioner nimmt eine referenz vom tempSensor und fragt ihn regelmäßig ab;
        //Als Listen sammeln, oder controller dient als dispatcher der nachrichten empfangt der abhängig davon einen redirect macht. Der Controller weiß, wer interessiert daran ist
        super(context);
        this.blind = getContext().spawn(Blind.create(), "Blind");
        this.mediasStation = getContext().spawn(MediaStation.create(this.blind), "MediaStation");
        this.airCondition = getContext().spawn(AirCondition.create(), "AirCondition");
        this.tempSensor = getContext().spawn(TemperatureSensor.create(this.airCondition), "temperatureSensor");
        this.weatherSensor = getContext().spawn(WeatherSensor.create(this.blind), "weatherSensor");

        //getContext().spawn(Environment.create(this.tempSensor, this.weatherSensor), "Environment");
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

