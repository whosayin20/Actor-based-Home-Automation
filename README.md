# Actor-based-Home-Automation

In addition, it was part of the task to implement a fridge including its components. The fridge is a stand-alone actor that is independent of the smarthome. 
It contains a weight sensor and a storage sensor, which were also implemented as actors. The fridge contains various products, each of which has a different weight. 
When the fridge receives an order, it spawns an Order Actor. In addition, products that are in the fridge can be consumed through the user interface. 
Furthermore, the command "fh" can be used to display an order history of the individual products. 
In order to illustrate the interaction of the entire actors more clearly using the fridge as an example, we will simulate a process: 
As soon as the programme is started, the sensors and the fridge are created. As soon as an order command is executed via the user interface, it is possible to choose from a selection of products (cheese, tomato, salad, egg). After a product has been selected, product to be ordered has been entered, the Actor Fridge sends the Actor Product, which is created when an order is executed, a message with the product to be ordered. The Actor product now first sends a message to the Actor weight sensor, which checks whether the fridge still has enough space in terms of weight for the item to be ordered. If this is the case, the storage sensor is informed and as soon as it also reports that the fridge has enough space, the order is placed and the product is stored in the fridge. At the same time as the order is placed, the capacity of the refrigerator is updated, an order history is created or updated, the Order Actor is reset and an automatic checking system is initialised, which checks whether the items already ordered are still available in the refrigerator. This works in such a way that the fridge sends a message to itself at predefined intervals in which it queries its contents. If the stock of an already ordered product is at 0 and the refrigerator has enough capacity, the required product is then ordered. When a product is consumed, it is only checked whether this item is available, if so, the item is removed from the refrigerator and the storage and weight values are updated.