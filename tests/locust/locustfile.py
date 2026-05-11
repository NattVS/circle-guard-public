import os
import random
from locust import HttpUser, task, between

class CampusLoadTestSimulator(HttpUser):
    wait_time = between(2, 5)

    def on_start(self):
        """
        Ejecución por cada usuario simulado al iniciar.
        Mapeo de URLs base para cada uno de los 6 microservicios.
        """
        self.common_headers = {"Content-Type": "application/json"}
        self.services = {
            "identity": os.environ.get("URL_IDENTITY", "http://localhost:8083"),
            "auth": os.environ.get("URL_AUTH", "http://localhost:8180"),
            "dashboard": os.environ.get("URL_DASHBOARD", "http://localhost:8084"),
            "file": os.environ.get("URL_FILE", "http://localhost:8085"),
            "form": os.environ.get("URL_FORM", "http://localhost:8086"),
            "gateway": os.environ.get("URL_GATEWAY", "http://localhost:8087")
        }

    @task(4)
    def simulate_visitor_registration(self):
        target = f"{self.services['identity']}/api/v1/identities/visitor"
        payload = {
            "name": f"Test User {random.randint(1, 1000)}",
            "email": "loadtest@university.edu",
            "reason_for_visit": "Load Testing"
        }
        
        with self.client.post(target, json=payload, headers=self.common_headers, catch_response=True, name="1. Identity: Reg Visitante") as response:
            if response.status_code in (200, 201, 409):
                response.success()

    @task(3)
    def simulate_auth_flow(self):
        target = f"{self.services['auth']}/api/v1/auth/login"
        payload = {"username": "admin", "password": "password"}
        
        with self.client.post(target, json=payload, headers=self.common_headers, catch_response=True, name="2. Auth: Login Attempt") as response:
            if response.status_code in (200, 401, 403):
                response.success()


    @task(2)
    def simulate_dashboard_query(self):
        target = f"{self.services['dashboard']}/api/v1/dashboard/campus-summary"
        
        with self.client.get(target, headers=self.common_headers, catch_response=True, name="3. Dashboard: Fetch Summary") as response:
            if response.status_code in (200, 404):
                response.success()

    @task(3)
    def simulate_form_submission(self):
        form_service = self.services.get(
            "form",
            "http://host.docker.internal:8086"
        )

        body = {
            "anonymousId": "00000000-0000-0000-0000-000000000001",
            "responses": {
                "q1": "YES"
            }
        }

        with self.client.post(
            url=f"{form_service}/api/v1/surveys",
            json=body,
            headers=self.common_headers,
            catch_response=True,
            name="4. Form: Health Survey"
        ) as response:

            if response.status_code in (200, 201, 202):
                response.success()
            else:
                response.failure(
                    f"Survey rejected ({response.status_code})"
                )

    @task(5)
    def simulate_gateway_checkin(self):
        gateway_service = self.services.get(
            "gateway",
            "http://host.docker.internal:8087"
        )

        qr_data = {
            "token": "test-token"
        }

        with self.client.post(
            url=f"{gateway_service}/api/v1/gate/validate",
            json=qr_data,
            headers=self.common_headers,
            catch_response=True,
            name="5. Gateway: QR Scan"
        ) as response:

            if response.status_code in (200, 401, 403):
                response.success()
            else:
                response.failure(
                    f"Gateway validation failed ({response.status_code})"
                )

    @task(1)
    def simulate_file_download(self):
        target = f"{self.services['file']}/api/v1/files/guidelines"
        
        with self.client.get(target, catch_response=True, name="6. File: Get Guidelines") as response:
            if response.status_code in (200, 404):
                response.success()