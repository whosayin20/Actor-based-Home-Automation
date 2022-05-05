package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.products.Product;

public class Order extends AbstractBehavior<Order.OrderCommand>  {

    public interface OrderCommand { }

    public static Behavior<OrderCommand> create(Product p) {
        return Behaviors.setup(context -> new Order(context));
    }

    private Order(ActorContext<OrderCommand> context) {
        super(context);
        getContext().getLog().info("Order started");
    }

    @Override
    public Receive<OrderCommand> createReceive() {
        return newReceiveBuilder()
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Order onPostStop() {
        getContext().getLog().info("Order actor {}-{} stopped");
        return this;
    }
}

