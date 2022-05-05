package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.products.*;
import java.util.Optional;
import java.util.Scanner;

//Void, weil er keine Nachrichten verarbeitet
public class UI extends AbstractBehavior<Void> {

    private ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private ActorRef<MediaStation.MediaCommand> mediaStation;

    private ActorRef<Fridge.FridgeCommand> fridge;

    public static Behavior<Void> create(ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<MediaStation.MediaCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge) {
        return Behaviors.setup(context -> new UI(context, tempSensor, airCondition, weatherSensor, mediaStation, fridge));
    }

    private  UI(ActorContext<Void> context, ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<MediaStation.MediaCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge) {
        super(context);
        this.airCondition = airCondition;
        this.tempSensor = tempSensor;
        this.weatherSensor = weatherSensor;
        this.mediaStation = mediaStation;
        this.fridge = fridge;
        new Thread(() -> { this.runCommandLine(); }).start();
        getContext().getLog().info("UI started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private UI onPostStop() {
        getContext().getLog().info("UI stopped");
        return this;
    }

    public void runCommandLine() {
        // TODO: Create Actor for UI Input-Handling
        Scanner scanner = new Scanner(System.in);
        String[] input = null;
        String reader = "";


        while (!reader.equalsIgnoreCase("quit") && scanner.hasNextLine()) {
            reader = scanner.nextLine();
            // TODO: change input handling
            String[] command = reader.split(" ");
            if(command[0].equals("t")) {
                this.tempSensor.tell(new TemperatureSensor.ReadTemperature(Optional.of(Double.valueOf(command[1])))); //Wer, Was, Wie
            }
            if(command[0].equals("w")) {
                this.weatherSensor.tell(new WeatherSensor.DetermineWeatherCondition(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equals("a")) {
                this.airCondition.tell(new AirCondition.PowerAirCondition(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equals("m")) {
                this.mediaStation.tell(new MediaStation.PowerMediaStation(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equals("pl")) {
                this.mediaStation.tell(new MediaStation.PlayMovie(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equals("fo")) {
                System.out.println("What do do you want to order?");
                reader = scanner.nextLine();
                Product p = null;
                switch (reader) {
                    case "cheese":
                        p = new Cheese();
                        break;
                    case "egg":
                        p = new Egg();
                        break;
                    case "salad":
                        p = new Salad();
                        break;
                    case "tomato":
                        p = new Tomato();
                }
                this.fridge.tell(new Fridge.OrderProduct(Optional.of(p)));
            }
            if(command[0].equals("fc")) {
                System.out.println("What do do you want to consume?");
                reader = scanner.nextLine();
                Product p = null;
                switch (reader) {
                    case "cheese":
                        p = new Cheese();
                        break;
                    case "egg":
                        p = new Egg();
                        break;
                    case "salad":
                        p = new Salad();
                        break;
                    case "tomato":
                        p = new Tomato();
                }
                this.fridge.tell(new Fridge.Consume(Optional.of(p)));
            }
            if(command[0].equals("fh")) {
                this.fridge.tell(new Fridge.ShowHistory());
            }
        }
        getContext().getLog().info("UI done");
    }
}
