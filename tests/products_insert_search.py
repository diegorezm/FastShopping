from locust import HttpUser, task, between
from faker import Faker
import random
import threading

fake = Faker()

created_products = []
created_products_lock = threading.Lock()


class ShoppingAPIUser(HttpUser):
    wait_time = between(0.3, 1.0)  

    def on_start(self):
        # Each user seeds a few products
        for _ in range(3):
            self.create_product_and_store()

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
                with created_products_lock:
                    created_products.append({
                        "id": data["id"],
                        "name": product["name"]
                    })
                response.success()
            elif response.status_code >= 500:
                response.failure(f"Server error: {response.status_code}")
            else:
                # 4xx during load test is not catastrophic
                response.success()

    def get_random_product(self):
        with created_products_lock:
            if not created_products:
                return None
            return random.choice(created_products)

    # ---------------------------
    # TASKS
    # ---------------------------

    @task(6)
    def get_product_by_id(self):
        product = self.get_random_product()
        if not product:
            return

        with self.client.get(
            f"/api/products/{product['id']}",
            catch_response=True,
            name="/api/products/[id] [GET]"
        ) as response:

            # 404 is acceptable under concurrency
            if response.status_code >= 500:
                response.failure(f"Server error: {response.status_code}")
            else:
                response.success()

    @task(1)
    def search_created_product(self):
        product = self.get_random_product()
        if not product:
            return

        with self.client.get(
            "/api/products",
            params={"name": product["name"], "page": 0, "size": 5},
            catch_response=True,
            name="/api/products?name=[term] [GET]"
        ) as response:

            if response.status_code >= 500:
                response.failure(f"Server error: {response.status_code}")
            else:
                response.success()

    @task(1)
    def create_new_product(self):
        self.create_product_and_store()

    @task(1)
    def update_random_product(self):
        product = self.get_random_product()
        if not product:
            return

        updated_data = {
            "name": f"{fake.word().capitalize()} {fake.word().capitalize()}",
            "price": round(random.uniform(5.00, 999.99), 2)
        }

        with self.client.put(
            f"/api/products/{product['id']}",
            json=updated_data,
            catch_response=True,
            name="/api/products/[id] [PUT]"
        ) as response:

            if response.status_code == 200:
                with created_products_lock:
                    product["name"] = updated_data["name"]
                response.success()
            elif response.status_code >= 500:
                response.failure(f"Server error: {response.status_code}")
            else:
                response.success()

    @task(1)
    def delete_random_product(self):
        product = self.get_random_product()
        if not product:
            return

        with self.client.delete(
            f"/api/products/{product['id']}",
            catch_response=True,
            name="/api/products/[id] [DELETE]"
        ) as response:

            if response.status_code == 204:
                with created_products_lock:
                    created_products[:] = [
                        p for p in created_products
                        if p["id"] != product["id"]
                    ]
                response.success()
            elif response.status_code >= 500:
                response.failure(f"Server error: {response.status_code}")
            else:
                response.success()
