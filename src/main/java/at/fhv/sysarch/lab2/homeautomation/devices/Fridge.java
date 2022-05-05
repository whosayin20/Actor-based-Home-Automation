package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.products.Product;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Fridge extends AbstractBehavior<Fridge.FridgeCommand> {

    public interface FridgeCommand {
    }

    public static final class PowerFridge implements FridgeCommand {
        final Optional<Boolean> value;

        public PowerFridge(Optional<Boolean> value) {
            this.value = value;
        }
    }

    public static final class Consume implements FridgeCommand {
        final Optional<Product> product;

        public Consume(Optional<Product> product) {
            this.product = product;
        }
    }

    public static final class OrderProduct implements FridgeCommand {
        final Optional<Product> product;

        public OrderProduct(Optional<Product> product) {
            this.product = product;
        }
    }

    public static final class ResponseWeightSensor implements FridgeCommand {
        final Optional<Boolean> value;
        final Optional<Product> product;

        public ResponseWeightSensor(Optional<Boolean> value, Optional<Product> product) {
            this.value = value;
            this.product = product;
        }
    }

    public static final class ResponseStorageSensor implements FridgeCommand {
        final Optional<Boolean> value;

        final Optional<Product> product;

        public ResponseStorageSensor(Optional<Boolean> value, Optional<Product> product) {
            this.value = value;
            this.product = product;
        }
    }

    public static final class CommitOrder implements FridgeCommand {
        final Optional<Product> product;

        public CommitOrder(Optional<Product> product) {
            this.product = product;
        }
    }

    public static final class ControlProducts implements FridgeCommand {

        public ControlProducts() {
        }
    }

    public static Behavior<FridgeCommand> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new Fridge(context, timers)));
    }

    private HashMap<Product, Integer> productQuantity;

    private ActorRef<WeightSensor.WeightSensorCommand> weightSensor;

    private ActorRef<StorageSensor.StorageSensorCommand> storageSensor;

    private final TimerScheduler<Fridge.FridgeCommand> fridgeTimeScheduler;

    private Fridge(ActorContext<FridgeCommand> context, TimerScheduler<Fridge.FridgeCommand> fridgeTimer) {
        super(context);
        this.fridgeTimeScheduler = fridgeTimer;
        this.fridgeTimeScheduler.startTimerAtFixedRate(new Fridge.ControlProducts(), Duration.ofSeconds(10));
        this.weightSensor = getContext().spawn(WeightSensor.create(1000), "weightSensor");
        this.storageSensor = getContext().spawn(StorageSensor.create(20), "storageSensor");
        getContext().getLog().info("Fridge started");
    }

    @Override
    public Receive<Fridge.FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ResponseWeightSensor.class, this::onResponseWeightSensor)
                .onMessage(ResponseStorageSensor.class, this::onResponseStorageSensor)
                .onMessage(ControlProducts.class, this::onControlProducts)
                .onMessage(OrderProduct.class, this::onOrderProduct)
                .onMessage(PowerFridge.class, this::onPowerFridgeOff)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeCommand> onPowerFridgeOff(PowerFridge pf) {
        if (pf.value.get() == false) {
            getContext().getLog().info("Powering Fridge off");
            return Behaviors.receive(FridgeCommand.class)
                    .onMessage(PowerFridge.class, this::onPowerFridgeOn)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        } else {
            getContext().getLog().info("Fridge is already powered on");
        }

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onPowerFridgeOn(PowerFridge pf) {
        if (pf.value.get() == true) {
            getContext().getLog().info("Powering Fridge on");
            return createReceive();
        } else {
            getContext().getLog().info("Fridge is already turned off");
        }
        return Behaviors.same();
    }



    private Behavior<FridgeCommand> onOrderProduct(OrderProduct op) {
        Product p = op.product.get();

        this.weightSensor.tell(new WeightSensor.PutWeight(super.getContext().getSelf(), Optional.of(p)));

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onControlProducts(ControlProducts cp) {
        for (Map.Entry<Product, Integer> pq : productQuantity.entrySet()) {
            if (pq.getValue() == 0) {
                super.getContext().getSelf().tell(new Fridge.OrderProduct(Optional.of(pq.getKey())));
            }
        }
        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onResponseWeightSensor(ResponseWeightSensor rws) {
        Product product = rws.product.get();
        if (rws.value.get()) {
            getContext().getLog().info("Fridge has enough weight load");
            this.storageSensor.tell(new StorageSensor.PutStorage(super.getContext().getSelf(), Optional.of(product)));
        } else {
            getContext().getLog().info("Fridge doesn't has enough weight load");
        }

        return Behaviors.same();
    }

    private Fridge onPostStop() {
        getContext().getLog().info("Fridge actor {}-{} stopped");
        return this;
    }
}
