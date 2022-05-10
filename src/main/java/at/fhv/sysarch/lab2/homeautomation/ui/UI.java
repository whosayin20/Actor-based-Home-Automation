package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.AirCondition;
import at.fhv.sysarch.lab2.homeautomation.devices.MediaStation;
import at.fhv.sysarch.lab2.homeautomation.devices.environment.Environment;
import at.fhv.sysarch.lab2.homeautomation.devices.environment.TemperatureSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.environment.Weather;
import at.fhv.sysarch.lab2.homeautomation.devices.environment.WeatherSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.Fridge;
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
    private ActorRef<Environment.EnvironmentCommand> environment;

    public static Behavior<Void> create(ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<MediaStation.MediaCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge, ActorRef<Environment.EnvironmentCommand> environment) {
        return Behaviors.setup(context -> new UI(context, tempSensor, airCondition, weatherSensor, mediaStation, fridge, environment));
    }

    private  UI(ActorContext<Void> context, ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<MediaStation.MediaCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge, ActorRef<Environment.EnvironmentCommand> environment) {
        super(context);
        this.airCondition = airCondition;
        this.tempSensor = tempSensor;
        this.weatherSensor = weatherSensor;
        this.mediaStation = mediaStation;
        this.fridge = fridge;
        this.environment = environment;
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
        Scanner scanner = new Scanner(System.in);
        String reader = "";

        System.out.println("-----------------ENVIRONMENT----------------");
        System.out.println("Environment Turn ON/OFF - E TRUE/FALSE");
        System.out.println("Manually Change Temperature - T *NUMBER*");
        System.out.println("Manually Change Weather - W SUNNY/CLOUDY ");
        System.out.println("-----------------AIR CONDITION----------------");
        System.out.println("Aircondition Power - AP TRUE/FALSE ");
        System.out.println("-----------------MEDIA STATION----------------");
        System.out.println("Mediastation Power - MP TRUE/FALSE");
        System.out.println("Mediastation Play Movie - MPM TRUE/FALSE");
        System.out.println("-----------------FRIDGE----------------");
        System.out.println("Fridge Power - FP TRUE/FALSE");
        System.out.println("Fridge Order - FO");
        System.out.println("Fridge Consume - FC");
        System.out.println("Fridge History - FH");
        System.out.println("-----------------------------------------");


        while (!reader.equalsIgnoreCase("quit") && scanner.hasNextLine()) {
            reader = scanner.nextLine();
            String[] command = reader.split(" ");
            if(command[0].equalsIgnoreCase("e")) {
                this.environment.tell(new Environment.ActivateEnvironment(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equalsIgnoreCase("t")) {
                this.tempSensor.tell(new TemperatureSensor.ReadTemperature(Optional.of(Double.valueOf(command[1]))));
            }
            if(command[0].equalsIgnoreCase("w")) {
                this.weatherSensor.tell(new WeatherSensor.DetermineWeatherCondition(Optional.of(Weather.valueOf(command[1].toUpperCase()))));
            }
            if(command[0].equalsIgnoreCase("ap")) {
                this.airCondition.tell(new AirCondition.PowerAirCondition(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equalsIgnoreCase("mp")) {
                this.mediaStation.tell(new MediaStation.PowerMediaStation(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equalsIgnoreCase("mpm")) {
                this.mediaStation.tell(new MediaStation.PlayMovie(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equalsIgnoreCase("fp")) {
                this.fridge.tell(new Fridge.PowerFridge(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equalsIgnoreCase("fo") || command[0].equals("fc")) {
                if((command[0].equals("fo"))) {
                    System.out.println("What do do you want to order?");
                } else if (command[0].equals("fc")) {
                    System.out.println("What do do you want to consume?");
                }
                System.out.println("Cheese | Egg | Salad | Tomato");
                reader = scanner.nextLine();
                Product p = null;
                switch (reader.toLowerCase()) {
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
                    default:
                        System.out.println("Invalid Product");
                }

                if((command[0].equals("fo")) && p != null) {
                    this.fridge.tell(new Fridge.OrderProduct(Optional.of(p)));
                } else if (command[0].equals("fc") && p != null) {
                    this.fridge.tell(new Fridge.Consume(Optional.of(p)));
                }
            }
            if(command[0].equals("fh")) {
                this.fridge.tell(new Fridge.ShowHistory());
            }
        }
        getContext().getLog().info("UI done");
    }
}
