package at.fhv.sysarch.lab2.homeautomation.devices;

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

    public static final class GracefulShutdown implements OrderCommand {
        public GracefulShutdown() {}
    }

    public static Behavior<OrderCommand> create(Product p, ActorRef<Fridge.FridgeCommand> fridge) {
        Product product = new Product(p.getName(), p.getWeight(), p.getPrice());

        fridge.tell(new Fridge.OrderCreated(
                Optional.of(product),
                Optional.of(LocalDateTime.now()),
                Optional.of(String.valueOf(nextOrderId++)),
                Optional.ofNullable(p.getPrice())
                ));
        return Behaviors.setup(context -> new Order(context));
    }

    private static int nextOrderId = 10000;

    private Order(ActorContext<OrderCommand> context) {
        super(context);
        getContext().getLog().info("Order created");
    }


    private Behavior<OrderCommand> onGracefulShutdown() {
        getContext().getSystem().log().info("Initiating graceful shutdown...");
        return Behaviors.stopped();
    }

    @Override
    public Receive<OrderCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(GracefulShutdown.class, message -> onGracefulShutdown())
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Order onPostStop() {
        getContext().getLog().info("Order actor {}-{} stopped");
        return this;
    }
}

