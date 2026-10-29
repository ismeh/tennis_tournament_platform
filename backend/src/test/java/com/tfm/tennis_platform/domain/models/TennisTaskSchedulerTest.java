//package com.tfm.tennis_platform.domain.models;
//
//import com.tfm.tennis_platform.TennisPlatformApplication;
//import org.awaitility.Durations;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
//
//import static org.awaitility.Awaitility.await;
//import static org.mockito.Mockito.atLeast;
//import static org.mockito.Mockito.verify;
//
//@SpringBootTest(classes = TennisTaskScheduler.class)
//@EnableScheduling
//public class TennisTaskSchedulerTest {
//    @MockitoSpyBean
//    TennisTaskScheduler tasks;
//
//    @Test
//    public void reportCurrentTime() {
//        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
//            verify(tasks, atLeast(2)).reportCurrentTime();
//        });
//    }
//}
