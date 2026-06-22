package com.tfm.tennis_platform.infrastructure.controller.docs;

import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;

import com.tfm.tennis_platform.infrastructure.integration.IntegrationTestBase;

@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public abstract class RestDocsTestBase extends IntegrationTestBase {
}