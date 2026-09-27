# 🍔 QuickBite

A JavaFX desktop food-ordering application built for a university project — inspired by
FoodPanda-style apps, built with traditional Java, JavaFX, and SQLite

---

## ✨ Features

**Customer**
- Register / log in with a real, persistent account (SQLite-backed)
- Browse restaurants and menus, with live search on both
- Real illustrated pictures for every dish and restaurant (or an admin-supplied image link)
- Add items to a cart, apply a time-limited coupon code, and check out
- Automatic delivery tracking — no button-mashing required, it updates itself
- Desktop pop-up notifications the moment an order's status changes
- Full order history in a sortable table
- **Offline mode**: menus stay browsable with no internet, a red banner appears, and placing
  an order is blocked with a clear "cannot connect to server" message
- "Discover a Dish" — pulls a random real recipe (name, photo, instructions) from a free
  public API, just for fun

**Restaurant**
- Separate restaurant login
- Live dashboard of incoming / in-progress / completed orders
- Accept or reject incoming orders

**Admin**
- One-step login (no password gate) into a full management console
- **Restaurants**: add, edit, delete, and whitelist/blacklist any restaurant
- **Menus**: add, edit, and delete food items per restaurant, with support for pasting a
  Google image link as the picture
- **Coupons**: create percentage-off discount codes with a start/end date range
- **Orders**: a live, real-time table of every order across the entire platform

---

## 🖼️ Screenshots

*(Add screenshots here after your first run — drag them into a `docs/screenshots/` folder and
reference them like this:)*

```markdown
![Login screen](docs/screenshots/login.png)
![Restaurant browsing](docs/screenshots/restaurants.png)
![Admin dashboard](docs/screenshots/admin.png)
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| UI | JavaFX 21 + FXML + CSS |
| Build | Maven |
| Database | SQLite (via JDBC) |
| JSON | Jackson |
| External API | [TheMealDB](https://www.themealdb.com) (free, no key) |
| Icons | Ikonli (FontAwesome5) |
| Extra controls | ControlsFX |
| Testing | JUnit 5 |

---

## 🚀 Getting Started

### Prerequisites
- JDK 21
- Maven 3.9+ (matched to the same JDK)

```bash
java -version   # must report 21.x
mvn -version    # "Java version" line must also report 21.x
```

### Demo logins

| Role | How to log in |
|---|---|
| Customer | Create an account from the login screen, or use one you've already registered |
| Restaurant | Click **Restaurant Login**, pick a restaurant, password `restaurant123` |
| Admin | Any non-empty username and password — this is intentional, see below |
| Coupon | Try `WELCOME10` at checkout for 10% off |

> **Note on security:** passwords are stored in plain text and the admin login performs no
> real check. This is a deliberate simplification for a classroom prototype — see
> [Known Limitations](#-known-limitations).

---

## 📂 Project Structure

```
QuickBite/
├── pom.xml
├── README.md
├── GIT_WORKFLOW.md
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── module-info.java
    │   │   └── com/quickbite/quickbite/
    │   │       ├── Launcher.java, QuickBiteApp.java
    │   │       ├── model/        (Restaurant, FoodItem, Order, OrderItem, OrderStatus, User, Coupon)
    │   │       ├── dao/          (RestaurantDAO, FoodDAO, OrderDAO, UserDAO, CouponDAO)
    │   │       ├── database/     (DatabaseManager, DatabaseInitializer)
    │   │       ├── service/      (OrderService, OrderTrackingService, NetworkMonitor)
    │   │       ├── api/          (ApiService, MealDto, MealResponse)
    │   │       ├── controller/   (one controller per screen)
    │   │       └── util/         (Navigator, NotificationHelper, PriceFormatter, FoodIconUtil, FoodImageUtil)
    │   └── resources/com/quickbite/quickbite/
    │       ├── fxml/             (one FXML file per screen)
    │       ├── css/quickbite.css
    │       └── images/           (app icon + illustrated food/restaurant pictures)
    └── test/java/com/quickbite/quickbite/  (JUnit 5 tests)
```

---

## 🏗️ Architecture

QuickBite follows a simple layered / MVC-style architecture — no Spring, no Hibernate, no
dependency injection frameworks, just plain Java:

- **Model** — data classes only, no logic
- **View** — FXML + CSS
- **Controller** — one per screen, handles UI events, delegates to services
- **Service** — business rules (order creation, coupon validation, automatic tracking, offline detection)
- **DAO** — the only classes that touch SQL, always via `PreparedStatement`
- **Database** — connection management, schema creation, and safe migration of older DB files
- **API** — isolated so a network failure can never break the rest of the app

### Concurrency

Three independent background thread pools, all daemon threads, all shut down cleanly in
`QuickBiteApp.stop()`:

1. **`OrderTrackingService`** — `ScheduledExecutorService`, advances an accepted order through
   its delivery stages on a random delay, persists each change, and notifies every screen
   watching that order via `Platform.runLater`.
2. **`ApiService`** — `ExecutorService` behind the "Discover a Dish" feature; calls `HttpClient`,
   parses JSON with Jackson, and never blocks the UI thread.
3. **`NetworkMonitor`** — a single background thread that polls connectivity every few seconds
   and raises the offline banner / blocks ordering the moment the connection drops.

---

## 📋 Known Limitations

Deliberately out of scope for this university project: 
- real payment processing
- real GPS tracking
- real delivery riders
- password hashing
- OAuth/real authentication
- microservices
- cloud hosting, and 
- WebSocket infrastructure.

---

## 🔮 Possible Future Work

- Password hashing (e.g. BCrypt) for the new `users` table
- Real restaurant-owner accounts instead of a shared password
- Order cancellation while still `PLACED`
- Packaging as a native installer with `jpackage`

---
