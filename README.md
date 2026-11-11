# Coding Factory Automation — Backend

Απλό εκπαιδευτικό REST API για μαθήματα testing. Java 21, Spring Boot 3.5.16,
Maven, Spring Security/JWT, Spring Data JPA και MySQL 8.4.
Βάση: **codingFactoryAutomation**. Δεν απαιτείται Lombok.

## Γρήγορη εκκίνηση με Docker

Χρειάζεσαι Docker Desktop (ή Docker Engine με Compose), ενεργό και με πρόσβαση
στο Internet για το πρώτο build. Δεν χρειάζεσαι εγκατάσταση Java/Maven/MySQL.

1. Κάνε extract το ZIP και άνοιξε terminal στον φάκελο με το `compose.yaml`.
2. Αντέγραψε το `.env.example` σε `.env`:
   - macOS/Linux: `cp .env.example .env`
   - Windows PowerShell: `Copy-Item .env.example .env`
3. Τρέξε `docker compose up --build -d`.
4. Δες logs: `docker compose logs -f backend`.
5. Άνοιξε `http://localhost:8080/api/health` → `{"status":"UP"}`.

Το Docker build εκτελεί τα integration tests πριν δημιουργήσει το image.
Η MySQL δημιουργεί αυτόματα τη βάση και τον χρήστη. Το Hibernate δημιουργεί
τους πίνακες. Τα δεδομένα μένουν σε Docker volume μετά το restart.
Το health endpoint δείχνει ότι απαντά η εφαρμογή, δεν είναι database readiness check.

Σταμάτημα: `docker compose down`.
**Πλήρες reset των εκπαιδευτικών δεδομένων (διαγράφει όλη τη βάση του project):**
`docker compose down -v`, μετά `docker compose up --build -d`.
Οι τοπικές θύρες 3306 και 8080 πρέπει να είναι ελεύθερες.

## Εκκίνηση από IDE / Maven

Εγκατάστησε JDK 21 και Maven 3.9+. Άνοιξε το `pom.xml` στο IntelliJ/Eclipse.
Ξεκίνα μόνο τη βάση: `docker compose up -d db` (αφού έχεις φτιάξει `.env`).
Στο Run Configuration όρισε:

```text
JWT_SECRET=classroom-only-replace-this-with-a-random-secret-32-bytes-minimum
DB_URL=jdbc:mysql://localhost:3306/codingFactoryAutomation?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
```

Τρέξε την `AutomationApplication`, ή στο terminal:

```bash
# macOS/Linux — οι env vars αφορούν το τρέχον terminal
export JWT_SECRET='classroom-only-replace-this-with-a-random-secret-32-bytes-minimum'
export DB_URL='jdbc:mysql://localhost:3306/codingFactoryAutomation?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false'
mvn spring-boot:run
```

```powershell
# Windows PowerShell
$env:JWT_SECRET='classroom-only-replace-this-with-a-random-secret-32-bytes-minimum'
$env:DB_URL='jdbc:mysql://localhost:3306/codingFactoryAutomation?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false'
mvn spring-boot:run
```

Το `.env` διαβάζεται από το Docker Compose, **όχι αυτόματα από το Spring/Maven**.
Για υπάρχουσα MySQL, δημιούργησε τη βάση `codingFactoryAutomation` και όρισε
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` για χρήστη με δικαιώματα στη συγκεκριμένη βάση.
Οι προεπιλογές είναι `cf_user` / `cf_local_password`.

## API contract

| Method | Endpoint | Token | Επιτυχία |
|---|---|---|---|
| GET | `/api/health` | Όχι | 200 |
| POST | `/api/auth/register` | Όχι | 201, token + user |
| POST | `/api/auth/login` | Όχι | 200, token + user |
| GET | `/api/users/me` | Ναι | 200, user |
| PUT | `/api/users/me` | Ναι | 200, ενημερωμένος user |

Registration body:

```json
{
  "firstName": "Maria",
  "lastName": "Papadopoulou",
  "email": "maria@example.com",
  "phone": "6912345678",
  "fatherName": "Nikos",
  "motherName": "Eleni",
  "age": 22,
  "password": "Student123!"
}
```

Login body: `{"email":"maria@example.com","password":"Student123!"}`.
Registration/login response:

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "firstName": "Maria",
    "lastName": "Papadopoulou",
    "email": "maria@example.com",
    "phone": "6912345678",
    "fatherName": "Nikos",
    "motherName": "Eleni",
    "age": 22
  }
}
```

Στα protected requests στέλνεις `Authorization: Bearer <accessToken>`.
Το PUT παίρνει όλα τα πεδία του registration **εκτός password**.
Είναι αντικατάσταση προφίλ: απαιτούνται όλα τα υποχρεωτικά πεδία και η παράλειψη
fatherName/motherName τα καθαρίζει. Δεν υπάρχει update password σε αυτή την έκδοση.
GET/PUT επιστρέφουν απευθείας το αντικείμενο user.
Δεν επιτρέπεται να ορίσεις `id` στο body ή να αλλάξεις άλλον χρήστη.
Με αλλαγή email, το επόμενο login γίνεται με το νέο email· το υπάρχον token
συνεχίζει να ισχύει επειδή συνδέεται με το σταθερό user ID.

