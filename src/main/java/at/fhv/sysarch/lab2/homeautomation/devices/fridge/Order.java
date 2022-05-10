package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.products.Product;
import java.time.LocalDateTime;
import java.util.Optional;

public class Order extends AbstractBehavior<Order.OrderCommand>  {

    public interface OrderCommand { }

    public static final class ResponseWeightSensor implements OrderCommand {
        final Optional<Boolean> value;
        final Optional<Product> product;

        public ResponseWeightSensor(Optional<Boolean> value, Optional<Product> product) {
            this.value = value;
            this.product = product;
        }
    }

    public static final class ResponseStorageSensor implements OrderCommand {
        final Optional<Boolean> value;

        final Optional<Product> product;

        public ResponseStorageSensor(Optional<Boolean> value, Optional<Product> product) {
            this.value = value;
            this.product = product;
        }
    }

    public static final class CommitOrder implements OrderCommand {
        final Optional<Product> product;

        public CommitOrder(Optional<Product> product) {
            this.product = product;
        }
    }


    public static Behavior<OrderCommand> create(Product product, ActorRef<Fridge.FridgeCommand> fridge, ActorRef<WeightSensor.WeightSensorCommand> weightSensor, ActorRef<StorageSensor.StorageSensorCommand> storageSensor) {
        return Behaviors.setup(context -> new Order(context, product, fridge, weightSensor, storageSensor));
    }

    private ActorRef<Fridge.FridgeCommand> fridge;
    private ActorRef<WeightSensor.WeightSensorCommand> weightSensor;

    private ActorRef<StorageSensor.StorageSensorCommand> storageSensor;


    private static int nextOrderId = 10000;

    private Order(ActorContext<OrderCommand> context, Product product, ActorRef<Fridge.FridgeCommand> fridge, ActorRef<WeightSensor.WeightSensorCommand> weightSensor, ActorRef<StorageSensor.StorageSensorCommand> storageSensor) {
        super(context);
        this.fridge = fridge;
        this.weightSensor = weightSensor;
        this.storageSensor = storageSensor;
        startOrderProcess(product);
    }

    @Override
    public Receive<OrderCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(CommitOrder.class, this::onCommitOrder)
                .onMessage(ResponseWeightSensor.class, this::onResponseWeightSensor)
                .onMessage(ResponseStorageSensor.class, this::onResponseStorageSensor)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    public void startOrderProcess(Product product) {
        getContext().getLog().info("Starting Order Process for {}", product.getClass().getSimpleName());
        this.weightSensor.tell(new WeightSensor.PutWeight(super.getContext().getSelf(), Optional.of(product)));
    }

    private Behavior<OrderCommand> onResponseWeightSensor(ResponseWeightSensor rws) {
        Product product = rws.product.get();
        if (rws.value.get()) {
            getContext().getLog().info("Fridge has enough weight load");
            this.storageSensor.tell(new StorageSensor.PutStorage(super.getContext().getSelf(), Optional.of(product)));
        } else {
            getContext().getLog().info("Fridge has not enough weight load");
            return Behaviors.stopped();
        }
        return Behaviors.same();
    }

    private Behavior<OrderCommand> onResponseStorageSensor(ResponseStorageSensor rs) {
        Product product = rs.product.get();
        if (rs.value.get()) {
            getContext().getLog().info("Fridge has enough storage");
            super.getContext().getSelf().tell(new Order.CommitOrder(Optional.of(product)));
        } else {
            getContext().getLog().info("Fridge has not enough storage");
            return Behaviors.stopped();
        }
        return Behaviors.same();
    }

    private Behavior<OrderCommand> onCommitOrder(CommitOrder co) {
        Product product = co.product.get();

        fridge.tell(new Fridge.OrderCreated(
                Optional.of(product),
                Optional.of(LocalDateTime.now()),
                Optional.of(String.valueOf(nextOrderId++)),
                Optional.of(product.getPrice())
        ));

        return Behaviors.stopped();
    }

    private Order onPostStop() {
        getContext().getLog().info("Order actor {}-{} stopped");
        return this;
    }
}
