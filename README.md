# Room Reservation REST API

Tämä projekti on tehtävän toteutus, jossa tuli toteuttaa huoneiden varausrajapinta.

Toteutuksena valmistui Spring Boot REST API, joka käyttää PostgreSQL-tietokantaa.

## Käyttämäni tekoälytyökalut

### JetBrains Junie:

- Mallina käytin Gemini 3 Flash
    - Käytin Gemini 3 Flash mallia, koska JetBrains ilmoittaa sen suoriutuvan hyvin heidän omissa benchmarkeissaan ja
      koska se on kustannustehokas.
- MCP serveriksi lisätty Context7

### JetBrains AI Assistant:

- Code completion
- Code suggestions
- Suggested refactoring
- Refactoring

## Oletukseni

### Tehtävänannon vaatimus: *"Varausten katselu: Listaa kaikki tietyn huoneen varaukset."*

Lähdin toteuttamaan sovellusta siltä pohjalta, että sen tulisi olla helposti ylläpidettävä ja jatkokehitettävissä.
Päädyin ratkaisuun, jossa tietyn huoneen kaikki varaukset haetaan endpointista `/api/reservations?roomId={roomId}`.
Tästä sivutuotteena sovellukseen tuli myös endpoint `/api/reservations`, joka palauttaa kaikki varaukset. Koska en
halunnut, että palautettava lista voisi olla rajattoman pitkä, toteutin myös paginoinnin ja siinä samalla sorttauksen.

### Virheitilanteiden ilmoitukset

Tehtävänannossa ei oltu määritelty, millaiseen käyttöön API tulisi. Koska sovellusta tullaan käyttämään demopohjaisena
ja sen toiminnallisuutta halutaan lähinnä tarkastella, toteutin virhetilanteiden ilmoitukset informatiivisiksi, kuitenkin tietoturva huomioiden.

## REST API Dokumentaatio

Sovellus tarjoaa rajapinnan huonevarausten hallintaan. Rajapinta noudattaa REST-periaatteita ja käyttää JSON-muotoista
dataa. Virhetilanteissa käytetään Problem Details (RFC 9457) -muotoa. Sovelluksen käynnistyksen yhteydessä tietokantaan
lisätään demo-data.

### Varausten haku

`GET /api/reservations`

Hakee listan varauksista. Tukee suodatusta huoneen perusteella, paginointia ja lajittelua.

**Query Parameters:**

- `roomId` (valinnainen, Integer): Suodata varaukset huoneen tunnuksen perusteella.
- `page` (valinnainen, oletus 0): Sivun numero.
- `size` (valinnainen, oletus 20, max 100): Sivun koko.
- `sortBy` (valinnainen, oletus `startTime`): Lajittelukenttä (`id`, `startTime`, `endTime`, `roomId`).
- `sortOrder` (valinnainen, oletus `asc`): Lajittelujärjestys (`asc`, `desc`).

**Response:**
`200 OK` - Palauttaa paginoidun listan varauksista.

```json
{
  "content": [
    {
      "id": 101,
      "roomId": 2,
      "startTime": "2026-02-01T10:00:00Z",
      "endTime": "2026-02-01T11:00:00Z"
    }
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### Varauksen luominen

`POST /api/reservations`

Luo uuden huonevarauksen.

**Request (Body):**

```json
{
  "roomId": 2,
  "startTime": "2026-02-01T12:00:00Z",
  "endTime": "2026-02-01T13:00:00Z"
}
```

**Response:**

- `201 Created`: Varauksen luonti onnistui. Palauttaa luodun varauksen tiedot.
- `400 Bad Request`: Virheellinen syöte (esim. menneisyydessä oleva aika tai lopetusaika ennen aloitusaikaa).
- `404 Not Found`: Huonetta ei löydy.
- `409 Conflict`: Huone on jo varattu valitulle ajanjaksolle.

### Varauksen poistaminen

`DELETE /api/reservations/{reservationId}`

Poistaa olemassa olevan varauksen.

**Path Parameters:**

- `reservationId` (Long): Poistettavan varauksen tunnus.

**Response:**

- `204 No Content`: Poisto onnistui.
- `404 Not Found`: Varausta ei löytynyt annetulla tunnuksella.

## Käyttöohjeet (How to run)

### Esivaatimukset

- Java 21 (Gradle Wrapperin ja sovelluksen ajamiseen).
- Docker Desktop (tai muu Docker engine) asennettuna ja käynnissä.

### Ajaminen paikallisesti (Java)

Käynnistä sovellus ja tietokanta komennolla:

```bash
./gradlew bootRun
```

Spring Bootin Docker Compose käynnistää automaattisesti tietokannan Docker-kontissa. Sovellus käynnistyy oletuksena
osoitteeseen `http://localhost:8080`.

### Ajaminen Dockerilla (koneella ei tarvitse olla Javaa asennettuna)

Käynnistä sovellus ja tietokanta komennolla:

```bash
docker-compose -f compose.demo.yaml up --build
```

Tämä komento rakentaa sovelluksen Docker-imagen ja käynnistää sekä tietokannan että sovelluksen. Sovellus on saatavilla
osoitteessa `http://localhost:8080`.

**Seuraava esimerkki hakee kaikki huoneen 1 varaukset:**

```
localhost:8080/api/reservations?roomId=1
```