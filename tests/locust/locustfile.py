from locust import HttpUser, task, between

class CircleGuardUser(HttpUser):
    wait_time = between(1, 3)

    def on_start(self):
        # Configurar auth si es necesario
        self.headers = {"Content-Type": "application/json"}
        # Ejemplo: self.token = self.client.post("/api/v1/auth/login", json={"user":"test","pass":"test"}).json()["token"]

    @task(3)
    def test_identity_resolve(self):
        # Simula una resolucion de identidad
        self.client.get("/api/v1/identity/resolve/00000000-0000-0000-0000-000000000001", headers=self.headers)

    @task(1)
    def test_dashboard_stats(self):
        # Simula visualizacion del dashboard
        self.client.get("/api/v1/dashboard/campus-summary", headers=self.headers)
        
    @task(2)
    def test_auth_visitor_handoff(self):
        self.client.post("/api/v1/auth/visitor/handoff", json={"anonymousId": "00000000-0000-0000-0000-000000000001"}, headers=self.headers)
