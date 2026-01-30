# Analyysi

## 1. Mitä tekoäly teki hyvin?

### PostgreSQL Exclusion Constraint käyttö ratkaisemaan varausten päällekkäisyyteen liittyvä vaatimus

Mielestäni Junien ehdottama ratkaisu estää päällekkäisten varausten syntyminen PostgreSQL:n Exclusion Constraintin
avulla oli hyvä. Tutkin asiaa Postgren dokumentaatiosta ja kyseinen toiminnallisuus on lisätty Postgreen juuri tällaista
tarkoitusta varten [^1].

## 2. Mitä tekoäly teki huonosti?

### Problem Details paljasti sovelluksen sisäisiä toteutusyksityiskohtia

Olin erikseen ohjeistanut .junie/guidelines.md tiedostossa, että poikkeusten hallinnan ei tulisi koskaan paljastaa
käyttäjälle sovelluksen sisäisiä toteutusyksityiskohtia ("internal implementation details"):

```markdown
## Exception handling

- Use a single `@RestControllerAdvice` class to centralize error handling.
- Return consistent error responses. Use ProblemDetails response format (RFC 9457).
- Strictly never expose to the client:
    - stack traces
    - SQL errors
    - internal implementation details
    - any confidential data
```

Junie kuitenkin toteutti ProblemDetailsin niin, että sisäisiä toteutuksia ilmoitettiin käyttäjälle:

```java

@ExceptionHandler(ConstraintViolationException.class)
public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex) {
    log.warn("Constraint violation: {}", ex.getMessage());
    String detail = ex.getConstraintViolations().stream()
            .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
            .collect(Collectors.joining(", "));
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    problemDetail.setTitle("Constraint Violation");
    return problemDetail;
}
```

```json
{
  "type": "about:blank",
  "title": "Constraint Violation",
  "status": 400,
  "detail": "deleteReservation.reservationId: must be greater than 0",
  "instance": "/api/reservations/-1"
}
```

`deleteReservation.reservationId` ovat Controllerin delete metodin ja parametrin nimet:

```java

@DeleteMapping("/{reservationId}")
public ResponseEntity<Void> deleteReservation(@PathVariable @Positive Long reservationId) {
    reservationService.deleteReservation(reservationId);
    return ResponseEntity.noContent().build();
}
```

Lisäksi mielestäni `"title": "Constraint Violation"` sanavalintana kuvaa tarpeettomasti toteutuksen eheyssääntöjä.

Vastaava eheyssääntöjä kuvaava sanavalinta Junien toteutuksessa oli myös:

```json
{
  "type": "about:blank",
  "title": "Data Conflict",
  "status": 409,
  "detail": "The requested operation conflicts with existing data or business rules.",
  "instance": "/api/reservations"
}
```

Refaktoroitaessa DataIntegrityViolationExceptionin handler methodia, Junie halusi lähettää käyttäjälle tiedon
tietokannan eheyssäännöistä ("Operation violates database constraint: " constraint):

```java

@ExceptionHandler(DataIntegrityViolationException.class)
public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
    Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);
    
    if (cause instanceof org.postgresql.util.PSQLException psqlException) {
        var serverError = psqlException.getServerErrorMessage();
        if (serverError != null && serverError.getConstraint() != null) {
            String constraint = serverError.getConstraint();
            log.warn("Data integrity violation: constraint={}", constraint);
            
            return switch (constraint) {
                case "start_before_end" ->
                        createProblemDetail(HttpStatus.CONFLICT, "The start time must be before the end time.", ErrorType.INVALID_RESERVATION_TIME);
                case "start_in_future" ->
                        createProblemDetail(HttpStatus.CONFLICT, "The reservation must start in the future.", ErrorType.INVALID_RESERVATION_TIME);
                case "reservation_room_id_tstzrange_excl" ->
                        createProblemDetail(HttpStatus.CONFLICT, "The room is already reserved for the requested time period.", ErrorType.OVERLAPPING_RESERVATION);
                case "reservation_room_id_fkey" ->
                        createProblemDetail(HttpStatus.NOT_FOUND, "The specified room does not exist.", ErrorType.ROOM_NOT_FOUND);
                default ->
                        createProblemDetail(HttpStatus.CONFLICT, "Operation violates database constraint: " + constraint, ErrorType.DATA_CONFLICT);
            };
        }
    }
    
    log.error("Unexpected data integrity violation", ex);
    return createProblemDetail(HttpStatus.CONFLICT, "The requested operation conflicts with existing data.", ErrorType.DATA_CONFLICT);
}

```

MethodArgumentNotValidException handler-methodissa minulla oli seuraava koodi:

```java
        Map<String, String> errors = ex.getBindingResult().getAllErrors().stream()
        .collect(Collectors.toMap(
                error -> error instanceof FieldError fieldError ? fieldError.getField() : "Unknown field",
                this::resolveErrorMessage,
                (existing, replacement) -> existing + ", " + replacement
        ));
```

Junien tekemässä versiossa "Unknown field" (lopullisessa versiossa "invalidField") tuolla paikalla oli
`error.getObjectName()`, jonka olin poistanut, koska kyseinen kutsu palauttaa nimensä mukaisesti olion nimen ja tässä
tapauksessa `reservationRequest`:

```json
{
  "type": "urn:room-reservation-api:invalid-request-body",
  "title": "Invalid Request Body",
  "status": 400,
  "detail": "The data provided in the request body is invalid.",
  "instance": "/api/reservations",
  "errors": {
    "reservationRequest": "Start time must be before end time"
  }
}
```

