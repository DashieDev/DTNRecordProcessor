package dtnrecordprocessor.processor;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import dtnrecordprocessor.lib.ImmutableWith;

@SupportedAnnotationTypes("dtnrecordprocessor.lib.ImmutableWith")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class ImmutableWithProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (var annotation : annotations) {
            final var annotated_elements = roundEnv.getElementsAnnotatedWith(annotation);
            for (var element : annotated_elements) {
                if (element.getKind() != ElementKind.RECORD) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, 
                        "@ImmutableWith can only be applied to records", element);
                    continue;
                }

                try {
                    generateWithInterface((TypeElement) element);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private void generateWithInterface(TypeElement target_record) throws IOException {
        final var package_name = this.processingEnv.getElementUtils().getPackageOf(target_record)
            .getQualifiedName().toString();
        final var record_name_full = ClassName.get(target_record);

        final var interface_posfix = "ImmutableWith";
        final var interface_name = interfaceNameFromRecord(
            package_name, target_record, interface_posfix);
        var interface_builder = TypeSpec.interfaceBuilder(interface_name)
            .addModifiers(Modifier.PUBLIC);

        final var interface_name_full = package_name.isEmpty() ? 
            interface_name : package_name + "." + interface_name;
        final boolean do_implement = target_record.getInterfaces()
            .stream()
            .anyMatch(x -> {
                var name = x.toString();
                return name.equals(interface_name) || name.equals(interface_name_full);
            });
        
        if (!do_implement) {
            final var error_msg = String.format(
                "Record %s annotated with @ImmutableWith should implement the generated interface: %s", 
                target_record.getSimpleName(), interface_name);
            
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR, error_msg, target_record);

            var output_java = 
                JavaFile.builder(package_name, interface_builder.build())
                    .build();
            output_java.writeTo(processingEnv.getFiler());
            return;
        }

        final var record_fields = target_record.getRecordComponents();
        
        for (final var record_field : record_fields) {
            final var field_name = record_field.getSimpleName().toString();
            final var field_type = TypeName.get(record_field.asType());
            
            final var getter_spec = MethodSpec.methodBuilder(field_name)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(field_type);

            
            final var wither_prefix = "with";
            final var wither_name = wither_prefix + toUpperCamelCase(field_name);
            final var new_val_name = "newVal";
            final var wither_spec = MethodSpec.methodBuilder(wither_name)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(record_name_full)
                .addParameter(field_type, new_val_name);

            boolean is_string_compare = field_type.equals(ClassName.get(String.class));;
            wither_spec
                .beginControlFlow(
                    is_string_compare ? 
                        "if (java.util.Objects.equals(this.$L(), $L))" 
                        : "if (this.$L() == $L)", 
                    field_name, new_val_name)
                .addStatement("return ($T) this", record_name_full)
                .endControlFlow();
            
            var joined_args = joinedNewRecordArgsWithNewVal(
                record_fields, record_field, new_val_name);
            wither_spec
                .addStatement("return new $T($L)", record_name_full, joined_args);


            interface_builder.addMethod(getter_spec.build());
            interface_builder.addMethod(wither_spec.build());
        }


        var output_java = 
            JavaFile.builder(package_name, interface_builder.build())
                .build();
        output_java.writeTo(processingEnv.getFiler());
    }

    private static String interfaceNameFromRecord(String package_name, 
        TypeElement target_record, String posfix) {
        
        var annotation = target_record.getAnnotation(ImmutableWith.class);
        var override_name = annotation != null ? annotation.className() : "";
        if (!override_name.isEmpty())
            return override_name;

        var qualified_name = target_record.getQualifiedName().toString();
        var record_name = !package_name.isEmpty() ? 
            qualified_name.substring(package_name.length() + 1) 
            : qualified_name;
        record_name = record_name.replace('.', '_') + posfix;
        return record_name;
    }

    private static String joinedNewRecordArgsWithNewVal(
        List<? extends Element> fields, Element targetField, String newValName) {
        
        return fields.stream()
            .map(x -> x == targetField ? 
                newValName 
                : "this." + x.getSimpleName() + "()")
            .collect(Collectors.joining(", "));
    }

    private static String toUpperCamelCase(String x) {
        return x.substring(0, 1).toUpperCase() + x.substring(1);
    }
}
