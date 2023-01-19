package net.accelbyte.util;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Base64.Decoder;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.extern.slf4j.Slf4j;
import net.accelbyte.sdk.api.iam.models.BloomFilterJSON;
import net.accelbyte.sdk.api.iam.models.OauthapiRevocationList;
import net.accelbyte.sdk.api.iam.models.OauthcommonJWKKey;
import net.accelbyte.sdk.api.iam.models.OauthcommonJWKSet;
import net.accelbyte.sdk.api.iam.operations.o_auth2_0.GetJWKSV3;
import net.accelbyte.sdk.api.iam.operations.o_auth2_0.GetRevocationListV3;
import net.accelbyte.sdk.api.iam.wrappers.OAuth20;
import net.accelbyte.sdk.core.AccelByteConfig;
import net.accelbyte.sdk.core.AccelByteSDK;
import net.accelbyte.sdk.core.client.OkhttpClient;
import net.accelbyte.sdk.core.repository.DefaultConfigRepository;
import net.accelbyte.sdk.core.repository.DefaultTokenRepository;

@Slf4j
public class ServerAuthProvider {

    private static String DEFAULT_CACHE_KEY = "default";

    public static String CLAIM_PERMISSIONS = "permissions";
    private static String PERMISSION_RESOURCE = "Resource";
    private static String PERMISSION_ACTION = "Action";

    private static final DefaultTokenRepository defaultTokenRepository = new DefaultTokenRepository();

    private AccelByteSDK sdk;
    private OAuth20 oauthWrapper;
    private BloomFilter bloomFilter;

    private LoadingCache<String, Map<String, RSAPublicKey>> jwksCache;
    private LoadingCache<String, OauthapiRevocationList> revocationListCache;

    public ServerAuthProvider() {
        final AccelByteConfig config = new AccelByteConfig(
                new OkhttpClient(),
                defaultTokenRepository,
                new DefaultConfigRepository());

        this.sdk = new AccelByteSDK(config);
        this.oauthWrapper = new OAuth20(sdk);
        this.bloomFilter = new BloomFilter();

        this.jwksCache = buildJWKSLoadingCache();
        this.revocationListCache = buildRevocationListLoadingCache();

        this.jwksCache.refresh(DEFAULT_CACHE_KEY);
        this.revocationListCache.refresh(DEFAULT_CACHE_KEY);
    }

    public boolean validate(String token, String permission, int action) {
        try {
            final SignedJWT signedJWT = SignedJWT.parse(token); // Client token only
            final String kid = signedJWT.getHeader().getKeyID();
            final RSAPublicKey pubKey = jwksCache.get(DEFAULT_CACHE_KEY).get(kid);

            if (pubKey == null) {
                throw new Exception("Matching JWK key not found");
            }

            final JWSVerifier verifier = new RSASSAVerifier(pubKey);

            if (!signedJWT.verify(verifier)) {
                throw new Exception("JWT signature verification failed");
            }

            final JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();

            if (jwtClaimsSet.getExpirationTime() == null
                    || jwtClaimsSet.getExpirationTime().before(new Date())) {
                throw new Exception("JWT expired");
            }

            final OauthapiRevocationList getRevocationListV3Result = revocationListCache.get(DEFAULT_CACHE_KEY);
            final BloomFilterJSON revokedTokens = getRevocationListV3Result.getRevokedTokens();
            final long[] bits = revokedTokens.getBits().stream()
                    .mapToLong(value -> Long.parseUnsignedLong(value.toString())).toArray();
            final int k = revokedTokens.getK();
            final int m = revokedTokens.getM();

            if (this.bloomFilter.mightContain(token, k, BitSet.valueOf(bits), m)) {
                // Revocation list is cached so there may be delay realizing token is revoked
                throw new Exception("Token revoked");
            }

            final List<Map<String, Object>> permissions = (List<Map<String, Object>>) jwtClaimsSet
                    .getClaim(CLAIM_PERMISSIONS);

            for (Map<String, Object> p : permissions) {
                if (permission.equals(p.get(PERMISSION_RESOURCE))
                        && Long.valueOf(action) == (Long) p.get(PERMISSION_ACTION)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Auth token validation error", e);
        }

        return false;
    }

    private LoadingCache<String, Map<String, RSAPublicKey>> buildJWKSLoadingCache() {
        final CacheLoader<String, Map<String, RSAPublicKey>> jwksLoader = new CacheLoader<String, Map<String, RSAPublicKey>>() {
            @Override
            public Map<String, RSAPublicKey> load(String key) {
                try {
                    final OauthcommonJWKSet getJwksV3Result = oauthWrapper.getJWKSV3(GetJWKSV3.builder().build());

                    final Decoder urlDecoder = Base64.getUrlDecoder();
                    final KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");

                    Map<String, RSAPublicKey> result = getJwksV3Result.getKeys().stream()
                            .collect(Collectors.toMap(OauthcommonJWKKey::getKid, jwkKey -> {
                                try {
                                    final BigInteger modulus = new BigInteger(1, urlDecoder.decode(jwkKey.getN()));
                                    final BigInteger exponent = new BigInteger(1, urlDecoder.decode(jwkKey.getE()));
                                    final RSAPublicKeySpec rsaPubKeySpec = new RSAPublicKeySpec(modulus, exponent);
                                    final RSAPublicKey pubKey = (RSAPublicKey) rsaKeyFactory
                                            .generatePublic(rsaPubKeySpec);
                                    return pubKey;
                                } catch (InvalidKeySpecException e) {
                                    log.warn("Failed to parse a JWKS pubkey", e);
                                    return null;
                                }
                            }));

                    log.info("Fetching JWKS done");

                    return result;
                } catch (Exception e) {
                    log.error("Fetching JWKS error", e);
                }

                return null;
            }
        };

        return CacheBuilder.newBuilder().refreshAfterWrite(300, TimeUnit.SECONDS).build(jwksLoader);
    }

    private LoadingCache<String, OauthapiRevocationList> buildRevocationListLoadingCache() {
        final CacheLoader<String, OauthapiRevocationList> revocationLoader = new CacheLoader<String, OauthapiRevocationList>() {
            @Override
            public OauthapiRevocationList load(String key) {
                try {
                    final OauthapiRevocationList getRevocationListV3Result = oauthWrapper
                            .getRevocationListV3(GetRevocationListV3.builder().build());

                    log.info("Fetching revocation done");

                    return getRevocationListV3Result;
                } catch (Exception e) {
                    log.error("Fetching revocation error", e);
                }

                return OauthapiRevocationList.builder().build();
            }
        };

        return CacheBuilder.newBuilder().refreshAfterWrite(300, TimeUnit.SECONDS).build(revocationLoader);
    }
}
