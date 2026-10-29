package com.tfm.tennis_platform.performance;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class TournamentDetailSimulation extends Simulation {

    private static final String BASE_URL = "http://localhost:8080";

    private static final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling/PerformanceTest");

    private static final ChainBuilder browseTournamentPage =
            exec(
                    http("List Tournaments")
                            .get("/api/tournaments")
                            .check(status().is(200))
                            .check(jsonPath("$[0].id").saveAs("tournamentId"))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Tournament Detail")
                            .get("/api/tournaments/#{tournamentId}")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Tournament Inscriptions")
                            .get("/api/tournaments/#{tournamentId}/inscriptions")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Tournament Ranking")
                            .get("/api/rankings/tournaments/#{tournamentId}")
                            .check(status().is(200))
            );

    private static final ChainBuilder authenticatedBrowseTournament =
            exec(session -> session.set("userEmail", "player" + System.nanoTime() + "@test.com"))
            .exec(
                    http("Register Player")
                            .post("/api/auth/register")
                            .body(StringBody("""
                                    {
                                        "email": "#{userEmail}",
                                        "password": "Password123!",
                                        "name": "LoadTestPlayer",
                                        "role": "PLAYER",
                                        "privacyPolicyAccepted": true
                                    }
                                    """))
                            .check(status().is(201))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Login Player")
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
                    http("List Tournaments (Auth)")
                            .get("/api/tournaments")
                            .check(status().is(200))
                            .check(jsonPath("$[0].id").saveAs("tournamentId"))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Tournament Detail (Auth)")
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
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Tournament Matches")
                            .get("/api/matches/tournament/#{tournamentId}")
                            .header("Authorization", "Bearer #{accessToken}")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Tournament Inscriptions (Auth)")
                            .get("/api/tournaments/#{tournamentId}/inscriptions")
                            .header("Authorization", "Bearer #{accessToken}")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1))
            .exec(
                    http("Get Tournament Ranking (Auth)")
                            .get("/api/rankings/tournaments/#{tournamentId}")
                            .header("Authorization", "Bearer #{accessToken}")
                            .check(status().is(200))
            );

    private ScenarioBuilder publicBrowseScenario = scenario("Public Tournament Browse")
            .exec(browseTournamentPage);

    private ScenarioBuilder authenticatedBrowseScenario = scenario("Authenticated Tournament Browse")
            .exec(authenticatedBrowseTournament);

    {
        setUp(
                publicBrowseScenario.injectOpen(
                        rampUsers(30).during(Duration.ofSeconds(30))
                ),
                authenticatedBrowseScenario.injectOpen(
                        rampUsers(15).during(Duration.ofSeconds(60))
                )
        ).protocols(httpProtocol)
        .assertions(
                global().responseTime().percentile(95).lt(3000),
                global().successfulRequests().percent().gt(95.0),
                details("Get Tournament Detail").responseTime().percentile(95).lt(300),
                details("Get Tournament Detail").successfulRequests().percent().gt(99.0),
                details("Get Tournament Inscriptions").responseTime().percentile(95).lt(300),
                details("Get Tournament Ranking").responseTime().percentile(95).lt(300)
        );
    }
}
