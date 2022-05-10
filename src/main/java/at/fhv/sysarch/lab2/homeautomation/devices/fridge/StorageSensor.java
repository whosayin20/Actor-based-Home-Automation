package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.products.Product;

import java.util.Optional;

public class StorageSensor extends AbstractBehavior<StorageSensor.StorageSensorCommand> {

    public interface StorageSensorCommand { }


    public static final class PutStorage implements StorageSensorCommand {
        public final ActorRef<Order.OrderCommand> replyTo;
        final Optional<Product> product;

        public PutStorage(ActorRef<Order.OrderCommand> replyTo, Optional<Product> product) {
            this.replyTo = replyTo;
            this.product = product;
        }
    }

    public static final class TakeStorage implements StorageSensorCommand {
        final Optional<Integer> storage;

        public TakeStorage(Optional<Integer> storage) {
            this.storage = storage;
        }
    }

    public static Behavior<StorageSensorCommand> create(int maxStorage){
        return Behaviors.setup(context -> new StorageSensor(context, maxStorage));
    }

    final int maxStorage;
    int currentStorage;

    private StorageSensor(ActorContext<StorageSensorCommand> context, int maxStorage){
        super(context);
        this.maxStorage = maxStorage;
        this.currentStorage = 0;
    }

    @Override
    public Receive<StorageSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(PutStorage.class, this::onPutStorage)
                .onMessage(TakeStorage.class, this::onTakeStorage)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<StorageSensorCommand> onPutStorage(PutStorage ps) {
        Product product = ps.product.get();
        int storage = 1;
        if(currentStorage + storage <= maxStorage && storage > 0) {
            currentStorage += storage;
            ps.replyTo.tell((new Order.ResponseStorageSensor(Optional.of(Boolean.TRUE), Optional.of((product)))));
        } else {
            ps.replyTo.tell((new Order.ResponseStorageSensor(Optional.of(Boolean.FALSE), Optional.of(product))));
        }
        return Behaviors.same();
    }

    private Behavior<StorageSensorCommand> onTakeStorage(TakeStorage ts) {
        int storage = ts.storage.get();
        if (storage > 0){
            getContext().getLog().info("Removing " + storage + " product from the fridge");
            this.currentStorage -= storage;
        }
        return Behaviors.same();
    }

    private StorageSensor onPostStop() {
        getContext().getLog().info("StorageSensor actor {}-{} stopped");
        return this;
    }
}

