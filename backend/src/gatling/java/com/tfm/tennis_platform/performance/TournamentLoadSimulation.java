package com.tfm.tennis_platform.performance;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class TournamentLoadSimulation extends Simulation {

    private static final String BASE_URL = "http://localhost:8085";

    private static final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling/PerformanceTest");

    private static final ChainBuilder publicTournamentRead =
            exec(
                    http("Get All Tournaments")
                            .get("/api/tournaments")
                            .check(status().is(200))
                            .check(jsonPath("$").isArray())
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Age Categories")
                            .get("/api/age-categories")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Nationalities")
                            .get("/api/refs/nationalities")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Rankings")
                            .get("/api/rankings/professionals")
                            .check(status().is(200))
            );

    private static final ChainBuilder authenticatedTournamentFlow =
            exec(
                    http("Register User")
                            .post("/api/auth/register")
                            .body(StringBody("""
                                    {
                                        "email": "perf#{randomLong()}@test.com",
                                        "password": "Password123!",
                                        "name": "PerfUser",
                                        "role": "ORGANIZER"
                                    }
                                    """))
                            .check(status().is(201))
                            .check(jsonPath("$.email").saveAs("userEmail"))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Login User")
                            .post("/api/auth/login")
                            .body(StringBody("""
                                    {
                                        "email": "#{userEmail}",
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
                                        "formalName": "Load Test Tournament #{randomLong()}",
                                        "playStartDate": "2026-08-01",
                                        "playEndDate": "2026-08-05",
                                        "tournamentStartTime": "09:00:00",
                                        "inscriptionStartDate": "2026-07-01",
                                        "inscriptionEndDate": "2026-07-25",
                                        "surfaceCategory": "CLAY",
                                        "maxPlayers": 32,
                                        "location": "Madrid, Spain",
                                        "locationLatitude": 40.4168,
                                        "locationLongitude": -3.7038,
                                        "courtCount": 2
                                    }
                                    """))
                            .check(status().is(201))
                            .check(jsonPath("$.id").saveAs("tournamentId"))
            )
            .pause(Duration.ofSeconds(2))
            .exec(
                    http("Get Tournament Detail")
                            .get("/api/tournaments/#{tournamentId}")
                            .header("Authorization", "Bearer #{accessToken}")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Tournament Courts")
                            .get("/api/tournaments/#{tournamentId}/courts")
                            .header("Authorization", "Bearer #{accessToken}")
                            .check(status().is(200))
            );

    private ScenarioBuilder publicReadScenario = scenario("Public Tournament Read")
            .exec(publicTournamentRead);

    private ScenarioBuilder fullTournamentFlowScenario = scenario("Full Tournament Flow")
            .exec(authenticatedTournamentFlow);

    {
        setUp(
                publicReadScenario.injectOpen(
                        rampUsers(50).during(Duration.ofSeconds(30))
                ),
                fullTournamentFlowScenario.injectOpen(
                        rampUsers(10).during(Duration.ofSeconds(60))
                )
        ).protocols(httpProtocol);
    }
}
