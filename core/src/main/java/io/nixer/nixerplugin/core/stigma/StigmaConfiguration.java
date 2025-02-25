package io.nixer.nixerplugin.core.stigma;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nonnull;
import javax.sql.DataSource;

import io.nixer.nixerplugin.core.stigma.embed.EmbeddedStigmaService;
import io.nixer.nixerplugin.core.stigma.jdbc.JdbcDAO;
import io.nixer.nixerplugin.core.stigma.jdbc.JdbcDAOConfigurer;
import io.nixer.nixerplugin.core.stigma.storage.JdbcStigmaRepository;
import io.nixer.nixerplugin.core.stigma.storage.StigmaRepository;
import io.nixer.nixerplugin.core.stigma.token.EncryptedStigmaTokenProvider;
import io.nixer.nixerplugin.core.stigma.token.PlainStigmaTokenProvider;
import io.nixer.nixerplugin.core.stigma.token.StigmaTokenConstants;
import io.nixer.nixerplugin.core.stigma.token.StigmaTokenProvider;
import io.nixer.nixerplugin.core.stigma.token.crypto.DirectDecrypterFactory;
import io.nixer.nixerplugin.core.stigma.token.crypto.DirectEncrypterFactory;
import io.nixer.nixerplugin.core.stigma.token.crypto.KeysLoader;
import io.nixer.nixerplugin.core.stigma.token.validation.EncryptedJwtValidator;
import io.nixer.nixerplugin.core.stigma.token.validation.StigmaTokenPayloadValidator;
import io.nixer.nixerplugin.core.stigma.token.validation.StigmaTokenValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(value = {StigmaProperties.class})
public class StigmaConfiguration {

    @Bean
    public JdbcDAO jdbcDAO(DataSource dataSource) {
        final JdbcDAO jdbcDAO = new JdbcDAO();
        jdbcDAO.setDataSource(dataSource);
        return jdbcDAO;
    }

    @Bean
    public JdbcDAOConfigurer JdbcDAOConfigurer(DataSource dataSource) {
        return new JdbcDAOConfigurer(dataSource);
    }

    @Bean
    public StigmaUtils stigmaUtils() {
        return new StigmaUtils();
    }

    @Bean
    public JdbcStigmaRepository jdbcStigmaRepository(JdbcDAO jdbcDAO) {
        return new JdbcStigmaRepository(jdbcDAO);
    }

    @Bean
    public StigmaTokenValidator buildStigmaTokenValidator(@Nonnull final EncryptedJwtValidator encryptedJwtValidator) {

        return new StigmaTokenValidator(encryptedJwtValidator);
    }

    @Bean
    public KeysLoader buildKeysLoader(final StigmaProperties config) throws FileNotFoundException {

        final File encryptionFile = ResourceUtils.getFile(config.getEncryptionKeyFile());
        final File decryptionFile = ResourceUtils.getFile(config.getDecryptionKeyFile());

        return KeysLoader.load(encryptionFile, decryptionFile);
    }

    @Bean
    public EncryptedJwtValidator buildEncryptedJwtValidator(final KeysLoader keysLoader,
                                                            final StigmaTokenPayloadValidator stigmaTokenPayloadValidator) {
        return new EncryptedJwtValidator(
                DirectDecrypterFactory.withKeysFrom(keysLoader),
                stigmaTokenPayloadValidator
        );
    }

    @Bean
    public StigmaTokenPayloadValidator buildStigmaTokenPayloadValidator(final StigmaProperties stigmaProperties) {
        final Duration tokenLifetime = !StringUtils.isEmpty(stigmaProperties.getTokenLifetime())
                ? Duration.parse(stigmaProperties.getTokenLifetime())
                : StigmaTokenConstants.DEFAULT_TOKEN_LIFETIME;

        return new StigmaTokenPayloadValidator(
                Instant::now,
                tokenLifetime
        );
    }

    @Bean
    public EncryptedStigmaTokenProvider buildEncryptedStigmaTokenProvider(final KeysLoader keysLoader) {

        return new EncryptedStigmaTokenProvider(
                new PlainStigmaTokenProvider(Instant::now),
                DirectEncrypterFactory.withKeysFrom(keysLoader)
        );
    }

    @Bean
    public EmbeddedStigmaService stigmaService(StigmaRepository stigmaRepository, StigmaTokenProvider tokenProvider, StigmaTokenValidator tokenValidator) {
        return new EmbeddedStigmaService(stigmaRepository, tokenProvider, tokenValidator);
    }
}
