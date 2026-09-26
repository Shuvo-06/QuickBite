# QuickBite

A JavaFX desktop food-ordering application built for a university project. QuickBite lets a
customer browse restaurants, order food, and track delivery in real time; a restaurant can log in
separately to accept or reject incoming orders.

## Technology checklist

| Requirement | Where it's used |
|---|---|
| Java 21 / JDK 21 | `pom.xml` (`<release>21</release>`), all source files |
| JavaFX 21 | `QuickBiteApp`, every controller |
| Maven | `pom.xml`, `javafx-maven-plugin` |
| SQLite | `database/DatabaseManager`, `database/DatabaseInitializer` |
| JDBC | `dao/RestaurantDAO`, `dao/OrderDAO` (all `PreparedStatement`) |
| JSON | `api/MealDto`, `api/MealResponse` parsed with Jackson |
| External/public API | `api/ApiService` calls TheMealDB (`themealdb.com`), no key needed |
| Concurrency | `service/OrderTrackingService` (`ScheduledExecutorService`), `api/ApiService` (`ExecutorService`) |
| Multithreading | Background threads named `order-tracker-N` and `api-worker`, kept off the JavaFX thread |
| Git/GitHub | `.gitignore`; see suggested commit history below |
| FXML | Every screen under `src/main/resources/.../fxml/` |
| JavaFX CSS | `src/main/resources/.../css/quickbite.css` |
| ControlsFX | `util/NotificationHelper` (pop-up notifications) |
| Ikonli | `FontIcon` used throughout the UI |

## Project structure

```
QuickBite/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── module-info.java
    │   │   └── com/quickbite/quickbite/
    │   │       ├── Launcher.java
    │   │       ├── QuickBiteApp.java
    │   │       ├── model/            (Restaurant, FoodItem, Order, OrderItem, OrderStatus)
    │   │       ├── dao/              (RestaurantDAO, OrderDAO)
    │   │       ├── database/         (DatabaseManager, DatabaseInitializer)
    │   │       ├── service/          (OrderService, OrderTrackingService)
    │   │       ├── api/              (ApiService, MealDto, MealResponse)
    │   │       ├── controller/       (one controller per screen)
    │   │       └── util/             (Navigator, NotificationHelper, PriceFormatter, FoodIconUtil)
    │   └── resources/com/quickbite/quickbite/
    │       ├── fxml/                 (one FXML file per screen)
    │       └── css/quickbite.css
    └── test/java/com/quickbite/quickbite/
        ├── model/                    (OrderStatusTest, OrderItemTest)
        └── util/                     (PriceFormatterTest, FoodIconUtilTest)
```

## Demo logins

| Role | How to log in |
|---|---|
| Customer | Any non-empty username and password |
| Restaurant | Click **Restaurant Login**, pick a restaurant, password `restaurant123` |
| Admin | Username `admin`, password `admin123` |

## Application flow

```
Login
 ├─ Customer Login ──► Restaurants & Menu ──► Place Order ──► Delivery Status
 │                          │                                     │
 │                          └───────────────► Order History ◄─────┘
 │
 ├─ Restaurant Login ──► Restaurant Dashboard (Accept / Reject / track orders)
 │
 └─ Admin Login ──► Admin Dashboard (placeholder)
```

Placing an order saves it with status `PLACED` and does **not** start moving automatically — it
waits in the restaurant's **Incoming Orders** column until accepted or rejected. Once accepted, a
background `ScheduledExecutorService` moves it through `CONFIRMED → PREPARING → READY →
OUT_FOR_DELIVERY → DELIVERED` on its own, with a random (3–8) second delay between steps. Every
change is written to SQLite and broadcast to whichever screens are listening (the customer's
delivery window, a ControlsFX pop-up, and the restaurant dashboard) via `Platform.runLater`.

### Concurrency

Two independent background thread pools exist, both created with daemon threads and both properly
shut down in `QuickBiteApp.stop()`:

1. **`OrderTrackingService`** — a 2-thread `ScheduledExecutorService` that advances an accepted
   order's status after a random delay, persists the change via `OrderService`/`OrderDAO`, then
   uses `Platform.runLater()` to notify every registered listener (the customer's delivery screen,
   the pop-up notifier, the restaurant dashboard) back on the JavaFX Application Thread.
2. **`ApiService`** — a 2-thread `ExecutorService` used only by the optional "Discover a Dish"
   button. It calls `HttpClient`, parses the JSON with Jackson into `MealDto`, and hands the result
   (or a friendly error message) back to the UI thread the same way.

Neither pool ever blocks the JavaFX Application Thread, and neither depends on the other.

### Database schema

```
restaurants(id, name, description, rating)
foods(id, restaurant_id -> restaurants, name, description, price)
orders(id, customer_name, restaurant_id -> restaurants, restaurant_name, total, status, created_at)
order_items(id, order_id -> orders, food_name, quantity, unit_price)
```

There is deliberately no `users` table yet — login is a Phase-1-style placeholder (any non-empty
customer credentials; fixed passwords for restaurant/admin roles), as scoped in the original
project brief. Adding real registration would be the natural next step, replacing `customer_name`
with a proper `user_id` foreign key.

### External API

`ApiService` calls **TheMealDB** (`https://www.themealdb.com/api/json/v1/1/random.php`), a free
public API that needs no key, returning a random real dish with its name, category, region and
photo. This satisfies the "external/public API" and "JSON" requirements without depending on the
core app: browsing, ordering and tracking all work identically whether or not the API or the
network is reachable. If the call fails (timeout, no connection, unexpected response), the failure
is caught on the background thread and reported to the user as a plain message — nothing crashes.

## Known limitations (explicitly out of scope)

Matching the original project brief: no real payment gateway, no real GPS tracking, no real
delivery riders, no coupons/recommendation engine, no microservices/cloud backend, no Spring
Boot, no real authentication (passwords are not hashed), no WebSocket infrastructure.


## Possible future work

- A real `users` table with registration, so customers and restaurant owners have proper accounts
  instead of name-matching and shared passwords.
- Admin dashboard functionality (the tiles are currently placeholders).
- Order cancellation by the customer while still `PLACED`.
- Packaging as a native installer with `jpackage`.
