package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.products.Product;

import java.util.Optional;

public class WeightSensor extends AbstractBehavior<WeightSensor.WeightSensorCommand> {

    public interface WeightSensorCommand { }

    public static final class PutWeight implements WeightSensorCommand {
        public final ActorRef<Fridge.FridgeCommand> replyTo;
        final Optional<Product> product;

        public PutWeight(ActorRef<Fridge.FridgeCommand> replyTo, Optional<Product> product) {
            this.replyTo = replyTo;
            this.product = product;
        }
    }

    public static final class TakeWeight implements WeightSensorCommand {
        final Optional<Double> weight;

        public TakeWeight(Optional<Double> weight) {
            this.weight = weight;
        }
    }

    public static Behavior<WeightSensorCommand> create(double maxWeight) {
        return Behaviors.setup(context -> new WeightSensor(context, maxWeight));
    }

    final double maxWeight; //in gram
    double currentWeight;

    private WeightSensor(ActorContext<WeightSensorCommand> context, double maxWeight) {
        super(context);
        this.maxWeight = maxWeight;
        this.currentWeight = 0;
    }

    @Override
    public Receive<WeightSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(PutWeight.class, this::onPutWeight)
                .onMessage(TakeWeight.class, this::onTakeWeight)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WeightSensorCommand> onPutWeight(PutWeight pw) {
        Product product = pw.product.get();
        double weight = product.getWeight();
        if(currentWeight + weight <= maxWeight && weight > 0) {
            currentWeight += weight;
            pw.replyTo.tell(new Fridge.ResponseWeightSensor(Optional.of(Boolean.TRUE), Optional.of(product)));
        } else {
            pw.replyTo.tell(new Fridge.ResponseWeightSensor(Optional.of(Boolean.FALSE), Optional.of(product)));
        }
        return Behaviors.same();
    }

    private Behavior<WeightSensorCommand> onTakeWeight(TakeWeight tw) {
        double weight = tw.weight.get();
        if (weight > 0) {
            getContext().getLog().info("Removing " + weight + "g from the fridge");
            this.currentWeight -= weight;
        }
        return Behaviors.same();
    }


    private WeightSensor onPostStop() {
        getContext().getLog().info("WeightSensor actor {}-{} stopped");
        return this;
    }

}
