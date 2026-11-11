package gr.aueb.cf.automation.license.crypto;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class LicensePublicKeyService {

    private static final String PUBLIC_KEY_RESOURCE =
            "classpath:license/license-signing-public.key";


    private final ResourceLoader resourceLoader;


    public LicensePublicKeyService(
            ResourceLoader resourceLoader
    ) {

        this.resourceLoader =
                resourceLoader;
    }


    public PublicKey loadPublicKey() {

        try {

            Resource resource =
                    resourceLoader.getResource(
                            PUBLIC_KEY_RESOURCE
                    );


            if (!resource.exists()) {

                throw new IllegalStateException(
                        "Embedded license public key does not exist."
                );
            }


            String encodedKey;

            try (var inputStream =
                         resource.getInputStream()) {

                encodedKey =
                        new String(
                                inputStream.readAllBytes(),
                                StandardCharsets.UTF_8
                        ).trim();
            }


            if (encodedKey.isBlank()) {

                throw new IllegalStateException(
                        "Embedded license public key is empty."
                );
            }


            byte[] keyBytes =
                    Base64.getDecoder()
                            .decode(
                                    encodedKey
                            );


            X509EncodedKeySpec keySpec =
                    new X509EncodedKeySpec(
                            keyBytes
                    );


            KeyFactory keyFactory =
                    KeyFactory.getInstance(
                            "Ed25519"
                    );


            return keyFactory.generatePublic(
                    keySpec
            );

        } catch (IllegalStateException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Could not load embedded license public key.",
                    exception
            );
        }
    }
}