package org.dromara.web3.wallet.kms;

import com.aliyun.credentials.Client;
import com.aliyun.kms20160120.models.DecryptRequest;
import com.aliyun.kms20160120.models.EncryptRequest;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class AliyunKmsClientFactory {

    @Bean
    AliyunKmsClient aliyunKmsClient(
        @Value("${wallet.kms.aliyun.region-id:}") String regionId) {
        return new DefaultAliyunKmsClient(regionId);
    }

    private static final class DefaultAliyunKmsClient implements AliyunKmsClient {

        private final String regionId;
        private volatile com.aliyun.kms20160120.Client sdkClient;

        private DefaultAliyunKmsClient(String regionId) {
            this.regionId = regionId;
        }

        @Override
        public void validateConfiguration() {
            if (!StringUtils.hasText(regionId)) {
                throw new IllegalStateException("阿里云 KMS region-id 未配置");
            }
        }

        @Override
        public String encrypt(String keyId, String base64Plaintext) throws Exception {
            EncryptRequest request = new EncryptRequest()
                .setKeyId(keyId)
                .setPlaintext(base64Plaintext);
            return client().encryptWithOptions(request, new RuntimeOptions())
                .getBody().getCiphertextBlob();
        }

        @Override
        public String decrypt(String ciphertextBlob) throws Exception {
            DecryptRequest request = new DecryptRequest().setCiphertextBlob(ciphertextBlob);
            return client().decryptWithOptions(request, new RuntimeOptions())
                .getBody().getPlaintext();
        }

        private com.aliyun.kms20160120.Client client() throws Exception {
            com.aliyun.kms20160120.Client current = sdkClient;
            if (current != null) {
                return current;
            }
            synchronized (this) {
                if (sdkClient == null) {
                    validateConfiguration();
                    Client credentialClient = new Client();
                    Config config = new Config()
                        .setCredential(credentialClient)
                        .setEndpoint("kms." + regionId.trim() + ".aliyuncs.com");
                    sdkClient = new com.aliyun.kms20160120.Client(config);
                }
                return sdkClient;
            }
        }
    }
}
