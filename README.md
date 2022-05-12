# Actor-based-Home-Automation
Hüseyin Arziman & Yusuf Cetinkaya

## General

In this lab exercise...

![img.png](img.png)

## Components:


## Air Condition
The `Air Condition` is an actor which gets notified as soon as the temperature changes. 
If the `Air Condition` receives a temperature which is below 20°C is turns itself off.
If it receives a temperature which is equal or above 20°C is turns itself on.

## Blind
The `Blind` is an actor which does  not take any reference to other actors at all. It does get notified, if
the weather sensor detects sunny or cloudy weather. Depending on the weather condition it will open or close.
Moreover, it does store an internal state, whether a movie  is currently playing. 
If a movie is currently playing, the `Blind` will not open, even if the weather is cloudy.
This is done by checking the internal state.

## Media Station

The `Media Station` allows us to play movies. It takes a reference of the `Blind` actor in order to inform it about whether a movie is
playing or not. It does this, as soon as a movie is turned on or off.

## Fridge
In addition, it was part of the task to implement a Fridge including its components. 
The `Fridge` is a stand-alone actor that is independent of the smart home. 
It contains a `Weight Sensor` and a `Storage Sensor`, which were also implemented as actors. 
The Fridge contains various products, each of which has a different weight. 
When the `Fridge` receives an order, it spawns an `Order` actor.
In addition, products that are in the Fridge can be consumed through the user interface. 
Furthermore, the command **"fh"** (Fridge History) can be used to display an order history of the individual products. 
In order to illustrate the interaction of the entire actors more clearly, we will use the Fridge as an example, we will simulate a process: <br>
1. After startup, the sensors and the Fridge are created. <br>
2. When the **"fo"** (Fridge Order) command is entered in the CL, it is necessary to choose from a selection of products (cheese, tomato, salad, egg). <br>
3. After a product has been selected, it is sent to the `Fridge` which spawns an `Order` actor. <br>
4. The `Order` actor first sends a message containing the product to the `Weight Sensor`, which checks whether the Fridge has enough weight available considering the product to be ordered.
5. If this is the case, the `Storage Sensor` is receiving a message containing the product and as soon as it also reports that the Fridge has enough space, the order is placed and the 
product is stored in the Fridge. Otherwise the `Storage Sensor` will report, that the `Fridge` has not enough storage.
6. A history entry is created and the `Order` actor is stopped 

### Automatic Product Checking System
An automatic product checking system is active, which controls whether an item that has to be ordered. 
This works in such a way that the `Fridge` sends a message to itself at predefined intervals in which it queries its contents. If the stock of an already 
ordered product is at zero the `Fridge` creates an `Order` actor. <br>
When a product is consumed, it is only checked whether this item is available. if so, the item is removed from the `Fridge`.
