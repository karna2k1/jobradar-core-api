package com.jobradar.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SkillExtractorTest {
    private final SkillExtractor extractor = new SkillExtractor();

    @Test
    void findsCanonicalSkillsViaAliases() {
        String text = "Built micro-services with springboot and React.js; deployed on AWS using Docker/k8s. "
                + "Wrote unit tests in JUnit and Mockito. Used Postgres and Kafka.";
        assertThat(extractor.extract(text)).contains(
                "Spring Boot", "Microservices", "React", "AWS", "Docker", "Kubernetes",
                "JUnit", "Mockito", "PostgreSQL", "Kafka");
    }

    @Test
    void avoidsSubstringAndPlainEnglishFalsePositives() {
        String text = "Wrote JavaScript for the rest of the team; my sql knowledge is basic.";
        assertThat(extractor.extract(text))
                .contains("JavaScript")
                .doesNotContain("Java", "REST", "SQL");
    }

    @Test
    void handlesSymbolSkills() {
        assertThat(extractor.extract("Experience with C++, C# and Node.js"))
                .contains("C++", "C#", "Node.js");
    }

    @Test
    void emptyTextReturnsNothing() {
        assertThat(extractor.extract("  ")).isEmpty();
    }
}
