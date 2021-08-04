# Logback Logging Library
Internal configuration Project used for streaming logs over a syslog log relay.

This configuration guarantees a large log throughput. 
In the event of an error, logging is performed as a fallback to standard-out, which still provides a best-effort guarantee for replication to Splunk.