package org.sopt.buddys.global.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RedisConfig {

  @Bean
  public LettuceConnectionFactory redisConnectionFactory(RedisConnectionProperties properties) {
    LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
        LettuceClientConfiguration.builder()
            .commandTimeout(properties.commandTimeout())
            .shutdownTimeout(Duration.ZERO)
            .clientOptions(clientOptions(properties));

    if (properties.sslEnabled()) {
      clientBuilder.useSsl();
    }

    RedisConfiguration serverConfiguration = switch (properties.mode()) {
      case STANDALONE -> standaloneConfiguration(properties);
      case CLUSTER -> clusterConfiguration(properties);
    };

    return new LettuceConnectionFactory(serverConfiguration, clientBuilder.build());
  }

  private ClientOptions clientOptions(RedisConnectionProperties properties) {
    SocketOptions socketOptions = SocketOptions.builder()
        .connectTimeout(properties.connectTimeout())
        .keepAlive(true)
        .build();

    if (properties.mode() == RedisConnectionProperties.Mode.CLUSTER) {
      ClusterTopologyRefreshOptions topologyRefreshOptions =
          ClusterTopologyRefreshOptions.builder()
              .enableAllAdaptiveRefreshTriggers()
              .enablePeriodicRefresh(Duration.ofSeconds(30))
              .dynamicRefreshSources(true)
              .build();
      return ClusterClientOptions.builder()
          .socketOptions(socketOptions)
          .topologyRefreshOptions(topologyRefreshOptions)
          .autoReconnect(true)
          .validateClusterNodeMembership(false)
          .build();
    }

    return ClientOptions.builder()
        .socketOptions(socketOptions)
        .autoReconnect(true)
        .build();
  }

  private RedisStandaloneConfiguration standaloneConfiguration(RedisConnectionProperties properties) {
    RedisStandaloneConfiguration configuration =
        new RedisStandaloneConfiguration(properties.host(), properties.port());
    applyCredentials(configuration, properties);
    return configuration;
  }

  private RedisClusterConfiguration clusterConfiguration(RedisConnectionProperties properties) {
    RedisClusterConfiguration configuration = new RedisClusterConfiguration(
        List.of(properties.host() + ":" + properties.port())
    );
    applyCredentials(configuration, properties);
    return configuration;
  }

  private void applyCredentials(RedisConfiguration.WithAuthentication configuration,
                                RedisConnectionProperties properties) {
    if (properties.username() != null && !properties.username().isBlank()) {
      configuration.setUsername(properties.username());
    }
    if (properties.password() != null && !properties.password().isBlank()) {
      configuration.setPassword(RedisPassword.of(properties.password()));
    }
  }
}
