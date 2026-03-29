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
            "doggytalents.state.OuterClass",
            "package doggytalents.state;",
            "import dtnrecordprocessor.lib.ImmutableWith;",
            "public class OuterClass {",
            "    @ImmutableWith",
            "    public record DogState(int health, String mode) implements OuterClass_DogStateImmutableWith {}",
            "}"
        );

        JavaFileObject expectedOutput = JavaFileObjects.forSourceLines(
            "doggytalents.state.OuterClass_DogStateImmutableWith",
            "package doggytalents.state;",
            "import java.lang.String;",
            "public interface OuterClass_DogStateImmutableWith {",
            "  int health();",
            "",
            "  default OuterClass.DogState withHealth(int newVal) {",
            "    if (this.health() == newVal) {",
            "      return (OuterClass.DogState) this;",
            "    }",
            "    return new OuterClass.DogState(newVal, this.mode());",
            "  }",
            "",
            "  String mode();",
            "",
            "  default OuterClass.DogState withMode(String newVal) {",
            "    if (java.util.Objects.equals(this.mode(), newVal)) {",
            "      return (OuterClass.DogState) this;",
            "    }",
            "    return new OuterClass.DogState(this.health(), newVal);",
            "  }",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new ImmutableWithProcessor())
            .compile(inputState);

        assertThat(compilation).succeeded();
        assertThat(compilation)
            .generatedSourceFile("doggytalents.state.OuterClass_DogStateImmutableWith")
            .hasSourceEquivalentTo(expectedOutput);
    }
}