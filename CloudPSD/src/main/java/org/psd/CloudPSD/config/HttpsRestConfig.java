package org.psd.CloudPSD.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Configuration
@Slf4j
public class HttpsRestConfig implements RestTemplateCustomizer {
    @Value("${trust-store}")
    private Resource trustStore;
    @Value("${trust-store-password}")
    private String trustStorePassword;
    String protocol = "TLSv1.2";



    @Override
    public void customize(RestTemplate restTemplate) {

        final SSLContext sslContext;
        try {
            sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(new URL(trustStore.getURL().toString()),
                            trustStorePassword.toCharArray())
                    .setProtocol(protocol)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to setup client SSL context", e
            );
        } finally {
            // it's good security practice to zero out passwords,
            // which is why they're char[]
            Arrays.fill(trustStorePassword.toCharArray(), (char) 0);
        }

        final HttpClient httpClient = HttpClientBuilder.create()
                .setSSLContext(sslContext).setSSLHostnameVerifier((s, sslSession) -> true).setSSLHostnameVerifier((s, sslSession) -> true)
                .build();

        final ClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        log.info("Registered SSL truststore {} for client requests",
                trustStore);
        restTemplate.setRequestFactory(requestFactory);
    }

    //    @Bean
//    public RestTemplate generateRestCustomTemplate() {
//        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
//        httpRequestFactory.setConnectionRequestTimeout(2000);
//        httpRequestFactory.setConnectTimeout(2000);
//        httpRequestFactory.setReadTimeout(2000);
//        RestTemplate restTemplate = new RestTemplate(httpRequestFactory);
//        customize(restTemplate);
//        return restTemplate;
//    }
    @Bean
    public RestTemplate generateRestTemplateCustom() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                new SSLContextBuilder()
                        .loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(), NoopHostnameVerifier.INSTANCE);

        HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(
                socketFactory).build();
        HttpComponentsClientHttpRequestFactory  requestFactory = new HttpComponentsClientHttpRequestFactory(
                httpClient);
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(2000);
        requestFactory.setConnectionRequestTimeout(2000);
        return new RestTemplate(requestFactory);
    }
}

