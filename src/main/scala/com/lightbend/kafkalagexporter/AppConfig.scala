/*
 * Copyright (C) 2019 Lightbend Inc. <http://www.lightbend.com>
 */

package com.lightbend.kafkalagexporter

import com.typesafe.config.Config

import scala.collection.JavaConverters._
import scala.compat.java8.DurationConverters._
import scala.concurrent.duration.FiniteDuration

object AppConfig {
  def apply(config: Config): AppConfig = {
    val c = config.getConfig("kafka-lag-exporter")
    val pollInterval = c.getDuration("poll-interval").toScala
    val lookupTableSize = c.getInt("lookup-table-size")
    val port = c.getInt("port")
    val clientGroupId = c.getString("client-group-id")
    val kafkaClientTimeout = c.getDuration("kafka-client-timeout").toScala
    val clusters = c.getConfigList("clusters").asScala.toList.map { clusterConfig =>
      KafkaCluster(
        clusterConfig.getString("name"),
        clusterConfig.getString("bootstrap-brokers"),
        if (clusterConfig.hasPath("security-protocol")) clusterConfig.getString("security-protocol") else "PLAINTEXT",
        if (clusterConfig.hasPath("sasl-mechanism")) clusterConfig.getString("sasl-mechanism") else "",
        if (clusterConfig.hasPath("sasl-jaas-config")) clusterConfig.getString("sasl-jaas-config") else ""
      )
    }
    val strimziWatcher = c.getString("watchers.strimzi").toBoolean
    AppConfig(pollInterval, lookupTableSize, port, clientGroupId, kafkaClientTimeout, clusters, strimziWatcher)
  }
}

final case class KafkaCluster(name: String, bootstrapBrokers: String, securityProtocol: String = "PLAINTEXT",
                              saslMechanism: String = "", saslJaasConfig: String = "")
final case class AppConfig(pollInterval: FiniteDuration, lookupTableSize: Int, port: Int, clientGroupId: String,
                           clientTimeout: FiniteDuration, clusters: List[KafkaCluster], strimziWatcher: Boolean) {
  override def toString(): String = {
    val clusterString =
      if (clusters.isEmpty)
        "  (none)"
      else
        clusters.map { cluster =>
          s"""
             |  Cluster name: ${cluster.name}
             |  Cluster Kafka bootstrap brokers: ${cluster.bootstrapBrokers}
             |  Cluster security protocol: ${cluster.securityProtocol}
             |  Cluster SASL mechanism: ${cluster.saslMechanism}
           """.stripMargin
        }.mkString("\n")
    s"""
       |Poll interval: $pollInterval
       |Lookup table size: $lookupTableSize
       |Prometheus metrics endpoint port: $port
       |Admin client consumer group id: $clientGroupId
       |Kafka client timeout: $clientTimeout
       |Statically defined Clusters:
       |$clusterString
       |Watchers:
       |  Strimzi: $strimziWatcher
     """.stripMargin
  }
}