Junie teki minulle tästä kuitenkin seuraavan sivuhuomautuksen (Prompti 45):

> 4. Professional Mapping: Replacing "Unknown field" with error.getObjectName() ensures that class-level constraints (
     like your >@ValidReservationRange) have a meaningful key in the error map instead of a generic placeholder.

### @Validated annotaation käyttö RestController luokassa

Junie lisäsi ReservationController luokkkaan `@Validated` annotaation ja se muutti `@Positive` annotaation toimintaa
delete metodissa:

```java

@DeleteMapping("/{reservationId}")
public ResponseEntity<Void> deleteReservation(@PathVariable @Positive Long reservationId) {
    reservationService.deleteReservation(reservationId);
    return ResponseEntity.noContent().build();
}
```

En ole vastaavissa tilanteessa käyttänyt `@Validated` annotaatiota ja kun kokeilin, niin negatiivinen `reservationId`
aiheutti `ConstraintViolationException`. Kun `@Validated` annotaation otti pois, aiheutti negatiivinen `reservationId`
`HandlerMethodValidationException`. Sovelluksessa `GlobalExceptionHandler` perii `ResponseEntityExceptionHandler`, josta
jo löytyy `HandlerMethodValidationException`, niin olisi mielestäni ollut parempi, että poikkeus menee sen käsiteltäväki
kuin asettaa uusi `ConstraintViolationException` handler metodi. Päädyin ylikirjoittamaan (@Override)
`handleHandlerMethodValidationException` metodin `ResponseEntityExceptionHandlerista`. Mielestäni tämä ratkaisu on
selkeämpi ja helpommin ylläpidettävä ratkaisu.

### Dublikaatit logitukset.

Sovelluksen ReservationService luokassa tehdään seuraavanlaisia tarkistuksia:

```java
if(!roomRepository.existsById(request.roomId())){
    throw new RoomNotFoundException(request.roomId());
}
```

Junie laittoi jokaiseen näistä oman logituksen ja toisti saman logituksen myös GlobalExceptionHandlerissa. Tällöin
poikkeustilanteessa logiin olisi kirjautunut samasta tapahtumasta kaksi logia. Poistin turhat logit ja toteutin
poikkeusten logituksen keskitetysti GlobalExceptionHandlerissa.

## 3. Mitkä olivat tärkeimmät parannukset, jotka teit tekoälyn tuottamaan koodiin ja miksi?

### ProblemDetail olion attribuuttien arvojen muokkaaminen

Muutin GlobalExceptionHandlerissa määritettyjen ProblemDetail attribuuttien arvot, niin että ne eivät paljasta
käyttäjälle sovelluksen sisäisiä toteutusyksityiskohtia. Lisäsin ReservationControllerin testeihin assertit sille, että
käyttäjälle menevät virheilmoitukset ovat halutun kaltaisia.

RFC 9457 standardi kehottaa tarkastamaan virheviestien sisällön huolellisesti, jotta vältytään toteutusyksityiskohtien
paljastumiselta, jotka voisivat vaarantaa järjestelmän tietoturvan tai käyttäjien yksityisyyden. [^2]

### Virheellisen tietokannan eheyssäännön poistaminen

Virheellinen eheyssääntö oli seuraava:

```postgresql
CONSTRAINT start_in_future CHECK (start_time >= now())
```

Alkuperäisesti virheellinen eheyssääntö päätyi tietokannan määritykseen omasta ajattelemattomuudestani heti projektin
alussa ja suunnitteluvaiheessa. Sen lisäksi, että ehdotin tekoälylle kyseistä eheyssääntöä käytin mielestäni myös hieman
heikkoa promptia (Prompti 4):

> I think it would be good if the database also had constraints for
>
>- start_time < end_time
>- start_time >= now()
>- foreing key constraints on room_id
>
>Then the database would act as a last guard of integrity. What do you think? If you think it's a good idea, please
> update the plan accordingly. @file:create-reservation-plan.md
>
>**Important**:
>
>- Don't generate, modify, or refactor any code. Only update the plan.

*"I think it would be good..."* on ehkä hieman voimakas muotoilu, jolloin tekoäly ei ehkä kauhean herkästi lähde
vastustelemaan. Kun myöhemmin promptissa kysyin: *"What do you think?"* ja vaikka eheyssääntö ei ole millään tavalla
järkeä, lisäsi tekoäly sen suunnitelmaan ja myöhemmin itse toteutukseen.

Alkuperäisestikin sovellus tarkistaa, että start_time ei ole menneisyydessä ja poistin tämän virheellisen eheyssäännön
relaation määrittelystä.

Kyseinen eheyssääntö tarkoittaisi sitä, että tietokanta pysyy eheänä vain ja ainoastaan silloin, kun start_time
sarakkeen arvot ovat tulevaisuudessa. Tietokannan eheyden rikkoutuminen olisi siis väistämätöntä.

### Lähteet

[^1]: postgresql.org. 8.17.10. Constraints on
Ranges: https://www.postgresql.org/docs/current/rangetypes.html#RANGETYPES-CONSTRAINT

[^2]: M. Nottingham, E. Wilde, S. Dalal. RFC 9457 Problem Details for HTTP
APIs https://www.rfc-editor.org/rfc/rfc9457.html#name-security-considerations