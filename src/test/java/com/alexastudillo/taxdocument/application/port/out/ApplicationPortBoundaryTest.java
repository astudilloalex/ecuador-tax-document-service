package com.alexastudillo.taxdocument.application.port.out;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.smallrye.mutiny.Uni;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ApplicationPortBoundaryTest {
    private static final Set<Class<?>> PORTS = Set.of(
            TaxDocumentRepository.class,
            IssuerAccessPolicyPort.class,
            SequenceNumberPort.class,
            AccessKeyGeneratorPort.class,
            SriAuthorizationPort.class,
            XmlStoragePort.class,
            TaxDocumentQueuePort.class,
            WebhookPublisherPort.class,
            ClockPort.class,
            TransactionPort.class,
            AuditLogPort.class);

    private static final Set<String> FORBIDDEN_SIGNATURE_TERMS = Set.of(
            "quarkus",
            "hibernate",
            "panache",
            "jakarta.ws.rs",
            "javax.ws.rs",
            "postgres",
            "redis",
            "filesystem",
            "httpclient",
            "adapter.",
            "soap");

    @Test
    void portsAreInterfacesOwnedByApplicationLayer() {
        for (Class<?> port : PORTS) {
            assertTrue(port.isInterface(), port.getName());
            assertTrue(port.getPackageName().startsWith("com.alexastudillo.taxdocument.application.port.out"));
        }
    }

    @Test
    void portSignaturesAvoidFrameworkAndAdapterTypes() {
        for (Class<?> port : PORTS) {
            for (Method method : declaredPortMethods(port)) {
                String signature = method.toGenericString().toLowerCase();
                for (String forbidden : FORBIDDEN_SIGNATURE_TERMS) {
                    assertFalse(signature.contains(forbidden), port.getName() + "#" + method.getName());
                }
            }
        }
    }

    @Test
    void portOperationsReturnMutinyUni() {
        for (Class<?> port : PORTS) {
            for (Method method : declaredPortMethods(port)) {
                assertEquals(Uni.class, method.getReturnType(), port.getName() + "#" + method.getName());
            }
        }
    }

    private static Set<Method> declaredPortMethods(Class<?> port) {
        return Arrays.stream(port.getDeclaredMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
