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
            "    @ImmutableWith(className=\"DogWith\")",
            "    public record DogState(int health, String mode) implements DogWith {}",
            "}"
        );

        JavaFileObject expectedOutput = JavaFileObjects.forSourceLines(
            "doggytalents.state.DogWith",
            "package doggytalents.state;",
            "",
            "import java.lang.Override;",
            "import java.lang.String;",
            "",
            "public interface DogWith {",
            "",
            "    default OuterClass.DogState produce() {",
            "        return (OuterClass.DogState) this; ",
            "    }",
            "",
            "    int health();",
            "    ",
            "    default DogWith withHealth(int newVal) {",
            "        if (this.health() == newVal) {",
            "            return this;",
            "        }",
            "        return new _Draft(this).withHealth(newVal); ",
            "    }",
            "",
            "    String mode();",
            "",
            "    default DogWith withMode(String newVal) {",
            "        if (java.util.Objects.equals(this.mode(), newVal)) {",
            "            return this;",
            "        }",
            "        return new _Draft(this).withMode(newVal);",
            "    }",
            "",
            "    ",
            "    class _Draft implements DogWith {",
            "        private int health;",
            "        private String mode;",
            "",
            "        private _Draft(DogWith source) {",
            "            this.health = source.health();",
            "            this.mode = source.mode();",
            "        }",
            "",
            "        @Override public int health() { return this.health; }",
            "",
            "        @Override",
            "        public DogWith withHealth(int newVal) {",
            "            this.health = newVal;",
            "            return this;",
            "        }",
            "",
            "        @Override public String mode() { return this.mode; }",
            "",
            "        @Override",
            "        public DogWith withMode(String newVal) {",
            "            this.mode = newVal;",
            "            return this;",
            "        }",
            "",
            "        @Override",
            "        public OuterClass.DogState produce() {",
            "            return new OuterClass.DogState(this.health, this.mode);",
            "        }",
            "    }",
            "}"
        );

        Compilation compilation = javac()
            .withProcessors(new ImmutableWithProcessor())
            .compile(inputState);

        assertThat(compilation).succeeded();
        assertThat(compilation)
            .generatedSourceFile("doggytalents.state.DogWith")
            .hasSourceEquivalentTo(expectedOutput);
    }
}