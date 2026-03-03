from locust import HttpUser, task, between, LoadTestShape
from faker import Faker
import random

fake = Faker()


class StepLoadShape(LoadTestShape):
    """
    Gradually steps up load, holds each level for 60s, then steps up again.
    Lets you find the exact point where the system starts to degrade.
    """
    steps = [
        (50,    5,   90),   # warmup — longer to let JVM + Redis warm up
        # (100,   10,  60),
       # (200,   20,  60),
        # (500,   50,  60),
        (1000,  100, 60),
        (2000,  100, 60),
        (3000,  100, 60),
        # (5000,  100, 60),
        # (10000,  100, 30),
    ]

    def tick(self):
        run_time = self.get_run_time()
        elapsed = 0
        for users, spawn_rate, duration in self.steps:
            if run_time < elapsed + duration:
                return (users, spawn_rate)
            elapsed += duration
        return None


class ShoppingAPIUser(HttpUser):
    wait_time = between(0.3, 1.0)

    def on_start(self):
        self.my_products = []
        self.my_orders = []
        self.create_product_and_store()
        self.place_order_and_store()

    # ---------------------------
    # Utility Methods
    # ---------------------------

    def create_product_and_store(self):
        product = {
            "name": f"{fake.word().capitalize()} {fake.word().capitalize()}",
            "price": round(random.uniform(5.00, 999.99), 2)
        }
        with self.client.post(
            "/api/products",
            json=product,
            catch_response=True,
            name="/api/products [POST]"
        ) as response:
            if response.status_code == 201:
                data = response.json()
                self.my_products.append({
                    "id": data["id"],
                    "name": product["name"]
                })
                response.success()
            elif response.status_code >= 500:
                response.failure(f"Server error: {response.status_code}")
            else:
                response.success()

    def place_order_and_store(self):
        if len(self.my_products) < 1:
            return
        selected = random.sample(self.my_products, k=min(3, len(self.my_products)))
        items = [
            {"productId": p["id"], "quantity": random.randint(1, 5)}
            for p in selected
        ]
        with self.client.post(
            "/api/orders",
            json={"items": items},
            catch_response=True,
            name="/api/orders [POST]"
        ) as response:
            if response.status_code == 201:
                data = response.json()
                self.my_orders.append({
                    "id": data["id"],
                    "status": data["status"]
                })
                response.success()
            elif response.status_code >= 500:
                response.failure(f"Server error: {response.status_code}")
            else:
                response.success()

    def get_random_product(self):
        if not self.my_products:
            return None
        return random.choice(self.my_products)

    def get_random_order(self):
        if not self.my_orders:
            return None
        return random.choice(self.my_orders)

    # ---------------------------
    # TASKS
    # ---------------------------

    @task(8)
    def get_all_orders(self):
        page = random.randint(0, 3)
        with self.client.get(
            "/api/orders",
            params={"page": page, "size": 10},
            catch_response=True,
            name="/api/orders [GET]"
        ) as response:
            if response.status_code >= 500:
                response.failure(f"Server error: {response.status_code}")
            else:
                response.success()

    @task(7)
    def get_product_by_id(self):
        product = self.get_random_product()
        if not product:
            return
        with self.client.get(
            f"/api/products/{product['id']}",
            catch_response=True,
            name="/api/products/[id] [GET]"
        ) as response:
            if response.status_code >= 500:
                response.failure(f"Server error: {response.status_code}")
            else:
                response.success()

    @task(6)
    def get_order_by_id(self):
        order = self.get_random_order()
        if not order:
            return
        with self.client.get(
            f"/api/orders/{order['id']}",
            catch_response=True,
            name="/api/orders/[id] [GET]"
        ) as response:
            if response.status_code >= 500:
                response.failure(f"Server error: {response.status_code}")
            else:
                response.success()

    @task(4)
    def place_order(self):
        self.place_order_and_store()

    @task(2)
    def product_crud(self):
        action = random.choices(
            ["create", "update", "delete", "search"],
            weights=[4, 3, 1, 2]
        )[0]

        if action == "create":
            self.create_product_and_store()

        elif action == "update":
            product = self.get_random_product()
            if not product:
                return
            with self.client.put(
                f"/api/products/{product['id']}",
                json={
                    "name": f"{fake.word().capitalize()} {fake.word().capitalize()}",
                    "price": round(random.uniform(5.00, 999.99), 2)
                },
                catch_response=True,
                name="/api/products/[id] [PUT]"
            ) as response:
                if response.status_code in (200, 202):  # accepts queued response
                    response.success()
                elif response.status_code >= 500:
                    response.failure(f"Server error: {response.status_code}")
                else:
                    response.success()

        elif action == "delete":
            if len(self.my_products) <= 3:
                self.create_product_and_store()  # replenish instead of deleting
                return
            product = random.choice(self.my_products)
            self.my_products.remove(product)
            with self.client.delete(
                f"/api/products/{product['id']}",
                catch_response=True,
                name="/api/products/[id] [DELETE]"
            ) as response:
                if response.status_code in (200, 202, 204):  # accepts queued response
                    response.success()
                elif response.status_code >= 500:
                    response.failure(f"Server error: {response.status_code}")
                else:
                    response.success()

        elif action == "search":
            product = self.get_random_product()
            if not product:
                return
            name = product["name"].split()[0]
            with self.client.get(
                "/api/products",
                params={"name": name, "page": 0, "size": 5},
                catch_response=True,
                name="/api/products?name=[term] [GET]"
            ) as response:
                if response.status_code >= 500:
                    response.failure(f"Server error: {response.status_code}")
                else:
                    response.success()

    @task(1)
    def cancel_order(self):
        pending = [o for o in self.my_orders if o["status"] == "PENDING"]
        if not pending:
            return
        order = random.choice(pending)
        with self.client.patch(
            f"/api/orders/{order['id']}/cancel",
            catch_response=True,
            name="/api/orders/[id]/cancel [PATCH]"
        ) as response:
            if response.status_code in (200, 202):  # accepts queued response
                order["status"] = "CANCELLED"
                response.success()
            elif response.status_code == 409:
                order["status"] = "CANCELLED"  # sync local state
                response.success()
            elif response.status_code >= 500:
                response.failure(f"Server error: {response.status_code}")
            else:
                response.success()
