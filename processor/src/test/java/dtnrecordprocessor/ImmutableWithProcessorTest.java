package dtnrecordprocessor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;

import dtnrecordprocessor.processor.ImmutableWithProcessor;

import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class ImmutableWithProcessorTest {

    @Test
    void generatesWithInterfaceForRecord() {
        JavaFileObject inputState = JavaFileObjects.forSourceLines(
            "doggytalents.state.DogState",
            "package doggytalents.state;",
            "",
            "import dtnrecordprocessor.lib.ImmutableWith;",
            "",
            "@ImmutableWith",
            "public record DogState(int health, String mode) implements DogStateImmutableWith {}"
        );

        JavaFileObject expectedOutput = JavaFileObjects.forSourceLines(
            "doggytalents.state.DogStateImmutableWith",
            "package doggytalents.state;",
            "import java.lang.String;",
            "public interface DogStateImmutableWith {",
            "  int health();",
            "",
            "  default DogState withHealth(int newVal) {",
            "    if (this.health() == newVal) {",
            "      return (DogState) this;",
            "    }",
            "    return new DogState(newVal, this.mode());",
            "  }",
            "",
            "  String mode();",
            "",
            "  default DogState withMode(String newVal) {",
            "    if (this.mode() == newVal) {",
            "      return (DogState) this;",
            "    }",
            "    return new DogState(this.health(), newVal);",
            "  }",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new ImmutableWithProcessor())
            .compile(inputState);

        assertThat(compilation).succeeded();
        assertThat(compilation)
            .generatedSourceFile("doggytalents.state.DogStateImmutableWith")
            .hasSourceEquivalentTo(expectedOutput);
    }
}