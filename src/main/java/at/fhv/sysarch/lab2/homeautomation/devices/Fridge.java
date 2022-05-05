package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.products.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    public static final class OrderCreated implements FridgeCommand {
        final Optional<Product> product;
        final Optional<LocalDateTime> dateTimeOfOrder;

        final Optional<String> orderId;

        final Optional<BigDecimal> price;

        public OrderCreated(Optional<Product> product, Optional<LocalDateTime> dateTimeOfOrder, Optional<String> orderId, Optional<BigDecimal> price) {
            this.product = product;
            this.dateTimeOfOrder = dateTimeOfOrder;
            this.orderId = orderId;
            this.price = price;
        }
    }

    public static final class ShowHistory implements FridgeCommand {
        public ShowHistory() {
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

    private List<OrderCreated> history;

    private ActorRef<WeightSensor.WeightSensorCommand> weightSensor;

    private ActorRef<StorageSensor.StorageSensorCommand> storageSensor;

    private final TimerScheduler<Fridge.FridgeCommand> fridgeTimeScheduler;

    private Fridge(ActorContext<FridgeCommand> context, TimerScheduler<Fridge.FridgeCommand> fridgeTimer) {
        super(context);
        this.fridgeTimeScheduler = fridgeTimer;
        this.fridgeTimeScheduler.startTimerAtFixedRate(new Fridge.ControlProducts(), Duration.ofSeconds(60));
        this.weightSensor = getContext().spawn(WeightSensor.create(80), "weightSensor");
        this.storageSensor = getContext().spawn(StorageSensor.create(5), "storageSensor");
        this.productQuantity = new HashMap<>();
        this.history = new ArrayList<>();
        getContext().getLog().info("Fridge started");
    }

    @Override
    public Receive<Fridge.FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(Consume.class, this::onConsume)
                .onMessage(ShowHistory.class, this::onShowHistory)
                .onMessage(OrderCreated.class, this::onOrderCreated)
                .onMessage(CommitOrder.class, this::onCommitOrder)
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
                getContext().getLog().info("{} is empty - ordering new", pq.getKey().getName());
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

    private Behavior<FridgeCommand> onResponseStorageSensor(ResponseStorageSensor rs) {
        Product product = rs.product.get();
        if (rs.value.get()) {
            getContext().getLog().info("Fridge has enough storage");
            super.getContext().getSelf().tell(new Fridge.CommitOrder(Optional.of(product)));
        } else {
            getContext().getLog().info("Fridge has not enough storage");
        }
        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onCommitOrder(CommitOrder co) {
        ActorRef<Order.OrderCommand> order;
        order = getContext().spawn(Order.create(co.product.get(), super.getContext().getSelf()), "Order");

        order.tell(new Order.GracefulShutdown());

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onOrderCreated(OrderCreated oc) {
        history.add(oc);
        Product product = oc.product.get();

        for (Map.Entry<Product, Integer> pq : productQuantity.entrySet()) {
            if (pq.getKey().getName().equals(product.getName())) {
                pq.setValue(pq.getValue() + 1);
                return Behaviors.same();
            }
        }
        this.productQuantity.put(product, 1);

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onShowHistory(ShowHistory sh) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (history.size() > 0) {
            getContext().getLog().info("____________________________________________________________");
        }
        for (int i = 0; i < history.size(); i++) {
            getContext().getLog().info("Order-Id: " + history.get(i).orderId.get() + " | "
                    + history.get(i).dateTimeOfOrder.get().format(formatter) + " | "
                    + history.get(i).product.get().getName() + " | €"
                    + history.get(i).price.get().setScale(2, RoundingMode.CEILING));
        }
        if (history.size() > 0) {
            getContext().getLog().info("____________________________________________________________");
        } else {
            getContext().getLog().info("The history is empty");
        }
        return Behaviors.same();
    }


    private Behavior<FridgeCommand> onConsume(Consume c) {
        Product product = c.product.get();

        boolean isAvailable = false;

        for (Map.Entry<Product, Integer> pq : productQuantity.entrySet()) {
            if (pq.getKey().getName().equals(product.getName())) {
                getContext().getLog().info("Consuming {}", product.getName());
                if (pq.getValue() - 1 >= 0) {
                    pq.setValue(pq.getValue() - 1);
                }
                this.weightSensor.tell(new WeightSensor.TakeWeight(Optional.of(product.getWeight())));
                this.storageSensor.tell(new StorageSensor.TakeStorage(Optional.of(1)));
                isAvailable = true;
            }
        }
        if (!isAvailable) {
            getContext().getLog().info("{} not available", product.getName());
        }
        return Behaviors.same();
    }

    private Fridge onPostStop() {
        getContext().getLog().info("Fridge actor {}-{} stopped");
        return this;
    }
}
