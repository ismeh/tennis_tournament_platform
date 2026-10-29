package com.tfm.tennis_platform.performance;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class MatchScoreUpdateSimulation extends Simulation {

    private static final String BASE_URL = "http://localhost:8080";

    private static final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling/PerformanceTest");

    private static final ChainBuilder fullScoreUpdateFlow =
            exec(session -> session.set("adminEmail", "admin" + System.nanoTime() + "@test.com"))
            .exec(
                    http("Register Admin")
                            .post("/api/auth/register")
                            .body(StringBody("""
                                    {
                                        "email": "#{adminEmail}",
                                        "password": "Password123!",
                                        "name": "LoadTestAdmin",
                                        "role": "ORGANIZER",
                                        "privacyPolicyAccepted": true
                                    }
                                    """))
                            .check(status().is(201))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Login Admin")
                            .post("/api/auth/login")
                            .body(StringBody("""
                                    {
                                        "email": "#{adminEmail}",
                                        "password": "Password123!"
                                    }
                                    """))
                            .check(status().is(200))
                            .check(jsonPath("$.accessToken").saveAs("accessToken"))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Create Tournament")
                            .post("/api/tournaments")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "formalName": "Score Test #{randomLong()}",
                                        "playStartDate": "2026-08-01",
                                        "playEndDate": "2026-08-10",
                                        "tournamentStartTime": "09:00:00",
                                        "inscriptionStartDate": "2026-07-01",
                                        "inscriptionEndDate": "2026-07-25",
                                        "surfaceCategory": "CLAY",
                                        "maxPlayers": 32,
                                        "location": "Barcelona, Spain",
                                        "locationLatitude": 41.3851,
                                        "locationLongitude": 2.1734,
                                        "courtCount": 4
                                    }
                                    """))
                            .check(status().is(201))
                            .check(jsonPath("$.id").saveAs("tournamentId"))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Add Event")
                            .post("/api/tournaments/#{tournamentId}/events")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "events": [
                                            {
                                                "categoryId": 8,
                                                "gender": "MALE",
                                                "stages": ["KNOCKOUT"]
                                            }
                                        ]
                                    }
                                    """))
                            .check(status().is(200))
                            .check(jsonPath("$.events[0].eventId").saveAs("eventId"))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Inscribe Player 1")
                            .post("/api/tournaments/#{tournamentId}/events/#{eventId}/manual-inscriptions")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "playerSource": "MANUAL",
                                        "firstName": "Carlos",
                                        "lastName": "Garcia",
                                        "gender": "MALE"
                                    }
                                    """))
                            .check(status().is(201))
            )
            .exec(
                    http("Inscribe Player 2")
                            .post("/api/tournaments/#{tournamentId}/events/#{eventId}/manual-inscriptions")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "playerSource": "MANUAL",
                                        "firstName": "Marcos",
                                        "lastName": "Lopez",
                                        "gender": "MALE"
                                    }
                                    """))
                            .check(status().is(201))
            )
            .exec(
                    http("Inscribe Player 3")
                            .post("/api/tournaments/#{tournamentId}/events/#{eventId}/manual-inscriptions")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "playerSource": "MANUAL",
                                        "firstName": "Ana",
                                        "lastName": "Martinez",
                                        "gender": "FEMALE"
                                    }
                                    """))
                            .check(status().is(201))
            )
            .exec(
                    http("Inscribe Player 4")
                            .post("/api/tournaments/#{tournamentId}/events/#{eventId}/manual-inscriptions")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "playerSource": "MANUAL",
                                        "firstName": "Laura",
                                        "lastName": "Rodriguez",
                                        "gender": "FEMALE"
                                    }
                                    """))
                            .check(status().is(201))
            )
            .exec(
                    http("Inscribe Player 5")
                            .post("/api/tournaments/#{tournamentId}/events/#{eventId}/manual-inscriptions")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "playerSource": "MANUAL",
                                        "firstName": "Pedro",
                                        "lastName": "Fernandez",
                                        "gender": "MALE"
                                    }
                                    """))
                            .check(status().is(201))
            )
            .exec(
                    http("Inscribe Player 6")
                            .post("/api/tournaments/#{tournamentId}/events/#{eventId}/manual-inscriptions")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "playerSource": "MANUAL",
                                        "firstName": "Maria",
                                        "lastName": "Sanchez",
                                        "gender": "FEMALE"
                                    }
                                    """))
                            .check(status().is(201))
            )
            .exec(
                    http("Inscribe Player 7")
                            .post("/api/tournaments/#{tournamentId}/events/#{eventId}/manual-inscriptions")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "playerSource": "MANUAL",
                                        "firstName": "Javier",
                                        "lastName": "Perez",
                                        "gender": "MALE"
                                    }
                                    """))
                            .check(status().is(201))
            )
            .exec(
                    http("Inscribe Player 8")
                            .post("/api/tournaments/#{tournamentId}/events/#{eventId}/manual-inscriptions")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "playerSource": "MANUAL",
                                        "firstName": "Sofia",
                                        "lastName": "Torres",
                                        "gender": "FEMALE"
                                    }
                                    """))
                            .check(status().is(201))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Generate Draws")
                            .post("/api/tournaments/#{tournamentId}/events/#{eventId}/generate-draws")
                            .header("Authorization", "Bearer #{accessToken}")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(2))
            .exec(
                    http("Start Tournament")
                            .patch("/api/tournaments/#{tournamentId}/status")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "status": "IN_PROGRESS"
                                    }
                                    """))
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Tournament Detail")
                            .get("/api/tournaments/#{tournamentId}")
                            .header("Authorization", "Bearer #{accessToken}")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Tournament Matches")
                            .get("/api/matches/tournament/#{tournamentId}")
                            .header("Authorization", "Bearer #{accessToken}")
                            .check(status().is(200))
                            .check(jsonPath("$[0].id").saveAs("matchId1"))
                            .check(jsonPath("$[0].firstInscriptionId").saveAs("firstId1"))
                            .check(jsonPath("$[1].id").saveAs("matchId2"))
                            .check(jsonPath("$[1].firstInscriptionId").saveAs("firstId2"))
                            .check(jsonPath("$[2].id").saveAs("matchId3"))
                            .check(jsonPath("$[2].firstInscriptionId").saveAs("firstId3"))
                            .check(jsonPath("$[3].id").saveAs("matchId4"))
                            .check(jsonPath("$[3].firstInscriptionId").saveAs("firstId4"))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Update Match 1 Result")
                            .post("/api/tournaments/#{tournamentId}/matches/#{matchId1}/result")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "winnerId": "#{firstId1}",
                                        "scoreString": "6-4, 7-5",
                                        "status": "COMPLETED"
                                    }
                                    """))
                            .check(status().is(200))
            )
            .exec(
                    http("Update Match 2 Result")
                            .post("/api/tournaments/#{tournamentId}/matches/#{matchId2}/result")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "winnerId": "#{firstId2}",
                                        "scoreString": "6-3, 6-4",
                                        "status": "COMPLETED"
                                    }
                                    """))
                            .check(status().is(200))
            )
            .exec(
                    http("Update Match 3 Result")
                            .post("/api/tournaments/#{tournamentId}/matches/#{matchId3}/result")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "winnerId": "#{firstId3}",
                                        "scoreString": "7-6, 6-3",
                                        "status": "COMPLETED"
                                    }
                                    """))
                            .check(status().is(200))
            )
            .exec(
                    http("Update Match 4 Result")
                            .post("/api/tournaments/#{tournamentId}/matches/#{matchId4}/result")
                            .header("Authorization", "Bearer #{accessToken}")
                            .body(StringBody("""
                                    {
                                        "winnerId": "#{firstId4}",
                                        "scoreString": "6-2, 6-4",
                                        "status": "COMPLETED"
                                    }
                                    """))
                            .check(status().is(200))
            );

    private ScenarioBuilder scoreUpdateScenario = scenario("Match Score Update Flow")
            .exec(fullScoreUpdateFlow);

    {
        setUp(
                scoreUpdateScenario.injectOpen(
                        rampUsers(5).during(Duration.ofSeconds(30))
                )
        ).protocols(httpProtocol)
        .assertions(
                global().responseTime().percentile(95).lt(2000),
                global().successfulRequests().percent().gt(90.0),
                details("Generate Draws").responseTime().percentile(95).lt(2000),
                details("Update Match 1 Result").responseTime().percentile(95).lt(1000),
                details("Update Match 2 Result").responseTime().percentile(95).lt(1000),
                details("Update Match 3 Result").responseTime().percentile(95).lt(1000),
                details("Update Match 4 Result").responseTime().percentile(95).lt(1000)
        );
    }
}
