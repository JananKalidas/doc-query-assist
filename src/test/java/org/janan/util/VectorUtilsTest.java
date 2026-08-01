package org.janan.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VectorUtilsTest {
    @Test
    void identicalVectors_haveSimilarityOfOne() {
        float[] a = {1f, 2f, 3f};
        float[] b = {1f, 2f, 3f};

        assertThat(VectorUtils.cosineSimilarity(a, b)).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void orthogonalVectors_haveSimilarityOfZero() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};

        assertThat(VectorUtils.cosineSimilarity(a, b)).isCloseTo(0.0, within(1e-6));
    }

    @Test
    void oppositeVectors_haveSimilarityOfNegativeOne() {
        float[] a = {1f, 0f};
        float[] b = {-1f, 0f};

        assertThat(VectorUtils.cosineSimilarity(a, b)).isCloseTo(-1.0, within(1e-6));
    }

    @Test
    void mismatchedDimensions_throwsIllegalArgumentException() {
        float[] a = {1f, 2f};
        float[] b = {1f, 2f, 3f};

        assertThatThrownBy(() -> VectorUtils.cosineSimilarity(a, b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimension mismatch");
    }

    @Test
    void zeroVector_returnsZeroSimilarityRatherThanDividingByZero() {
        float[] a = {0f, 0f, 0f};
        float[] b = {1f, 2f, 3f};

        assertThat(VectorUtils.cosineSimilarity(a, b)).isEqualTo(0.0);
    }

    @Test
    void toPgVectorLiteral_formatsAsExpected() {
        float[] embedding = {0.1f, 0.2f, 0.3f};

        String literal = VectorUtils.toPgVectorLiteral(embedding);

        assertThat(literal).isEqualTo("[0.1,0.2,0.3]");
    }

    @Test
    void toPgVectorLiteral_handlesSingleElement() {
        float[] embedding = {0.5f};

        assertThat(VectorUtils.toPgVectorLiteral(embedding)).isEqualTo("[0.5]");
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
