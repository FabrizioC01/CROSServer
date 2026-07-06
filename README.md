# CROSS

**Author:** Fabrizio Congiu
**Date:** June 2025

---

## 1. First Startup

### 1.1 Requirements

Google's **GSON** library, version 2.13.1, was used to build this project, for serializing and deserializing Client-Server messages and for persisting data to file. The library is included in the zip archive, in the `/lib` directory.

### 1.2 Compilation

Once the zip archive has been extracted and placed in a folder, the project can be compiled from the CROSServer/CROSSclient subfolders with the command:

```bash
javac -cp "lib/gson-2.13.1.jar" -d out -sourcepath src src/*.java
```

Regarding execution:

**On Windows**:
```bash
java -cp "lib/gson-2.13.1.jar;out" main.Main
```

**On Linux/MacOS**:
```bash
java -cp "lib/gson-2.13.1.jar:out" main.Main
```

### 1.3 Properties

Once launched, the Server and Client generate the `server.properties` (server side) and `client.properties` (client side) files. In both programs, the configuration files are initialized with default values.

#### server.properties

In the Server's configuration file, the following can be modified: the **port** (*default 1234*) the Server listens on, the **timeout** for connections with Clients (*0 to disable it*), and the **file names** used for persisting the book, the users, and the order history.

#### client.properties

In the Client's configuration file, the IP and port of the remote Server can be modified; on first startup they are initialized with default values (*127.0.0.1:1234*).

---

## 2. Usage

Interaction with the programs (*Client and Server*) takes place via the command line.

### 2.1 Server

When the Server starts, a thread is launched to manage the command line. Typing `help` will print the list of commands that can be executed on the Server, namely:

- `online` - prints the list of online users
- `book` - prints the order book
- `stops` - prints the stop orders waiting to be activated
- `exit` - shuts down the Server (*similar to ALT+F4*)

The console will also display the logs of the operations performed by the Clients.

### 2.2 Client

All operations are guided through the console. Menu items are selected using numeric values printed on screen.

---

## 3. Implementation Details

### 3.1 Server

At startup, the thread that manages the command line is created, and the `stopThread` thread is registered as a `shutdownHook`, to allow the Server to shut down safely. Multiple connections are handled by a `CachedThreadPool`, which initially starts the notification thread, and subsequently the threads for incoming connections.

#### 3.1.1 Order Management

To manage the order book, separate data structures were used for sell and buy orders, both based on `TreeMap<Integer, LinkedList<Order>>`, which allow ordered insertion based on price; at equal price, orders are inserted into a FIFO list, guaranteeing extraction in insertion order.

For stop-type orders, two maps identical to those of the book are used. Stops are inserted into their respective maps only if the market price is not favorable to, or equal to, the stop price; once inserted, the price check will be performed whenever a new limit-type order is inserted.

The identifiers for stop, bid, and market orders (*id*) are managed through the `AtomicInteger` class, initialized with the value read from the book's persistence file or, alternatively, with the maximum ID present in the history, in order to guarantee the uniqueness of the identifier (*giving priority to the book*). In the absence of these files, the variable is initialized to 0.

At startup, the book and the history are loaded (if available) from the files specified in the `properties`, which will be overwritten with the new data when the Server shuts down.

#### 3.1.2 Authentication

User management is handled by the `AuthManager` class, initialized via the static `init` method, which checks for the presence of the users file specified in the `properties`; if it is not present, it creates it. Each new profile is saved to the file at the time of registration.

Each user, at login time, transmits not only their username and password but also the UDP port for receiving notifications. If the credentials are valid, the user is added to an array of online users, and their IP and UDP port are passed to the class that manages notifications. At logout, a method removes the user from the list of online users and from the notification system.

#### 3.1.3 Notification System

The class that manages the notification system receives (*at login time*), in a `ConcurrentHashMap`, the pair username -- address and UDP socket port of the Client. Threads wishing to send a notification do so via the `notify` method, which inserts the order into a `BlockingQueue`, waiting to be picked up by the notification thread, following a producer (*connected Client threads*) -- consumer (*notification thread*) scheme.

### 3.2 Client

Once started, the Client connects to the address specified in the `properties` file. A UDP socket is also opened, listening on a random port, which will be transmitted in the first login message, together with the username and password. The UDP socket remains listening on a thread that receives, deserializes, and prints the notifications.

Initially, the connection allows registration, login, and password change to be performed. If registration or password change is selected, once the operation is completed, the connection is closed. In the case of a (successfully completed) login, the connection instead remains active.

All input procedures are specified in the `InputProcedures` class, which receives the incoming data and returns the serialized requests. The quantities indicated in the input procedures are expressed in thousandths: for example, to place a *market* order for 1 BTC, 1000 must be entered on the command line.

---

## 4. Synchronization

The components that require synchronization are: the order book, the authentication system, and the notification system.

For the order book and the authentication system, synchronization was handled using `synchronized` methods where necessary, in order to guarantee data consistency during concurrent access.

The notification system, on the other hand, relies on thread-safe data structures, in particular `ConcurrentHashMap` and `BlockingQueue`, which allow concurrent access by multiple threads without the need for explicit synchronization.

---

## 5. List of Active Threads

Below are listed the threads that are activated by the Client and the Server.

### 5.1 Server

- **CachedThreadPool** — Manages Client connections.
- **NotificationService** — Sends notifications to connected Clients.
- **Console** — Handles printing the information requested from the console.
- **stopThread** — Activated only when the Server shuts down; saves the data.

### 5.2 Client

- **Main** — Communicates with the Server.
- **Notifications** — Receives and prints notifications.
