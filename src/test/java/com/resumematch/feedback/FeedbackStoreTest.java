package com.resumematch.feedback;

import static com.resumematch.matcher.MatchStatus.MATCH;
import static com.resumematch.matcher.MatchStatus.MISSING;
import static com.resumematch.matcher.MatchStatus.PARTIAL;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FeedbackStoreTest {

    @TempDir
    Path dir;

    @Test
    void gravaELeAsMarcacoes() throws IOException {
        Path file = dir.resolve("sub/feedback.csv");
        FeedbackStore store = new FeedbackStore(file, true);

        store.record(new Feedback("a1", "Java", MATCH, MATCH));
        store.record(new Feedback("a1", "TCP/IP + DNS + DHCP", PARTIAL, MISSING));

        assertThat(store.all()).containsExactly(
                new Feedback("a1", "Java", MATCH, MATCH),
                new Feedback("a1", "TCP/IP + DNS + DHCP", PARTIAL, MISSING));
        assertThat(Files.readAllLines(file)).first().isEqualTo(FeedbackStore.HEADER);
        assertThat(Files.readAllLines(file)).hasSize(3);
    }

    @Test
    void nomeComVirgulaOuFormulaVoltaIgual() {
        FeedbackStore store = new FeedbackStore(dir.resolve("feedback.csv"), true);
        Feedback tricky = new Feedback("a1", "=Java, \"Spring\"", MATCH, PARTIAL);

        store.record(tricky);

        assertThat(store.all()).containsExactly(tricky);
    }

    @Test
    void pulaLinhaQueNaoConsegueLer() throws IOException {
        Path file = dir.resolve("feedback.csv");
        Files.writeString(file, FeedbackStore.HEADER + "\n2026-11-24,a1,Java,TALVEZ,MATCH\n"
                + "2026-11-24,a1,Docker,MATCH,MATCH\n");

        assertThat(new FeedbackStore(file, true).all())
                .containsExactly(new Feedback("a1", "Docker", MATCH, MATCH));
    }

    @Test
    void desligadoNaoGravaNemLe() {
        Path file = dir.resolve("feedback.csv");
        FeedbackStore store = new FeedbackStore(file, false);

        store.record(new Feedback("a1", "Java", MATCH, MATCH));

        assertThat(file).doesNotExist();
        assertThat(store.all()).isEmpty();
    }
}