| Πεδίο | Κανόνας |
|---|---|
| firstName, lastName | Υποχρεωτικά, όχι κενά, έως 100 χαρακτήρες |
| email | Έγκυρο, έως 254 χαρακτήρες, μοναδικό· αποθηκεύεται lowercase |
| phone | 10–15 ψηφία, προαιρετικό αρχικό +, χωρίς κενά/παύλες |
| fatherName, motherName | Προαιρετικά, έως 100 χαρακτήρες |
| age | Ακέραιος 1–120, υποχρεωτικός |
| password | 8–72 εκτυπώσιμοι ASCII χαρακτήρες, όχι μόνο κενά |

Άγνωστα JSON πεδία και λάθος τύποι απορρίπτονται. Το email δεν πρέπει να έχει
εξωτερικά κενά. Οι κωδικοί αποθηκεύονται ως BCrypt hashes και δεν επιστρέφονται.
Η ηλικία αποθηκεύεται ως αριθμός για απλότητα και δεν αυξάνεται αυτόματα.

## Errors

- 400: validation, malformed JSON ή άγνωστο πεδίο.
- 401: λάθος login, απουσία/αλλοίωση/λήξη token.
- 403: μη επιτρεπόμενη πρόσβαση.
- 404: ο χρήστης του token δεν υπάρχει πλέον.
- 409: email που υπάρχει ήδη (registration ή update).

Παράδειγμα:

```json
{
  "timestamp": "2026-09-30T17:00:00Z",
  "status": 400,
  "message": "Validation failed",
  "errors": {"age": "must be greater than or equal to 1"}
}
```

Τα βασικά API/security errors έχουν αυτή τη μορφή. Δεν βασίζουμε assertions
στο ακριβές timestamp ή στη σειρά των validation errors.

## Requests και αρχικά tests

Το `requests.http` έχει έτοιμα requests για IntelliJ HTTP Client και αποθηκεύει
αυτόματα το token από registration/login. Εναλλακτικά αντέγραψε τα bodies στο Postman.

```bash
mvn test
mvn clean verify
```

Τα 7 integration tests χρησιμοποιούν Spring Boot + MockMvc + H2 στο test profile:
registration/login/read/update, hashing, duplicate email, validation, λάθος password,
missing/invalid tokens, conflicts/ID injection και malformed JSON.
Δεν χρειάζονται εξωτερική βάση. Το H2 profile υπάρχει μόνο στο test classpath.
Δεν αποτελούν ακόμη ανεξάρτητη backend system-test suite ούτε έλεγχο πραγματικής MySQL.

**Κατάσταση επαλήθευσης παράδοσης:** έγινε στατικός έλεγχος αρχείων και δομής.
Το build και τα tests δεν εκτελέστηκαν στο περιβάλλον δημιουργίας, επειδή δεν
υπάρχουν λειτουργικά Java/Maven/Docker. Τρέξε τις παραπάνω εντολές ή το Docker build
για την πρώτη επιβεβαίωση στο δικό σου περιβάλλον.

## Δομή για το μάθημα

```text
src/main/java/gr/aueb/cf/automation/
  auth/        Registration και login DTOs/controller
  user/        Entity, repository, service, profile controller/DTOs
  security/    Spring Security, JWT encoder/decoder/token service, CORS
  error/       Κοινό error response και exception handling
src/test/      Αρχικά integration tests με H2
```

Ροή: Controller → Service → Repository → Database.
Για το μελλοντικό frontend επιτρέπονται origins localhost:3000 και localhost:5173.
Αλλάζουν με `CORS_ORIGINS` (comma-separated URLs).

Το scope αυτής της παράδοσης είναι το backend. Επόμενη φάση: frontend και ξεχωριστές
Playwright / backend system-test suites απέναντι σε εφαρμογή που τρέχει με MySQL.
Ιδέες ασκήσεων: age boundaries 0/1/120/121, email collision, ληγμένο JWT,
refresh σελίδας μετά το login, επιβεβαίωση persistence μετά από restart.

Το setup προορίζεται για τοπικό μάθημα: demo credentials, Hibernate ddl-auto=update,
χωρίς refresh tokens, password reset, email verification, roles ή rate limiting.
Το logout του μελλοντικού frontend θα διαγράφει το token τοπικά· δεν υπάρχει server
revocation και το token ισχύει έως τη λήξη του (με μικρό clock skew του validator).
Πριν από δημόσια χρήση χρειάζονται διαφορετικά secrets, HTTPS και production ρυθμίσεις.

Τεκμηρίωση συμβατότητας Spring Boot:
https://docs.spring.io/spring-boot/3.5/system-requirements.html
